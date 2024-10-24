package com.example.emeowtions.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.common.LoginActivity;
import com.example.emeowtions.activities.common.ProfileActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.databinding.ActivityAdminMainBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.example.emeowtions.fragments.admin.AdminClinicRegistrationsFragment;
import com.example.emeowtions.fragments.admin.AdminClinicsFragment;
import com.example.emeowtions.fragments.admin.AdminDashboardFragment;
import com.example.emeowtions.fragments.admin.AdminUsersFragment;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class AdminMainActivity extends AppCompatActivity {

    private static final String TAG = "AdminMainActivity";
    private ActivityAdminMainBinding binding;
    private Fragment selectedFragment;

    // Firebase variables
    private FirebaseApp adminApp;
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetRegsRef;
    private CollectionReference feedbackRef;
    private CollectionReference analysisFeedbackRef;

    // Public variables
    public AdminDashboardFragment adminDashboardFragment;
    public AdminClinicsFragment adminClinicsFragment;
    public AdminClinicRegistrationsFragment adminClinicRegistrationsFragment;
    public AdminUsersFragment adminUsersFragment;
    public BottomNavigationView adminBottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Admin app instance
        if (!doesAppExist("admin")) {
            adminApp = FirebaseApp.initializeApp(this, FirebaseApp.getInstance().getOptions(), "admin");
        } else {
            adminApp = FirebaseApp.getInstance("admin");
        }

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();

        // Initialize Firestore references
        usersRef = db.collection("users");
        vetRegsRef = db.collection("veterinaryClinicRegistrations");

        // Get ViewBinding and set content view
        binding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            binding.layoutLogout.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        binding.adminBottomNavigation.setOnApplyWindowInsetsListener(null);
        binding.adminBottomNavigation.setPadding(0, 0, 0, 0);

        // Initialize Fragments
        adminDashboardFragment = new AdminDashboardFragment();
        adminClinicsFragment = new AdminClinicsFragment();
        adminClinicRegistrationsFragment = new AdminClinicRegistrationsFragment();
        adminUsersFragment = new AdminUsersFragment();

        createFragment(adminDashboardFragment);
        createFragment(adminClinicRegistrationsFragment);
        createFragment(adminClinicsFragment);
        createFragment(adminUsersFragment);

        selectedFragment = adminDashboardFragment;
        showFragment(selectedFragment);

        // Switch User View dialog
        MaterialAlertDialogBuilder switchUserViewDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.switch_to_user_view)
                        .setMessage(R.string.switch_user_view_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Unused
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // Redirect to UserMainActivity
                            Toast.makeText(this, "Switched to User view.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, UserMainActivity.class));
                        });

        // Logout dialog
        MaterialAlertDialogBuilder logoutDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.log_out)
                        .setMessage(R.string.logout_dialog_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Unused
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // Return to Login screen
                            firebaseAuthUtils.signOut();
                            Toast.makeText(this, "Successfully logged out.", Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, LoginActivity.class));
                        });
        //endregion

        //region Navigation Listeners
        // topAppBar navigationIcon: open navigation drawer
        binding.topAppBar.setNavigationOnClickListener(view -> binding.adminDrawerLayout.open());

        // drawerNavigationView item: redirect to selected screen
        binding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            binding.adminDrawerLayout.close();

            if (itemId == R.id.admin_profile_item) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.admin_general_feedback_item) {
                startActivity(new Intent(this, GeneralFeedbackActivity.class));
            } else if (itemId == R.id.admin_analysis_feedback_item) {
                startActivity(new Intent(this, AnalysisFeedbackActivity.class));
            } else if (itemId == R.id.admin_recommendation_ratings_item) {
                startActivity(new Intent(this, RecommendationRatingsActivity.class));
            }

            return false;
        });

        // topAppBar menu: manage action buttons for each fragment's respective action menu
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            // AdminUsersFragment
            if (itemId == R.id.action_add_user) {
                startActivity(new Intent(this, AddUserActivity.class));
            }

            return false;
        });

        // adminBottomNavigation item: change to selected fragment
        adminBottomNavigation = binding.adminBottomNavigation;
        adminBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            binding.topAppBar.getMenu().clear();

            // Top app bar scaling
            float scale = getResources().getDisplayMetrics().density;
            binding.topAppBar.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

            // No need for fragment replacement in Admin view
            boolean toReplace = false;

            if (itemId == R.id.admin_dashboard_item) {
                binding.topAppBar.setTitle(R.string.dashboard);
                changeFragment(adminDashboardFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_clinics_item) {
                binding.topAppBar.setTitle(R.string.clinics);
                changeFragment(adminClinicsFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_vet_registrations_item) {
                binding.topAppBar.setTitle(R.string.clinic_registrations);
                binding.topAppBar.getLayoutParams().height = (int) (40 * scale + 0.5f);    // Make app bar smaller cause the tabs are chonky
                changeFragment(adminClinicRegistrationsFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_users_item) {
                binding.topAppBar.setTitle(R.string.users);
                binding.topAppBar.inflateMenu(R.menu.top_app_bar_user_management);
                binding.topAppBar.getLayoutParams().height = (int) (40 * scale + 0.5f);    // Make app bar smaller cause the tabs are chonky
                changeFragment(adminUsersFragment, toReplace);
                return true;
            }

            return false;
        });
        //endregion

        //region Load Data
        // Get drawer header views
        View drawerHeader = binding.drawerNavigationView.getHeaderView(0);
        ImageView imgDrawerProfilePicture = drawerHeader.findViewById(R.id.img_profile_picture);
        TextView txtDrawerDisplayName = drawerHeader.findViewById(R.id.txt_drawer_username);
        TextView txtDrawerEmail = drawerHeader.findViewById(R.id.txt_drawer_email);

        // Load user information in drawer header
        usersRef.document(firebaseAuthUtils.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed to listen on current user document changes", error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        User user = value.toObject(User.class);

                        loadProfilePicture(user.getProfilePicture(), imgDrawerProfilePicture);
                        txtDrawerDisplayName.setText(user.getDisplayName());
                        txtDrawerEmail.setText(firebaseAuthUtils.getFirebaseEmail());
                    }
                });

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
                        // Update badge: No pending registrations
                        binding.adminBottomNavigation.removeBadge(R.id.admin_vet_registrations_item);
                    } else {
                        // Update badge: Existing pending registrations
                        binding.adminBottomNavigation.getOrCreateBadge(R.id.admin_vet_registrations_item).setNumber(values.size());
                    }
                });
        
        // Retrieve general Feedback
        feedbackRef.whereEqualTo("read", false)
                .addSnapshotListener((values, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to retrieve unread Feedback", error);
                        return;
                    }
                    // Success
                    if (values.isEmpty()) {
                        // Update badge: No unread Feedback

                    } else {
                        // Update badge: Existing unread Feedback
                    }
                });
        
        // Retrieve AnalysisFeedback
        

        //endregion

        //region onClick Listeners
        // Switch to User view
        binding.txtSwitchUserView.setOnClickListener(view -> {
            switchUserViewDialog.show();
        });

        // Log the user out from account
        binding.txtLogout.setOnClickListener(view -> {
            logoutDialog.show();
        });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // Fragment management helpers
    private void createFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.admin_screen_fragment_container, fragment)
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

    // Loads user's profile picture
    private void loadProfilePicture(String profilePictureUrl, ImageView imageView) {
        if (profilePictureUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_person_24)
                    .into(imageView);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(profilePictureUrl)
                    .into(imageView);
        }
    }

    // Checks if adminApp already exists
    private boolean doesAppExist(String name) {
        for (FirebaseApp app : FirebaseApp.getApps(this)) {
            if (app.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}