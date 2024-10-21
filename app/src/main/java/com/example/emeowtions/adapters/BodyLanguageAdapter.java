package com.example.emeowtions.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emeowtions.R;
import com.example.emeowtions.models.BodyLanguage;

import java.util.ArrayList;

public class BodyLanguageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<BodyLanguage> bodyLanguageList;
    private Context context;

    public BodyLanguageAdapter(ArrayList<BodyLanguage> bodyLanguageList, Context context) {
        this.bodyLanguageList = bodyLanguageList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.holder_body_language, parent, false);
        return new BodyLanguageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BodyLanguage item = bodyLanguageList.get(position);
        BodyLanguageHolder bodyLanguageHolder = (BodyLanguageHolder) holder;

        // Populate data
        bodyLanguageHolder.txtValue.setText(item.getValue());
        bodyLanguageHolder.txtDescription.setText(item.getDescription());
        bodyLanguageHolder.txtProbability.setText(String.format("%s%%", item.getProbability()));
    }

    @Override
    public int getItemCount() {
        return bodyLanguageList.size();
    }

    private class BodyLanguageHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtValue;
        TextView txtDescription;
        TextView txtProbability;

        public BodyLanguageHolder(@NonNull View itemView) {
            super(itemView);
            txtValue = itemView.findViewById(R.id.txt_value);
            txtDescription = itemView.findViewById(R.id.txt_description);
            txtProbability = itemView.findViewById(R.id.txt_probability);
        }
    }
}
