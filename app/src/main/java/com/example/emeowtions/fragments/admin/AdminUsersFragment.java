package com.example.emeowtions.fragments.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentAdminUsersBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class AdminUsersFragment extends Fragment {

    private static final String TAG = "AdminUsersFragment";

    // Layout variables
    private FragmentAdminUsersBinding binding;
    private UserManagementFragment userFragment;
    private VetManagementFragment vetFragment;
    private AdminManagementFragment adminFragment;
    private Fragment selectedFragment;

    public AdminUsersFragment() {
        super(R.layout.fragment_admin_users);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //region UI Setups
        userFragment = new UserManagementFragment();
        vetFragment = new VetManagementFragment();
        adminFragment = new AdminManagementFragment();

        createFragment(userFragment);
        createFragment(vetFragment);
        createFragment(adminFragment);

        selectedFragment = userFragment;
        showFragment(selectedFragment);
        //endregion

        //region Navigation Listeners
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int tabPosition = tab.getPosition();

                // No need for fragment replacement
                boolean toReplace = false;

                if (tabPosition == 0) {
                    changeFragment(userFragment, toReplace);
                } else if (tabPosition == 1) {
                    changeFragment(vetFragment, toReplace);
                } else if (tabPosition == 2) {
                    changeFragment(adminFragment, toReplace);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        //endregion
    }

    // Fragment management helpers
    private void createFragment(Fragment fragment){
        getChildFragmentManager().beginTransaction()
                .add(R.id.users_fragment_container, fragment)
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