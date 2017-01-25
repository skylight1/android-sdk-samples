package com.affectiva.framedetectordemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.List;

/**
 * This is a sample app using the FrameDetector object, which is not multi-threaded, and running it on a background thread in a custom object called
 * AsyncFrameDetector.
 *
 * This app also contains sample code for using the camera.
 */
public class MainActivity extends Activity implements CameraView.OnCameraViewEventListener, AsyncFrameDetector.OnDetectorEventListener {

    private static final String LOG_TAG = "Affectiva";

    MetricsPanel metricsPanel; //Fragment to display metric scores

    //UI Elements
    Button sdkButton;
    Button cameraButton;
    TextView processorFPS;
    TextView cameraFPS;
    ToggleButton frontBackToggle;

    //state booleans
    boolean isCameraStarted  = false;
    boolean isCameraFront = true;
    boolean isCameraRequestedByUser = false;
    boolean isSDKRunning = false;

    //variables used to determine the FPS rates of frames sent by the camera and processed by the SDK
    long numberCameraFramesReceived = 0;
    long lastCameraFPSResetTime = -1L;
    long numberSDKFramesReceived = 0;
    long lastSDKFPSResetTime = -1L;

    int startTime = 0;
    //floats to ensure the timestamps we send to FrameDetector are sequentially increasing
    float lastTimestamp = -1f;
    final float epsilon = .01f;
    long firstFrameTime = -1;

    CameraView cameraView; // controls the camera
    AsyncFrameDetector asyncDetector; // runs FrameDetector on a background thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set up metrics view
        metricsPanel = new MetricsPanel();
        getFragmentManager().beginTransaction().add(R.id.fragment_container,metricsPanel).commit();

        //Init TextViews
        cameraFPS = (TextView) findViewById(R.id.camera_fps_text);
        processorFPS = (TextView) findViewById(R.id.processor_fps_text);

        //set up CameraView
        cameraView = (CameraView) findViewById(R.id.camera_view);
        cameraView.setOnCameraViewEventListener(this);

