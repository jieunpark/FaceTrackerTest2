package com.example.jepark.facetrackertest2.camera.tracker;

import com.example.jepark.facetrackertest2.overlay.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

/**
 * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
 * uses this factory to create face trackers as needed -- one for each individual.
 * Created by jepark on 2018. 2. 23..
 */
public class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {

    private GraphicOverlay mGraphicOverlay;

    public GraphicFaceTrackerFactory(GraphicOverlay overlay) {
        mGraphicOverlay = overlay;
    }

    @Override
    public Tracker<Face> create(Face face) {
        return new GraphicFaceTracker(mGraphicOverlay);
    }
}
