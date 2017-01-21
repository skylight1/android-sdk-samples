package com.affectiva.videodetectordemo;

/**
 * An enum representing all metrics currently available in the Affectiva SDK
 */
public enum Metrics {
    //Emotions
    ANGER,
    DISGUST,
    FEAR,
    JOY,
    SADNESS,
    SURPRISE,
    CONTEMPT,
    ENGAGEMENT,
    VALENCE,

    //Expressions
    ATTENTION,
    BROW_FURROW,
    BROW_RAISE,
    CHEEK_RAISE,
    CHIN_RAISER,
    DIMPLER,
    EYE_CLOSURE,
    EYE_WIDEN,
    INNER_BROW_RAISER,
    JAW_DROP,
    LID_TIGHTEN,
    LIP_DEPRESSOR,
    LIP_PRESS,
    LIP_PUCKER,
    LIP_STRETCH,
    LIP_SUCK,
    MOUTH_OPEN,
    NOSE_WRINKLER,
    SMILE,
    SMIRK,
    UPPER_LIP_RAISER,

    //Measurements
    YAW,
    PITCH,
    ROLL,
    INTER_OCULAR_DISTANCE,

    //Qualities
    BRIGHTNESS,

    //Appearance
    GENDER,
    AGE,
    ETHNICITY;

    String getUpperCaseName() {
        return toString().replace("_", " ");
    }

    String getLowerCaseName() {
        return toString().toLowerCase();
    }

    static int numberOfEmotions() {
        return VALENCE.ordinal() + 1;
    }

    static int numberOfExpressions() {
        return UPPER_LIP_RAISER.ordinal() - numberOfEmotions() + 1;
    }

    static int numberOfMeasurements() {
        return INTER_OCULAR_DISTANCE.ordinal() - numberOfEmotions() - numberOfExpressions() + 1;
    }

    static int numberOfQualities() {
        return BRIGHTNESS.ordinal() - numberOfEmotions() - numberOfExpressions()
                - numberOfMeasurements() + 1;
    }

    static int numberOfAppearances() {
        return ETHNICITY.ordinal() - numberOfQualities() - numberOfMeasurements()
                - numberOfExpressions() - numberOfEmotions() + 1;
    }


    static int numberOfMetrics() {
        return Metrics.values().length;
    }

    /**
     * Returns an array to allow for iteration through all Emotions
     */
    static Metrics[] getEmotions() {
        Metrics[] emotions = new Metrics[numberOfEmotions()];
        Metrics[] allMetrics = Metrics.values();
        System.arraycopy(allMetrics, 0, emotions, 0, numberOfEmotions());
        return emotions;
    }

    /**
     * Returns an array to allow for iteration through all Expressions
     */
    static Metrics[] getExpressions() {
        Metrics[] expressions = new Metrics[numberOfExpressions()];
        Metrics[] allMetrics = Metrics.values();
        System.arraycopy(allMetrics, numberOfEmotions(), expressions, 0, numberOfExpressions());
        return expressions;
    }

    static Metrics[] getMeasurements() {
        Metrics[] measurements = new Metrics[numberOfMeasurements()];
        Metrics[] allMetrics = Metrics.values();
        System.arraycopy(allMetrics, numberOfEmotions()+ numberOfExpressions(),
                measurements, 0, numberOfMeasurements());
        return measurements;
    }

    static Metrics[] getQualities() {
        Metrics[] qualities = new Metrics[numberOfQualities()];
        Metrics[] allMetrics = Metrics.values();
        System.arraycopy(allMetrics, numberOfEmotions()+ numberOfExpressions()
                + numberOfMeasurements(), qualities, 0, numberOfQualities());
        return qualities;
    }

    static Metrics[] getAppearances() {
        Metrics[] appearances = new Metrics[numberOfAppearances()];
        Metrics[] allMetrics = Metrics.values();
        System.arraycopy(allMetrics, numberOfEmotions()+ numberOfExpressions()
                + numberOfMeasurements() + numberOfQualities(),
                appearances, 0, numberOfAppearances());
        return appearances;
    }
}