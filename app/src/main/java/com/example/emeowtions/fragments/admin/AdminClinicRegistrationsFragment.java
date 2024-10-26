package com.example.emeowtions.fragments.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentAdminClinicRegistrationsBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminClinicRegistrationsFragment extends Fragment {

    private static final String TAG = "AdminClinicRegistrationsFragment";

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference vetRegsRef;

    // Private variables
    private FragmentAdminClinicRegistrationsBinding clinicRegsBinding;
    private Fragment selectedFragment;
    private PendingClinicRegistrationsFragment pendingFragment;
    private ApprovedClinicRegistrationsFragment approvedFragment;
    private RejectedClinicRegistrationsFragment rejectedFragment;

    public AdminClinicRegistrationsFragment() {
        super(R.layout.fragment_admin_clinic_registrations);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        clinicRegsBinding = FragmentAdminClinicRegistrationsBinding.inflate(inflater, container, false);
        return clinicRegsBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        vetRegsRef = db.collection("veterinaryClinicRegistrations");

        //region UI Setups
        // Initialize Fragments
        pendingFragment = new PendingClinicRegistrationsFragment();
        approvedFragment = new ApprovedClinicRegistrationsFragment();
        rejectedFragment = new RejectedClinicRegistrationsFragment();

        createFragment(pendingFragment);
        createFragment(approvedFragment);
        createFragment(rejectedFragment);

        selectedFragment = pendingFragment;
        showFragment(selectedFragment);
        //endregion

        //region Navigation Listeners
        clinicRegsBinding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();

                // No need for fragment replacement
                boolean toReplace = false;

                if (tabPosition == 0) {
                    changeFragment(pendingFragment, toReplace);
                } else if (tabPosition == 1) {
                    changeFragment(approvedFragment, toReplace);
                } else if (tabPosition == 2) {
                    changeFragment(rejectedFragment, toReplace);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        //endregion

        //region Load Data
        // Retrieve pending registrations
        vetRegsRef.whereEqualTo("status", VeterinaryClinicRegistrationStatus.PENDING.getTitle())
                .addSnapshotListener((values, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "onViewCreated: Failed to retrieve pending veterinaryClinicRegistrations", error);
                return;
            }
            // Success
            if (values.isEmpty()) {
                // No pending registrations
                // Update tab badge
                clinicRegsBinding.tabLayout.getTabAt(0).removeBadge();
            } else {
                // Existing pending registrations
                // Update tab badge
                clinicRegsBinding.tabLayout.getTabAt(0).getOrCreateBadge().setNumber(values.size());
            }
        });
        //endregion
    }

    // Fragment management helpers
    private void createFragment(Fragment fragment){
        getChildFragmentManager().beginTransaction()
                .add(R.id.regs_fragment_container, fragment)
                .hide(fragment)
                .commit();
    }

    private void removeFragment(Fragment fragment) {
        getChildFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
    }

    private void showFragment(Fragment fragment){
        getChildFragmentManager().beginTransaction()
                .show(fragment)
                .commit();
    }

    private void hideFragment(Fragment fragment){
        getChildFragmentManager().beginTransaction()
                .hide(fragment)
                .commit();
    }

    public void changeFragment(Fragment fragment, boolean replace) {
        hideFragment(selectedFragment);

        if (replace) {
            removeFragment(selectedFragment);
        }

        selectedFragment = fragment;
        showFragment(fragment);
    }
}