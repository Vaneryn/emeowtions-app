package com.example.emeowtions.utils;

import android.content.Context;

import com.example.emeowtions.models.Breed;
import com.opencsv.CSVReader;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BreedUtils {

    private static final String TAG = "BreedUtils";
    private static final String FILE_PATH = "cat_breeds.csv";

    private Context context;
    private ArrayList<Breed> breeds = new ArrayList<>();

    public BreedUtils(Context context) {
        this.context = context;

        try (CSVReader reader = new CSVReader(new InputStreamReader(this.context.getAssets().open(FILE_PATH)))) {
            String[] nextLine;
            // Skip header
            reader.readNext();
            while ((nextLine = reader.readNext()) != null) {
                String name = nextLine[0];
                String temperament = nextLine[1];
                String size = nextLine[2];
                String activityLevel = nextLine[3];
                String grooming = nextLine[4];
                String description = nextLine[5];
                this.breeds.add(new Breed(name, temperament, size, activityLevel, grooming, description));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getBreedNames() {
        return breeds.stream().map(Breed::getName).collect(Collectors.toList());
    }

    public String getTemperament(String breedName) {
        for (Breed breed : breeds) {
            if (breed.getName().equals(breedName)) {
                return breed.getTemperament();
            }
        }
        return null;
    }

    public String getSize(String breedName) {
        for (Breed breed : breeds) {
            if (breed.getName().equals(breedName)) {
                return breed.getSize();
            }
        }
        return null;
    }

    public String getActivityLevel(String breedName) {
        for (Breed breed : breeds) {
            if (breed.getName().equals(breedName)) {
                return breed.getActivityLevel();
            }
        }
        return null;
    }

    public String getGrooming(String breedName) {
        for (Breed breed : breeds) {
            if (breed.getName().equals(breedName)) {
                return breed.getGrooming();
            }
        }
        return null;
    }

    public String getDescription(String breedName) {
        for (Breed breed : breeds) {
            if (breed.getName().equals(breedName)) {
                return breed.getDescription();
            }
        }
        return null;
    }
}
