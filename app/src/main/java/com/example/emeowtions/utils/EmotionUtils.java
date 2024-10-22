package com.example.emeowtions.utils;

import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class EmotionUtils {

    private static final String TAG = "EmotionUtils";
    public static final HashSet<String> EMOTIONS = new HashSet<>();

    static {
        EMOTIONS.add("angry");
        EMOTIONS.add("happy");
        EMOTIONS.add("neutral");
        EMOTIONS.add("scared");
    }

    public static Pair<String, Float> getTrueEmotion(HashMap<String, Float> labelProbabilities) {
        String highestEmotion = null;
        float highestProbability = -1.0f;  // Initialize to a very low value

        // Iterate over the label probabilities
        for (Map.Entry<String, Float> entry : labelProbabilities.entrySet()) {
            String label = entry.getKey();
            float probability = entry.getValue();

            // Check if the label is one of the emotions
            if (EMOTIONS.contains(label) && probability > highestProbability) {
                highestEmotion = label;
                highestProbability = probability;
            }
        }

        return new Pair<>(highestEmotion, highestProbability); // Return the emotion and its probability
    }

    public static boolean isEmotion(String label) {
        return EMOTIONS.contains(label);
    }
}
