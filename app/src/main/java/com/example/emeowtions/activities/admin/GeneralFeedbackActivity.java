package com.example.emeowtions.activities.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emeowtions.R;
import com.example.emeowtions.adapters.FeedbackAdapter;
import com.example.emeowtions.databinding.ActivityGeneralFeedbackBinding;
import com.example.emeowtions.models.Feedback;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class GeneralFeedbackActivity extends AppCompatActivity {

    private static final String TAG = "GeneralFeedbackActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference feedbackRef;

    // Layout variables
    private ActivityGeneralFeedbackBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<Feedback> options;
    private FeedbackAdapter feedbackAdapter;
    private boolean isInitialLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        feedbackRef = db.collection("feedback");

        // Get ViewBinding and set content view
        binding = ActivityGeneralFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        if (feedbackAdapter != null)
            feedbackAdapter.notifyDataSetChanged();
    }

    private void loadData() {
        isInitialLoad = true;

        // Query options
        Query feedbackQuery = feedbackRef
                .orderBy("read", Query.Direction.ASCENDING)
                .orderBy("userDisplayName", Query.Direction.ASCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<Feedback>()
                .setQuery(feedbackQuery, Feedback.class)
                .setLifecycleOwner(this)
                .build();

        // Create and set adapter
        feedbackAdapter = new FeedbackAdapter(options, this);
        binding.recyclerviewGeneralFeedback.setAdapter(feedbackAdapter);

        // Listen for changes to options
        feedbackAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
        bindTextChangeListeners(this);
    }

    private void bindNavigationListeners() {
        binding.appBarGeneralFeedback.setNavigationOnClickListener(view -> finish());
    }

    private void bindOnClickListeners() {
        binding.btnBack.setOnClickListener(view -> {
            finish();
        });

        binding.btnClearSearch.setOnClickListener(view -> {
            binding.edtSearchbar.getText().clear();
        });
    }

    private void bindTextChangeListeners(LifecycleOwner lifecycleOwner) {
        // Dynamic search
        binding.edtSearchbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String queryText = binding.edtSearchbar.getText().toString();

                if (queryText.isBlank()) {
                    isInitialLoad = true;
                    feedbackAdapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery =
                            feedbackRef.where(Filter.or(
                                            Filter.equalTo("userDisplayName", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("userDisplayName", queryText),
                                                    Filter.lessThanOrEqualTo("userDisplayName", queryText + "\uf8ff")
                                            )
                                    ))
                                    .orderBy("createdAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<Feedback> searchOptions =
                            new FirestoreRecyclerOptions.Builder<Feedback>()
                                    .setQuery(searchQuery, Feedback.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    feedbackAdapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        binding.layoutNoGeneralFeedback.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (feedbackAdapter == null || feedbackAdapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoGeneralFeedback.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}