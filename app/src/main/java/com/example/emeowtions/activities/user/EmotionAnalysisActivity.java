package com.example.emeowtions.activities.user;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.BodyLanguageAdapter;
import com.example.emeowtions.databinding.ActivityEmotionAnalysisBinding;
import com.example.emeowtions.fragments.user.EmotionFragment;
import com.example.emeowtions.models.BodyLanguage;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.utils.EmotionExtractor;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class EmotionAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "EmotionAnalysisActivity";
    private ActivityEmotionAnalysisBinding binding;
    private BodyLanguageAdapter bodyLanguageAdapter;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference catsRef;
    private StorageReference storageRef;
    private StorageReference profilePictureRef;

    private String catId;
    private String tempCatImageUrl;
    private ArrayList<String> predictedLabels;
    private String emotion;
    private ArrayList<BodyLanguage> bodyLanguageList;
    private ArrayList<String> recommendationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        catId = passedIntent.getStringExtra(EmotionFragment.KEY_CAT_ID);
        tempCatImageUrl = passedIntent.getStringExtra(EmotionFragment.KEY_TEMP_CAT_IMAGE_URL);
        predictedLabels = passedIntent.getStringArrayListExtra(EmotionFragment.KEY_PREDICTED_LABELS);

        bodyLanguageList = new ArrayList<>();
        recommendationList = new ArrayList<>();

        if (!predictedLabels.isEmpty()) {
            // Get emotion
            emotion = EmotionExtractor.getSingleEmotion(predictedLabels);

            // Convert labels to corresponding body language types
            for (String label : predictedLabels) {
                bodyLanguageList.add(new BodyLanguage(label, "Lorem ipsum", Timestamp.now(), Timestamp.now()));
            }
        }

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
        Glide.with(getApplicationContext())
                .load(tempCatImageUrl)
                .into(binding.imgDetectedCat);
        //endregion

        //region Load Data
        // Get cat's name
        if (catId.isEmpty()) {
            // Unspecified cat
            binding.txtCatName.setText(getString(R.string.unspecified));
        } else {
            // Specified cat
            catsRef.document(catId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Cat cat = documentSnapshot.toObject(Cat.class);
                            binding.txtCatName.setText(cat.getName());
                        } else {
                            binding.txtCatName.setText(getString(R.string.unspecified));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "onCreate: Failed to get from catsRef", e);
                    });
        }

        // Set emotion
        if (emotion != null)
            binding.txtEmotion.setText(emotion.substring(0, 1).toUpperCase() + emotion.substring(1));
        else
            binding.txtEmotion.setText(R.string.none);

        // Load detected body language types
        if (bodyLanguageList.isEmpty()) {
            // No results
            binding.layoutNoBodyLanguage.setVisibility(View.VISIBLE);
        } else {
            // Show results
            bodyLanguageAdapter = new BodyLanguageAdapter(bodyLanguageList, this);
            binding.recyclerviewBodyLanguage.setAdapter(bodyLanguageAdapter);
            binding.layoutNoBodyLanguage.setVisibility(View.GONE);
        }

        // Load recommendations
        if (recommendationList.isEmpty()) {
            // No results
            binding.layoutNoRecommendations.setVisibility(View.VISIBLE);
        } else {
            // Show results
            binding.layoutNoRecommendations.setVisibility(View.GONE);
        }
        //endregion

        //region Listeners
        // appBarEmotionAnalysis back button: return to Emotion screen
        binding.appBarEmotionAnalysis.setNavigationOnClickListener(view -> finish());

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
}