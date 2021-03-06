package com.iwsbrazil.futurefaces.babelfish.util;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.iwsbrazil.futurefaces.babelfish.LoginActivity;
import com.iwsbrazil.futurefaces.babelfish.MainActivity;
import com.iwsbrazil.futurefaces.babelfish.model.BabelMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by administrador on 30/10/16.
 */
public class FirebaseManager {
    private static FirebaseManager firebaseManager;
    private static String room = "TestRoom";
    private DatabaseReference databaseReference;
    private List<String> friends = new ArrayList<>();
    private ChildEventListener chatListener;
    private ChildEventListener friendsListener;


    private FirebaseManager() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    public static FirebaseManager getInstance() {
        if (firebaseManager == null) {
            firebaseManager = new FirebaseManager();
        }
        return firebaseManager;
    }

    public List<String> getFriends() {
        return friends;
    }

    private DatabaseReference getDatabaseReference() {
        return databaseReference;
    }

    public void doLogin(LoginActivity activity) {
        FirebaseAuth.getInstance().signInWithEmailAndPassword("teste@teste.com", "123456")
                .addOnCompleteListener(activity, activity);
    }

    public void removeUser(String userName) {
        Log.d("remove", userName);
        getDatabaseReference().child(room).child("friends").child(userName).removeValue();
        getDatabaseReference().removeEventListener(chatListener);
        getDatabaseReference().removeEventListener(friendsListener);
    }

    public void addUser(String userName, final MainActivity mainActivity, final String language) {
        getDatabaseReference().child(room).child("friends").child(userName).setValue("name");

        chatListener = getDatabaseReference().child(room).child("chat").child(userName).addChildEventListener(
                 new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        BabelMessage message = dataSnapshot.getValue(BabelMessage.class);
                        TextToSpeechHelper.getInstance(mainActivity).textToSpeechPlay(message, language);
                        dataSnapshot.getRef().removeValue();
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );

        friendsListener = getDatabaseReference().child(room).child("friends").addChildEventListener(
                new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        friends.add(dataSnapshot.getKey());
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );

    }

    public void sendBabelMessage(String friendName, BabelMessage message) {
        getDatabaseReference().child(room).child("chat").child(friendName).push().setValue(message);
    }
}
