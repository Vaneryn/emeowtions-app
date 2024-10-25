package com.example.emeowtions.activities.user;

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
import com.example.emeowtions.adapters.AnalysisAdapter;
import com.example.emeowtions.databinding.ActivitySavedAnalysesBinding;
import com.example.emeowtions.models.Analysis;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class SavedAnalysesActivity extends AppCompatActivity {

    private static final String TAG = "SavedAnalysesActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference analysesRef;

    // Layout variables
    private ActivitySavedAnalysesBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<Analysis> options;
    private AnalysisAdapter analysisAdapter;
    private boolean isInitialLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        analysesRef = db.collection("analyses");

        // Get ViewBinding and set content view
        binding = ActivitySavedAnalysesBinding.inflate(getLayoutInflater());
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
        if (analysisAdapter != null)
            analysisAdapter.notifyDataSetChanged();
    }

    private void loadData() {
        isInitialLoad = true;

        // Query options
        Query analysesQuery = analysesRef
                .whereEqualTo("uid", firebaseAuthUtils.getUid())
                .whereEqualTo("deleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<Analysis>()
                .setQuery(analysesQuery, Analysis.class)
                .setLifecycleOwner(this)
                .build();

        // Create and set adapter
        analysisAdapter = new AnalysisAdapter(options, this);
        binding.recyclerviewAnalyses.setAdapter(analysisAdapter);

        // Listen for changes to options
        analysisAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
        binding.appBarSavedAnalyses.setNavigationOnClickListener(view -> finish());
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
                    analysisAdapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery =
                            analysesRef.whereEqualTo("uid", firebaseAuthUtils.getUid())
                                    .whereEqualTo("deleted", false)
                                    .where(Filter.or(
                                            Filter.equalTo("catName", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("catName", queryText),
                                                    Filter.lessThanOrEqualTo("catName", queryText + "\uf8ff")
                                            )
                                    ))
                                    .orderBy("createdAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<Analysis> searchOptions =
                            new FirestoreRecyclerOptions.Builder<Analysis>()
                                    .setQuery(searchQuery, Analysis.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    analysisAdapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        binding.layoutNoAnalyses.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (analysisAdapter == null || analysisAdapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoAnalyses.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}