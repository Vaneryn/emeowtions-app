package com.example.emeowtions.utils;

import android.content.Context;

import com.example.emeowtions.models.BodyLanguage;
import com.opencsv.CSVReader;

import java.io.InputStreamReader;
import java.util.ArrayList;

public class BodyLanguageUtils {

    private static final String TAG = "BodyLanguageUtils";
    private static final String FILE_PATH = "body_language_types.csv";

    private Context context;
    private ArrayList<BodyLanguage> bodyLanguages = new ArrayList<>();

    public BodyLanguageUtils(Context context) {
        this.context = context;

        try (CSVReader reader = new CSVReader(new InputStreamReader(this.context.getAssets().open(FILE_PATH)))) {
            String[] nextLine;
            // Skip header
            reader.readNext();
            while ((nextLine = reader.readNext()) != null) {
                String key = nextLine[0];
                String value = nextLine[1];
                String description = nextLine[2];
                this.bodyLanguages.add(new BodyLanguage(key, value, description, 0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isBodyLanguage(String label) {
        for (BodyLanguage bodyLanguage : bodyLanguages) {
            if (bodyLanguage.getKey().equals(label)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEar(String label) {
        return label.split("_")[0].equals("ears");
    }

    public boolean isEye(String label) {
        return label.split("_")[0].equals("eyes");
    }

    public boolean isFang(String label) {
        return label.equals("mouth_fangs");
    }

    public boolean isPosture(String label) {
        return label.split("_")[0].equals("posture");
    }

    public boolean isTail(String label) {
        return label.split("_")[0].equals("tail");
    }

    public String getValue(String label) {
        for (BodyLanguage bodyLanguage : bodyLanguages) {
            if (bodyLanguage.getKey().equals(label)) {
                return bodyLanguage.getValue();
            }
        }
        return null;
    }

    public String getDescription(String label) {
        for (BodyLanguage bodyLanguage : bodyLanguages) {
            if (bodyLanguage.getKey().equals(label)) {
                return bodyLanguage.getDescription();
            }
        }
        return null;
    }
}
