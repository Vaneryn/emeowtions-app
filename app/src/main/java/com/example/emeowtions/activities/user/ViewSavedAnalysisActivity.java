package com.example.emeowtions.activities.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.AnalysisAdapter;
import com.example.emeowtions.adapters.BodyLanguageAdapter;
import com.example.emeowtions.adapters.RecommendedBehaviourStrategyAdapter;
import com.example.emeowtions.databinding.ActivityViewSavedAnalysisBinding;
import com.example.emeowtions.models.Analysis;
import com.example.emeowtions.models.AnalysisFeedback;
import com.example.emeowtions.models.BodyLanguage;
import com.example.emeowtions.models.Recommendation;
import com.example.emeowtions.models.RecommendedBehaviourStrategy;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewSavedAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "ViewSavedAnalysisActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference analysesRef;
    private CollectionReference recommendationsRef;
    private CollectionReference analysisFeedbackRef;

    // Layout variables
    private ActivityViewSavedAnalysisBinding binding;
    private MaterialAlertDialogBuilder deleteDialog;

    // Private variables
    private String analysisId;
    private BodyLanguageAdapter bodyLanguageAdapter;
    private RecommendedBehaviourStrategyAdapter recommendedBehaviourStrategyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        analysisId = passedIntent.getStringExtra(AnalysisAdapter.KEY_ANALYSIS_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        analysesRef = db.collection("analyses");
        analysisFeedbackRef = db.collection("analysisFeedback");
        recommendationsRef = db.collection("recommendations");

        // Get ViewBinding and set content view
        binding = ActivityViewSavedAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupUi();
        loadData();
        bindListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    private void setupUi() {
        // Delete dialog
        deleteDialog =
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete)
                    .setMessage(R.string.delete_analysis_message)
                    .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                        // Do nothing
                    })
                    .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                        deleteAnalysis(analysisId);
                    });
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

                        // Check whether this analysis has been rated
                        if (analysis.isRated()) {
                            disableMenuItem(R.id.action_rate_saved_analysis);
                        }

                        // Summary
                        binding.txtEmotion.setText(
                                String.format("%s %s",
                                        analysis.getEmotion(),
                                        analysis.getProbability() == 0 ? "" : String.format("(%s%%)", (int) (analysis.getProbability() * 100))
                                )
                        );
                        binding.txtCatName.setText(analysis.getCatName());
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
                            bodyLanguageAdapter = new BodyLanguageAdapter(bodyLanguageList, this);
                            binding.recyclerviewBodyLanguage.setAdapter(bodyLanguageAdapter);
                            binding.txtBodyLanguageCount.setText(String.format("%s", bodyLanguageAdapter.getItemCount()));
                            binding.layoutNoBodyLanguage.setVisibility(View.GONE);
                        }

                        // Recommendations
                        recommendationsRef.whereEqualTo("analysisId", analysisId)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.getDocuments().isEmpty()) {
                                        Recommendation recommendation = queryDocumentSnapshots.getDocuments().get(0).toObject(Recommendation.class);
                                        ArrayList<RecommendedBehaviourStrategy> stratList = new ArrayList<>(recommendation.getStrategies());

                                        if (stratList.isEmpty()) {
                                            // No results
                                            binding.recyclerviewRecommendations.setVisibility(View.GONE);
                                            binding.layoutNoRecommendations.setVisibility(View.VISIBLE);
                                        } else {
                                            recommendedBehaviourStrategyAdapter = new RecommendedBehaviourStrategyAdapter(stratList, this);
                                            binding.recyclerviewRecommendations.setAdapter(recommendedBehaviourStrategyAdapter);
                                            binding.txtRecommendationsCount.setText(String.format("%s", recommendedBehaviourStrategyAdapter.getItemCount()));
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
        binding.appBarViewSavedAnalysis.setNavigationOnClickListener(view -> finish());

        binding.appBarViewSavedAnalysis.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_rate_saved_analysis) {
                showRateAnalysisDialog();
            } else if (itemId == R.id.action_delete_saved_analysis) {
                deleteDialog.show();
            }

            return false;
        });
    }

    private void bindOnClickListeners() {
        binding.btnBackBodyLanguage.setOnClickListener(view -> finish());

        binding.btnBackRecommendations.setOnClickListener(view-> finish());
    }

    private void showRateAnalysisDialog() {
        View rateDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_rate_analysis, null);
        TextView txtRateAnalysisHint = rateDialogLayout.findViewById(R.id.txt_rate_analysis_hint);
        RatingBar ratingBar = rateDialogLayout.findViewById(R.id.rating_bar);
        TextInputLayout txtfieldDescription = rateDialogLayout.findViewById(R.id.txtfield_description);
        TextInputEditText edtDescription = rateDialogLayout.findViewById(R.id.edt_description);

        // Hide hint
        txtRateAnalysisHint.setVisibility(View.GONE);

        MaterialAlertDialogBuilder rateDialogBuilder =
                new MaterialAlertDialogBuilder(this)
                        .setView(rateDialogLayout)
                        .setTitle(R.string.rate_analysis)
                        .setMessage(R.string.rate_analysis_message)
                        .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                            // Unused
                        });

        AlertDialog rateDialog = rateDialogBuilder.create();
        rateDialog.show();

        // Override the positive button's click behavior
        rateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate inputs
            float rating = ratingBar.getRating();
            String description = edtDescription.getText().toString();

            if (rating == 0) {
                // No rating
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_LONG).show();
            } else if (description.isBlank()) {
                txtfieldDescription.setError(getString(R.string.description_required_error));
            } else {
                // Valid
                rateAnalysis(analysisId, rating, description);
                rateDialog.dismiss();
            }
        });

        // Reset error when text is changed
        edtDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                txtfieldDescription.setErrorEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    // Creates AnalysisFeedback linked to this Analysis
    private void rateAnalysis(String analysisId, float rating, String description) {
        // Get user data
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        // Add AnalysisFeedback document
                        AnalysisFeedback newAnalysisFeedback = new AnalysisFeedback(
                                analysisId,
                                firebaseAuthUtils.getUid(),
                                user.getEmail(),
                                user.getDisplayName(),
                                user.getProfilePicture(),
                                rating,
                                description,
                                false,
                                Timestamp.now(),
                                Timestamp.now()
                        );
                        analysisFeedbackRef.add(newAnalysisFeedback)
                                .addOnSuccessListener(documentReference -> {
                                    // Update existing Analysis to rated
                                    analysesRef.document(analysisId)
                                            .update("rated", true, "updatedAt", Timestamp.now())
                                            .addOnSuccessListener(unused -> {
                                                // COMPLETE
                                                Toast.makeText(this, "Thank you, we have received your rating and feedback.", Toast.LENGTH_SHORT).show();
                                                disableMenuItem(R.id.action_rate_saved_analysis);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "rateAnalysis: Failed to update Analysis to rated", e);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "rateAnalysis: Failed to add AnalysisFeedback", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "rateAnalysis: Failed to retrieve User data", e);
                    Toast.makeText(this, "Failed to submit rating, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    // Soft deletes analysis
    private void deleteAnalysis(String analysisId) {
        analysesRef.document(analysisId)
                .update("deleted", true, "updatedAt", Timestamp.now())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Successfully deleted analysis.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "deleteAnalysis: Failed to soft delete Analysis", e);
                    Toast.makeText(this, "Failed to delete, please try again later.", Toast.LENGTH_SHORT).show();
                });
    }

    private void disableMenuItem(int itemId) {
        MenuItem menuItem = binding.appBarViewSavedAnalysis.getMenu().findItem(itemId);
        menuItem.setEnabled(false);
        menuItem.setIconTintList(ContextCompat.getColorStateList(this, R.color.gray_200));
    }
}