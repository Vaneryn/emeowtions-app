package com.example.emeowtions.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.emeowtions.enums.CatBackground;
import com.example.emeowtions.enums.Gender;
import com.example.emeowtions.enums.MedicalConditions;
import com.example.emeowtions.models.BehaviourStrategy;
import com.example.emeowtions.models.Cat;
import com.example.emeowtions.models.Recommendation;
import com.example.emeowtions.models.RecommendedBehaviourStrategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Recommender {

    private static final String TAG = "Recommender";

    // Configuration
    private Context context;
    private BreedUtils breedUtils;
    private boolean isInitialized;

    // Firebase variables
    private FirebaseFirestore db;
    private CollectionReference stratsRef;

    // Recommendation factors
    private String emotion;
    private String age;
    private String gender;
    private String temperament;
    private String activityLevel;
    private String background;
    private String medicalConditions;

    public interface Callback {
        void onComplete(ArrayList<BehaviourStrategy> strats);
    }

    public Recommender(Context context) {
        this.context = context;
        this.breedUtils = new BreedUtils(context);
        this.isInitialized = false;
        this.db = FirebaseFirestore.getInstance();
        this.stratsRef = db.collection("behaviourStrategies");
    }

    public void initialize(String emotion, Cat cat) {
        this.isInitialized = true;
        this.emotion = emotion;

        if (cat != null) {
            this.age = categorizeAge(cat.getDateOfBirth());
            this.gender = categorizeGender(cat.getGender());
            this.temperament = categorizeTemperament(cat.getBreed());
            this.activityLevel = categorizeActivityLevel(cat.getBreed());
            this.background = categorizeBackground(cat.getBackground());
            this.medicalConditions = categorizeMedicalConditions(cat.getMedicalConditions());
        }

        Log.d(TAG, "initialize: emotion=" + this.emotion);
        Log.d(TAG, "initialize: age=" + this.age);
        Log.d(TAG, "initialize: gender=" + this.gender);
        Log.d(TAG, "initialize: temperament=" + this.temperament);
        Log.d(TAG, "initialize: activityLevel=" + this.activityLevel);
        Log.d(TAG, "initialize: background=" + this.background);
        Log.d(TAG, "initialize: medicalConditions=" + this.medicalConditions);
    }

    // Retrieves all available behaviour strategies
    public void retrieveRecommendations(Callback callback) {
        stratsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(context, "No recommended behaviour strategies found.", Toast.LENGTH_SHORT).show();
                        callback.onComplete(new ArrayList<>());
                    } else {
                        ArrayList<BehaviourStrategy> strats = generateRecommendation(queryDocumentSnapshots.getDocuments());
                        callback.onComplete(strats);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "generateRecommendation: Failed to retrieve behaviourStrategies", e);
                    Toast.makeText(context, "An error occurred while trying to generate recommendations.", Toast.LENGTH_SHORT).show();
                    callback.onComplete(new ArrayList<>());
                });
    }

    private ArrayList<BehaviourStrategy> generateRecommendation(List<DocumentSnapshot> documentSnapshots) {
        if (documentSnapshots.isEmpty()) {
            return null;
        } else {
            // Filter through available behaviour strategies to find the ones the match the emotion type and recommendation factors
            ArrayList<BehaviourStrategy> matchedStrats = new ArrayList<>();

            if (this.emotion != null) {
                // Categorize emotion
                String emotionType = (this.emotion.equals("happy") || this.emotion.equals("neutral")) ? POSITVE : NEGATIVE;

                for (DocumentSnapshot documentSnapshot : documentSnapshots) {
                    BehaviourStrategy strat = documentSnapshot.toObject(BehaviourStrategy.class);
                    String stratEmotionType = strat.getEmotionType();
                    String factorType = strat.getFactorType();
                    String factorValue = strat.getFactorValue();

                    // General
                    if (stratEmotionType.equals(emotionType) && factorType == null) {
                        matchedStrats.add(strat);
                    } else if (factorType != null) {
                        // Age
                        if (factorType.equals(AGE)) {
                            if (stratEmotionType.equals(emotionType) && factorValue.equals(this.age)) {
                                matchedStrats.add(strat);
                            }
                        }
                        // Gender
                        else if (factorType.equals(GENDER)) {
                            if (stratEmotionType.equals(emotionType) && factorValue.equals(this.gender)) {
                                matchedStrats.add(strat);
                            }
                        }
                        // Temperament
                        else if (factorType.equals(TEMPERAMENT)) {
                            if (stratEmotionType.equals(emotionType) && factorValue.equals(this.temperament)) {
                                matchedStrats.add(strat);
                            }
                        }
                        // Activity Level
                        else if (factorType.equals(ACTIVITY_LEVEL)) {
                            if (stratEmotionType.equals(emotionType) && factorValue.equals(this.activityLevel)) {
                                matchedStrats.add(strat);
                            }
                            // For MODERATE and VARIABLE Activity Level, recommend both LOW and HIGH
                            else if ((stratEmotionType.equals(emotionType)) &&
                                    (this.activityLevel != null) &&
                                    (this.activityLevel.equals(MODERATE) || this.activityLevel.equals(VARIABLE))) {
                                matchedStrats.add(strat);
                            }
                        }
                        // Background
                        else if (factorType.equals(BACKGROUND)) {
                            if (stratEmotionType.equals(emotionType) && factorValue.equals(this.background)) {
                                matchedStrats.add(strat);
                            }
                        }
                        // Medical Conditions
                        else if (factorType.equals(MEDICAL_CONDITIONS)) {
                            // No recommendations for NO Medical Conditions
                            if (stratEmotionType.equals(emotionType) && factorValue.equals(this.medicalConditions)) {
                                matchedStrats.add(strat);
                            }
                        }
                    }
                }
            }

            // Return strategies
            return matchedStrats;
        }
    }

    // TODO: Script for generating BehaviourStrategies. To be removed.
    public void generateStrategies() {
        // IDs
        List<String> ids = new ArrayList<>();
        // General
        ids.add(GENERAL_POSITIVE1);
        ids.add(GENERAL_POSITIVE2);
        ids.add(GENERAL_NEGATIVE1);
        ids.add(GENERAL_NEGATIVE2);
        // Age
        ids.add(AGE_KITTEN_POSITIVE);
        ids.add(AGE_KITTEN_NEGATIVE);
        ids.add(AGE_ADULT_POSITIVE);
        ids.add(AGE_ADULT_NEGATIVE);
        ids.add(AGE_SENIOR_POSITIVE);
        ids.add(AGE_SENIOR_NEGATIVE);
        // Gender
        ids.add(GENDER_MALE_POSITIVE);
        ids.add(GENDER_MALE_NEGATIVE);
        ids.add(GENDER_FEMALE_POSITIVE);
        ids.add(GENDER_FEMALE_NEGATIVE);
        // Temperament
        ids.add(TEMPERAMENT_AFFECTIONATE_POSITIVE);
        ids.add(TEMPERAMENT_AFFECTIONATE_NEGATIVE);
        ids.add(TEMPERAMENT_GENTLE_POSITIVE);
        ids.add(TEMPERAMENT_GENTLE_NEGATIVE);
        ids.add(TEMPERAMENT_PLAYFUL_POSITIVE);
        ids.add(TEMPERAMENT_PLAYFUL_NEGATIVE);
        // Activity Level
        ids.add(ACTIVITY_LOW_POSITIVE);
        ids.add(ACTIVITY_LOW_NEGATIVE);
        ids.add(ACTIVITY_HIGH_POSITIVE);
        ids.add(ACTIVITY_HIGH_NEGATIVE);
        // Background
        ids.add(BACKGROUND_DOMESTIC_POSITIVE);
        ids.add(BACKGROUND_DOMESTIC_NEGATIVE);
        ids.add(BACKGROUND_STRAY_POSITIVE);
        ids.add(BACKGROUND_STRAY_NEGATIVE);
        ids.add(BACKGROUND_FERAL_POSITIVE);
        ids.add(BACKGROUND_FERAL_NEGATIVE);
        // Medical Conditions
        ids.add(MEDICAL_YES_POSITIVE);
        ids.add(MEDICAL_YES_NEGATIVE);

        // Behaviour Strategies
        List<BehaviourStrategy> strats = new ArrayList<>();

        // General
        // General Positive
        strats.add(new BehaviourStrategy(GENERAL_POSITIVE1,
                "Keep up whatever you're doing, as the cat is displaying positive emotions.",
                null, null,  null, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(GENERAL_POSITIVE2,
                "Interact through playing or petting to stimulate and maintain good mental health.",
                null, null,  null, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // General Negative
        strats.add(new BehaviourStrategy(GENERAL_NEGATIVE1,
                "Remove surrounding sources of stress or anxiety such as strangers, noises, or other pets.",
                null, null,  null, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(GENERAL_NEGATIVE2,
                "Avoid further interaction with the cat, give it a safe space to retreat and calm down.",
                null, null,  null, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Age
        // Kitten
        strats.add(new BehaviourStrategy(AGE_KITTEN_POSITIVE,
                "Encourage playtime with toys to promote healthy growth and bonding.",
                "age1", AGE, KITTEN, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(AGE_KITTEN_NEGATIVE,
                "Gradually introduce new experiences including interactions to avoid overwhelming the kitten.",
                "age1", AGE, KITTEN, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Adult
        strats.add(new BehaviourStrategy(AGE_ADULT_POSITIVE,
                "Continue with daily interactions and provide stimulating activities.",
                "age2", AGE, ADULT, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(AGE_ADULT_NEGATIVE,
                "Consider other factors like breed, background, medical conditions. Consult a vet if issues persist.",
                "age2", AGE, ADULT, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Senior
        strats.add(new BehaviourStrategy(AGE_SENIOR_POSITIVE,
                "Provide quiet and comfortable resting places, ensure easy access to food, water, and litter. This helps ease mobility issues.",
                "age3", AGE, SENIOR, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(AGE_SENIOR_NEGATIVE,
                "Minimize changes in routine or environment, as older cats are more sensitive to changes, causing stress.",
                "age3", AGE, SENIOR, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Gender
        // Male
        strats.add(new BehaviourStrategy(GENDER_MALE_POSITIVE,
                "Ensure sufficient territory and space, as male cats are more territorial.",
                "gender1", GENDER, MALE, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(GENDER_MALE_NEGATIVE,
                "Neutering male cats may reduce aggressive or territorial behaviour.",
                "gender1", GENDER, MALE, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Female
        strats.add(new BehaviourStrategy(GENDER_FEMALE_POSITIVE,
                "Provide calm and secure resting place, especially if the cat is pregnant.",
                "gender2", GENDER, FEMALE, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(GENDER_FEMALE_NEGATIVE,
                "Avoid stressing the cat through loud noise or excessive interaction, especially when pregnant or nursing.",
                "gender2", GENDER, FEMALE, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Temperament
        // Affectionate
        strats.add(new BehaviourStrategy(TEMPERAMENT_AFFECTIONATE_POSITIVE,
                "Provide plenty of interaction and attention as this breed thrives on affection.",
                "temperament1", TEMPERAMENT, AFFECTIONATE, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(TEMPERAMENT_AFFECTIONATE_NEGATIVE,
                "Avoid leaving them alone for extended periods, as it may lead to loneliness or anxiety.",
                "temperament1", TEMPERAMENT, AFFECTIONATE, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Gentle
        strats.add(new BehaviourStrategy(TEMPERAMENT_GENTLE_POSITIVE,
                "Provide a calm and relaxing environment with soft resting spots.",
                "temperament2", TEMPERAMENT, GENTLE, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(TEMPERAMENT_GENTLE_NEGATIVE,
                "Minimize disruptions and loud noises, as the cat may be easily stressed.",
                "temperament2", TEMPERAMENT, GENTLE, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Playful
        strats.add(new BehaviourStrategy(TEMPERAMENT_PLAYFUL_POSITIVE,
                "Provide plenty of toys and playtime to keep the cat entertained.",
                "temperament3", TEMPERAMENT, PLAYFUL, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(TEMPERAMENT_PLAYFUL_NEGATIVE,
                "Introduce new toys or challenges to increase stimulation, as playful cats may play rough if not stimulated for long periods.",
                "temperament3", TEMPERAMENT, PLAYFUL, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Activity Level
        // Low
        strats.add(new BehaviourStrategy(ACTIVITY_LOW_POSITIVE,
                "Provide cozy and quiet spaces where they can relax without interruptions.",
                "activityLevel1", ACTIVITY_LEVEL, LOW, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(ACTIVITY_LOW_NEGATIVE,
                "Reduce stimuli or noise if they appear anxious, as this breed may prefer a slower pace and calm environment.",
                "activityLevel1", ACTIVITY_LEVEL, LOW, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // High
        strats.add(new BehaviourStrategy(ACTIVITY_HIGH_POSITIVE,
                "Offer climbing structures, tunnels, high spaces, and toys to satisfy their energetic nature.",
                "activityLevel3", ACTIVITY_LEVEL, HIGH, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(ACTIVITY_HIGH_NEGATIVE,
                "Increase playtime if they are showing signs of frustration due to lack of stimulation.",
                "activityLevel3", ACTIVITY_LEVEL, HIGH, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Background
        // Domestic
        strats.add(new BehaviourStrategy(BACKGROUND_DOMESTIC_POSITIVE,
                "Maintain a familiar environment and routine, as domestic cats thrive on consistency.",
                "background1", BACKGROUND, DOMESTIC, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(BACKGROUND_DOMESTIC_NEGATIVE,
                "Avoid sudden changes in routine, as this may cause stress.",
                "background1", BACKGROUND, DOMESTIC, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Stray
        strats.add(new BehaviourStrategy(BACKGROUND_STRAY_POSITIVE,
                "Consistently socialize and interact to improve trust and bond.",
                "background2", BACKGROUND, STRAY, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(BACKGROUND_STRAY_NEGATIVE,
                "Provide space and minimize interaction if the cat appears scared, as strays may not be fully accustomed to human interaction.",
                "background2", BACKGROUND, STRAY, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        // Feral
        strats.add(new BehaviourStrategy(BACKGROUND_FERAL_POSITIVE,
                "Respect their space and privacy, minimize direct interaction, as feral cats prefer to be independent.",
                "background3", BACKGROUND, FERAL, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(BACKGROUND_FERAL_NEGATIVE,
                "Avoid interaction and allow the cat to retreat to their safe zones when they feel threatened.",
                "background3", BACKGROUND, FERAL, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Medical Conditions
        // Yes
        strats.add(new BehaviourStrategy(MEDICAL_YES_POSITIVE,
                "Provide easy access to food, water, and litter to reduce their need to move around, reducing risk of stress.",
                "medicalConditions1", MEDICAL_CONDITIONS, YES, POSITVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));
        strats.add(new BehaviourStrategy(MEDICAL_YES_NEGATIVE,
                "Communicate closely with your vet to monitor behaviour, as negative emotions often indicate health issues.",
                "medicalConditions1", MEDICAL_CONDITIONS, YES, NEGATIVE,
                0, 0,
                Timestamp.now(), Timestamp.now()
        ));

        // Push to Firestore
        for (int i = 0; i < ids.size(); i++) {
            final int idx = i;

            stratsRef.document(ids.get(idx))
                    .set(strats.get(idx))
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "generateStrategies: Set " + ids.get(idx));
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "generateStrategies: Failed to set BehaviourStrategy", e);
                    });
        }
    }

    // Converts dateOfBirth into age category
    private String categorizeAge(Timestamp dob) {
        if (dob == null) {
            return null;
        } else {
            // Convert the Firestore Timestamp to a Date object
            Date birthDate = dob.toDate();
            Date currentDate = new Date(); // Get the current date

            // Calculate the time difference in milliseconds
            long timeDifferenceMillis = currentDate.getTime() - birthDate.getTime();

            // Convert time difference to days
            long daysDifference = TimeUnit.MILLISECONDS.toDays(timeDifferenceMillis);

            // Calculate the age in years
            int years = (int) (daysDifference / 365);

            // Kitten: less than 1 year old
            if (years < 1) {
                return "Kitten";
            }
            // Adult: between 1 to 11 years old
            else if (years < 11) {
                return "Adult";
            }
            // Senior: 11 years old or older
            else {
                return "Senior";
            }
        }
    }

    // Check if gender is unspecified
    private String categorizeGender(String gender) {
        if (gender.equals(Gender.UNSPECIFIED.getTitle())) {
            return null;
        } else {
            return gender;
        }
    }

    // Gets temperament of breed
    private String categorizeTemperament(String breed) {
        return breedUtils.getTemperament(breed);
    }

    // Gets activity level of breed
    private String categorizeActivityLevel(String breed) {
        return breedUtils.getActivityLevel(breed);
    }

    // Check if background is unspecified
    private String categorizeBackground(String background) {
        if (background.equals(CatBackground.UNSPECIFIED.getTitle())) {
            return null;
        } else {
            return background;
        }
    }

    // Check if medicalConditions is unspecified
    private String categorizeMedicalConditions(String medicalConditions) {
        if (medicalConditions.equals(MedicalConditions.UNSPECIFIED.getTitle())) {
            return null;
        } else {
            return medicalConditions;
        }
    }

    // Constant strings
    // IDs
    private static final String GENERAL_POSITIVE1 = "generalPositive1";
    private static final String GENERAL_POSITIVE2 = "generalPositive2";
    private static final String GENERAL_NEGATIVE1 = "generalNegative1";
    private static final String GENERAL_NEGATIVE2 = "generalNegative2";

    private static final String AGE_KITTEN_POSITIVE = "ageKittenPositive";
    private static final String AGE_KITTEN_NEGATIVE = "ageKittenNegative";
    private static final String AGE_ADULT_POSITIVE = "ageAdultPositive";
    private static final String AGE_ADULT_NEGATIVE = "ageAdultNegative";
    private static final String AGE_SENIOR_POSITIVE = "ageSeniorPositive";
    private static final String AGE_SENIOR_NEGATIVE = "ageSeniorNegative";

    private static final String GENDER_MALE_POSITIVE = "genderMalePositive";
    private static final String GENDER_MALE_NEGATIVE = "genderMaleNegative";
    private static final String GENDER_FEMALE_POSITIVE = "genderFemalePositive";
    private static final String GENDER_FEMALE_NEGATIVE = "genderFemaleNegative";

    private static final String TEMPERAMENT_AFFECTIONATE_POSITIVE = "temperamentAffectionatePositive";
    private static final String TEMPERAMENT_AFFECTIONATE_NEGATIVE = "temperamentAffectionateNegative";
    private static final String TEMPERAMENT_GENTLE_POSITIVE = "temperamentGentlePositive";
    private static final String TEMPERAMENT_GENTLE_NEGATIVE = "temperamentGentleNegative";
    private static final String TEMPERAMENT_PLAYFUL_POSITIVE = "temperamentPlayfulPositive";
    private static final String TEMPERAMENT_PLAYFUL_NEGATIVE = "temperamentPlayfulNegative";

    private static final String ACTIVITY_LOW_POSITIVE = "activityLowPositive";
    private static final String ACTIVITY_LOW_NEGATIVE = "activityLowNegative";
    private static final String ACTIVITY_HIGH_POSITIVE = "activityHighPositive";
    private static final String ACTIVITY_HIGH_NEGATIVE = "activityHighNegative";

    private static final String BACKGROUND_DOMESTIC_POSITIVE = "backgroundDomesticPositive";
    private static final String BACKGROUND_DOMESTIC_NEGATIVE = "backgroundDomesticNegative";
    private static final String BACKGROUND_STRAY_POSITIVE = "backgroundStrayPositive";
    private static final String BACKGROUND_STRAY_NEGATIVE = "backgroundStrayNegative";
    private static final String BACKGROUND_FERAL_POSITIVE = "backgroundFeralPositive";
    private static final String BACKGROUND_FERAL_NEGATIVE = "backgroundFeralNegative";

    private static final String MEDICAL_YES_POSITIVE = "medicalYesPositive";
    private static final String MEDICAL_YES_NEGATIVE = "medicalYesNegative";

    // Emotion types
    private static final String POSITVE = "Positive";
    private static final String NEGATIVE = "Negative";
    // Factor types
    private static final String AGE = "Age";
    private static final String GENDER = "Gender";
    private static final String TEMPERAMENT = "Temperament";
    private static final String ACTIVITY_LEVEL = "Activity Level";
    private static final String BACKGROUND = "Background";
    private static final String MEDICAL_CONDITIONS = "Medical Conditions";
    // FActor values
    private static final String MALE = "Male";
    private static final String FEMALE = "Female";
    private static final String KITTEN = "Kitten";
    private static final String ADULT = "Adult";
    private static final String SENIOR = "Senior";
    private static final String AFFECTIONATE = "Affectionate";
    private static final String GENTLE = "Gentle";
    private static final String PLAYFUL = "Playful";
    private static final String LOW = "Low";
    private static final String MODERATE = "Moderate";
    private static final String HIGH = "High";
    private static final String VARIABLE = "Variable";
    private static final String DOMESTIC = "Domestic";
    private static final String STRAY = "Stray";
    private static final String FERAL = "Feral";
    private static final String YES = "Yes";
    private static final String NO = "No";
}
