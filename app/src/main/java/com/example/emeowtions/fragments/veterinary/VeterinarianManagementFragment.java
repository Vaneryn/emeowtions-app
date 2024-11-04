package com.example.emeowtions.fragments.veterinary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.veterinary.AddStaffActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.adapters.UserAdapter;
import com.example.emeowtions.databinding.FragmentVeterinarianManagementBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class VeterinarianManagementFragment extends Fragment {

    private static final String TAG = "VeterinarianManagementFragment";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetsRef;

    // Layout variables
    private FragmentVeterinarianManagementBinding binding;
    private UserAdapter userAdapter;

    // Private variables
    private boolean isInitialLoad;
    private String veterinaryClinicId;
    private List<String> vetUidList;
    private FirestoreRecyclerOptions<User> options;

    public VeterinarianManagementFragment() {
        super(R.layout.fragment_veterinarian_management);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVeterinarianManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner lifecycleOwner = this;

        // Shared preferences
        sharedPreferences = getContext().getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        veterinaryClinicId = sharedPreferences.getString("veterinaryClinicId", null);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        vetsRef = db.collection("veterinarians");

        //region UI Setups
        // Hide no results views
        binding.layoutNoVets.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);
        //endregion

        //region Load Data
        isInitialLoad = true;

        // Query veterinarian collection to get UIDs for the current clinic
        vetsRef.whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Store list of vet UIDs
                    vetUidList = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        String uid = documentSnapshot.getString("uid");
                        if (uid != null)
                            vetUidList.add(uid);
                    }

                    // Query Users whose UID match the retrieved vet UIDs
                    if (!vetUidList.isEmpty()) {
                        Query usersQuery = usersRef
                                .whereIn(FieldPath.documentId(), vetUidList)
                                .whereEqualTo("role", Role.VETERINARIAN.getTitle())
                                .whereEqualTo("deleted", false)
                                .orderBy("displayName", Query.Direction.ASCENDING);

                        options = new FirestoreRecyclerOptions.Builder<User>()
                                .setQuery(usersQuery, User.class)
                                .setLifecycleOwner(lifecycleOwner)
                                .build();

                        // Create and set adapter
                        userAdapter = new UserAdapter(options, getContext());
                        binding.recyclerviewVets.setAdapter(userAdapter);

                        // Listen for changes to options
                        userAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                            @Override
                            public void onChanged() {
                                super.onChanged();
                                updateResultsView(isInitialLoad);
                            }
                        });
                    } else {
                        updateResultsView(isInitialLoad);
                    }
                });

        // Dynamic search
        binding.edtSearchbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String queryText = binding.edtSearchbar.getText().toString();

                if (queryText.isBlank()) {
                    isInitialLoad = true;
                    userAdapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    if (!vetUidList.isEmpty()) {
                        Query searchQuery = usersRef
                                .whereIn(FieldPath.documentId(), vetUidList)
                                .whereEqualTo("role", Role.VETERINARIAN.getTitle())
                                .whereEqualTo("deleted", false)
                                .where(Filter.or(
                                        Filter.equalTo("displayName", queryText),
                                        Filter.and(
                                                Filter.greaterThanOrEqualTo("displayName", queryText),
                                                Filter.lessThanOrEqualTo("displayName", queryText + "\uf8ff")
                                        )
                                ))
                                .orderBy("displayName", Query.Direction.ASCENDING);

                        FirestoreRecyclerOptions<User> searchOptions =
                                new FirestoreRecyclerOptions.Builder<User>()
                                        .setQuery(searchQuery, User.class)
                                        .setLifecycleOwner(lifecycleOwner)
                                        .build();

                        userAdapter.updateOptions(searchOptions);
                    } else {
                        updateResultsView(isInitialLoad);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion

        //region onClick Listeners
        binding.btnAddStaff.setOnClickListener(view1 -> {
            startActivity(new Intent(getContext(), AddStaffActivity.class));
        });

        binding.btnClearSearch.setOnClickListener(view1 -> {
            binding.edtSearchbar.getText().clear();
        });
        //endregion
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userAdapter != null)
            userAdapter.notifyDataSetChanged();
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        binding.layoutNoVets.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no existing items or no query results
        if (userAdapter == null || userAdapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoVets.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}