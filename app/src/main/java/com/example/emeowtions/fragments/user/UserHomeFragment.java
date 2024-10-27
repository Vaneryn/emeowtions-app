package com.example.emeowtions.fragments.user;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.UserMainActivity;
import com.example.emeowtions.adapters.CarouselDiscoverItemAdapter;
import com.example.emeowtions.adapters.VeterinaryClinicAdapter;
import com.example.emeowtions.databinding.FragmentUserHomeBinding;
import com.example.emeowtions.models.VeterinaryClinic;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;

public class UserHomeFragment extends Fragment {

    private static final String TAG = "UserHomeFragment";

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference clinicsRef;

    // Layout variables
    private FragmentUserHomeBinding binding;

    // Private variables
    private FirestoreRecyclerOptions<VeterinaryClinic> options;
    private VeterinaryClinicAdapter adapter;

    public UserHomeFragment() {
        super(R.layout.fragment_user_home);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase service instances
        db = FirebaseFirestore.getInstance();
        // Initialize Firestore references
        clinicsRef = db.collection("veterinaryClinics");

        setupUi();
        loadData();
        bindListeners();
    }

    private void setupUi() {
        // Load Discover carousel items
        ArrayList<String> imagePaths = new ArrayList<>();
        imagePaths.add("https://uploads.dailydot.com/2018/10/olli-the-polite-cat.jpg?auto=compress&fm=pjpg");
        imagePaths.add("https://cats.com/wp-content/uploads/2024/07/Beautiful-red-cat-stretches-and-shows-tongue.jpg");
        imagePaths.add("https://www.comfortzone.com/-/media/project/oneweb/comfortzone/images/blog/how-to-pet-a-cat.jpeg?h=667&iar=0&w=1000&hash=E90C72B30499CF7688A56CD788A276EF");

        CarouselDiscoverItemAdapter carouselAdapter = new CarouselDiscoverItemAdapter(getContext(), imagePaths);
        binding.recyclerviewCarousel.setAdapter(carouselAdapter);
    }

    private void loadData() {
        Query query = clinicsRef
                .whereEqualTo("deleted", false)
                .orderBy("averageRating", Query.Direction.DESCENDING)
                .orderBy("name", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(10);

        options = new FirestoreRecyclerOptions.Builder<VeterinaryClinic>()
                .setQuery(query, VeterinaryClinic.class)
                .setLifecycleOwner(this)
                .build();

        adapter = new VeterinaryClinicAdapter(options, getContext(), VeterinaryClinicAdapter.VIEW_TYPE_TOP);
        binding.recyclerviewTopClinics.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });
    }

    private void bindListeners() {
        bindOnClickListeners();
    }

    private void bindOnClickListeners() {
        UserMainActivity parentActivity = (UserMainActivity) getActivity();

        // Feature cards
        binding.cardFeatureEmotions.setOnClickListener(view -> {
            parentActivity.getBottomNavigationView().setSelectedItemId(R.id.emotions_item);
        });

        binding.cardFeatureClinics.setOnClickListener(view -> {
            parentActivity.getBottomNavigationView().setSelectedItemId(R.id.clinics_item);
        });

        binding.cardFeatureChat.setOnClickListener(view -> {
            parentActivity.getBottomNavigationView().setSelectedItemId(R.id.chat_item);
        });

        binding.cardFeatureMyCats.setOnClickListener(view -> {
            parentActivity.getBottomNavigationView().setSelectedItemId(R.id.my_cats_item);
        });

        // Top Clinics
        binding.btnGoToClinics.setOnClickListener(view -> {
            parentActivity.getBottomNavigationView().setSelectedItemId(R.id.clinics_item);
        });
    }
}