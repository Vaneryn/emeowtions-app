package com.example.emeowtions.activities.veterinary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.AddUserActivity;
import com.example.emeowtions.activities.common.FeedbackActivity;
import com.example.emeowtions.activities.common.LoginActivity;
import com.example.emeowtions.activities.common.ProfileActivity;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.databinding.ActivityVetMainBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.fragments.veterinary.VetChatFragment;
import com.example.emeowtions.fragments.veterinary.VetClinicProfileFragment;
import com.example.emeowtions.fragments.veterinary.VetDashboardFragment;
import com.example.emeowtions.fragments.veterinary.VetStaffFragment;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.Veterinarian;
import com.example.emeowtions.models.VeterinaryStaff;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class VetMainActivity extends AppCompatActivity {

    private static final String TAG = "VetMainActivity";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseApp vetApp;
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetsRef;
    private CollectionReference vetStaffRef;
    private CollectionReference chatsRef;

    // Layout variables
    public ActivityVetMainBinding binding;
    private Fragment selectedFragment;
    public BottomNavigationView vetBottomNavigation;

    // Private variables
    private String currentUserRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Veterinary app instance
        if (!doesAppExist("vet")) {
            vetApp = FirebaseApp.initializeApp(this, FirebaseApp.getInstance().getOptions(), "vet");
        } else {
            vetApp = FirebaseApp.getInstance("vet");
        }

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        vetsRef = db.collection("veterinarians");
        vetStaffRef = db.collection("veterinaryStaff");
        chatsRef = db.collection("chats");

        //region Shared preferences
        // Get user role
        sharedPreferences = getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        currentUserRole = sharedPreferences.getString("role", "Veterinarian");

        // Get and set veterinaryClinicId
        if (currentUserRole.equals(Role.VETERINARIAN.getTitle())) {
            vetsRef.whereEqualTo("uid", firebaseAuthUtils.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        // Error
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onCreate: Failed to listen on Veterinarian changes", task.getException());
                            return;
                        }
                        // Success
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            Veterinarian vet = documentSnapshot.toObject(Veterinarian.class);
                            sharedPreferences.edit().putString("veterinaryClinicId", vet.getVeterinaryClinicId()).apply();
                            sharedPreferences.edit().putString("veterinaryStaffId", documentSnapshot.getId()).apply();
                            continueOnCreate();
                        } else {
                            Toast.makeText(this, "Your account is not recognized as veterinary staff. Please contact support.", Toast.LENGTH_LONG).show();
                        }
                    });
        } else if (currentUserRole.equals(Role.VETERINARY_STAFF.getTitle())) {
            vetStaffRef.whereEqualTo("uid", firebaseAuthUtils.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        // Error
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onCreate: Failed to listen on VeterinaryStaff changes", task.getException());
                            return;
                        }
                        // Success
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            VeterinaryStaff vetStaff = documentSnapshot.toObject(VeterinaryStaff.class);
                            sharedPreferences.edit().putString("veterinaryClinicId", vetStaff.getVeterinaryClinicId()).apply();
                            sharedPreferences.edit().putString("veterinarianId", documentSnapshot.getId()).apply();
                            continueOnCreate();
                        } else {
                            Toast.makeText(this, "Your account is not recognized as veterinary staff. Please contact support.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
        //endregion
    }

    private void continueOnCreate() {
        // Get ViewBinding and set content view
        binding = ActivityVetMainBinding.inflate(getLayoutInflater());
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
        // Bottom nav setup
        binding.vetBottomNavigation.setOnApplyWindowInsetsListener(null);
        binding.vetBottomNavigation.setPadding(0, 0, 0, 0);
        if (currentUserRole.equals(Role.VETERINARIAN.getTitle()))
            binding.vetBottomNavigation.getMenu().removeItem(R.id.vet_staff_item);

        // Initialize Fragments
        VetDashboardFragment vetDashboardFragment = new VetDashboardFragment();
        VetChatFragment vetChatFragment = new VetChatFragment();
        VetStaffFragment vetStaffFragment = new VetStaffFragment();
        VetClinicProfileFragment vetClinicProfileFragment = new VetClinicProfileFragment();

        createFragment(vetDashboardFragment);
        createFragment(vetChatFragment);
        createFragment(vetStaffFragment);
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
        binding.topAppBar.setNavigationOnClickListener(view -> binding.vetDrawerLayout.open());

        // drawerNavigationView item: redirect to selected screen
        binding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            binding.vetDrawerLayout.close();

            if (itemId == R.id.vet_profile_item) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (itemId == R.id.vet_clinic_inbox_item) {
                startActivity(new Intent(this, ClinicInboxActivity.class));
            } else if (itemId == R.id.vet_clinic_reviews_item) {
                startActivity(new Intent(this, ClinicReviewsActivity.class));
            } else if (itemId == R.id.vet_feedback_item) {
                startActivity(new Intent(this, FeedbackActivity.class));
            }

            return false;
        });

        // topAppBar menu: manage action buttons for each fragment's respective action menu
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_clinic_inbox) {
                // VetChatFragment
                startActivity(new Intent(this, ClinicInboxActivity.class));
            } else if (itemId == R.id.action_add_staff) {
                // VetStaffFragment
                startActivity(new Intent(this, AddStaffActivity.class));
            } else if (itemId == R.id.action_edit_vet_clinic_profile) {
                // VetClinicProfileFragment
                vetClinicProfileFragment.toggleEditMode(true);
            }

            return false;
        });

        // vetBottomNavigation item: change to selected fragment
        vetBottomNavigation = binding.vetBottomNavigation;
        vetBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Reset top app bar
            binding.topAppBar.getMenu().clear();
            togglePrimaryTheme(false);

            // Top app bar scaling
            float scale = getResources().getDisplayMetrics().density;
            binding.topAppBar.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;

            // No need for fragment replacement in Admin view
            boolean toReplace = false;

            if (itemId == R.id.vet_dashboard_item) {
                binding.topAppBar.setTitle(R.string.dashboard);
                togglePrimaryTheme(true);
                changeFragment(vetDashboardFragment, toReplace);
                return true;
            } else if (itemId == R.id.vet_chat_item) {
                binding.topAppBar.setTitle(R.string.chat);
                binding.topAppBar.inflateMenu(R.menu.top_app_bar_vet_chat);
                changeFragment(vetChatFragment, toReplace);
                return true;
            } else if (itemId == R.id.vet_staff_item) {
                binding.topAppBar.setTitle(R.string.staff);
                binding.topAppBar.inflateMenu(R.menu.top_app_bar_vet_staff);
                binding.topAppBar.getLayoutParams().height = (int) (40 * scale + 0.5f);    // Make app bar smaller cause the tabs are chonky
                changeFragment(vetStaffFragment, toReplace);
                return true;
            } else if (itemId == R.id.vet_clinic_profile_item) {
                binding.topAppBar.setTitle(R.string.clinic_profile);
                binding.topAppBar.inflateMenu(R.menu.top_app_bar_vet_clinic_profile);
                togglePrimaryTheme(true);
                changeFragment(vetClinicProfileFragment, toReplace);
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

        // Retrieve unread chats
        chatsRef.whereEqualTo("vetId", firebaseAuthUtils.getUid())
                .whereEqualTo("readByVet", false)
                .addSnapshotListener((values, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to listen on Chat changes", error);
                        return;
                    }
                    // Success
                    if (values.isEmpty()) {
                        // Update Chat badge: no unread chats
                        binding.vetBottomNavigation.removeBadge(R.id.vet_chat_item);
                    } else {
                        // Update Chat badge: Existing unread chats
                        binding.vetBottomNavigation.getOrCreateBadge(R.id.vet_chat_item).setNumber(values.size());
                    }
                });
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

    private boolean doesAppExist(String name) {
        for (FirebaseApp app : FirebaseApp.getApps(this)) {
            if (app.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void togglePrimaryTheme(boolean enabled) {
        if (enabled) {
            binding.main.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_300));
            binding.topAppBar.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_300));
            binding.topAppBar.setNavigationIconTint(ContextCompat.getColor(this, R.color.white));
            binding.topAppBar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            binding.main.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            binding.topAppBar.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
            binding.topAppBar.setNavigationIconTint(ContextCompat.getColor(this, R.color.gray_700));
            binding.topAppBar.setTitleTextColor(ContextCompat.getColor(this, R.color.gray_700));
        }
    }

    public void selectBottomNavigationMenuItem(int itemId) {
        binding.vetBottomNavigation.setSelectedItemId(itemId);
    }
}