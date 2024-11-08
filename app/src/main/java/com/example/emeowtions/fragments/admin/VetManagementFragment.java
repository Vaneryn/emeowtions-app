package com.example.emeowtions.fragments.admin;

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
import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.adapters.UserAdapter;
import com.example.emeowtions.databinding.FragmentVetManagementBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class VetManagementFragment extends Fragment {

    private static final String TAG = "VetManagementFragment";

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    // Private variables
    private FragmentVetManagementBinding binding;
    private UserAdapter userAdapter;
    private boolean isInitialLoad;

    public VetManagementFragment() {
        super(R.layout.fragment_vet_management);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVetManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner lifecycleOwner = this;

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        //region UI Setups
        // Hide no results views
        binding.layoutNoVets.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);
        //endregion

        //region Load Data
        isInitialLoad = true;

        // Query options
        Query vetsQuery =
                usersRef.where(Filter.or(
                            Filter.equalTo("role", Role.VETERINARY_STAFF.getTitle()),
                            Filter.equalTo("role", Role.VETERINARIAN.getTitle())
                        ))
                        .whereEqualTo("deleted", false)
                        .orderBy("displayName", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<User> options =
                new FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(vetsQuery, User.class)
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

                    Query searchQuery =
                            usersRef.where(Filter.or(
                                            Filter.equalTo("role", Role.VETERINARY_STAFF.getTitle()),
                                            Filter.equalTo("role", Role.VETERINARIAN.getTitle())
                                    ))
                                    .where(Filter.or(
                                            Filter.equalTo("displayName", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("displayName", queryText),
                                                    Filter.lessThanOrEqualTo("displayName", queryText + "\uf8ff")
                                            )
                                    ))
                                    .whereEqualTo("deleted", false)
                                    .orderBy("displayName", Query.Direction.ASCENDING);

                    FirestoreRecyclerOptions<User> searchOptions =
                            new FirestoreRecyclerOptions.Builder<User>()
                                    .setQuery(searchQuery, User.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    userAdapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion

        //region onClick Listeners
        binding.btnGoToDashboard.setOnClickListener(view1 -> {
            AdminMainActivity parentActivity = (AdminMainActivity) getActivity();
            parentActivity.adminBottomNavigation.setSelectedItemId(R.id.admin_dashboard_item);
            parentActivity.changeFragment(parentActivity.adminDashboardFragment, false);
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