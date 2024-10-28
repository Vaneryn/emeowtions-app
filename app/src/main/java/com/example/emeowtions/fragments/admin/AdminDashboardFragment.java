package com.example.emeowtions.fragments.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.emeowtions.R;
import com.example.emeowtions.activities.admin.AdminMainActivity;
import com.example.emeowtions.activities.admin.AnalysisFeedbackActivity;
import com.example.emeowtions.activities.admin.GeneralFeedbackActivity;
import com.example.emeowtions.activities.admin.RecommendationRatingsActivity;
import com.example.emeowtions.databinding.FragmentAdminDashboardBinding;
import com.example.emeowtions.enums.VeterinaryClinicRegistrationStatus;
import com.example.emeowtions.models.AnalysisFeedback;
import com.example.emeowtions.models.Feedback;
import com.example.emeowtions.models.User;
import com.example.emeowtions.models.VeterinaryClinicRegistration;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private static final String TAG = "AdminDashboardFragment";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference clinicsRef;
    private CollectionReference vetRegsRef;
    private CollectionReference generalFeedbackRef;
    private CollectionReference analysisFeedbackRef;

    // Layout variables
    private FragmentAdminDashboardBinding binding;

    public AdminDashboardFragment() {
        super(R.layout.fragment_admin_dashboard);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        usersRef = db.collection("users");
        clinicsRef = db.collection("veterinaryClinics");
        vetRegsRef = db.collection("veterinaryClinicRegistrations");
        generalFeedbackRef = db.collection("feedback");
        analysisFeedbackRef = db.collection("analysisFeedback");

        setupUi();
        loadData();
        bindListeners();
    }

    private void setupUi() {

    }

    private void loadData() {
        //region Hero Section
        usersRef.document(firebaseAuthUtils.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        SimpleDateFormat sdfDatetime = new SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault());

                        // Populate hero fields
                        Glide.with(getContext().getApplicationContext()).load(user.getProfilePicture()).into(binding.imgPfp);
                        binding.txtDisplayName.setText(user.getDisplayName());
                        binding.txtCurrentDatetime.setText(String.format("Logged in at: %s", sdfDatetime.format(Timestamp.now().toDate())));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "loadData: Failed to retrieve User data", e);
                });
        //endregion

        //region Overview
        // Total Users
        usersRef.addSnapshotListener((value, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "loadData: Failed to listen on Users changes", error);
                return;
            }
            // Success
            if (value != null) {
                // Get and set total user count
                int totalUserCount = value.size();
                binding.txtTotalUserCount.setText(String.format("%s", totalUserCount));
            }
        });

        // New Users (Today)
        // Get the start and end of today in Timestamp format
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Timestamp startOfToday = new Timestamp(calendar.getTime());
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp endOfToday = new Timestamp(calendar.getTime());

        usersRef.whereGreaterThanOrEqualTo("createdAt", startOfToday)
                .whereLessThan("createdAt", endOfToday)
                .addSnapshotListener((value, error) -> {
                   // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on new Users changes", error);
                        return;
                    }
                   // Success
                   if (value != null) {
                       // Get and set new users count
                       int newUsersCount = value.size();
                       binding.txtNewUsersToday.setText(String.format("%s", newUsersCount));
                   }
                });

        // Veterinary Clinics
        clinicsRef.whereEqualTo("deleted", false)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on VeterinaryClinics changes", error);
                        return;
                    }
                    // Success
                    if (value != null) {
                        // Get and set clinic count
                        int clinicCount = value.size();
                        binding.txtClinicCount.setText(String.format("%s", clinicCount));
                    }
                });

        // Pending Clinic Registrations
        vetRegsRef.whereEqualTo("status", VeterinaryClinicRegistrationStatus.PENDING.getTitle())
                .addSnapshotListener((value, error) -> {
                   // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on VeterinaryClinicRegistration changes", error);
                        return;
                    }
                   // Success
                   if (value != null) {
                       // Get and set pending registration count
                       int pendingRegistrationCount = value.size();
                       binding.txtPendingRegistrationCount.setText(String.format("%s", pendingRegistrationCount));
                   }
                });

        // Unread General Feedback
        generalFeedbackRef.whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on Feedback changes", error);
                        return;
                    }
                    // Success
                    if (value != null) {
                        // Get and set unread general feedback count
                        int unreadGeneralFeedbackCount = value.size();
                        binding.txtGeneralFeedbackCount.setText(String.format("%s", unreadGeneralFeedbackCount));
                    }
                });

        // Unread Analysis Feedback
        analysisFeedbackRef.whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on AnalysisFeedback changes", error);
                        return;
                    }
                    // Success
                    if (value != null) {
                        // Get and set unread analysis feedback count
                        int unreadAnalysisFeedbackCount = value.size();
                        binding.txtAnalysisFeedbackCount.setText(String.format("%s", unreadAnalysisFeedbackCount));
                    }
                });
        //endregion

        //region Analytics
        // Clinic Registrations by Category
        vetRegsRef.addSnapshotListener((value, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "loadData: Failed to listen on VeterinaryClinicRegistrations changes", error);
                return;
            }
            // Success
            if (value != null) {
                int pendingCount, approvedCount, rejectedCount;
                pendingCount = approvedCount = rejectedCount = 0;

                for (VeterinaryClinicRegistration registration : value.toObjects(VeterinaryClinicRegistration.class)) {
                    if (registration.getStatus().equals(VeterinaryClinicRegistrationStatus.PENDING.getTitle())) {
                        pendingCount++;
                    } else if (registration.getStatus().equals(VeterinaryClinicRegistrationStatus.APPROVED.getTitle())) {
                        approvedCount++;
                    } else if (registration.getStatus().equals(VeterinaryClinicRegistrationStatus.REJECTED.getTitle())) {
                        rejectedCount++;
                    }
                }

                renderClinicRegistrationsChart(pendingCount, approvedCount, rejectedCount);
            }
        });

        // General Feedback Ratings
        generalFeedbackRef.addSnapshotListener((value, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "loadData: Failed to listen on Feedback changes", error);
                return;
            }
            // Success
            if (value != null) {
                int oneStar, twoStar, threeStar, fourStar, fiveStar;
                oneStar = twoStar = threeStar = fourStar = fiveStar = 0;

                for (Feedback feedback : value.toObjects(Feedback.class)) {
                    if (feedback.getRating() == 1) {
                        oneStar++;
                    } else if (feedback.getRating() == 2) {
                        twoStar++;
                    } else if (feedback.getRating() == 3) {
                        threeStar++;
                    } else if (feedback.getRating() == 4) {
                        fourStar++;
                    } else if (feedback.getRating() == 5) {
                        fiveStar++;
                    }
                }

                renderGeneralFeedbackRatingsChart(oneStar, twoStar, threeStar, fourStar, fiveStar);
            }
        });

        // Analysis Feedback Ratings
        analysisFeedbackRef.addSnapshotListener((value, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "loadData: Failed to listen on AnalysisFeedback changes", error);
                return;
            }
            // Success
            if (value != null) {
                int oneStar, twoStar, threeStar, fourStar, fiveStar;
                oneStar = twoStar = threeStar = fourStar = fiveStar = 0;

                for (AnalysisFeedback analysisFeedback : value.toObjects(AnalysisFeedback.class)) {
                    if (analysisFeedback.getRating() == 1) {
                        oneStar++;
                    } else if (analysisFeedback.getRating() == 2) {
                        twoStar++;
                    } else if (analysisFeedback.getRating() == 3) {
                        threeStar++;
                    } else if (analysisFeedback.getRating() == 4) {
                        fourStar++;
                    } else if (analysisFeedback.getRating() == 5) {
                        fiveStar++;
                    }
                }

                renderAnalysisFeedbackRatingsChart(oneStar, twoStar, threeStar, fourStar, fiveStar);
            }
        });
        //endregion
    }

    private void bindListeners() {
        bindOnClickListeners();
    }

    private void bindOnClickListeners() {
        AdminMainActivity parentActivity = (AdminMainActivity) getActivity();

        binding.cardShortcutClinics.setOnClickListener(view -> {
            parentActivity.selectBottomNavigationMenuItem(R.id.admin_clinics_item);
        });

        binding.cardShortcutRegistrations.setOnClickListener(view -> {
            parentActivity.selectBottomNavigationMenuItem(R.id.admin_vet_registrations_item);
        });

        binding.cardShortcutUsers.setOnClickListener(view -> {
            parentActivity.selectBottomNavigationMenuItem(R.id.admin_users_item);
        });

        binding.cardShortcutGeneralFeedback.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), GeneralFeedbackActivity.class));
        });

        binding.cardShortcutAnalysisFeedback.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), AnalysisFeedbackActivity.class));
        });

        binding.cardShortcutRecommendationReviews.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), RecommendationRatingsActivity.class));
        });
    }

    private void renderClinicRegistrationsChart(int pendingCount, int approvedCount, int rejectedCount) {
        BarChart barChart = binding.barChartRegistrations;
        barChart.getDescription().setEnabled(false);

        // Create BarEntries
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, pendingCount));
        entries.add(new BarEntry(1, approvedCount));
        entries.add(new BarEntry(2, rejectedCount));

        // Set up BarDataSet with label and color
        BarDataSet barDataSet = new BarDataSet(entries, "Number of Registrations");
        barDataSet.setValueFormatter(new NoDecimalValueFormatter());
        barDataSet.setColors(
                ContextCompat.getColor(getContext(), R.color.warning_3),
                ContextCompat.getColor(getContext(), R.color.quaternary_200),
                ContextCompat.getColor(getContext(), R.color.error_4)
        );

        // Setup chart data
        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.5f);

        // Legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // Configure axes and add data
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Pending", "Approved", "Rejected"}));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0);
        yAxis.setGranularity(1f);

        barChart.getAxisRight().setEnabled(false);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void renderGeneralFeedbackRatingsChart(int oneStar, int twoStar, int threeStar, int fourStar, int fiveStar) {
        BarChart barChart = binding.barChartGeneralFeedback;
        barChart.getDescription().setEnabled(false);

        // Create BarEntries
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, oneStar));
        entries.add(new BarEntry(1, twoStar));
        entries.add(new BarEntry(2, threeStar));
        entries.add(new BarEntry(3, fourStar));
        entries.add(new BarEntry(4, fiveStar));

        // Set up BarDataSet with label and color
        BarDataSet barDataSet = new BarDataSet(entries, "Number of Feedback");
        barDataSet.setValueFormatter(new NoDecimalValueFormatter());
        barDataSet.setColors(
                ContextCompat.getColor(getContext(), R.color.rating_1star),
                ContextCompat.getColor(getContext(), R.color.rating_2star),
                ContextCompat.getColor(getContext(), R.color.rating_3star),
                ContextCompat.getColor(getContext(), R.color.rating_4star),
                ContextCompat.getColor(getContext(), R.color.rating_5star)
        );

        // Setup chart data
        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.5f);

        // Legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // Configure axes and add data
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"1 Star", "2 Stars", "3 Stars", "4 Stars", "5 Stars"}));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0);
        yAxis.setGranularity(1f);

        barChart.getAxisRight().setEnabled(false);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void renderAnalysisFeedbackRatingsChart(int oneStar, int twoStar, int threeStar, int fourStar, int fiveStar) {
        BarChart barChart = binding.barChartAnalysisFeedback;
        barChart.getDescription().setEnabled(false);

        // Create BarEntries
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, oneStar));
        entries.add(new BarEntry(1, twoStar));
        entries.add(new BarEntry(2, threeStar));
        entries.add(new BarEntry(3, fourStar));
        entries.add(new BarEntry(4, fiveStar));

        // Set up BarDataSet with label and color
        BarDataSet barDataSet = new BarDataSet(entries, "Number of Feedback");
        barDataSet.setValueFormatter(new NoDecimalValueFormatter());
        barDataSet.setColors(
                ContextCompat.getColor(getContext(), R.color.rating_1star),
                ContextCompat.getColor(getContext(), R.color.rating_2star),
                ContextCompat.getColor(getContext(), R.color.rating_3star),
                ContextCompat.getColor(getContext(), R.color.rating_4star),
                ContextCompat.getColor(getContext(), R.color.rating_5star)
        );

        // Setup chart data
        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.5f);

        // Legend
        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // Configure axes and add data
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"1 Star", "2 Stars", "3 Stars", "4 Stars", "5 Stars"}));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setAxisMinimum(0);
        yAxis.setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);

        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    // Utility classes
    public class NoDecimalValueFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            return String.format("%.0f", value); // Shows the value without decimal points
        }
    }
}