package com.example.emeowtions.fragments.veterinary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import com.example.emeowtions.activities.veterinary.ClinicInboxActivity;
import com.example.emeowtions.activities.veterinary.ClinicReviewsActivity;
import com.example.emeowtions.activities.veterinary.VetMainActivity;
import com.example.emeowtions.databinding.FragmentVetDashboardBinding;
import com.example.emeowtions.enums.Role;
import com.example.emeowtions.models.ChatRequest;
import com.example.emeowtions.models.Review;
import com.example.emeowtions.models.User;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class VetDashboardFragment extends Fragment {

    private static final String TAG = "VetDashboardFragment";
    private SharedPreferences sharedPreferences;

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
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
    private String currentUserRole;

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
        currentUserRole = sharedPreferences.getString("role", "Veterinarian");

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
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
        // Hide staff management shortcut for Veterinarian role
        if (currentUserRole.equals(Role.VETERINARIAN.getTitle())) {
            binding.cardShortcutStaff.setVisibility(View.GONE);
        }
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

        //region Analytics
        // Staff data
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
                    if (value != null) {
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

                                        renderStaffMemberChart(vetCount.get(), vetStaffCount.get());
                                    }
                                });
                    }
                });

        // Chats data
        chatsRef.whereEqualTo("vetId", firebaseAuthUtils.getUid())
                .whereGreaterThan("vetUnreadCount", 0)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on Chat changes", error);
                        return;
                    }
                    // Success
                    if (value != null) {
                        // Get unread chat count
                        int unreadChatsCount = value.size();

                        // Set unread chat count
                        binding.txtUnreadChatCount.setText(String.format("%s", unreadChatsCount));
                    }
                });

        // ChatRequests pending data
        chatRequestsRef.whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                .whereEqualTo("accepted", false)
                .addSnapshotListener((value, error) -> {
                   // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on ChatRequest changes", error);
                        return;
                    }
                   // Success
                   if (value != null) {
                       // Get pending requests count
                       int pendingRequestsCount = value.size();

                       // Set pending requests count
                       binding.txtUnreadRequests.setText(String.format("%s", pendingRequestsCount));
                   }
                });
        // ChatRequests all data for analytics
        chatRequestsRef.whereEqualTo("veterinaryClinicId", veterinaryClinicId)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on ChatRequest changes", error);
                        return;
                    }
                    // Success
                    if (value != null) {
                        // Prepare data for line chart
                        Map<String, Integer> pendingCounts = new HashMap<>();
                        Map<String, Integer> acceptedCounts = new HashMap<>();

                        for (DocumentSnapshot document : value.getDocuments()) {
                            ChatRequest chatRequest = document.toObject(ChatRequest.class);

                            // Increment count for each status on each date
                            if (!chatRequest.isAccepted()) {
                                String date = new SimpleDateFormat("yyyy-MM-dd").format(chatRequest.getCreatedAt().toDate());
                                pendingCounts.put(date, pendingCounts.getOrDefault(date, 0) + 1);
                            } else {
                                String date = new SimpleDateFormat("yyyy-MM-dd").format(chatRequest.getUpdatedAt().toDate());
                                acceptedCounts.put(date, acceptedCounts.getOrDefault(date, 0) + 1);
                            }
                        }

                        renderRequestsChart(pendingCounts, acceptedCounts);
                    }
                });

        // Reviews unread data
        reviewsRef.whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    // Error
                    if (error != null) {
                        Log.w(TAG, "loadData: Failed to listen on Review changes", error);
                        return;
                    }
                    // Success
                    if (value != null) {
                        // Get unread reviews count
                        int unreadReviewsCount = value.size();

                        // Set unread reviews count
                        binding.txtUnreadReviews.setText(String.format("%s", unreadReviewsCount));
                    }
                });
        // Reviews all data for analytics
        reviewsRef.addSnapshotListener((value, error) -> {
            // Error
            if (error != null) {
                Log.w(TAG, "loadData: Failed to listen on Review changes", error);
                return;
            }
            // Success
            if (value != null) {
                renderReviewsChart(aggregateReviewsByRating(value.toObjects(Review.class)));
            }
        });
        //endregion
    }

    private void bindListeners() {
        bindOnClickListeners();
    }

    private void bindOnClickListeners() {
        VetMainActivity parentActivity = (VetMainActivity) getActivity();

        binding.cardShortcutChat.setOnClickListener(view -> {
            parentActivity.selectBottomNavigationMenuItem(R.id.vet_chat_item);
        });

        binding.cardShortcutStaff.setOnClickListener(view -> {
            parentActivity.selectBottomNavigationMenuItem(R.id.vet_staff_item);
        });

        binding.cardShortcutClinicProfile.setOnClickListener(view -> {
            parentActivity.selectBottomNavigationMenuItem(R.id.vet_clinic_profile_item);
        });

        binding.cardShortcutClinicInbox.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), ClinicInboxActivity.class));
        });

        binding.cardShortcutClinicReviews.setOnClickListener(view -> {
            startActivity(new Intent(getContext(), ClinicReviewsActivity.class));
        });
    }

    private void renderStaffMemberChart(int vetCount, int vetStaffCount) {
        BarChart barChart = binding.barChartStaffMembers;

        // Create BarEntries
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, vetCount));
        entries.add(new BarEntry(1, vetStaffCount));

        // Set up BarDataSet with label and color
        BarDataSet barDataSet = new BarDataSet(entries, "Staff Distribution");
        barDataSet.setColors(ContextCompat.getColor(getContext(), R.color.primary_300), ContextCompat.getColor(getContext(), R.color.secondary_300));

        // Setup chart data
        BarData data = new BarData(barDataSet);
        data.setBarWidth(0.5f);

        // Description
        barChart.getDescription().setEnabled(false);

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
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Veterinarians", "Veterinary Staff"}));
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

    private void renderRequestsChart(Map<String, Integer> pendingCounts, Map<String, Integer> acceptedCounts) {
        LineChart lineChart = binding.lineChartRequests;
        lineChart.getDescription().setEnabled(false);

        ArrayList<Entry> pendingEntries = new ArrayList<>();
        ArrayList<Entry> acceptedEntries = new ArrayList<>();

        // Use a TreeSet to combine dates and keep them sorted
        TreeSet<String> allDates = new TreeSet<>();
        allDates.addAll(pendingCounts.keySet());
        allDates.addAll(acceptedCounts.keySet());

        // List for x-axis labels
        ArrayList<String> dateLabels = new ArrayList<>(allDates);

        int index = 0;
        for (String date : allDates) {
            // Add entry or zero if the date is missing
            pendingEntries.add(new Entry(index, pendingCounts.getOrDefault(date, 0)));
            acceptedEntries.add(new Entry(index, acceptedCounts.getOrDefault(date, 0)));
            index++;
        }

        LineDataSet pendingDataSet = new LineDataSet(pendingEntries, "Pending");
        pendingDataSet.setColor(ContextCompat.getColor(getContext(), R.color.primary_300));
        pendingDataSet.setLineWidth(2f);

        LineDataSet acceptedDataSet = new LineDataSet(acceptedEntries, "Accepted");
        acceptedDataSet.setColor(ContextCompat.getColor(getContext(), R.color.secondary_300));
        acceptedDataSet.setLineWidth(2f);

        LineData lineData = new LineData(pendingDataSet, acceptedDataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setGranularity(1f);
        YAxis yAxisRight = lineChart.getAxisRight();
        yAxisRight.setAxisMinimum(0);
        yAxisRight.setGranularity(1f);

        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private void renderReviewsChart(int[] ratingCounts) {
        BarChart barChart = binding.barChartClinicRatings;
        List<BarEntry> entries = prepareReviewBarChartData(ratingCounts);

        BarDataSet barDataSet = new BarDataSet(entries, "Number of Reviews");
        barDataSet.setColors(
                ContextCompat.getColor(getContext(), R.color.error_4),
                ContextCompat.getColor(getContext(), R.color.tertiary_100),
                ContextCompat.getColor(getContext(), R.color.quaternary_100),
                ContextCompat.getColor(getContext(), R.color.secondary_200),
                ContextCompat.getColor(getContext(), R.color.primary_300)
        );

        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f); // Set bar width

        barChart.setData(barData);

        // Configure the X-Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"1 Star", "2 Stars", "3 Stars", "4 Stars", "5 Stars"}));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setCenterAxisLabels(false);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setGranularity(1f);
        yAxis.setGranularityEnabled(true);
        barChart.getAxisRight().setEnabled(false);

        // Hide the description label
        barChart.getDescription().setEnabled(false);

        // Set legend position if needed
        Legend legend = barChart.getLegend();
        legend.setEnabled(false);

        // Refresh the chart
        barChart.animateY(1000);
        barChart.invalidate(); // Refresh the chart
    }

    private int[] aggregateReviewsByRating(List<Review> reviews) {
        int[] ratingCounts = new int[5]; // For 1 to 5 stars

        for (Review review : reviews) {
            int ratingIndex = (int) review.getRating() - 1; // rating 1-5 maps to index 0-4
            if (ratingIndex >= 0 && ratingIndex < 5) {
                ratingCounts[ratingIndex]++;
            }
        }

        return ratingCounts;
    }

    private List<BarEntry> prepareReviewBarChartData(int[] ratingCounts) {
        List<BarEntry> entries = new ArrayList<>();

        // Ensure the ratingCounts has 5 elements corresponding to 1 to 5 stars
        for (int i = 0; i < ratingCounts.length; i++) {
            entries.add(new BarEntry(i, ratingCounts[i])); // i is 0 to 4 for ratings
        }

        return entries;
    }
}