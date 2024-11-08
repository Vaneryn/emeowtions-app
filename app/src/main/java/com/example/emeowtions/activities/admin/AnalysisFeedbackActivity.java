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
import com.example.emeowtions.adapters.AnalysisFeedbackAdapter;
import com.example.emeowtions.databinding.ActivityAnalysisFeedbackBinding;
import com.example.emeowtions.models.AnalysisFeedback;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class AnalysisFeedbackActivity extends AppCompatActivity {

    private static final String TAG = "AnalysisFeedbackActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference analysisFeedbackRef;

    // Layout variables
    private ActivityAnalysisFeedbackBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<AnalysisFeedback> options;
    private AnalysisFeedbackAdapter adapter;
    private boolean isInitialLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        analysisFeedbackRef = db.collection("analysisFeedback");

        // Get ViewBinding and set content view
        binding = ActivityAnalysisFeedbackBinding.inflate(getLayoutInflater());
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
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    private void loadData() {
        isInitialLoad = true;

        // Query options
        Query query = analysisFeedbackRef
                .orderBy("read", Query.Direction.ASCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("userDisplayName", Query.Direction.ASCENDING);

        options = new FirestoreRecyclerOptions.Builder<AnalysisFeedback>()
                .setQuery(query, AnalysisFeedback.class)
                .setLifecycleOwner(this)
                .build();

        // Create and set adapter
        adapter = new AnalysisFeedbackAdapter(options, this);
        binding.recyclerviewAnalysisFeedback.setAdapter(adapter);

        // Listen for changes to options
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
        binding.appBarAnalysisFeedback.setNavigationOnClickListener(view -> finish());
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
                    adapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery =
                            analysisFeedbackRef.where(Filter.or(
                                            Filter.equalTo("userDisplayName", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("userDisplayName", queryText),
                                                    Filter.lessThanOrEqualTo("userDisplayName", queryText + "\uf8ff")
                                            )
                                    ))
                                    .orderBy("read", Query.Direction.ASCENDING)
                                    .orderBy("userDisplayName", Query.Direction.ASCENDING)
                                    .orderBy("createdAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<AnalysisFeedback> searchOptions =
                            new FirestoreRecyclerOptions.Builder<AnalysisFeedback>()
                                    .setQuery(searchQuery, AnalysisFeedback.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    adapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        binding.layoutNoAnalysisFeedback.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (adapter == null || adapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoAnalysisFeedback.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}