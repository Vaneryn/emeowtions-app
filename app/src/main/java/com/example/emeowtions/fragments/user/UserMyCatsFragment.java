package com.example.emeowtions.fragments.user;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.emeowtions.R;
import com.example.emeowtions.activities.user.AddCatActivity;
import com.example.emeowtions.adapters.CatAdapter;
import com.example.emeowtions.databinding.FragmentUserMyCatsBinding;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.utils.FirebaseAuthUtils;
import com.example.emeowtions.utils.GridSpacingItemDecoration;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class UserMyCatsFragment extends Fragment {

    private static final String TAG = "UserMyCatsFragment";

    // Firebase variables
    private FirebaseAuthUtils firebaseAuthUtils;
    private FirebaseFirestore db;
    private CollectionReference catsRef;

    // Layout variables
    private FragmentUserMyCatsBinding myCatsBinding;
    private CatAdapter catAdapter;
    private boolean hasCats;

    public UserMyCatsFragment() {
        super(R.layout.fragment_user_my_cats);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myCatsBinding = FragmentUserMyCatsBinding.inflate(inflater, container, false);
        return myCatsBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "UserMyCatsFragment onViewCreated");

        LifecycleOwner lifecycleOwner = this;

        // Initialize Firebase service instances
        firebaseAuthUtils = new FirebaseAuthUtils();
        db = FirebaseFirestore.getInstance();
        catsRef = db.collection("cats");

        //region UI setups
        // Hide no results views
        myCatsBinding.layoutNoCats.setVisibility(View.GONE);
        myCatsBinding.layoutNoResults.setVisibility(View.GONE);

        // Add equal spacing between items
        int spanCount = 2;
        int spacing = 25;
        boolean includeEdge = false;
        myCatsBinding.recyclerviewCatResults.addItemDecoration(new GridSpacingItemDecoration(spanCount, spacing, includeEdge));
        //endregion

        //region Load data
        // Query user's cats
        Query catsQuery =
                catsRef.whereEqualTo("ownerId", firebaseAuthUtils.getUid())
                        .whereEqualTo("deleted", false)
                        .orderBy("updatedAt", Query.Direction.DESCENDING);

        // Check if user has any existing cats
        catsQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.w(TAG, "Failed to listen on user's cat changes", error);
                return;
            }
            if (value.getDocuments().isEmpty()) {
                updateResultsView(true);
            } else {
                // Query data into RecyclerView
                FirestoreRecyclerOptions<Cat> options = new FirestoreRecyclerOptions.Builder<Cat>()
                        .setQuery(catsQuery, Cat.class)
                        .setLifecycleOwner(lifecycleOwner)
                        .build();

                // Create and set adapter to RecyclerView
                catAdapter = new CatAdapter(options, getContext());
                myCatsBinding.recyclerviewCatResults.setAdapter(catAdapter);

                catAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        updateResultsView(false);
                    }
                });

                // Only allow searching if the user has any existing cats
                myCatsBinding.edtSearchbar.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String queryText = myCatsBinding.edtSearchbar.getText().toString();

                        if (queryText.isBlank()) {
                            // Revert to initial results
                            catAdapter.updateOptions(options);
                            myCatsBinding.layoutNoResults.setVisibility(View.GONE);
                        } else {
                            // Queries for names that are either EXACT matches or START WITH the queryText
                            Query catsSearchQuery =
                                    catsRef.whereEqualTo("ownerId", firebaseAuthUtils.getUid())
                                            .whereEqualTo("deleted", false)
                                            .where(Filter.or(
                                                    Filter.equalTo("name", queryText),
                                                    Filter.and(
                                                            Filter.greaterThanOrEqualTo("name", queryText),
                                                            Filter.lessThanOrEqualTo("name", queryText + "\uf8ff")
                                                    )
                                            ))
                                            .orderBy("updatedAt", Query.Direction.DESCENDING);

                            FirestoreRecyclerOptions<Cat> searchOptions = new FirestoreRecyclerOptions.Builder<Cat>()
                                    .setQuery(catsSearchQuery, Cat.class)
                                    .setLifecycleOwner(lifecycleOwner)
                                    .build();

                            catAdapter.updateOptions(searchOptions);
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {}
                });
            }
        });
        //endregion

        //region Listeners
        myCatsBinding.btnAddCatSecondary.setOnClickListener(view1 -> {
            startActivity(new Intent(getContext(), AddCatActivity.class));
        });

        myCatsBinding.btnClearSearch.setOnClickListener(view2 -> {
            myCatsBinding.edtSearchbar.getText().clear();
        });
        //endregion
    }

    public void updateResultsView(boolean isInitialLoad) {
        if (isInitialLoad) {
            myCatsBinding.layoutNoResults.setVisibility(View.GONE);
            myCatsBinding.layoutNoCats.setVisibility(View.VISIBLE);
        } else {
            myCatsBinding.layoutNoCats.setVisibility(View.GONE);

            if (catAdapter.getItemCount() > 0)
                myCatsBinding.layoutNoResults.setVisibility(View.GONE);
            else
                myCatsBinding.layoutNoResults.setVisibility(View.VISIBLE);
        }
    }
}