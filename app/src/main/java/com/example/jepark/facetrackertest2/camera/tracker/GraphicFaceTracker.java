package com.example.jepark.facetrackertest2.camera.tracker;

import android.util.Log;

import com.example.jepark.facetrackertest2.overlay.FaceGraphic;
import com.example.jepark.facetrackertest2.overlay.GraphicOverlay;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Face tracker for each detected individual. This maintains a face graphic within the app's
 * associated face overlay.
 * Created by jepark on 2018. 2. 23..
 */

public class GraphicFaceTracker extends Tracker<Face> {

    private static final String TAG = "GraphicFaceTracker";

    private GraphicOverlay mOverlay;
    private FaceGraphic mFaceGraphic;

    public GraphicFaceTracker(GraphicOverlay overlay) {
        mOverlay = overlay;
        mFaceGraphic = new FaceGraphic(overlay);
    }

    /**
     * Start tracking the detected face instance within the face overlay.
     */
    @Override
    public void onNewItem(int faceId, Face item) {
        mFaceGraphic.setId(faceId);
    }

    /**
     * Update the position/characteristics of the face within the overlay.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
        Log.e(TAG,">>> Face : " + face);
        mOverlay.add(mFaceGraphic);
        mFaceGraphic.updateFace(face);
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily (e.g., if the face was momentarily blocked from
     * view).
     */
    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        mOverlay.remove(mFaceGraphic);
    }

    /**
     * Called when the face is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mFaceGraphic);
    }
}
