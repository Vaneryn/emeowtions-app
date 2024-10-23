package com.example.emeowtions.activities.user;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.example.emeowtions.models.Analysis;
import com.example.emeowtions.models.AnalysisFeedback;
import com.example.emeowtions.models.BehaviourStrategy;
import com.example.emeowtions.models.BodyLanguage;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.models.Recommendation;
import com.example.emeowtions.models.RecommendedBehaviourStrategy;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.BodyLanguageUtils;
import com.example.emeowtions.utils.EmotionUtils;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.example.emeowtions.utils.Recommender;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EmotionAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "EmotionAnalysisActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference catsRef;
    private CollectionReference analysesRef;
    private CollectionReference analysisFeedbackRef;
    private CollectionReference recommendationsRef;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private StorageReference tempImageRef;
    private StorageReference analyzedImageRef;

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

    private boolean isRated;
    private boolean isSaved;
    private String currentAnalysisId;

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
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        catsRef = db.collection("cats");
        analysesRef = db.collection("analyses");
        analysisFeedbackRef = db.collection("analysisFeedback");
        recommendationsRef = db.collection("recommendations");
        // Initialize Storage references
        storageRef = storage.getReference();
        tempImageRef = storageRef.child("images/users/" + firebaseAuthUtils.getUid() + "/temp_detected_cat_image.jpg");

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
        isRated = false;
        isSaved = false;

        // Cat image
        Glide.with(getApplicationContext())
                .load(tempCatImageUrl)
                .into(binding.imgDetectedCat);

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
        currentAnalysisId = null;

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
            String emotion = trueEmotion.first == null ? "Undetectable" : trueEmotion.first;
            int probability = trueEmotion.second == -1 ? 0 : (int) (trueEmotion.second * 100);
            Log.d(TAG, "onCreate: " + trueEmotion.second);
            binding.txtEmotion.setText(
                    String.format("%s%s", String.format("%s ", emotion.substring(0, 1).toUpperCase() + emotion.substring(1)), trueEmotion.second == -1 ? "" : String.format("(%s%%)", probability))
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
                showRateAnalysisDialog();
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

    private void showRateAnalysisDialog() {
        View rateDialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_rate_analysis, null);
        RatingBar ratingBar = rateDialogLayout.findViewById(R.id.rating_bar);
        TextInputLayout txtfieldDescription = rateDialogLayout.findViewById(R.id.txtfield_description);
        TextInputEditText edtDescription = rateDialogLayout.findViewById(R.id.edt_description);

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
                rateAnalysis(rating, description);
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

    private void rateAnalysis(float rating, String description) {
        String emotion;
        float probability;

        // Parse emotion and probability
        if (trueEmotion == null || trueEmotion.first == null) {
            emotion = "Undetectable";
            probability = 0;
        } else {
            emotion = trueEmotion.first.substring(0, 1).toUpperCase() + trueEmotion.first.substring(1);
            probability = trueEmotion.second;
        }

        // Check if analysis has already been saved
        if (isSaved) {
            // Add new AnalysisFeedback document and link it to existing Analysis
            createAnalysisFeedback(true, currentAnalysisId, rating, description);
        } else {
            // Add new Analysis document (along with AnalysisFeedback)
            boolean rated = true;
            boolean deleted = false;

            Analysis newAnalysis = new Analysis(
                    catId == null ? null : catId,
                    firebaseAuthUtils.getUid(),
                    selectedCat == null ? null : selectedCat.getName(),
                    emotion,
                    probability,
                    bodyLanguageList,
                    null,
                    Timestamp.now(),
                    Timestamp.now(),
                    rated,
                    deleted
            );

            analysesRef.add(newAnalysis)
                    .addOnSuccessListener(documentReference -> {
                        // Upload analyzed image to Storage
                        byte[] imageData = null;
                        binding.imgDetectedCat.setDrawingCacheEnabled(true);
                        binding.imgDetectedCat.buildDrawingCache();
                        Bitmap bitmap = ((BitmapDrawable) binding.imgDetectedCat.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        imageData = baos.toByteArray();

                        analyzedImageRef = storageRef.child("images/analyses/" + documentReference.getId() + "/analyzed_image.jpg");
                        analyzedImageRef.putBytes(imageData)
                                .continueWithTask(imageUploadTask -> {
                                    if (!imageUploadTask.isSuccessful()) {
                                        throw imageUploadTask.getException();
                                    }
                                    return analyzedImageRef.getDownloadUrl();
                                })
                                .addOnCompleteListener(imageUploadTask -> {
                                    Uri imageUrl = imageUploadTask.getResult();

                                    // Update Analysis imageUrl
                                    analysesRef.document(documentReference.getId())
                                            .update("imageUrl", imageUrl)
                                            .addOnSuccessListener(unused -> {
                                                createAnalysisFeedback(false, documentReference.getId(), rating, description);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "saveAnalysis: Failed to update Analysis imageUrl", e);
                                            });
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "saveAnalysis: Failed to add Analysis document", e);
                        Toast.makeText(this, "Unable to rate analysis, please try again later.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void createAnalysisFeedback(boolean isAlreadySaved, String analysisId, float rating, String description) {
        // Get user data
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        // Add AnalysisFeedback document
                        AnalysisFeedback newAnalysisFeedback = new AnalysisFeedback(
                                analysisId,
                                firebaseAuthUtils.getUid(),
                                user.getDisplayName(),
                                user.getProfilePicture(),
                                rating,
                                description,
                                Timestamp.now(),
                                Timestamp.now()
                        );

                        analysisFeedbackRef.add(newAnalysisFeedback)
                                .addOnSuccessListener(documentReference -> {
                                    // COMPLETE
                                    // Set existing analysis to rated (if already saved via savedAnalysis)
                                    if (isAlreadySaved) {
                                        analysesRef.document(analysisId)
                                                .update("rated", true, "updatedAt", Timestamp.now())
                                                .addOnCompleteListener(task -> {
                                                    // Error
                                                    if (!task.isSuccessful()) {
                                                        Log.w(TAG, "createAnalysisFeedback: Failed to update Analysis to rated", task.getException());
                                                        return;
                                                    }
                                                    // Success
                                                    Log.d(TAG, "createAnalysisFeedback: Successfully updated Analysis to rated");
                                                });
                                    }

                                    saveRecommendation(analysisId);
                                    Toast.makeText(this, "Thank you, we have received your rating and feedback.", Toast.LENGTH_SHORT).show();
                                    disableMenuItem(R.id.action_rate_analysis);
                                    disableMenuItem(R.id.action_save_analysis);
                                    isRated = true;
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "createAnalysisFeedback: Failed to add AnalysisFeedbackDocument", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "createAnalysisFeedback: Failed to retrieve User data", e);
                });
    }

    private void saveAnalysis() {
        String emotion;
        float probability;

        // Parse emotion and probability
        if (trueEmotion == null || trueEmotion.first == null) {
            emotion = "Undetectable";
            probability = 0;
        } else {
            emotion = trueEmotion.first.substring(0, 1).toUpperCase() + trueEmotion.first.substring(1);
            probability = trueEmotion.second;
        }

        // The Analysis is already saved if it has been rated
        if (!isRated) {
            // Add Analysis document
            boolean rated = false;
            boolean deleted = false;

            Analysis newAnalysis =new Analysis(
                    catId == null ? null : catId,
                    firebaseAuthUtils.getUid(),
                    selectedCat == null ? null : selectedCat.getName(),
                    emotion,
                    probability,
                    bodyLanguageList,
                    null,
                    Timestamp.now(),
                    Timestamp.now(),
                    rated,
                    deleted
            );

            analysesRef.add(newAnalysis)
                    .addOnSuccessListener(documentReference -> {
                        currentAnalysisId = documentReference.getId();

                        // Upload analyzed image to Storage
                        byte[] imageData = null;
                        binding.imgDetectedCat.setDrawingCacheEnabled(true);
                        binding.imgDetectedCat.buildDrawingCache();
                        Bitmap bitmap = ((BitmapDrawable) binding.imgDetectedCat.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        imageData = baos.toByteArray();

                        analyzedImageRef = storageRef.child("images/analyses/" + documentReference.getId() + "/analyzed_image.jpg");
                        analyzedImageRef.putBytes(imageData)
                                .continueWithTask(imageUploadTask -> {
                                    if (!imageUploadTask.isSuccessful()) {
                                        throw imageUploadTask.getException();
                                    }
                                    return analyzedImageRef.getDownloadUrl();
                                })
                                .addOnCompleteListener(imageUploadTask -> {
                                    Uri imageUrl = imageUploadTask.getResult();

                                    // Update Analysis imageUrl
                                    analysesRef.document(documentReference.getId())
                                            .update("imageUrl", imageUrl)
                                            .addOnSuccessListener(unused -> {
                                                // COMPLETE
                                                saveRecommendation(documentReference.getId());
                                                Toast.makeText(this, "Successfully saved.", Toast.LENGTH_SHORT).show();
                                                disableMenuItem(R.id.action_save_analysis);
                                                isSaved = true;
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.w(TAG, "saveAnalysis: Failed to update Analysis imageUrl", e);
                                            });
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "saveAnalysis: Failed to add Analysis document", e);
                        Toast.makeText(this, "Unable to save analysis, please try again later.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveRecommendation(String analysisId) {
        // Create Recommendation
        Recommendation newRecommendation = new Recommendation(
                analysisId,
                null,
                Timestamp.now(),
                Timestamp.now()
        );

        recommendationsRef.add(newRecommendation)
                .addOnSuccessListener(documentReference -> {
                    // Update RecommendedBehaviourStrategy list
                    List<RecommendedBehaviourStrategy> recommendedStratList = new ArrayList<>();
                    for (BehaviourStrategy strat : stratList) {
                        boolean rated = false;
                        boolean liked = false;
                        boolean disliked = false;

                        recommendedStratList.add(new RecommendedBehaviourStrategy(
                                documentReference.getId(),
                                strat.getId(),
                                strat.getDescription(),
                                strat.getRecommendationFactorId(),
                                strat.getFactorType(),
                                strat.getFactorValue(),
                                rated,
                                liked,
                                disliked,
                                Timestamp.now(),
                                Timestamp.now()
                        ));

                        recommendationsRef.document(documentReference.getId())
                                .update("strategies", recommendedStratList)
                                .addOnSuccessListener(unused -> {
                                    Log.d(TAG, "saveRecommendation: Successfully updated Recommendation strategies");
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "saveRecommendation: Failed to update Recommendation strategies", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "saveRecommendation: Failed to add new Recommendation", e);
                });
    }

    private void disableMenuItem(int itemId) {
        MenuItem menuItem = binding.appBarEmotionAnalysis.getMenu().findItem(itemId);
        menuItem.setEnabled(false);
        menuItem.setIconTintList(ContextCompat.getColorStateList(this, R.color.gray_200));
    }
}