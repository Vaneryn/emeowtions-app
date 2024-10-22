package com.example.emeowtions.activities.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.BehaviourStrategyAdapter;
import com.example.emeowtions.adapters.BodyLanguageAdapter;
import com.example.emeowtions.databinding.ActivityEmotionAnalysisBinding;
import com.example.emeowtions.fragments.user.EmotionFragment;
import com.example.emeowtions.models.BehaviourStrategy;
import com.example.emeowtions.models.BodyLanguage;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.models.Recommendation;
import com.example.emeowtions.utils.BodyLanguageUtils;
import com.example.emeowtions.utils.EmotionUtils;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.example.emeowtions.utils.Recommender;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;

public class EmotionAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "EmotionAnalysisActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference catsRef;
    private StorageReference storageRef;
    private StorageReference profilePictureRef;

    // Layout variables
    private ActivityEmotionAnalysisBinding binding;
    private BodyLanguageAdapter bodyLanguageAdapter;

    // Private variables
    private String catId;
    private Cat selectedCat;
    private String tempCatImageUrl;
    private HashMap<String, Float> predictedLabels;
    private Pair<String, Float> trueEmotion;
    private ArrayList<BodyLanguage> bodyLanguageList;
    private ArrayList<BehaviourStrategy> stratList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        catId = passedIntent.getStringExtra(EmotionFragment.KEY_CAT_ID);
        tempCatImageUrl = passedIntent.getStringExtra(EmotionFragment.KEY_TEMP_CAT_IMAGE_URL);
        predictedLabels = (HashMap<String, Float>) passedIntent.getSerializableExtra(EmotionFragment.KEY_PREDICTED_LABELS);
        Log.d(TAG, "onCreate: " + predictedLabels);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        catsRef = db.collection("cats");

        // Get ViewBinding and set content view
        binding = ActivityEmotionAnalysisBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        // Cat image
        Glide.with(getApplicationContext())
                .load(tempCatImageUrl)
                .into(binding.imgDetectedCat);

        // Rate analysis dialog
        // Save analysis dialog
        MaterialAlertDialogBuilder saveDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.save_analysis)
                        .setMessage(R.string.are_you_sure)
                        .setNegativeButton(R.string.no, (dialogInterface, i) -> {
                            // Do nothing
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            saveAnalysis();
                        });
        //endregion

        //region Load Data
        // Extract true emotion and body languages
        BodyLanguageUtils bodyLanguageUtils = new BodyLanguageUtils(this);
        bodyLanguageList = new ArrayList<>();

        if (!predictedLabels.isEmpty()) {
            // Get emotion with highest probability
            trueEmotion = EmotionUtils.getTrueEmotion(predictedLabels);

            // Convert remaining labels to corresponding body language types
            for (String label : predictedLabels.keySet()) {
                if (bodyLanguageUtils.isBodyLanguage(label))
                    bodyLanguageList.add(
                            new BodyLanguage(
                                    label,
                                    bodyLanguageUtils.getValue(label),
                                    bodyLanguageUtils.getDescription(label),
                                    (int) (predictedLabels.get(label) * 100)
                            )
                    );
            }

            // Load number of detected body language types
            binding.txtBodyLanguageCount.setText(String.format("%s", bodyLanguageList.size()));
        }

        // - GET CAT DATA
        // - GENERATE RECOMMENDATIONS
        // - LOAD RECOMMENDATIONS
        if (catId == null) {
            // Unspecified cat
            selectedCat = null;
            binding.txtCatName.setText(getString(R.string.unspecified));
            // Initialize and run Recommender
            getRecommendations();
        } else {
            // Specified cat
            catsRef.document(catId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            selectedCat = documentSnapshot.toObject(Cat.class);
                            binding.txtCatName.setText(selectedCat.getName());
                        } else {
                            binding.txtCatName.setText(getString(R.string.unspecified));
                        }

                        // Initialize and run Recommender
                        getRecommendations();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "onCreate: Failed to get from catsRef", e);
                    });
        }

        // Load emotion text
        if (trueEmotion != null) {
            String emotion = trueEmotion.first == null ? "None" : trueEmotion.first;
            int probability = (int) (trueEmotion.second * 100);

            binding.txtEmotion.setText(
                    String.format(
                            "%s (%s%%)",
                            emotion.substring(0, 1).toUpperCase() + emotion.substring(1),
                            probability
                    )
            );
        } else {
            binding.txtEmotion.setText(R.string.none);
        }

        // Load detected body language types
        if (bodyLanguageList.isEmpty()) {
            // No results
            binding.recyclerviewBodyLanguage.setVisibility(View.GONE);
            binding.layoutNoBodyLanguage.setVisibility(View.VISIBLE);
        } else {
            // Show results
            bodyLanguageAdapter = new BodyLanguageAdapter(bodyLanguageList, this);
            binding.recyclerviewBodyLanguage.setAdapter(bodyLanguageAdapter);
            binding.layoutNoBodyLanguage.setVisibility(View.GONE);
        }
        //endregion

        //region Listeners
        // appBarEmotionAnalysis back button: return to Emotion screen
        binding.appBarEmotionAnalysis.setNavigationOnClickListener(view -> finish());

        // appBarEmotionAnalysis action buttons
        binding.appBarEmotionAnalysis.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_rate_analysis) {
                rateAnalysis();
            } else if (itemId == R.id.action_save_analysis) {
                saveDialog.show();
            }

            return false;
        });

        // btnBackBodyLanguage: return to Emotion screen
        binding.btnBackBodyLanguage.setOnClickListener(view -> {
            finish();
        });

        // btnBackRecommendations: return to Emotion screen
        binding.btnBackRecommendations.setOnClickListener(view -> {
            finish();
        });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    private void getRecommendations() {
        Context context = this;
        stratList = new ArrayList<>();

        // Initialize and run Recommender
        Recommender recommender = new Recommender(this);
        recommender.initialize(trueEmotion.first, selectedCat);
        //recommender.generateStrategies();
        recommender.retrieveRecommendations(strats -> {
            if (strats != null || !strats.isEmpty()) {
                stratList = strats;
                Log.d(TAG, "getRecommendations: " + stratList);

                // Display recommendations
                if (stratList == null || stratList.isEmpty()) {
                    // No results
                    binding.recyclerviewRecommendations.setVisibility(View.GONE);
                    binding.layoutNoRecommendations.setVisibility(View.VISIBLE);
                } else {
                    // Show results
                    BehaviourStrategyAdapter stratAdapter = new BehaviourStrategyAdapter(stratList, context);
                    binding.recyclerviewRecommendations.setAdapter(stratAdapter);
                    binding.txtRecommendationsCount.setText(String.format("%s", stratList.size()));
                    binding.layoutNoRecommendations.setVisibility(View.GONE);
                }
            }
        });
    }

    private void rateAnalysis() {
        MenuItem menuItem = binding.appBarEmotionAnalysis.getMenu().findItem(R.id.action_rate_analysis);
        menuItem.setEnabled(false);
        menuItem.setIconTintList(ContextCompat.getColorStateList(this, R.color.gray_200));
    }

    private void saveAnalysis() {
        MenuItem menuItem = binding.appBarEmotionAnalysis.getMenu().findItem(R.id.action_save_analysis);
        menuItem.setEnabled(false);
        menuItem.setIconTintList(ContextCompat.getColorStateList(this, R.color.gray_200));
    }
}