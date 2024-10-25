package com.example.emeowtions.activities.veterinary;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.emeowtions.adapters.ChatRequestAdapter;
import com.example.emeowtions.databinding.ActivityClinicInboxBinding;
import com.example.emeowtions.models.ChatRequest;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ClinicInboxActivity extends AppCompatActivity {

    private static final String TAG = "ClinicInboxActivity";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference chatRequestsRef;

    // Layout variables
    private ActivityClinicInboxBinding binding;

    // Private variables
    private String veterinaryClinicId;
    private FirestoreRecyclerOptions<ChatRequest> options;
    private ChatRequestAdapter adapter;
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
        chatRequestsRef = db.collection("chatRequests");

        // Get ViewBinding and set content view
        binding = ActivityClinicInboxBinding.inflate(getLayoutInflater());
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
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    private void setupUi() {

    }

    private void loadData() {
        isInitialLoad = true;

        Query query = chatRequestsRef
                .whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                .orderBy("accepted", Query.Direction.ASCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<ChatRequest>()
                .setQuery(query, ChatRequest.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new ChatRequestAdapter(options, this);

        binding.recyclerviewChatRequests.setAdapter(adapter);

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
        bindTextChangeListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarClinicInbox.setNavigationOnClickListener(view -> finish());
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
                    adapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery = chatRequestsRef
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
                            .orderBy("accepted", Query.Direction.ASCENDING)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .orderBy("updatedAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<ChatRequest> searchOptions =
                            new FirestoreRecyclerOptions.Builder<ChatRequest>()
                                    .setQuery(searchQuery, ChatRequest.class)
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
        binding.layoutNoChatRequests.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (adapter == null || adapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoChatRequests.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}