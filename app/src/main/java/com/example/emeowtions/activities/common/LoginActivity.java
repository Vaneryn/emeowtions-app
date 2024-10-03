package com.example.emeowtions.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.databinding.ActivityLoginBinding;
import com.example.emeowtions.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    private ActivityLoginBinding loginBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Get ViewBinding and set content view
        loginBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(loginBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // onClick listeners
        // btnLogin: redirect to role-based main activity
        loginBinding.btnLogin.setOnClickListener(view -> {
            // Get inputs
            String email = loginBinding.edtEmail.getText().toString();
            String password = loginBinding.edtPassword.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(email, password);

            // Log the user in if inputs are valid
            if (isValid) {
                login(email, password);
            }
        });

        // txtSignUp: redirect to RegisterActivity
        loginBinding.txtSignup.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private boolean validateInputs(String email, String password) {
        // Reset errors
        loginBinding.txtfieldEmail.setErrorEnabled(false);
        loginBinding.txtfieldPassword.setErrorEnabled(false);

        // Validate email
        // Empty input
        if (email.isEmpty()) {
            loginBinding.txtfieldEmail.setError("Email is required");
            loginBinding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginBinding.txtfieldEmail.setError("Enter a valid email address");
            loginBinding.txtfieldEmail.requestFocus();
            return false;
        }

        // Validate password
        // Empty input
        if (password.isEmpty()) {
            loginBinding.txtfieldPassword.setError("Password is required");
            loginBinding.txtfieldPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, redirect to role-based home screen
                        Log.d(TAG, "signInWithEmail:success");
                        redirectToHome();
                    } else {
                        // Sign in failed
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            // Email not registered
                            Toast.makeText(this, "Email or password incorrect.", Toast.LENGTH_SHORT).show();
                        }  else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            // Incorrect password
                            Toast.makeText(this, "Email or password incorrect.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Some other error occurred
                            Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void redirectToHome() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.w(TAG, "signInWithEmail:success but FirebaseUser returned null");
        } else {
            usersRef.whereEqualTo("authId", currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                // Get user's role
                                User user = doc.toObject(User.class);
                                String role = user.getRole();

                                // Redirect to appropriate MainActivity based on role
                                Intent redirectRoleIntent;
                                switch (role) {
                                    case "user":
                                        redirectRoleIntent = new Intent(this, UserMainActivity.class);
                                        break;
                                    case "veterinary_staff":
                                        redirectRoleIntent = new Intent(this, VetMainActivity.class);
                                        break;
                                    case "admin":
                                        redirectRoleIntent = new Intent(this, AdminMainActivity.class);
                                        break;
                                    default:
                                        // Handle unknown roles or errors
                                        redirectRoleIntent = new Intent(this, LoginActivity.class);
                                        break;
                                }
                                Toast.makeText(this, "Successfully logged in.", Toast.LENGTH_SHORT).show();
                                startActivity(redirectRoleIntent);
                            }
                        } else {
                            Toast.makeText(this, "Error checking role: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}