        //set up CameraButton
        cameraButton = (Button) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCameraRequestedByUser) { //Turn camera off
                    isCameraRequestedByUser = false;
                    cameraButton.setText("Start Camera");
                    stopCamera();
                } else { //Turn camera on
                    isCameraRequestedByUser = true;
                    cameraButton.setText("Stop Camera");
                    startCamera();
                }
                resetFPS();
            }
        });

        //Set up front toggle button
        frontBackToggle = (ToggleButton) findViewById(R.id.front_back_toggle_button);
        frontBackToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isCameraFront = !isChecked;
                if (isCameraRequestedByUser) {
                    startCamera();
                }
            }
        });

        asyncDetector = new AsyncFrameDetector(this);
        asyncDetector.setOnDetectorEventListener(this);

        //Set up SDK Button
        sdkButton = (Button) findViewById(R.id.start_sdk_button);
        sdkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKRunning) {
                    isSDKRunning = false;
                    asyncDetector.stop();
                    sdkButton.setText("Start SDK");
                } else {
                    isSDKRunning = true;
                    asyncDetector.start();
                    sdkButton.setText("Stop SDK");
                }
                resetFPS();
            }
        });
        sdkButton.setText("Start SDK");
    }

    void resetFPS() {
        lastCameraFPSResetTime = lastSDKFPSResetTime = SystemClock.elapsedRealtime();
        numberCameraFramesReceived = numberSDKFramesReceived = 0;
    }

    void startCamera() {
        if (isCameraStarted) {
            cameraView.stopCamera();
        }
        cameraView.startCamera(isCameraFront ? CameraHelper.CameraType.CAMERA_FRONT : CameraHelper.CameraType.CAMERA_BACK);
        isCameraStarted = true;
        asyncDetector.reset();
    }

    void stopCamera() {
        if (!isCameraStarted)
            return;

        cameraView.stopCamera();
        isCameraStarted = false;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (isSDKRunning) {
            asyncDetector.start();
        }
        if (isCameraRequestedByUser) {
            startCamera();
        }

        resetFPS();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (asyncDetector.isRunning()) {
            asyncDetector.stop();
        }
        stopCamera();
    }

    private void setMetricTextViewText(Face face) {
        // set the text for all the numeric metrics (scored or measured)
        for (Metrics metric : Metrics.getEmotions()) {
            metricsPanel.setMetricFloatValue(metric,getScore(metric,face));
        }
        for (Metrics metric : Metrics.getExpressions()) {
            metricsPanel.setMetricFloatValue(metric,getScore(metric,face));
        }
        for (Metrics metric : Metrics.getMeasurements()) {
            metricsPanel.setMetricFloatValue(metric,getScore(metric,face));
        }
        for (Metrics metric : Metrics.getQualities()) {
            metricsPanel.setMetricFloatValue(metric,getScore(metric,face));
        }

        // set the text for the appearance metrics
        String textValue="";

        switch (face.appearance.getGlasses()) {
            case NO:
                textValue = "no";
                break;
            case YES:
                textValue = "yes";
                break;
        }
        metricsPanel.setMetricTextValue(Metrics.GLASSES, textValue);

        switch (face.appearance.getGender()) {
            case UNKNOWN:
                textValue = "unknown";
                break;
            case FEMALE:
                textValue = "female";
                break;
            case MALE:
                textValue = "male";
                break;
        }
        metricsPanel.setMetricTextValue(Metrics.GENDER, textValue);

        switch (face.appearance.getAge()) {
            case AGE_UNKNOWN:
                textValue = "unknown";
                break;
            case AGE_UNDER_18:
                textValue = "under 18";
                break;
            case AGE_18_24:
                textValue = "18-24";
                break;
            case AGE_25_34:
                textValue = "25-34";
                break;
            case AGE_35_44:
                textValue = "35-44";
                break;
            case AGE_45_54:
                textValue = "45-54";
                break;
            case AGE_55_64:
                textValue = "55-64";
                break;
            case AGE_65_PLUS:
                textValue = "65+";
                break;
        }
        metricsPanel.setMetricTextValue(Metrics.AGE, textValue);

        switch (face.appearance.getEthnicity()) {
            case UNKNOWN:
                textValue = "unknown";
                break;
            case CAUCASIAN:
                textValue = "caucasian";
                break;
            case BLACK_AFRICAN:
                textValue = "black african";
                break;
            case EAST_ASIAN:
                textValue = "east asian";
                break;
            case SOUTH_ASIAN:
                textValue = "south asian";
                break;
            case HISPANIC:
                textValue = "hispanic";
                break;
        }
        metricsPanel.setMetricTextValue(Metrics.ETHNICITY, textValue);
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
            case CHIN_RAISE:
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
            case INNER_BROW_RAISE:
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
            case NOSE_WRINKLE:
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

    @Override
    public void onCameraFrameAvailable(byte[] frame, int width, int height, Frame.ROTATE rotation) {
        numberCameraFramesReceived += 1;
        cameraFPS.setText(String.format("CAM: %.3f", 1000f * (float) numberCameraFramesReceived / (SystemClock.elapsedRealtime() - lastCameraFPSResetTime)));

        float timestamp = 0;
        long currentTime = SystemClock.elapsedRealtime();
        if (firstFrameTime == -1) {
            firstFrameTime = currentTime;
        } else {
            timestamp = (currentTime - firstFrameTime) / 1000f;
        }

        if (timestamp > (lastTimestamp + epsilon)) {
            lastTimestamp = timestamp;
            asyncDetector.process(createFrameFromData(frame,width,height,rotation),timestamp);
        }
    }

    @Override
    public void onCameraStarted(boolean success, Throwable error) {
        //TODO: change status here
    }

    @Override
    public void onSurfaceViewSizeChanged() {
        asyncDetector.reset();
    }

    float lastReceivedTimestamp = -1f;

    @Override
    public void onImageResults(List<Face> faces, Frame image, float timeStamp) {
        //statusTextView.setText(String.format("Most recent time stamp: %.4f",timeStamp));
        if (timeStamp < lastReceivedTimestamp)
            throw new RuntimeException("Got a timestamp out of order!");
        lastReceivedTimestamp = timeStamp;
        Log.e("MainActivity", String.valueOf(timeStamp));

        if (faces == null)
            return; //No Face Detected
        if (faces.size() ==0) {
            for (Metrics metric : Metrics.values()) {
                metricsPanel.setMetricNA(metric);
            }
        } else {
            Face face = faces.get(0);
            setMetricTextViewText(face);
        }

        numberSDKFramesReceived += 1;
        processorFPS.setText(String.format("SDK: %.3f", 1000f * (float) numberSDKFramesReceived / (SystemClock.elapsedRealtime() - lastSDKFPSResetTime)));

    }

    @Override
    public void onDetectorStarted() {

    }

    static Frame createFrameFromData(byte[] frameData, int width, int height, Frame.ROTATE rotation) {
        Frame.ByteArrayFrame frame = new Frame.ByteArrayFrame(frameData, width, height, Frame.COLOR_FORMAT.YUV_NV21);
        frame.setTargetRotation(rotation);
        return frame;
    }
}
