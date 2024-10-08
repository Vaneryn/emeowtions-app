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
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.common.FeedbackActivity;
import com.example.emeowtions.activities.common.LoginActivity;
import com.example.emeowtions.activities.common.ProfileActivity;
import com.example.emeowtions.databinding.ActivityUserMainBinding;
import com.example.emeowtions.fragments.user.EmotionFragment;
import com.example.emeowtions.fragments.user.UserChatListFragment;
import com.example.emeowtions.fragments.user.UserClinicsFragment;
import com.example.emeowtions.fragments.user.UserHomeFragment;
import com.example.emeowtions.fragments.user.UserMyCatsFragment;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserMainActivity extends AppCompatActivity {

    private static final String TAG = "UserMainActivity";
    private ActivityUserMainBinding userMainBinding;

    private FirebaseAuth mAuth;
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        mAuth = FirebaseAuth.getInstance();
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");

        // Get ViewBinding and set content view
        userMainBinding = ActivityUserMainBinding.inflate(getLayoutInflater());
        setContentView(userMainBinding.getRoot());
        // Get FragmentManager
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            userMainBinding.layoutLogout.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // Fix bottom navigation broken padding
        userMainBinding.userBottomNavigation.setOnApplyWindowInsetsListener(null);
        userMainBinding.userBottomNavigation.setPadding(0,0,0,0);

        //region onClick listeners
        // topAppBar navigationIcon: open navigation drawer
        userMainBinding.topAppBar.setNavigationOnClickListener(view -> userMainBinding.drawerLayout.open());

        // txtLogout: sign out and redirect to login screen
        userMainBinding.txtLogout.setOnClickListener(view -> {
            mAuth.signOut();
            Toast.makeText(this, "Successfully signed out.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
        });
        //endregion

        //region Navigation listeners
        // drawerNavigationView item: redirect to selected screen
        userMainBinding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            userMainBinding.drawerLayout.close();

            if (itemId == R.id.profile_item) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.analysis_history_item) {
                startActivity(new Intent(this, AnalysisHistoryActivity.class));
            } else if (itemId == R.id.feedback_item) {
                startActivity(new Intent(this, FeedbackActivity.class));
            }

            return false;
        });

        // topAppBar menu: manage action buttons for each fragment's respective action menu
        userMainBinding.topAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            // UserMyCatsFragment
            if (itemId == R.id.action_add_cat) {
                startActivity(new Intent(UserMainActivity.this, AddCatActivity.class));
            }

            return false;
        });

        // userBottomNavigation item: change to selected fragment
        userMainBinding.userBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            userMainBinding.topAppBar.getMenu().clear();

            if (itemId == R.id.home_item) {
                userMainBinding.topAppBar.setTitle(R.string.home);
                fragmentManager.beginTransaction().replace(R.id.userScreenFragmentContainer, UserHomeFragment.class, null).commit();
                return true;
            } else if (itemId == R.id.emotions_item) {
                // TODO: Crashes if you leave this fragment
                userMainBinding.topAppBar.setTitle(R.string.emotions);
                fragmentManager.beginTransaction().replace(R.id.userScreenFragmentContainer, EmotionFragment.class, null).commit();
                return true;
            } else if (itemId == R.id.clinics_item) {
                userMainBinding.topAppBar.setTitle(R.string.clinics);
                fragmentManager.beginTransaction().replace(R.id.userScreenFragmentContainer, UserClinicsFragment.class, null).commit();
                return true;
            } else if (itemId == R.id.chat_item) {
                userMainBinding.topAppBar.setTitle(R.string.chat);
                fragmentManager.beginTransaction().replace(R.id.userScreenFragmentContainer, UserChatListFragment.class, null).commit();
                return true;
            } else if (itemId == R.id.my_cats_item) {
                userMainBinding.topAppBar.setTitle(R.string.my_cats);
                userMainBinding.topAppBar.inflateMenu(R.menu.top_app_bar_mycats);
                fragmentManager.beginTransaction().replace(R.id.userScreenFragmentContainer, UserMyCatsFragment.class, null).commit();
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

    private void loadProfilePicture(String profilePictureUrl, ImageView imageView) {
        if (profilePictureUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(this)
                    .load(R.drawable.baseline_person_24)
                    .into(imageView);
        } else {
            // Load original profile picture
            Glide.with(this)
                    .load(profilePictureUrl)
                    .into(imageView);
        }
    }
}