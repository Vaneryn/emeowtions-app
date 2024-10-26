package com.example.emeowtions.fragments.user;

import android.content.Intent;
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
import com.example.emeowtions.activities.user.ConsultationRequestsActivity;
import com.example.emeowtions.adapters.ChatAdapter;
import com.example.emeowtions.databinding.FragmentUserChatListBinding;
import com.example.emeowtions.models.Chat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;


public class UserChatListFragment extends Fragment {

    private static final String TAG = "UserChatListFragment";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference chatsRef;

    // Layout variables
    private FragmentUserChatListBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<Chat> options;
    private ChatAdapter adapter;
    private boolean isInitialLoad;

    public UserChatListFragment() {
        super(R.layout.fragment_user_chat_list);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserChatListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        chatsRef = db.collection("chats");

        setupUi();
        loadData();
        bindListeners();
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

        LifecycleOwner lifecycleOwner = this;

        // Query options
        Query query =
                chatsRef.whereEqualTo("userId", firebaseAuthUtils.getUid())
                        .orderBy("readByUser", Query.Direction.ASCENDING)
                        .orderBy("updatedAt", Query.Direction.DESCENDING)
                        .orderBy("vetDisplayName", Query.Direction.ASCENDING);

        options = new FirestoreRecyclerOptions.Builder<Chat>()
                .setQuery(query, Chat.class)
                .setLifecycleOwner(lifecycleOwner)
                .build();

        // Create and set adapter
        adapter = new ChatAdapter(options, getContext());
        binding.recyclerviewChats.setAdapter(adapter);

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
        bindOnClickListeners();
        bindTextChangeListeners();
    }

    private void bindOnClickListeners() {
        binding.btnViewConsultationRequests.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), ConsultationRequestsActivity.class));
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

                    Query searchQuery =
                            chatsRef.whereEqualTo("userId", firebaseAuthUtils.getUid())
                                    .where(Filter.or(
                                            Filter.equalTo("vetDisplayName", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("vetDisplayName", queryText),
                                                    Filter.lessThanOrEqualTo("vetDisplayName", queryText + "\uf8ff")
                                            )
                                    ))
                                    .orderBy("readByUser", Query.Direction.ASCENDING)
                                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                                    .orderBy("vetDisplayName", Query.Direction.ASCENDING);

                    FirestoreRecyclerOptions<Chat> searchOptions =
                            new FirestoreRecyclerOptions.Builder<Chat>()
                                    .setQuery(searchQuery, Chat.class)
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
        binding.layoutNoChats.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (adapter == null || adapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoChats.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}