package com.example.emeowtions.activities.veterinary;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.example.emeowtions.databinding.ActivityVetMainBinding;
import com.example.emeowtions.fragments.veterinary.VetChatFragment;
import com.example.emeowtions.fragments.veterinary.VetClinicProfileFragment;
import com.example.emeowtions.fragments.veterinary.VetDashboardFragment;
import com.example.emeowtions.fragments.veterinary.VetStaffFragment;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class VetMainActivity extends AppCompatActivity {

    private static final String TAG = "VetMainActivity";
    private ActivityVetMainBinding vetMainBinding;
    private Fragment selectedFragment;

    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Get ViewBinding and set content view
        vetMainBinding = ActivityVetMainBinding.inflate(getLayoutInflater());
        setContentView(vetMainBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            vetMainBinding.layoutLogout.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        vetMainBinding.vetBottomNavigation.setOnApplyWindowInsetsListener(null);
        vetMainBinding.vetBottomNavigation.setPadding(0, 0, 0, 0);

        // Initialize Fragments
        VetDashboardFragment vetDashboardFragment = new VetDashboardFragment();
        VetStaffFragment vetStaffFragment = new VetStaffFragment();
        VetChatFragment vetChatFragment = new VetChatFragment();
        VetClinicProfileFragment vetClinicProfileFragment = new VetClinicProfileFragment();

        createFragment(vetDashboardFragment);
        createFragment(vetStaffFragment);
        createFragment(vetChatFragment);
        createFragment(vetClinicProfileFragment);

        selectedFragment = vetDashboardFragment;
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
        vetMainBinding.topAppBar.setNavigationOnClickListener(view -> vetMainBinding.vetDrawerLayout.open());

        // drawerNavigationView item: redirect to selected screen
        vetMainBinding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            vetMainBinding.vetDrawerLayout.close();

            if (itemId == R.id.vet_profile_item) {
                startActivity(new Intent(this, ProfileActivity.class));
            }

            return false;
        });

        // vetBottomNavigation item: change to selected fragment
        vetMainBinding.vetBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            vetMainBinding.topAppBar.getMenu().clear();

            // No need for fragment replacement in Admin view
            boolean toReplace = false;

            if (itemId == R.id.vet_dashboard_item) {
                vetMainBinding.topAppBar.setTitle(R.string.dashboard);
                changeFragment(vetDashboardFragment, toReplace);
                return true;
            } else if (itemId == R.id.vet_staff_item) {
                vetMainBinding.topAppBar.setTitle(R.string.staff);
                changeFragment(vetStaffFragment, toReplace);
                return true;
            } else if (itemId == R.id.vet_chat_item) {
                vetMainBinding.topAppBar.setTitle(R.string.chat);
                changeFragment(vetChatFragment, toReplace);
                return true;
            } else if (itemId == R.id.vet_clinic_profile_item) {
                vetMainBinding.topAppBar.setTitle(R.string.clinic_profile);
                changeFragment(vetClinicProfileFragment, toReplace);
                return true;
            }

            return false;
        });
        //endregion

        //region Load Data
        // Get drawer header views
        View drawerHeader = vetMainBinding.drawerNavigationView.getHeaderView(0);
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
        //endregion

        //region onClick Listeners
        // Switch to User view
        vetMainBinding.txtSwitchUserView.setOnClickListener(view -> {
            switchUserViewDialog.show();
        });

        // Log the user out from account
        vetMainBinding.txtLogout.setOnClickListener(view -> {
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
                .add(R.id.vet_screen_fragment_container, fragment)
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

    private void changeFragment(Fragment fragment, boolean replace) {
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