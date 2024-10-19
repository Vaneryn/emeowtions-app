package com.example.emeowtions.activities.user;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.adapters.VeterinaryClinicAdapter;
import com.example.emeowtions.databinding.ActivityUserClinicProfileBinding;
import com.example.emeowtions.models.VeterinaryClinic;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class UserClinicProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserClinicProfileActivity";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference clinicsRef;

    // Layout variables
    private ActivityUserClinicProfileBinding binding;

    // Private variables
    private String clinicId;
    private String clinicName;
    private String clinicAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get intent data
        Intent passedIntent = getIntent();
        clinicId = passedIntent.getStringExtra(VeterinaryClinicAdapter.KEY_CLINIC_ID);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        clinicsRef = db.collection("veterinaryClinics");

        // Get ViewBinding and set content view
        binding = ActivityUserClinicProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable edge-to-edge layout
        EdgeToEdge.enable(this);
        // Adjust main layout padding based on system bar size to prevent obstruction
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //region UI Setups
        //endregion

        //region Navigation Listeners
        binding.appBarClinicProfile.setNavigationOnClickListener(view -> finish());

        binding.appBarClinicProfile.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_get_directions) {
                openGoogleMaps(clinicName, clinicAddress);
            } else if (itemId == R.id.action_review_clinic) {
                // TODO
            }

            return false;
        });
        //endregion

        //region Load Data
        clinicsRef.document(clinicId)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "onCreate: Failed to listen on current VeterinaryClinic changes", error);
                        return;
                    }
                    // Success
                    if (value != null && value.exists()) {
                        VeterinaryClinic clinic = value.toObject(VeterinaryClinic.class);
                        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());

                        // Get field values
                        String logoUrl = clinic.getLogoUrl();
                        clinicName = clinic.getName();
                        String email = clinic.getEmail();
                        String phoneNumber = clinic.getPhoneNumber();
                        clinicAddress = clinic.getAddress();
                        String description = clinic.getDescription();

                        // Load banner
                        loadLogo(logoUrl);
                        binding.txtBannerName.setText(clinicName);
                        binding.txtBannerJoinedDate.setText(String.format("Joined %s", sdf.format(clinic.getCreatedAt().toDate())));
                        binding.txtBannerUpdatedDate.setText(String.format("Updated %s", sdf.format(clinic.getUpdatedAt().toDate())));

                        // Load fields
                        binding.txtEmail.setText(email);
                        binding.txtPhoneNumber.setText(phoneNumber);
                        binding.txtAddress.setText(clinicAddress);
                        binding.txtDescription.setText(description);
                    }
                });
        //endregion

        //region onClick Listeners
        //endregion
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuthUtils.checkSignedIn(this);
    }

    public void openGoogleMaps(String name, String address) {
        String query = name + " " + address;
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Check if Google Maps is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
        }
    }

    // Loads logo into ImageView
    private void loadLogo(String logoUrl) {
        if (logoUrl == null) {
            // Load default picture if no profile picture exists
            Glide.with(getApplicationContext())
                    .load(R.drawable.baseline_local_hospital_24)
                    .into(binding.imgClinicLogo);
        } else {
            // Load original profile picture
            Glide.with(getApplicationContext())
                    .load(logoUrl)
                    .into(binding.imgClinicLogo);
        }
    }
}