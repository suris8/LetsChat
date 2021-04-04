package edu.neu.madcourse.letschat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FloatingActionButton fabAdd;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                // means user is not logged in - go back to login
                if (firebaseAuth.getCurrentUser() == null) {
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                    // if user is logged in go to main
                } else {
                    goMain();
                }
            }
        };
    }

    private void goMain() {
        // get logged in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();

        fabAdd = findViewById(R.id.fabAdd);
        // set on click listener on fab
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.setTitle("Enter User UID");
                dialog.setContentView(R.layout.dialog_add);
                dialog.show();


                final EditText editId = dialog.findViewById(R.id.editId);
                Button btnOk = dialog.findViewById(R.id.btnOk);

                // set on click listener  on ok button
                btnOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // get the entered userId
                        String idUser = editId.getText().toString();
                        if (TextUtils.isEmpty(idUser)){
                            editId.setError("required");
                        } else {
                            // get the user doc where the id is equal to the entered id
                            db.collection("user").whereEqualTo("id", idUser).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    // if not found
                                    if(queryDocumentSnapshots.isEmpty()) {
                                        editId.setError("ID is not found");
                                    } else {
                                        // loop through all of the documents in user
                                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                                            String uidFriend = documentSnapshot.getId();
                                            // if the id entered matched the current user's id
                                            if(uid.equals(uidFriend)){
                                                editId.setError("wrong Id");
                                            } else {
                                                Log.i("hello", "checking user");
                                                dialog.cancel();
                                                // check if friend exists
                                                checkFriendExist(uidFriend);
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
    }

    private void checkFriendExist(final String uidFriend) {
        db.collection("user").document(uid).collection("friend").document().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists()){
                        String idChatRoom = documentSnapshot.get("idChatRoom", String.class);
                        goChatRoom(idChatRoom, uidFriend);
                    } else {
                        Log.i("hello", "creating chat room");
                        createNewChatRoom(uidFriend);
                    }
                }
            }
        });
    }

    private void createNewChatRoom(final String uidFriend) {
        HashMap<String,Object> dataChatRoom = new HashMap<>();
        dataChatRoom.put("dateAdded", FieldValue.serverTimestamp());
        // create chatroom collection doc with both uids
        db.collection("chatroom").document(uid+uidFriend).set(dataChatRoom).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // write user data
                HashMap<String, Object> dataFriend = new HashMap<>();
                dataFriend.put("idChatRoom",uid+uidFriend);
                Log.i("hello", "added user");
                db.collection("user").document(uid).collection("friend").document(uidFriend).set(dataFriend).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // write on user's friend data
                        HashMap<String, Object> dataUserFriend = new HashMap<>();
                        dataFriend.put("idChatRoom",uid+uidFriend);
                        // add a friend collection to user and add the chatroom id to it
                        db.collection("user").document(uidFriend).collection("friend").document(uid).set(dataUserFriend).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                goChatRoom(uid+uidFriend,uidFriend);
                            }
                        });
                    }
                });
            }
        });
    }

    private void goChatRoom(String idChatRoom, String uidFriend) {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        } else if (id == R.id.action_profile){
            Intent i = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    protected void onStop(){
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
    }
}