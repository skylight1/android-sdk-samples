package com.affectiva.android.affdex.sdk.samples.serviceframedetector;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

/**
 * The launch Activity for the app.  Handles camera permission request on Marshmallow+ and
 * provides a button to launch the second Activity.
 *
 * See the README.md file at the root of the ServiceFrameDetectorDemo module for more info on this
 * sample app.
 */
public class MainActivity extends BaseActivity {
    private final static int CAMERA_PERMISSIONS_REQUEST_CODE = 0;
    private final static String[] CAMERA_PERMISSIONS_REQUEST = new String[]{Manifest.permission.CAMERA};
    private boolean handleCameraPermissionGrant;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // on Marshmallow+, we have to ask for the camera permission the first time
        if (!CameraHelper.checkPermission(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(CAMERA_PERMISSIONS_REQUEST, CAMERA_PERMISSIONS_REQUEST_CODE);
        }

        // hook up a click handler for the "2nd activity" button
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SecondActivity.class));
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onResume() {
        super.onResume();
        if (handleCameraPermissionGrant) {
            // a response to our camera permission request was received
            if (CameraHelper.checkPermission(this)) {
                startService(new Intent(this, DetectorService.class));
            } else {
                ((TextView)findViewById(R.id.text)).setText(R.string.camera_permission_denied);
                findViewById(R.id.button).setVisibility(View.INVISIBLE);
            }
            handleCameraPermissionGrant = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSIONS_REQUEST_CODE) {
            for (String permission : permissions) {
                if (permission.equals(Manifest.permission.CAMERA)) {
                    // next time through onResume, handle the grant result
                    handleCameraPermissionGrant = true;
                    break;
                }
            }
        }
    }
}
