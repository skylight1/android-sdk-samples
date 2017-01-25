package com.affectiva.videodetectordemo;

import android.app.Activity;
import android.graphics.PointF;
import android.util.Log;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;
import com.affectiva.android.affdex.sdk.detector.VideoFileDetector;

import java.util.List;

/**
 * A thread to manage the VideoDetector.
 * <p>
 * Note: This is required since running the VideoDetector in the main thread will crash the application.
 */
public class VideoDetectorThread extends Thread implements Detector.ImageListener {

    private static String LOG_TAG = "Affectiva";
    private String filename;
    private VideoFileDetector detector;
    private Activity activity;
    private DrawingView drawingView;
    private MetricsPanel metricsPanel;
    private volatile boolean abortRequested;
    private Object completeSignal = new Object();

    public VideoDetectorThread(String file, Activity context, MetricsPanel metricsPanel, DrawingView drawingView) {
        filename = file;
        activity = context;
        this.drawingView = drawingView;
        this.metricsPanel = metricsPanel;
    }

    @Override
    public void run() {
        detector = new VideoFileDetector(activity, filename, 1, Detector.FaceDetectorMode.LARGE_FACES);
        detector.setDetectAllEmotions(true);
        detector.setDetectAllExpressions(true);
        detector.setDetectAllAppearances(true);
        detector.setImageListener(this);
        try {
            detector.start();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            if (detector.isRunning()) {
                detector.stop();
            }
            // notify waiting threads that we're done
            synchronized (completeSignal) {
                completeSignal.notify();
            }
        }
    }

