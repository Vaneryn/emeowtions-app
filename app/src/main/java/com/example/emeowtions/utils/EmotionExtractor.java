package com.example.emeowtions.utils;

import java.util.ArrayList;
import java.util.HashSet;

public class EmotionExtractor {

    // Define a set of emotions as a static constant
    private static final HashSet<String> EMOTIONS = new HashSet<>();

    static {
        EMOTIONS.add("angry");
        EMOTIONS.add("happy");
        EMOTIONS.add("neutral");
        EMOTIONS.add("scared");
    }

    // Method to extract the single emotion
    public static String getSingleEmotion(ArrayList<String> labels) {
        for (String label : labels) {
            if (EMOTIONS.contains(label)) {
                return label; // Return the first matching emotion
            }
        }
        return null; // Return null if no valid emotion is found
    }
}
