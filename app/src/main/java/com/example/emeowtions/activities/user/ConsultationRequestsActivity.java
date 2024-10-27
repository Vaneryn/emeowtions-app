package com.example.emeowtions.activities.user;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityConsultationRequestsBinding;
import com.example.emeowtions.fragments.user.AcceptedConsultRequestsFragment;
import com.example.emeowtions.fragments.user.PendingConsultRequestsFragment;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ConsultationRequestsActivity extends AppCompatActivity {

    private static final String TAG = "ConsultationRequestsActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference chatRequestsRef;

    // Layout variables
    private ActivityConsultationRequestsBinding binding;
    private PendingConsultRequestsFragment pendingFragment;
    private AcceptedConsultRequestsFragment acceptedFragment;
    private Fragment selectedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        chatRequestsRef = db.collection("chatRequests");

        // Get ViewBinding and set content view
        binding = ActivityConsultationRequestsBinding.inflate(getLayoutInflater());
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

    private void setupUi() {
        pendingFragment = new PendingConsultRequestsFragment();
        acceptedFragment = new AcceptedConsultRequestsFragment();

        createFragment(pendingFragment);
        createFragment(acceptedFragment);

        selectedFragment = pendingFragment;
        showFragment(selectedFragment);
    }

    private void loadData() {
        chatRequestsRef
                .whereEqualTo("uid", firebaseAuthUtils.getUid())
                .whereEqualTo("accepted", false)
                .addSnapshotListener((values, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on ChatRequest changes", error);
                        return;
                    }
                    // Success
                    if (values.isEmpty()) {
                        // No pending registrations
                        // Update tab badge
                        binding.tabLayout.getTabAt(0).removeBadge();
                    } else {
                        // Existing pending registrations
                        // Update tab badge
                        binding.tabLayout.getTabAt(0).getOrCreateBadge().setNumber(values.size());
                    }
                });
    }

    private void bindListeners() {
        bindNavigationListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarConsultationRequests.setNavigationOnClickListener(view -> finish());

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();

                // No need for fragment replacement
                boolean toReplace = false;

                if (tabPosition == 0) {
                    changeFragment(pendingFragment, toReplace);
                } else {
                    changeFragment(acceptedFragment, toReplace);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // Fragment management helpers
    private void createFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.consultation_requests_fragment_container, fragment)
                .hide(fragment)
                .commit();
    }

    private void removeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
    }

    private void showFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .show(fragment)
                .commit();
    }

    private void hideFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
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