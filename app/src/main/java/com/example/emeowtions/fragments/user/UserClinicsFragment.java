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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.common.FeedbackActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.adapters.VeterinaryClinicAdapter;
import com.example.emeowtions.databinding.FragmentUserClinicsBinding;
import com.example.emeowtions.models.VeterinaryClinic;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class UserClinicsFragment extends Fragment {

    private static final String TAG = "UserClinicsFragment";

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference clinicsRef;

    // Layout variables
    private FragmentUserClinicsBinding binding;

    // Private variables
    private boolean isInitialLoad;
    private VeterinaryClinicAdapter clinicAdapter;

    public UserClinicsFragment() {
        super(R.layout.fragment_user_clinics);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserClinicsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner lifecycleOwner = this;

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        clinicsRef = db.collection("veterinaryClinics");

        //region UI Setups
        // Hide empty-result views
        binding.layoutNoClinics.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);
        //endregion

        //region Load Data
        isInitialLoad = true;

        // Query options
        Query clinicQuery =
                clinicsRef.whereEqualTo("deleted", false)
                        .orderBy("updatedAt", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<VeterinaryClinic> options =
                new FirestoreRecyclerOptions.Builder<VeterinaryClinic>()
                        .setQuery(clinicQuery, VeterinaryClinic.class)
                        .setLifecycleOwner(lifecycleOwner)
                        .build();

        // Create and set adapter
        clinicAdapter = new VeterinaryClinicAdapter(options, getContext());
        binding.recyclerviewClinics.setAdapter(clinicAdapter);

        // Listen for changes to options
        clinicAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
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
                    clinicAdapter.updateOptions(options);
                } else {
                    isInitialLoad = false;

                    Query searchQuery =
                            clinicsRef.whereEqualTo("deleted", false)
                                    .where(Filter.or(
                                            Filter.equalTo("name", queryText),
                                            Filter.and(
                                                    Filter.greaterThanOrEqualTo("name", queryText),
                                                    Filter.lessThanOrEqualTo("name", queryText + "\uf8ff")
                                            )
                                    ))
                                    .orderBy("updatedAt", Query.Direction.DESCENDING);

                    FirestoreRecyclerOptions<VeterinaryClinic> searchOptions =
                            new FirestoreRecyclerOptions.Builder<VeterinaryClinic>()
                                    .setQuery(searchQuery, VeterinaryClinic.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                    clinicAdapter.updateOptions(searchOptions);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        //endregion

        //region onClick Listeners
        binding.btnSendFeedback.setOnClickListener(view1 -> {
            startActivity(new Intent(getContext(), FeedbackActivity.class));
        });

        binding.btnClearSearch.setOnClickListener(view1 -> {
            binding.edtSearchbar.getText().clear();
        });
        //endregion
    }

    private void updateResultsView(boolean isInitialLoad) {
        // Reset visibility
        binding.layoutNoClinics.setVisibility(View.GONE);
        binding.layoutNoResults.setVisibility(View.GONE);

        // Determine no registrations or no query results
        if (clinicAdapter == null || clinicAdapter.getItemCount() == 0) {
            if (isInitialLoad)
                binding.layoutNoClinics.setVisibility(View.VISIBLE);
            else
                binding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}