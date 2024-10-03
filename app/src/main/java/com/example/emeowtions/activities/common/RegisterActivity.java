package com.example.emeowtions.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.databinding.ActivityRegisterBinding;
import com.example.emeowtions.models.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    private ActivityRegisterBinding registerBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Get ViewBinding and set content view
        registerBinding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(registerBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region onClick listeners
        // txtSignUp - Clicked: Redirect to RegisterActivity
        registerBinding.btnSignup.setOnClickListener(view -> {
            // Get inputs
            String email = registerBinding.edtEmail.getText().toString();
            String password = registerBinding.edtPassword.getText().toString();
            String confirmPassword = registerBinding.edtConfirmPassword.getText().toString();

            // Validate inputs
            boolean isValid = validateInputs(email, password, confirmPassword);

            // Create new user account if inputs are valid
            if (isValid) {
                createUserAccount(email, password);
            }
        });

        registerBinding.txtLogin.setOnClickListener(view -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));
        //endregion

        //region Other listeners
        registerBinding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerBinding.txtfieldEmail.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        registerBinding.edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerBinding.txtfieldPassword.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        registerBinding.edtConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                registerBinding.txtfieldConfirmPassword.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion
    }

    private boolean validateInputs(String email, String password, String confirmPassword) {
        // Reset errors
        registerBinding.txtfieldEmail.setErrorEnabled(false);
        registerBinding.txtfieldPassword.setErrorEnabled(false);
        registerBinding.txtfieldConfirmPassword.setErrorEnabled(false);

        // Validate email
        // Empty input
        if (email.isEmpty()) {
            registerBinding.txtfieldEmail.setError("Email is required");
            registerBinding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            registerBinding.txtfieldEmail.setError("Enter a valid email address");
            registerBinding.txtfieldEmail.requestFocus();
            return false;
        }
        // Unique email
        usersRef.whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            // Email already exists
                            registerBinding.txtfieldEmail.setError("Email is already in use");
                        }
                    } else {
                        Toast.makeText(this, "Error checking email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Validate password
        // Empty input
        if (password.isEmpty()) {
            registerBinding.txtfieldPassword.setError("Password is required");
            registerBinding.txtfieldPassword.requestFocus();
            return false;
        }
        /* Password strength
           - At least 8 characters
           - Contains at least 1 letter, 1 number, and 1 special character
        */
        String passwordRegex = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
        if (!password.matches(passwordRegex)) {
            registerBinding.txtfieldPassword.setError("Password should be at least 8 characters, containing at least 1 letter, 1 number, and 1 special character");
            registerBinding.txtfieldPassword.requestFocus();
            return false;
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            registerBinding.txtfieldConfirmPassword.setError("Confirm password is required");
            registerBinding.txtfieldConfirmPassword.requestFocus();
            return false;
        }
        // Check if password and confirm password match
        if (!password.equals(confirmPassword)) {
            registerBinding.txtfieldConfirmPassword.setError("Passwords do not match");
            registerBinding.txtfieldConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void createUserAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign up success
                        Log.d(TAG, "createUserWithEmail:success");

                        // Check if FirebaseUser was successfully created
                        FirebaseUser currentUser = mAuth.getCurrentUser();

                        if (currentUser == null) {
                            Log.w(TAG, "createUserWithEmail:success but FirebaseUser returned null", task.getException());
                            Toast.makeText(this, "Failed to create account. Please try again later.", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                // Add to users collection
                                User newUser = new User(
                                        currentUser.getUid(),
                                        email.split("@")[0],
                                        email,
                                        null,
                                        "user",
                                        null,
                                        null,
                                        Timestamp.now(),
                                        Timestamp.now()
                                );
                                usersRef.add(newUser);

                                // Redirect to user home screen
                                startActivity(new Intent(this, UserMainActivity.class));
                            } catch (Exception e) {
                                Log.e(TAG, "createUserWithEmail:success but failed to create user document", task.getException());
                            }
                        }
                    } else {
                        // Sign up failed
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(this, "Sign-up failed. " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}