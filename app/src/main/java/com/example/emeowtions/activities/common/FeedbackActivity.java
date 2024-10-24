package com.example.emeowtions.activities.common;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityFeedbackBinding;
import com.example.emeowtions.models.Feedback;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackActivity";

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference feedbackRef;

    private ActivityFeedbackBinding feedbackBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        feedbackRef = db.collection("feedback");

        // Get ViewBinding and set content view
        feedbackBinding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(feedbackBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region onClick listeners
        // appBarFeedback back button: return to home screen
        feedbackBinding.appBarFeedback.setNavigationOnClickListener(view -> finish());

        // btnSubmitFeedback: create new feedback
        feedbackBinding.btnSubmitFeedback.setOnClickListener(view -> {
            float rating = feedbackBinding.ratingBar.getRating();
            String description = feedbackBinding.edtDescription.getText().toString();

            // Validate input
            if (rating == 0) {
                // No rating
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_LONG).show();
            } else if (description.isBlank()) {
                // No description
                feedbackBinding.txtfieldDescription.setError("Description is required");
            } else {
                // Valid
                firebaseAuthUtils.checkSignedIn(this);

                // Add new feedback
                usersRef.document(firebaseAuthUtils.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            User user = documentSnapshot.toObject(User.class);
                            // Add new feedback
                            Feedback newFeedback = new Feedback(
                                    documentSnapshot.getId(),
                                    user.getDisplayName(),
                                    user.getProfilePicture(),
                                    rating,
                                    description,
                                    false,
                                    Timestamp.now(),
                                    Timestamp.now()
                            );
                            feedbackRef.add(newFeedback)
                                    .addOnSuccessListener(documentReference -> {
                                        // Clear inputs
                                        feedbackBinding.ratingBar.setRating(0);
                                        feedbackBinding.edtDescription.getText().clear();
                                        Toast.makeText(this, "Thank you, we have received your feedback.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Error adding Feedback document", e);
                                        Toast.makeText(this, "Unable to submit feedback, please try again later.", Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Error getting User document", e);
                        });
            }
        });
        //endregion

        //region Other listeners
        feedbackBinding.edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                feedbackBinding.txtfieldDescription.setErrorEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }
}