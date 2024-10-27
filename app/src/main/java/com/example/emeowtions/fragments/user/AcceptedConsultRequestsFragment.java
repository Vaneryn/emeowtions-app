package com.example.emeowtions.fragments.user;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.adapters.UserChatRequestAdapter;
import com.example.emeowtions.databinding.FragmentAcceptedConsultRequestsBinding;
import com.example.emeowtions.models.ChatRequest;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class AcceptedConsultRequestsFragment extends Fragment {

    private static final String TAG = "AcceptedConsultRequestsFragment";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference chatRequestsRef;

    // Layout variables
    private FragmentAcceptedConsultRequestsBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<ChatRequest> options;
    private UserChatRequestAdapter adapter;

    public AcceptedConsultRequestsFragment() {
        super(R.layout.fragment_accepted_consult_requests);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAcceptedConsultRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        chatRequestsRef = db.collection("chatRequests");

        loadData();
        bindListeners();
    }

    private void loadData() {
        Query query = chatRequestsRef
                .whereEqualTo("uid", firebaseAuthUtils.getUid())
                .whereEqualTo("accepted", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.DESCENDING);

        options = new FirestoreRecyclerOptions.Builder<ChatRequest>()
                .setQuery(query, ChatRequest.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new UserChatRequestAdapter(options, getContext());

        binding.recyclerviewChatRequests.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateResultsView();
            }
        });
    }

    private void bindListeners() {
        bindOnClickListeners();
    }

    private void bindOnClickListeners() {
        binding.btnBack.setOnClickListener(view -> {
            getActivity().finish();
        });
    }

    private void updateResultsView() {
        // Reset visibility
        binding.layoutNoChatRequests.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (adapter == null || adapter.getItemCount() == 0) {
            binding.layoutNoChatRequests.setVisibility(View.VISIBLE);
        }
    }
}