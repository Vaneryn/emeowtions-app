package com.example.emeowtions.activities.admin;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emeowtions.R;
import com.example.emeowtions.adapters.RecommendationRatingAdapter;
import com.example.emeowtions.databinding.ActivityRecommendationRatingsBinding;
import com.example.emeowtions.models.BehaviourStrategy;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class RecommendationRatingsActivity extends AppCompatActivity {

    private static final String TAG = "RecommendationRatingsActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference stratsRef;

    // Layout variables
    private ActivityRecommendationRatingsBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<BehaviourStrategy> generalOptions;
    private FirestoreRecyclerOptions<BehaviourStrategy> ageOptions;
    private FirestoreRecyclerOptions<BehaviourStrategy> genderOptions;
    private FirestoreRecyclerOptions<BehaviourStrategy> temperamentOptions;
    private FirestoreRecyclerOptions<BehaviourStrategy> activityLevelOptions;
    private FirestoreRecyclerOptions<BehaviourStrategy> backgroundOptions;
    private FirestoreRecyclerOptions<BehaviourStrategy> medicalConditionsOptions;
    private RecommendationRatingAdapter generalAdapter;
    private RecommendationRatingAdapter ageAdapter;
    private RecommendationRatingAdapter genderAdapter;
    private RecommendationRatingAdapter temperamentAdapter;
    private RecommendationRatingAdapter activityLevelAdapter;
    private RecommendationRatingAdapter backgroundAdapter;
    private RecommendationRatingAdapter medicalConditionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        stratsRef = db.collection("behaviourStrategies");

        // Get ViewBinding and set content view
        binding = ActivityRecommendationRatingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.scrollView.setEdgeEffectColor(getResources().getColor(R.color.gray_200));
        }

        loadData();
        bindListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (generalAdapter != null)
            generalAdapter.notifyDataSetChanged();
        if (ageAdapter != null)
            ageAdapter.notifyDataSetChanged();
        if (genderAdapter != null)
            genderAdapter.notifyDataSetChanged();
        if (temperamentAdapter != null)
            temperamentAdapter.notifyDataSetChanged();
        if (activityLevelAdapter != null)
            activityLevelAdapter.notifyDataSetChanged();
        if (backgroundAdapter != null)
            backgroundAdapter.notifyDataSetChanged();
        if (medicalConditionsAdapter != null)
            medicalConditionsAdapter.notifyDataSetChanged();
    }

    private void loadData() {
        // Queries
        Query generalQuery = stratsRef.whereEqualTo("factorType", null).orderBy("id", Query.Direction.DESCENDING).orderBy("emotionType", Query.Direction.DESCENDING);
        Query ageQuery = stratsRef.whereEqualTo("factorType", "Age").orderBy("recommendationFactorId", Query.Direction.ASCENDING).orderBy("emotionType", Query.Direction.DESCENDING);
        Query genderQuery = stratsRef.whereEqualTo("factorType", "Gender").orderBy("recommendationFactorId", Query.Direction.ASCENDING).orderBy("emotionType", Query.Direction.DESCENDING);
        Query temperamentQuery = stratsRef.whereEqualTo("factorType", "Temperament").orderBy("recommendationFactorId", Query.Direction.ASCENDING).orderBy("emotionType", Query.Direction.DESCENDING);
        Query activityLevelQuery = stratsRef.whereEqualTo("factorType", "Activity Level").orderBy("recommendationFactorId", Query.Direction.ASCENDING).orderBy("emotionType", Query.Direction.DESCENDING);
        Query backgroundQuery = stratsRef.whereEqualTo("factorType", "Background").orderBy("recommendationFactorId", Query.Direction.ASCENDING).orderBy("emotionType", Query.Direction.DESCENDING);
        Query medicalConditionsQuery = stratsRef.whereEqualTo("factorType", "Medical Conditions").orderBy("recommendationFactorId", Query.Direction.ASCENDING).orderBy("emotionType", Query.Direction.DESCENDING);

        // Options
        generalOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(generalQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();
        ageOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(ageQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();
        genderOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(genderQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();
        temperamentOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(temperamentQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();
        activityLevelOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(activityLevelQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();
        backgroundOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(backgroundQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();
        medicalConditionsOptions = new FirestoreRecyclerOptions.Builder<BehaviourStrategy>()
                .setQuery(medicalConditionsQuery, BehaviourStrategy.class)
                .setLifecycleOwner(this)
                .build();

        // Create adapters
        generalAdapter = new RecommendationRatingAdapter(generalOptions, this);
        ageAdapter = new RecommendationRatingAdapter(ageOptions, this);
        genderAdapter = new RecommendationRatingAdapter(genderOptions, this);
        temperamentAdapter = new RecommendationRatingAdapter(temperamentOptions, this);
        activityLevelAdapter = new RecommendationRatingAdapter(activityLevelOptions, this);
        backgroundAdapter = new RecommendationRatingAdapter(backgroundOptions, this);
        medicalConditionsAdapter = new RecommendationRatingAdapter(medicalConditionsOptions, this);

        // Set adapters to RecyclerViews
        binding.recyclerviewGeneral.setAdapter(generalAdapter);
        binding.recyclerviewAge.setAdapter(ageAdapter);
        binding.recyclerviewGender.setAdapter(genderAdapter);
        binding.recyclerviewTemperament.setAdapter(temperamentAdapter);
        binding.recyclerviewActivityLevel.setAdapter(activityLevelAdapter);
        binding.recyclerviewBackground.setAdapter(backgroundAdapter);
        binding.recyclerviewMedicalConditions.setAdapter(medicalConditionsAdapter);

        // Listen for changes to options
        generalAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
        ageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
        genderAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
        temperamentAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
        activityLevelAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
        backgroundAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
        medicalConditionsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
    }

    private void bindListeners() {
        bindNavigationListeners();
        bindOnClickListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarRecommendationRatings.setNavigationOnClickListener(view -> finish());
    }

    private void bindOnClickListeners() {
        binding.btnBack.setOnClickListener(view -> {
            finish();
        });
    }
}