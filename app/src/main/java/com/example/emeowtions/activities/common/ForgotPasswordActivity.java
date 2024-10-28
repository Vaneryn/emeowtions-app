package com.example.emeowtions.activities.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.emeowtions.databinding.ActivityForgotPasswordBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    // Firebase variables
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    // Layout variables
    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Get ViewBinding and set content view
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge to edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindListeners();
    }

    private void bindListeners() {
        bindOnClickListeners();
        bindTextChangeListeners();
    }

    private void bindOnClickListeners() {
        binding.btnSendPasswordResetEmail.setOnClickListener(view -> {
            // Get input
            String email = binding.edtEmail.getText().toString().trim();

            // Validate input
            boolean isValid = validateInput(email);

            if (isValid) {
                checkEmailRegistered(email);
            }
        });
    }

    private void bindTextChangeListeners() {
        binding.edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.txtfieldEmail.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private boolean validateInput(String email) {
        // Validate email
        // Empty input
        if (email.isBlank()) {
            binding.txtfieldEmail.setError(getString(R.string.email_required_error));
            binding.txtfieldEmail.requestFocus();
            return false;
        }
        // Email format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.txtfieldEmail.setError(getString(R.string.email_format_invalid));
            binding.txtfieldEmail.requestFocus();
            return false;
        }

        return true;
    }

    private void checkEmailRegistered(String email) {
        // Check if email is registered
        usersRef.whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Not registered
                        binding.txtfieldEmail.setError(getString(R.string.email_not_registered_error));
                        binding.txtfieldEmail.requestFocus();
                    } else {
                        // Registered
                        sendPasswordResetEmail(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "checkEmailRegistered: Failed to retrieve User email data", e);
                    Toast.makeText(this, "Unable to send password reset email, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendPasswordResetEmail(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        binding.edtEmail.getText().clear();
                        Toast.makeText(this, String.format("Password reset email has been sent to %s.", email), Toast.LENGTH_LONG).show();
                    } else {
                        Log.w(TAG, "sendPasswordResetEmail: Failed to send password reset email", task.getException());
                        Toast.makeText(this, "Unable to send password reset email, please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}