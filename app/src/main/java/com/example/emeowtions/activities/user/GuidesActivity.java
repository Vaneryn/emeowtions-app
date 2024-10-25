package com.example.emeowtions.activities.user;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityGuidesBinding;
import com.example.emeowtions.fragments.user.GuideBodyLanguageFragment;
import com.example.emeowtions.fragments.user.GuideEmotionAnalysisFragment;
import com.example.emeowtions.fragments.user.GuideRecommendationsFragment;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

public class GuidesActivity extends AppCompatActivity {

    private static final String TAG = "GuidesActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;

    // Layout variables
    private ActivityGuidesBinding binding;
    private GuideEmotionAnalysisFragment emotionFragment;
    private GuideBodyLanguageFragment blFragment;
    private GuideRecommendationsFragment reccFragment;
    private Fragment selectedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();

        // Get ViewBinding and set content view
        binding = ActivityGuidesBinding.inflate(getLayoutInflater());
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
        emotionFragment = new GuideEmotionAnalysisFragment();
        blFragment = new GuideBodyLanguageFragment();
        reccFragment = new GuideRecommendationsFragment();

        createFragment(emotionFragment);
        createFragment(blFragment);
        createFragment(reccFragment);

        selectedFragment = emotionFragment;
        showFragment(selectedFragment);
    }

    private void loadData() {

    }

    private void bindListeners() {
        bindNavigationListeners();
        bindOnClickListeners();
    }

    private void bindNavigationListeners() {
        binding.appBarGuides.setNavigationOnClickListener(view -> finish());

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();

                // No need for fragment replacement
                boolean toReplace = false;

                if (tabPosition == 0) {
                    changeFragment(emotionFragment, toReplace);
                } else if (tabPosition == 1) {
                    changeFragment(blFragment, toReplace);
                } else if (tabPosition == 2) {
                    changeFragment(reccFragment, toReplace);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void bindOnClickListeners() {

    }

    // Fragment management helpers
    private void createFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.guides_fragment_container, fragment)
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