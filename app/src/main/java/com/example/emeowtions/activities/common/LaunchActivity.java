package com.example.emeowtions.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LaunchActivity extends AppCompatActivity {

    private static final String TAG = "LaunchActivity";

    private FirebaseFirestore db;
    private CollectionReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Handle splash screen transition
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if the user is authenticated
        FirebaseAuthUtils firebaseAuthUtils = new FirebaseAuthUtils();

        if (!firebaseAuthUtils.isSignedIn()) {
            // Redirect to LoginActivity if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            usersRef.document(firebaseAuthUtils.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        String role = user.getRole();

                        // Redirect to appropriate MainActivity based on role
                        Intent redirectRoleIntent;

                        if (role.equals(Role.SUPER_ADMIN.getTitle()) || role.equals(Role.ADMIN.getTitle())) {
                            redirectRoleIntent = new Intent(this, AdminMainActivity.class);
                        } else if (role.equals(Role.VETERINARY_STAFF.getTitle())) {
                            redirectRoleIntent = new Intent(this, VetMainActivity.class);
                        } else if (role.equals(Role.VETERINARIAN.getTitle())) {
                            redirectRoleIntent = new Intent(this, VetMainActivity.class);
                        } else if (role.equals(Role.USER.getTitle())) {
                            redirectRoleIntent = new Intent(this, UserMainActivity.class);
                        } else {
                            // Handle unknown roles or errors
                            Toast.makeText(this, "Unable to authenticate your account.", Toast.LENGTH_SHORT).show();
                            redirectRoleIntent = new Intent(this, LoginActivity.class);
                        }

                        finish();
                        startActivity(redirectRoleIntent);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "signInWithEmail:success but unable to retrieve User document");
                        Toast.makeText(this, "Login failed.", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}