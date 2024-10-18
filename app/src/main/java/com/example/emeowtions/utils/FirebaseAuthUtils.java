package com.example.emeowtions.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.emeowtions.activities.common.LoginActivity;
import com.example.emeowtions.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseAuthUtils {

    private static final String TAG = "FirebaseAuthUtils";
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    public FirebaseAuthUtils() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
    }

    // Gets user ID
    public String getUid() {
        return auth.getUid();
    }

    // Gets email
    public String getFirebaseEmail() {
        return auth.getCurrentUser().getEmail();
    }

    // Gets FirebaseUser
    public FirebaseUser getFirebaseUser() {
        return auth.getCurrentUser();
    }

    // Checks if user is verified in FirebaseAuth
    public boolean isVerifiedInFirebaseAuth() {
        return auth.getCurrentUser().isEmailVerified();
    }

    // Checks if user is signed in
    public boolean isSignedIn() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null;
    }

    // Redirects user to Login screen if not signed in
    public void checkSignedIn(Context context) {
        if (isSignedIn()) {
            Log.d(TAG, "checkSignedIn: user is signed in");
        } else {
            Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            context.startActivity(new Intent(context, LoginActivity.class));
        }
    }

    // Refresh verification status
    public void refreshVerification(Context context) {
        /* Account is not verified if:
           - oldEmail before reload() is different from newEmail after reload()
           - Firestore User email is different from FirebaseAuth email
           This is because FirebaseUser email is only changed AFTER the user clicks on the verification link to update email,
            while Firestore User email is updated immediately after profile update.
           Additionally, after verification, the app still requires a reload() to update the FirebaseUser's email.
         */
        String oldEmail = getFirebaseEmail();

        auth.getCurrentUser()
            .reload()
            .addOnSuccessListener(unused -> {
                String newEmail = getFirebaseEmail();

                usersRef.document(getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            User user = documentSnapshot.toObject(User.class);

                            // Update verification status
                            usersRef.document(getUid())
                                    .update("verified", user.getEmail().equals(newEmail))
                                    .addOnSuccessListener(unused1 -> {
                                        Log.d(TAG, "Successfully refreshed verification status");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Failed to refresh verification status");
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Failed to retrieve User documebt");
                        });
            })
            .addOnFailureListener(e -> {
                Log.w(TAG, "Unable to reload FirebaseUser");
            });
    }

    // Updates email
    public void updateEmail(Context context, String email) {
        FirebaseUser user = getFirebaseUser();

        user.verifyBeforeUpdateEmail(email)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Verification email sent to " + email);
                    Toast.makeText(context, "Verification email sent to " + email + ". Please verify your new email address.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Verification email failed to send", e);
                });
    }

    // Signs out
    public void signOut() {
        auth.signOut();
    }
}