    @Override
    public void onImageResults(List<Face> list, Frame image, final float timestamp) {

        final Frame frame = image;
        final List<Face> faces = list;

        if (abortRequested) {
            detector.stop();
            abortRequested = false;
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @SuppressWarnings("SuspiciousNameCombination")
            @Override
            public void run() {
                //update metrics
                if (faces != null && faces.size() > 0) {
                    Face face = faces.get(0);
                    // process the numeric metrics (scored or measured)
                    Metrics[] allMetrics = Metrics.values();
                    for (int n = 0; n < Metrics.numberOfEmotions() + Metrics.numberOfExpressions()
                            + Metrics.numberOfMeasurements() + Metrics.numberOfQualities(); n++) {
                        Metrics metric = allMetrics[n];
                        metricsPanel.setMetricValue(metric, getScore(metric, face));
                    }

                    // set the text for the appearance metrics
                    int resId = 0;
                    switch (face.appearance.getGender()) {
                        case UNKNOWN:
                            resId = R.string.unknown;
                            break;
                        case FEMALE:
                            resId = R.string.gender_female;
                            break;
                        case MALE:
                            resId = R.string.gender_male;
                            break;
                    }
                    metricsPanel.setMetricText(Metrics.GENDER, resId);

                    switch (face.appearance.getAge()) {
                        case AGE_UNKNOWN:
                            resId = R.string.unknown;
                            break;
                        case AGE_UNDER_18:
                            resId = R.string.age_under_18;
                            break;
                        case AGE_18_24:
                            resId = R.string.age_18_24;
                            break;
                        case AGE_25_34:
                            resId = R.string.age_25_34;
                            break;
                        case AGE_35_44:
                            resId = R.string.age_35_44;
                            break;
                        case AGE_45_54:
                            resId = R.string.age_45_54;
                            break;
                        case AGE_55_64:
                            resId = R.string.age_55_64;
                            break;
                        case AGE_65_PLUS:
                            resId = R.string.age_65_plus;
                            break;
                    }
                    metricsPanel.setMetricText(Metrics.AGE, resId);

                    switch (face.appearance.getEthnicity()) {
                        case UNKNOWN:
                            resId = R.string.unknown;
                            break;
                        case CAUCASIAN:
                            resId = R.string.ethnicity_caucasian;
                            break;
                        case BLACK_AFRICAN:
                            resId = R.string.ethnicity_black_african;
                            break;
                        case EAST_ASIAN:
                            resId = R.string.ethnicity_east_asian;
                            break;
                        case SOUTH_ASIAN:
                            resId = R.string.ethnicity_south_asian;
                            break;
                        case HISPANIC:
                            resId = R.string.ethnicity_hispanic;
                            break;
                    }
                    metricsPanel.setMetricText(Metrics.ETHNICITY, resId);


                    PointF[] facePoints = face.getFacePoints();
                    int frameWidth = frame.getWidth();
                    int frameHeight = frame.getHeight();
                    Frame.ROTATE rotate = frame.getTargetRotation();

                    if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
                        int temp = frameWidth;
                        frameWidth = frameHeight;
                        frameHeight = temp;
                    }
                    drawingView.drawFrame(frame, facePoints);
                } else {
                    for (Metrics metric : Metrics.values()) {
                        metricsPanel.setMetricNA(metric);
                    }
                    drawingView.drawFrame(frame, null);
                }

            }
        });
    }

    /**
     * If detection is in progress, abort.  This call will wait for detection to stop before
     * returning.
     */
    void abort() {
        if (isAlive()) {
            // set a flag which will be monitored in onImageResults
            abortRequested = true;

            // wait for background thread to finish before returning
            synchronized (completeSignal) {
                try {
                    completeSignal.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private float getScore(Metrics metric, Face face) {

        float score;

        switch (metric) {
            case ANGER:
                score = face.emotions.getAnger();
                break;
            case CONTEMPT:
                score = face.emotions.getContempt();
                break;
            case DISGUST:
                score = face.emotions.getDisgust();
                break;
            case FEAR:
                score = face.emotions.getFear();
                break;
            case JOY:
                score = face.emotions.getJoy();
                break;
            case SADNESS:
                score = face.emotions.getSadness();
                break;
            case SURPRISE:
                score = face.emotions.getSurprise();
                break;
            case ATTENTION:
                score = face.expressions.getAttention();
                break;
            case BROW_FURROW:
                score = face.expressions.getBrowFurrow();
                break;
            case BROW_RAISE:
                score = face.expressions.getBrowRaise();
                break;
            case CHEEK_RAISE:
                score = face.expressions.getCheekRaise();
                break;
            case CHIN_RAISER:
                score = face.expressions.getChinRaise();
                break;
            case DIMPLER:
                score = face.expressions.getDimpler();
                break;
            case ENGAGEMENT:
                score = face.emotions.getEngagement();
                break;
            case EYE_CLOSURE:
                score = face.expressions.getEyeClosure();
                break;
            case EYE_WIDEN:
                score = face.expressions.getEyeWiden();
                break;
            case INNER_BROW_RAISER:
                score = face.expressions.getInnerBrowRaise();
                break;
            case JAW_DROP:
                score = face.expressions.getJawDrop();
                break;
            case LID_TIGHTEN:
                score = face.expressions.getLidTighten();
                break;
            case LIP_DEPRESSOR:
                score = face.expressions.getLipCornerDepressor();
                break;
            case LIP_PRESS:
                score = face.expressions.getLipPress();
                break;
            case LIP_PUCKER:
                score = face.expressions.getLipPucker();
                break;
            case LIP_STRETCH:
                score = face.expressions.getLipStretch();
                break;
            case LIP_SUCK:
                score = face.expressions.getLipSuck();
                break;
            case MOUTH_OPEN:
                score = face.expressions.getMouthOpen();
                break;
            case NOSE_WRINKLER:
                score = face.expressions.getNoseWrinkle();
                break;
            case SMILE:
                score = face.expressions.getSmile();
                break;
            case SMIRK:
                score = face.expressions.getSmirk();
                break;
            case UPPER_LIP_RAISER:
                score = face.expressions.getUpperLipRaise();
                break;
            case VALENCE:
                score = face.emotions.getValence();
                break;
            case YAW:
                score = face.measurements.orientation.getYaw();
                break;
            case ROLL:
                score = face.measurements.orientation.getRoll();
                break;
            case PITCH:
                score = face.measurements.orientation.getPitch();
                break;
            case INTER_OCULAR_DISTANCE:
                score = face.measurements.getInterocularDistance();
                break;
            case BRIGHTNESS:
                score = face.qualities.getBrightness();
                break;
            default:
                score = Float.NaN;
                break;
        }
        return score;
    }
}
