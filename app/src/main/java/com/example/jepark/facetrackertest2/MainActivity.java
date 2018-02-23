package com.example.jepark.facetrackertest2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.example.jepark.facetrackertest2.camera.CameraSourcePreview;
import com.example.jepark.facetrackertest2.camera.tracker.GraphicFaceTrackerFactory;
import com.example.jepark.facetrackertest2.overlay.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;

import com.example.jepark.facetrackertest2.camera.Camera2Source;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private Context mContext;

    private Camera2Source mCamera2Source = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;

    private ImageView imgPreview;
    private Button btnOK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        initLayout();

        if (checkGooglePlayAvailability()) {
            requestPermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage("This application cannot run because it does not have the camera permission.  The application will now exit.")
                .setPositiveButton("OK", listener)
                .show();

    }

    //==============================================================================================
    // Permission
    //==============================================================================================

    /**
     * 권한 체크
     */
    private void requestPermissions() {
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * 카메라 권한 받기
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, "Access to the camera is needed for detection",
                Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", listener)
                .show();
    }

    /**
     * Google Play Service 사용가능여부
     * check that the device has play services available.
     * @return
     */
    private boolean checkGooglePlayAvailability() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(mContext);
        if(resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            if(googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(MainActivity.this, resultCode, 2404).show();
            }
        }
        return false;
    }

    /**
     * 레이아웃 초기화
     */
    private void initLayout() {
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        btnOK = (Button) findViewById(R.id.btnOK);
        btnOK.setOnClickListener(mOnClickListener);
        imgPreview = (ImageView) findViewById(R.id.imgPreview);
    }

    /**
     * 카메라 생성
     */
    private void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(mContext)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory(mGraphicOverlay))
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCamera2Source = new Camera2Source.Builder(mContext, detector)
                .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                .setFacing(Camera2Source.CAMERA_FACING_BACK)
                .build();

    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (mCamera2Source != null) {
            try {
                mPreview.start(mCamera2Source, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCamera2Source.release();
                mCamera2Source = null;
            }
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.btnOK) {
                startGif();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        endGif();
                    }
                },3000);
            }
        }
    };

    /**
     * Gif 생성 시작
     */
    private void startGif() {
        mCamera2Source.setPreviewCallBack(new Camera2Source.PreviewFrameCallback() {
            @Override
            public void onPreviewFrame(byte[] byteBuffer, int width, int height) {
                Log.d(TAG, ">>> onPreviewFrame ");
                byte[] data = NV21toJPEG(byteBuffer, width, height, 100);

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

                final Bitmap finalBitmap = bitmap;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgPreview.setImageBitmap(finalBitmap);
                    }
                });
            }
        });
    }

    /**
     * Gif 완료
     */
    private void endGif() {
        mCamera2Source.setPreviewCallBack(null);
    }

    /**
     * nv21 을 JPEG 데이터로 변환
     * @param nv21
     * @param width
     * @param height
     * @param quality
     * @return
     */
    public byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }
}
