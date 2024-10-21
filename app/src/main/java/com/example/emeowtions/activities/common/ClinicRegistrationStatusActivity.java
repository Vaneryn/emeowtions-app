package com.example.emeowtions.activities.common;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.databinding.ActivityClinicRegistrationStatusBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.example.emeowtions.models.VeterinaryClinicRegistration;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ClinicRegistrationStatusActivity extends AppCompatActivity {

    private static final String TAG = "ClinicRegistrationStatusActivity";
    private ActivityClinicRegistrationStatusBinding regStatusBinding;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private CollectionReference vetRegsRef;

    // Private variables
    private String vetRegId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Intent data
        Intent passedIntent = getIntent();
        vetRegId = passedIntent.getStringExtra(RegisterClinicActivity.KEY_VET_REG_ID);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        // Initialize Firestore references
        vetRegsRef = db.collection("veterinaryClinicRegistrations");

        // Get ViewBinding and set content view
        regStatusBinding = ActivityClinicRegistrationStatusBinding.inflate(getLayoutInflater());
        setContentView(regStatusBinding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region Load Data
        vetRegsRef.document(vetRegId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to listen on VeterinaryClinicRegistration changes", error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault());
                        VeterinaryClinicRegistration veterinaryClinicRegistration = value.toObject(VeterinaryClinicRegistration.class);
                        String status = veterinaryClinicRegistration.getStatus();

                        // Load clinic details
                        Glide.with(getApplicationContext())
                                .load(veterinaryClinicRegistration.getLogoUrl())
                                .into(regStatusBinding.imgClinicLogo);
                        regStatusBinding.txtStatus.setText(status);
                        regStatusBinding.txtClinicName.setText(veterinaryClinicRegistration.getName());
                        regStatusBinding.txtClinicEmail.setText(veterinaryClinicRegistration.getEmail());
                        regStatusBinding.txtClinicPhoneNumber.setText(veterinaryClinicRegistration.getPhoneNumber());
                        regStatusBinding.txtClinicAddress.setText(veterinaryClinicRegistration.getAddress());
                        regStatusBinding.txtClinicDescription.setText(veterinaryClinicRegistration.getDescription());

                        // Update status text
                        if (status.equals(VeterinaryClinicRegistrationStatus.PENDING.getTitle())) {
                            regStatusBinding.txtStatus.setTextColor(getColor(R.color.warning_3));
                        } else if (status.equals(VeterinaryClinicRegistrationStatus.APPROVED.getTitle())) {
                            regStatusBinding.txtStatus.setTextColor(getColor(R.color.quaternary_200));

                            // Load approval date
                            regStatusBinding.layoutDatetime.setVisibility(View.VISIBLE);
                            regStatusBinding.txtDatetimeTitle.setText(R.string.approval_date);
                            regStatusBinding.txtDatetimeValue.setText(sdf.format(veterinaryClinicRegistration.getUpdatedAt().toDate()));
                        } else if (status.equals(VeterinaryClinicRegistrationStatus.REJECTED.getTitle())) {
                            regStatusBinding.txtStatus.setTextColor(getColor(R.color.error_2));

                            // Load rejection rejection
                            regStatusBinding.layoutRejectionReason.setVisibility(View.VISIBLE);
                            regStatusBinding.txtRejectionReason.setText(
                                    veterinaryClinicRegistration.getRejectionReason() != null ?
                                            veterinaryClinicRegistration.getRejectionReason() :
                                            "Unspecified"
                            );
                            // Load rejection date
                            regStatusBinding.layoutDatetime.setVisibility(View.VISIBLE);
                            regStatusBinding.txtDatetimeTitle.setText(R.string.rejection_date);
                            regStatusBinding.txtDatetimeValue.setText(sdf.format(veterinaryClinicRegistration.getUpdatedAt().toDate()));
                        }
                    }
                });
        //endregion
        
        //region onClick Listeners
        regStatusBinding.appBarRegistrationStatus.setNavigationOnClickListener(view -> finish());
        //endregion
    }
}