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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
import com.example.emeowtions.fragments.admin.AdminFeedbackFragment;
import com.example.emeowtions.fragments.admin.AdminUsersFragment;
import com.example.emeowtions.fragments.user.EmotionFragment;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminMainActivity extends AppCompatActivity {

    private static final String TAG = "AdminMainActivity";
    private ActivityAdminMainBinding adminMainBinding;
    private Fragment selectedFragment;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetRegsRef;

    // Public variables
    public AdminDashboardFragment adminDashboardFragment;
    public AdminClinicsFragment adminClinicsFragment;
    public AdminClinicRegistrationsFragment adminClinicRegistrationsFragment;
    public AdminUsersFragment adminUsersFragment;
    public AdminFeedbackFragment adminFeedbackFragment;
    public BottomNavigationView adminBottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        vetRegsRef = db.collection("veterinaryClinicRegistrations");

        // Get ViewBinding and set content view
        adminMainBinding = ActivityAdminMainBinding.inflate(getLayoutInflater());
        setContentView(adminMainBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            adminMainBinding.layoutLogout.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        adminMainBinding.adminBottomNavigation.setOnApplyWindowInsetsListener(null);
        adminMainBinding.adminBottomNavigation.setPadding(0, 0, 0, 0);

        // Initialize Fragments
        adminDashboardFragment = new AdminDashboardFragment();
        adminClinicsFragment = new AdminClinicsFragment();
        adminClinicRegistrationsFragment = new AdminClinicRegistrationsFragment();
        adminUsersFragment = new AdminUsersFragment();
        adminFeedbackFragment = new AdminFeedbackFragment();

        createFragment(adminDashboardFragment);
        createFragment(adminClinicRegistrationsFragment);
        createFragment(adminClinicsFragment);
        createFragment(adminUsersFragment);
        createFragment(adminFeedbackFragment);

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
        adminMainBinding.topAppBar.setNavigationOnClickListener(view -> adminMainBinding.adminDrawerLayout.open());

        // drawerNavigationView item: redirect to selected screen
        adminMainBinding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            adminMainBinding.adminDrawerLayout.close();

            if (itemId == R.id.admin_profile_item) {
                startActivity(new Intent(this, ProfileActivity.class));
            }

            return false;
        });

        // adminBottomNavigation item: change to selected fragment
        adminBottomNavigation = adminMainBinding.adminBottomNavigation;
        adminBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            adminMainBinding.topAppBar.getMenu().clear();

            // Top app bar sizing
            float scale = getResources().getDisplayMetrics().density;
            adminMainBinding.topAppBar.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

            // No need for fragment replacement in Admin view
            boolean toReplace = false;

            if (itemId == R.id.admin_dashboard_item) {
                adminMainBinding.topAppBar.setTitle(R.string.dashboard);
                changeFragment(adminDashboardFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_clinics_item) {
                adminMainBinding.topAppBar.setTitle(R.string.clinics);
                changeFragment(adminClinicsFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_vet_registrations_item) {
                adminMainBinding.topAppBar.setTitle(R.string.clinic_registrations);
                adminMainBinding.topAppBar.getLayoutParams().height = (int) (40 * scale + 0.5f);    // Make app bar smaller cause the tabs are chonky
                changeFragment(adminClinicRegistrationsFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_users_item) {
                adminMainBinding.topAppBar.setTitle(R.string.users);
                changeFragment(adminUsersFragment, toReplace);
                return true;
            } else if (itemId == R.id.admin_feedback_item) {
                adminMainBinding.topAppBar.setTitle(R.string.feedback);
                changeFragment(adminFeedbackFragment, toReplace);
                return true;
            }

            return false;
        });
        //endregion

        //region Load Data
        // Get drawer header views
        View drawerHeader = adminMainBinding.drawerNavigationView.getHeaderView(0);
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
                    }
                    // Success
                    if (values.isEmpty()) {
                        // No pending registrations
                        // Update tab badge
                        adminMainBinding.adminBottomNavigation.removeBadge(R.id.admin_vet_registrations_item);
                    } else {
                        // Existing pending registrations
                        // Update tab badge
                        adminMainBinding.adminBottomNavigation.getOrCreateBadge(R.id.admin_vet_registrations_item).setNumber(values.size());
                    }
                });

        //endregion

        //region onClick Listeners
        // Switch to User view
        adminMainBinding.txtSwitchUserView.setOnClickListener(view -> {
            switchUserViewDialog.show();
        });

        // Log the user out from account
        adminMainBinding.txtLogout.setOnClickListener(view -> {
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
}