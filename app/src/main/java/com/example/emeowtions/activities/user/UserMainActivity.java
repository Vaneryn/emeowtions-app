package com.example.emeowtions.activities.user;

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
import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.activities.common.FeedbackActivity;
import com.example.emeowtions.activities.common.LoginActivity;
import com.example.emeowtions.activities.common.ProfileActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.databinding.ActivityUserMainBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.fragments.user.EmotionFragment;
import com.example.emeowtions.fragments.user.UserChatListFragment;
import com.example.emeowtions.fragments.user.UserClinicsFragment;
import com.example.emeowtions.fragments.user.UserHomeFragment;
import com.example.emeowtions.fragments.user.UserMyCatsFragment;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserMainActivity extends AppCompatActivity {

    private static final String TAG = "UserMainActivity";
    private ActivityUserMainBinding userMainBinding;
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
        userMainBinding = ActivityUserMainBinding.inflate(getLayoutInflater());
        setContentView(userMainBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            userMainBinding.layoutLogout.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        //region UI setups
        // Fix bottom navigation broken padding
        userMainBinding.userBottomNavigation.setOnApplyWindowInsetsListener(null);
        userMainBinding.userBottomNavigation.setPadding(0,0,0,0);

        // Initialize Fragments
        UserHomeFragment userHomeFragment = new UserHomeFragment();
        UserClinicsFragment userClinicsFragment = new UserClinicsFragment();
        UserChatListFragment userChatListFragment = new UserChatListFragment();
        UserMyCatsFragment userMyCatsFragment = new UserMyCatsFragment();

        createFragment(userHomeFragment);
        createFragment(userClinicsFragment);
        createFragment(userChatListFragment);
        createFragment(userMyCatsFragment);

        selectedFragment = userHomeFragment;
        showFragment(selectedFragment);

        // Switch Admin View dialog
        MaterialAlertDialogBuilder switchAdminViewDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.switch_to_admin_view)
                        .setMessage(R.string.switch_admin_view_message)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Unused
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // Return to AdminMainActivity
                            Toast.makeText(this, "Switched to Admin view.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, AdminMainActivity.class));
                        });

        // Switch Vet View dialog
        MaterialAlertDialogBuilder switchVetViewDialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.switch_to_vet_view)
                        .setMessage(R.string.are_you_sure_you_want_to_switch_to_vet_view)
                        .setNegativeButton(getString(R.string.no), (dialogInterface, i) -> {
                            // Unused
                        })
                        .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                            // Redirect to VetMainActivity
                            Toast.makeText(this, "Switched to Vet view.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, VetMainActivity.class));
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

        //region onClick listeners
        // topAppBar navigationIcon: open navigation drawer
        userMainBinding.topAppBar.setNavigationOnClickListener(view -> userMainBinding.drawerLayout.open());

        // txtSwitchAdminView: switch to Admin view
        userMainBinding.txtSwitchAdminView.setOnClickListener(view -> {
            switchAdminViewDialog.show();
        });

        // txtSwitchVetView: switch to Vet view
        userMainBinding.txtSwitchVetView.setOnClickListener(view -> {
            switchVetViewDialog.show();
        });

        // txtLogout: sign out and redirect to login screen
        userMainBinding.txtLogout.setOnClickListener(view -> {
            logoutDialog.show();
        });
        //endregion

        //region Navigation listeners
        // drawerNavigationView item: redirect to selected screen
        userMainBinding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            userMainBinding.drawerLayout.close();

            if (itemId == R.id.profile_item) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.saved_analyses_item) {
                startActivity(new Intent(this, SavedAnalysesActivity.class));
            }  else if (itemId == R.id.guide_item) {
                startActivity(new Intent(this, GuidesActivity.class));
            } else if (itemId == R.id.feedback_item) {
                startActivity(new Intent(this, FeedbackActivity.class));
            }

            return false;
        });

        // topAppBar menu: manage action buttons for each fragment's respective action menu
        userMainBinding.topAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.consultation_requests) {
                // UserChatListFragment
                startActivity(new Intent(this, ConsultationRequestsActivity.class));
            } else if (itemId == R.id.action_add_cat) {
                // UserMyCatsFragment
                startActivity(new Intent(this, AddCatActivity.class));
            }

            return false;
        });

        // userBottomNavigation item: change to selected fragment
        userMainBinding.userBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            userMainBinding.topAppBar.getMenu().clear();

            // If current fragment is EmotionFragment, replace it to destroy it and prevent object detector from running in background
            boolean toReplace = selectedFragment.getClass() == EmotionFragment.class;

            if (itemId == R.id.home_item) {
                userMainBinding.topAppBar.setTitle(R.string.home);
                changeFragment(userHomeFragment, toReplace);
                return true;
            } else if (itemId == R.id.emotions_item) {
                userMainBinding.topAppBar.setTitle(R.string.emotions);
                EmotionFragment emotionFragment = new EmotionFragment();
                createFragment(emotionFragment);
                changeFragment(emotionFragment, toReplace);
                return true;
            } else if (itemId == R.id.clinics_item) {
                userMainBinding.topAppBar.setTitle(R.string.clinics);
                changeFragment(userClinicsFragment, toReplace);
                return true;
            } else if (itemId == R.id.chat_item) {
                userMainBinding.topAppBar.setTitle(R.string.chat);
                userMainBinding.topAppBar.inflateMenu(R.menu.top_app_bar_user_chat);
                changeFragment(userChatListFragment, toReplace);
                return true;
            } else if (itemId == R.id.my_cats_item) {
                userMainBinding.topAppBar.setTitle(R.string.my_cats);
                userMainBinding.topAppBar.inflateMenu(R.menu.top_app_bar_mycats);
                changeFragment(userMyCatsFragment, toReplace);
                return true;
            }

            return false;
        });

        userMainBinding.userBottomNavigation.setOnItemReselectedListener(item -> {
            // Prevent replacing fragment on reselect
        });
        //endregion

        //region Load data
        // Get drawer header views
        View drawerHeader = userMainBinding.drawerNavigationView.getHeaderView(0);
        ImageView imgDrawerProfilePicture = drawerHeader.findViewById(R.id.img_profile_picture);
        TextView txtDrawerDisplayName = drawerHeader.findViewById(R.id.txt_drawer_username);
        TextView txtDrawerEmail = drawerHeader.findViewById(R.id.txt_drawer_email);

        // Load user information in drawer header / View switching management
        usersRef.document(firebaseAuthUtils.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed to listen on current user document changes", error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        User user = value.toObject(User.class);

                        // Load user information
                        loadProfilePicture(user.getProfilePicture(), imgDrawerProfilePicture);
                        txtDrawerDisplayName.setText(user.getDisplayName());
                        txtDrawerEmail.setText(firebaseAuthUtils.getFirebaseEmail());

                        // Only show view switcher to authorized roles
                        String role = user.getRole();
                        if (role.equals(Role.SUPER_ADMIN.getTitle()) || role.equals(Role.ADMIN.getTitle())) {
                            userMainBinding.txtSwitchAdminView.setVisibility(View.VISIBLE);
                        } else if (role.equals(Role.VETERINARY_STAFF.getTitle()) || role.equals(Role.VETERINARIAN.getTitle())) {
                            userMainBinding.txtSwitchVetView.setVisibility(View.VISIBLE);
                        }
                    }
                });
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);

        // Check if email is verified
        usersRef.document(firebaseAuthUtils.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Failed to listen on current user document changes for verification status", error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        User user = value.toObject(User.class);

                        if (!user.isVerified()) {
                            MaterialAlertDialogBuilder verifyEmailDialog =
                                    new MaterialAlertDialogBuilder(this)
                                            .setTitle(getString(R.string.email_not_verified))
                                            .setMessage(user.getEmail().equals(firebaseAuthUtils.getFirebaseEmail()) ?
                                                    getString(R.string.email_not_verified_message, firebaseAuthUtils.getFirebaseEmail()) :
                                                    getString(R.string.new_email_not_verified_message, user.getEmail()))
                                            .setOnDismissListener(dialogInterface -> {
                                                // Dialog dismissed: redirect to profile
                                                startActivity(new Intent(this, ProfileActivity.class));
                                            })
                                            .setNeutralButton("Resend link", (dialogInterface, i) -> {
                                                // Resend verification email
                                                firebaseAuthUtils.updateEmail(this, user.getEmail());
                                            })
                                            .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                                                // Dialog acknowledged: redirect to profile
                                            });
                            verifyEmailDialog.show();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseAuthUtils.refreshVerification(this);
    }

    private void createFragment(Fragment fragment){
        getSupportFragmentManager().beginTransaction()
                .add(R.id.userScreenFragmentContainer, fragment)
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