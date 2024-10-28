package com.example.emeowtions.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.AnalysisFeedbackAdapter;
import com.example.emeowtions.adapters.BodyLanguageAdapter;
import com.example.emeowtions.adapters.RecommendedBehaviourStrategyAdapter;
import com.example.emeowtions.databinding.ActivityViewAnalysisFeedbackBinding;
import com.example.emeowtions.models.Analysis;
import com.example.emeowtions.models.BodyLanguage;
import com.example.emeowtions.models.Recommendation;
import com.example.emeowtions.models.RecommendedBehaviourStrategy;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class ViewAnalysisFeedbackActivity extends AppCompatActivity {

    private static final String TAG = "ViewAnalysisFeedbackActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference analysesRef;
    private CollectionReference recommendationsRef;
    private CollectionReference analysisFeedbackRef;

    // Layout variables
    private ActivityViewAnalysisFeedbackBinding binding;

    // Private variables
    private String analysisId;
    private String analysisFeedbackId;
    private String userDisplayName;
    private String userEmail;
    private Timestamp createdAt;
    private float rating;
    private String description;
    private boolean read;

    private BodyLanguageAdapter blAdapter;
    private RecommendedBehaviourStrategyAdapter rbsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        analysisId = passedIntent.getStringExtra(AnalysisFeedbackAdapter.KEY_ANALYSIS_ID);
        analysisFeedbackId = passedIntent.getStringExtra(AnalysisFeedbackAdapter.KEY_ANALYSIS_FEEDBACK_ID);
        userDisplayName = passedIntent.getStringExtra(AnalysisFeedbackAdapter.KEY_USER_DISPLAY_NAME);
        userEmail = passedIntent.getStringExtra(AnalysisFeedbackAdapter.KEY_USER_EMAIL);
        createdAt = passedIntent.getParcelableExtra(AnalysisFeedbackAdapter.KEY_CREATED_AT);
        rating = passedIntent.getFloatExtra(AnalysisFeedbackAdapter.KEY_RATING, 0);
        description = passedIntent.getStringExtra(AnalysisFeedbackAdapter.KEY_DESCRIPTION);
        read = passedIntent.getBooleanExtra(AnalysisFeedbackAdapter.KEY_READ, false);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        analysesRef = db.collection("analyses");
        analysisFeedbackRef = db.collection("analysisFeedback");
        recommendationsRef = db.collection("recommendations");

        // Get ViewBinding and set content view
        binding = ActivityViewAnalysisFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUI();
        loadData();
        bindListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    private void setupUI() {
        toggleFab(binding.fabSetRead, !read);
    }

    private void loadData() {
        analysesRef.document(analysisId)
                .addSnapshotListener((value, error) -> {
                   // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to retrieve Analysis data", error);
                        return;
                    }
                   // Success
                    if (value != null && value.exists()) {
                        Analysis analysis = value.toObject(Analysis.class);

                        // Summary
                        binding.txtEmotion.setText(
                                String.format("%s %s",
                                        analysis.getEmotion(),
                                        analysis.getProbability() == 0 ? "" : String.format("(%s%%)", (int) (analysis.getProbability() * 100))
                                )
                        );
                        binding.txtCatName.setText(analysis.getCatName() == null ? "Unspecified" : analysis.getCatName());
                        Glide.with(getApplicationContext())
                                .load(analysis.getImageUrl())
                                .into(binding.imgDetectedCat);

                        // Body Languages
                        ArrayList<BodyLanguage> bodyLanguageList = new ArrayList<>(analysis.getBodyLanguages());
                        if (bodyLanguageList.isEmpty()) {
                            // No results
                            binding.recyclerviewBodyLanguage.setVisibility(View.GONE);
                            binding.layoutNoBodyLanguage.setVisibility(View.VISIBLE);
                        } else {
                            // Show results
                            blAdapter = new BodyLanguageAdapter(bodyLanguageList, this);
                            binding.recyclerviewBodyLanguage.setAdapter(blAdapter);
                            binding.txtBodyLanguageCount.setText(String.format("%s", blAdapter.getItemCount()));
                            binding.layoutNoBodyLanguage.setVisibility(View.GONE);
                        }

                        // Recommendations
                        recommendationsRef.whereEqualTo("analysisId", analysisId)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.getDocuments().isEmpty()) {
                                        Recommendation recommendation = queryDocumentSnapshots.getDocuments().get(0).toObject(Recommendation.class);
                                        ArrayList<RecommendedBehaviourStrategy> stratList =
                                                new ArrayList<>(recommendation.getStrategies());

                                        if (stratList.isEmpty()) {
                                            // No results
                                            binding.recyclerviewRecommendations.setVisibility(View.GONE);
                                            binding.layoutNoRecommendations.setVisibility(View.VISIBLE);
                                        } else {
                                            rbsAdapter = new RecommendedBehaviourStrategyAdapter(stratList, this, false);
                                            binding.recyclerviewRecommendations.setAdapter(rbsAdapter);
                                            binding.txtRecommendationsCount.setText(String.format("%s", rbsAdapter.getItemCount()));
                                            binding.layoutNoRecommendations.setVisibility(View.GONE);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "loadData: Failed to retrieve Recommendation data", e);
                                });
                    }
                });
    }

    private void bindListeners() {
        bindNavigationListeners();
        bindOnClickListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarViewAnalysisFeedback.setNavigationOnClickListener(view -> finish());
    }

    private void bindOnClickListeners() {
        binding.btnBackBodyLanguage.setOnClickListener(view -> finish());

        binding.btnBackRecommendations.setOnClickListener(view-> finish());

        binding.fabDetails.setOnClickListener(view -> {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());

            // Open details dialog
            View feedbackDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_analysis_feedback, null);
            TextView txtDisplayName = feedbackDialogLayout.findViewById(R.id.txt_display_name);
            TextView txtEmail = feedbackDialogLayout.findViewById(R.id.txt_email);
            TextView txtSubmitted = feedbackDialogLayout.findViewById(R.id.txt_created_date);
            TextView txtRating = feedbackDialogLayout.findViewById(R.id.txt_rating);
            TextView txtDescription = feedbackDialogLayout.findViewById(R.id.txt_description);

            // Set fields
            txtDisplayName.setText(userDisplayName);
            txtEmail.setText(userEmail);
            txtSubmitted.setText(String.format("%s", sdf.format(createdAt.toDate())));
            txtRating.setText(String.format("%s", rating));
            txtDescription.setText(description);

            MaterialAlertDialogBuilder feedbackDialogBuilder =
                    new MaterialAlertDialogBuilder(this)
                            .setView(feedbackDialogLayout)
                            .setTitle(R.string.feedback_details)
                            .setPositiveButton(R.string.close, (dialogInterface, i) -> {
                                // Unused
                            });

            AlertDialog feedbackDialog = feedbackDialogBuilder.create();
            feedbackDialog.show();
        });

        binding.fabSetRead.setOnClickListener(view -> {
            // Update "read" to true
            analysisFeedbackRef.document(analysisFeedbackId)
                    .update("read", true, "updatedAt", Timestamp.now())
                    .addOnSuccessListener(unused -> {
                        toggleFab(binding.fabSetRead, false);
                        Toast.makeText(this, "Feedback set to reviewed.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "onBindViewHolder: Failed to update AnalysisFeedback read status", e);
                        Toast.makeText(this, "Unable to set feedback to reviewed..", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void toggleFab(FloatingActionButton fab, boolean enabled) {
        fab.setEnabled(enabled);

        if (enabled) {
            fab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_400));
            fab.setImageTintList(ContextCompat.getColorStateList(this, R.color.white));
        } else {
            fab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray_100));
            fab.setImageTintList(ContextCompat.getColorStateList(this, R.color.gray_400));
        }
    }
}