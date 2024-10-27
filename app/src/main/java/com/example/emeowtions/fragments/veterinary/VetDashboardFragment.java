package com.example.emeowtions.fragments.veterinary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.FragmentVetDashboardBinding;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class VetDashboardFragment extends Fragment {

    private static final String TAG = "VetDashboardFragment";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseAuthUtils authUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference vetsRef;
    private CollectionReference vetStaffRef;
    private CollectionReference chatsRef;
    private CollectionReference chatRequestsRef;
    private CollectionReference reviewsRef;

    // Layout variables
    private FragmentVetDashboardBinding binding;

    // Private variables
    private String veterinaryClinicId;

    public VetDashboardFragment() {
        super(R.layout.fragment_vet_dashboard);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVetDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Shared preferences
        sharedPreferences = getContext().getSharedPreferences("com.emeowtions", Context.MODE_PRIVATE);
        veterinaryClinicId = sharedPreferences.getString("veterinaryClinicId", null);

        // Initialize Firebase service instances
        authUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        vetsRef = db.collection("veterinarians");
        vetStaffRef = db.collection("veterinaryStaff");
        chatsRef = db.collection("chats");
        chatRequestsRef = db.collection("chatRequests");
        reviewsRef = db.collection("veterinaryClinics").document(veterinaryClinicId).collection("reviews");

        setupUi();
        loadData();
        bindListeners();
    }

    private void setupUi() {

    }

    private void loadData() {
        //region Hero Section
        usersRef.document(authUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault());

                        // Populate hero fields
                        Glide.with(getContext().getApplicationContext()).load(user.getProfilePicture()).into(binding.imgPfp);
                        binding.txtDisplayName.setText(user.getDisplayName());
                        binding.txtCurrentDatetime.setText(String.format("%s", sdfDatetime.format(Timestamp.now().toDate())));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "loadData: Failed to retrieve User data", e);
                });
        //endregion

        //region Analytics
        // Staff Data
        AtomicInteger vetCount = new AtomicInteger();
        AtomicInteger vetStaffCount = new AtomicInteger();
        AtomicInteger staffCount = new AtomicInteger();

        vetsRef.whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                .addSnapshotListener((value, error) -> {
                    // Success
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on Veterinarian changes", error);
                        return;
                    }
                    // Error
                    if (value != null && !value.isEmpty()) {
                        // Get vet count and increment staff count
                        vetCount.set(value.size());
                        staffCount.addAndGet(value.size());

                        vetStaffRef.whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                                .addSnapshotListener((value1, error1) -> {
                                    // Success
                                    if (error1 != null) {
                                        Log.w(TAG, "loadData: Failed to listen on VeterinaryStaff changes", error);
                                        return;
                                    }
                                    // Error
                                    if (value1 != null && !value1.isEmpty()) {
                                        // Get vetStaff count and increment staff count
                                        vetStaffCount.set(value1.size());
                                        staffCount.addAndGet(value1.size());

                                        // Set staff count
                                        binding.txtStaffCount.setText(String.format("%s", staffCount));
                                    }
                                });
                    }
                });
        //endregion
    }

    private void bindListeners() {

    }
}