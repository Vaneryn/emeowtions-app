package com.example.emeowtions.activities.veterinary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emeowtions.R;
import com.example.emeowtions.adapters.ReviewAdapter;
import com.example.emeowtions.databinding.ActivityClinicReviewsBinding;
import com.example.emeowtions.models.Review;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ClinicReviewsActivity extends AppCompatActivity {

    private static final String TAG = "ClinicReviewsActivity";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference clinicsRef;
    private CollectionReference reviewsRef;

    // Layout variables
    private ActivityClinicReviewsBinding binding;

    // Private variables
    private String veterinaryClinicId;
    private FirestoreRecyclerOptions<Review> options;
    private ReviewAdapter reviewAdapter;
    private boolean isInitialLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shared preferences
        sharedPreferences = getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        veterinaryClinicId = sharedPreferences.getString("veterinaryClinicId", null);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        clinicsRef = db.collection("veterinaryClinics");
        reviewsRef = clinicsRef.document(veterinaryClinicId).collection("reviews");

        // Get ViewBinding and set content view
        binding = ActivityClinicReviewsBinding.inflate(getLayoutInflater());
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

    @Override
    public void onResume() {
        super.onResume();
        if (reviewAdapter != null)
            reviewAdapter.notifyDataSetChanged();
    }

    private void setupUi() {

    }

    private void loadData() {
        isInitialLoad = true;

        Query reviewQuery = reviewsRef
                .orderBy("read", Query.Direction.ASCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<Review>()
                .setQuery(reviewQuery, Review.class)
                .setLifecycleOwner(this)
                .build();

        reviewAdapter = new ReviewAdapter(options, this);

        binding.recyclerviewReviews.setAdapter(reviewAdapter);

        reviewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateResultsView(isInitialLoad);
            }
        });
    }

    private void bindListeners() {
        bindNavigationListeners();
        bindOnClickListeners();
        bindTextChangeListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarClinicReviews.setNavigationOnClickListener(view -> finish());
    }

    private void bindOnClickListeners() {
        binding.btnBack.setOnClickListener(view -> {
            finish();
        });

        binding.btnClearSearch.setOnClickListener(view -> {
            binding.edtSearchbar.getText().clear();
        });
    }

    private void bindTextChangeListeners() {
        LifecycleOwner lifecycleOwner = this;

        // Dynamic search
        binding.edtSearchbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String queryText = binding.edtSearchbar.getText().toString();

                if (queryText.isBlank()) {
                    isInitialLoad = true;
                    reviewAdapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery = reviewsRef
                            .where(
                                    Filter.or(
                                            Filter.or(
                                                Filter.equalTo("userDisplayName", queryText),
                                                Filter.and(
                                                        Filter.greaterThanOrEqualTo("userDisplayName", queryText),
                                                        Filter.lessThanOrEqualTo("userDisplayName", queryText + "\uf8ff")
                                                )
                                            ),
                                            Filter.or(
                                                    Filter.equalTo("description", queryText),
                                                    Filter.and(
                                                            Filter.greaterThanOrEqualTo("description", queryText),
                                                            Filter.lessThanOrEqualTo("description", queryText + "\uf8ff")
                                                    )
                                            )
                                    )
                            )
                            .orderBy("read", Query.Direction.ASCENDING)
                            .orderBy("createdAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<Review> searchOptions =
                            new FirestoreRecyclerOptions.Builder<Review>()
                                    .setQuery(searchQuery, Review.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    reviewAdapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        binding.layoutNoReviews.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (reviewAdapter == null || reviewAdapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoReviews.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}