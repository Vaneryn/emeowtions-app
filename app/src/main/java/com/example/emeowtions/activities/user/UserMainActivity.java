package com.example.emeowtions.activities.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserMainActivity extends AppCompatActivity {

    private static final String TAG = "UserMainActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    private ActivityUserMainBinding userMainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase service instances
        mAuth = FirebaseAuth.getInstance();
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

        //region Other listeners
        // userBottomNavigation item: change to selected fragment
        userMainBinding.userBottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

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
                fragmentManager.beginTransaction().replace(R.id.userScreenFragmentContainer, UserMyCatsFragment.class, null).commit();
                return true;
            }

            return false;
        });

        userMainBinding.userBottomNavigation.setOnItemReselectedListener(item -> {
            // Prevent replacing fragment on reselect
        });

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
        //endregion
    }
}