package com.example.emeowtions.fragments.veterinary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.adapters.VetChatRequestAdapter;
import com.example.emeowtions.databinding.FragmentAcceptedChatRequestBinding;
import com.example.emeowtions.models.ChatRequest;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class AcceptedChatRequestFragment extends Fragment {

    private static final String TAG = "AcceptedChatRequestFragment";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference chatRequestsRef;

    // Layout variables
    private FragmentAcceptedChatRequestBinding binding;

    // Private variables
    private String veterinaryClinicId;
    private FirestoreRecyclerOptions<ChatRequest> options;
    private VetChatRequestAdapter adapter;
    private boolean isInitialLoad;

    public AcceptedChatRequestFragment() {
        super(R.layout.fragment_pending_chat_request);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAcceptedChatRequestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Shared preferences
        sharedPreferences = getContext().getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        veterinaryClinicId = sharedPreferences.getString("veterinaryClinicId", null);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        chatRequestsRef = db.collection("chatRequests");

        loadData();
        bindListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    private void loadData() {
        isInitialLoad = true;

        Query query = chatRequestsRef
                .whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                .whereEqualTo("accepted", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<ChatRequest>()
                .setQuery(query, ChatRequest.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new VetChatRequestAdapter(options, getContext());

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
        bindOnClickListeners();
        bindTextChangeListeners();
    }

    private void bindOnClickListeners() {
        binding.btnBack.setOnClickListener(view -> {
            getActivity().finish();
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
                            .whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                            .whereEqualTo("accepted", true)
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