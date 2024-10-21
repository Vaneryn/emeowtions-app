package com.example.emeowtions.utils;

import com.example.emeowtions.models.BodyLanguage;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashSet;

public class BodyLanguageExtractor {

    public static final ArrayList<BodyLanguage> BODY_LANGUAGES = new ArrayList<>();

    static {
        BODY_LANGUAGES.add(new BodyLanguage("ears_flat", "Ears Flat", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("ears_up", "Ears Up", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("eyes_large_pupils", "Large Pupils", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("eyes_narrowed", "Eyes Narrowed", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("eyes_small_pupils", "Small Pupils", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("mouth_fangs", "Mouth Fangs", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("posture_arched_back", "Arched Back", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("posture_exposed_belly", "Exposed Belly", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("posture_neutral", "Neutral Posture", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("posture_small", "Small Posture", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("posture_stretch", "Stretching", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("tail_neutral", "Neutral Tail", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("tail_tucked", "Tail Tucked", "Lorem ipsum", 0));
        BODY_LANGUAGES.add(new BodyLanguage("tail_up", "Tail Up", "Lorem ipsum", 0));
    }

    public static boolean isBodyLanguage(String label) {
        for (BodyLanguage bodyLanguage : BODY_LANGUAGES) {
            if (bodyLanguage.getKey().equals(label)) {
                return true;
            }
        }
        return false;
    }

    public static String getValue(String label) {
        for (BodyLanguage bodyLanguage : BODY_LANGUAGES) {
            if (bodyLanguage.getKey().equals(label)) {
                return bodyLanguage.getValue();
            }
        }
        return null;
    }

    public static String getDescription(String label) {
        for (BodyLanguage bodyLanguage : BODY_LANGUAGES) {
            if (bodyLanguage.getKey().equals(label)) {
                return bodyLanguage.getDescription();
            }
        }
        return null;
    }
}
