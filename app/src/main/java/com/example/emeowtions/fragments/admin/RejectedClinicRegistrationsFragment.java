package com.example.emeowtions.fragments.admin;

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
import android.widget.AutoCompleteTextView;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.adapters.VeterinaryClinicRegistrationAdapter;
import com.example.emeowtions.databinding.FragmentRejectedClinicRegistrationsBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.example.emeowtions.models.VeterinaryClinicRegistration;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class RejectedClinicRegistrationsFragment extends Fragment {

    private static final String TAG = "RejectedClinicRegistrationsFragment";

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference vetRegsRef;

    // Private variables
    private FragmentRejectedClinicRegistrationsBinding rejectedBinding;
    private VeterinaryClinicRegistrationAdapter vetRegAdapter;
    private boolean isInitialLoad;

    public RejectedClinicRegistrationsFragment() {
        super(R.layout.fragment_rejected_clinic_registrations);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rejectedBinding = FragmentRejectedClinicRegistrationsBinding.inflate(inflater, container, false);
        return rejectedBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner lifecycleOwner = this;

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        vetRegsRef = db.collection("veterinaryClinicRegistrations");

        //region UI Setups
        // Hide no results views
        rejectedBinding.layoutNoRegistrations.setVisibility(View.GONE);
        rejectedBinding.layoutNoResults.setVisibility(View.GONE);
        //endregion

        //region Load Data
        isInitialLoad = true;

        // Query options
        Query rejectedDescQuey =
                vetRegsRef.whereEqualTo("status", VeterinaryClinicRegistrationStatus.REJECTED.getTitle())
                        .orderBy("updatedAt", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<VeterinaryClinicRegistration> options =
                new FirestoreRecyclerOptions.Builder<VeterinaryClinicRegistration>()
                        .setQuery(rejectedDescQuey, VeterinaryClinicRegistration.class)
                        .setLifecycleOwner(lifecycleOwner)
                        .build();

        // Create and set adapter
        vetRegAdapter = new VeterinaryClinicRegistrationAdapter(options, getContext());
        rejectedBinding.recyclerviewClinicRegistrations.setAdapter(vetRegAdapter);

        // Listen for changes to options
        vetRegAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateResultsView(isInitialLoad);
            }
        });

        // Only allow searching if there are existing registrations
        rejectedBinding.edtSearchbar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String queryText = rejectedBinding.edtSearchbar.getText().toString();

                if (queryText.isBlank()) {
                    isInitialLoad = true;
                    vetRegAdapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery =
                            vetRegsRef.whereEqualTo("status", VeterinaryClinicRegistrationStatus.REJECTED.getTitle())
                                    .where(Filter.or(
                                            Filter.equalTo("name", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("name", queryText),
                                                    Filter.lessThanOrEqualTo("name", queryText + "\uf8ff")
                                            )
                                    ))
                                    .orderBy("updatedAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<VeterinaryClinicRegistration> searchOptions =
                            new FirestoreRecyclerOptions.Builder<VeterinaryClinicRegistration>()
                                    .setQuery(searchQuery, VeterinaryClinicRegistration.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    vetRegAdapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        //endregion

        //region onClick Listeners
        rejectedBinding.btnSearchOptions.setOnClickListener(view1 -> {
            View searchOptionsDialogLayout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_vet_reg_search_options, null);
            AutoCompleteTextView edmSort = searchOptionsDialogLayout.findViewById(R.id.edm_sort);

            MaterialAlertDialogBuilder sortDialog =
                    new MaterialAlertDialogBuilder(getContext())
                            .setView(searchOptionsDialogLayout)
                            .setTitle(R.string.search_options)
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                                // Do nothing
                            })
                            .setPositiveButton(R.string.confirm, (dialogInterface, i) -> {
                                String sortOption = edmSort.getText().toString();
                                // TODO: if time permits
                            });
            sortDialog.show();
        });

        rejectedBinding.btnGoToDashboard.setOnClickListener(view1 -> {
            AdminMainActivity parentActivity = (AdminMainActivity) getActivity();
            parentActivity.adminBottomNavigation.setSelectedItemId(R.id.admin_dashboard_item);
            parentActivity.changeFragment(parentActivity.adminDashboardFragment, false);
        });

        rejectedBinding.btnClearSearch.setOnClickListener(view1 -> {
            rejectedBinding.edtSearchbar.getText().clear();
        });
        //endregion
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vetRegAdapter != null)
            vetRegAdapter.notifyDataSetChanged();
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        rejectedBinding.layoutNoRegistrations.setVisibility(View.GONE);
        rejectedBinding.layoutNoResults.setVisibility(View.GONE);

        // Determine no registrations or no query results
        if (vetRegAdapter == null || vetRegAdapter.getItemCount() == 0) {
            if (isInitialLoad)
                rejectedBinding.layoutNoRegistrations.setVisibility(View.VISIBLE);
            else
                rejectedBinding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}