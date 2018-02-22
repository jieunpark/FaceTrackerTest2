/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.jepark.facetrackertest2.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;

import com.example.jepark.facetrackertest2.overlay.GraphicOverlay;
import com.example.jepark.facetrackertest2.utils.Utils;
import com.google.android.gms.common.images.Size;

import java.io.IOException;

public class CameraSourcePreview extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";

    private Context mContext;
    private AutoFitTextureView mAutoFitTextureView;

    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private Camera2Source mCamera2Source;

    private GraphicOverlay mOverlay;
    private int screenRotation;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;
        screenRotation = Utils.getScreenRotation(context);

        mAutoFitTextureView = new AutoFitTextureView(context);
        mAutoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        addView(mAutoFitTextureView);
    }

    public void start(Camera2Source camera2Source) throws IOException {
        if (camera2Source == null) {
            stop();
        }

        mCamera2Source = camera2Source;

        if (mCamera2Source != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(Camera2Source camera2Source, GraphicOverlay overlay) throws IOException {
        mOverlay = overlay;
        start(camera2Source);
    }

    public void stop() {
        if (mCamera2Source != null) {
            mCamera2Source.stop();
        }
    }

    public void release() {
        if (mCamera2Source != null) {
            mCamera2Source.release();
            mCamera2Source = null;
        }
    }

    private void startIfReady() throws IOException {
        if (mStartRequested && mSurfaceAvailable) {
            mCamera2Source.start(mAutoFitTextureView, screenRotation);
            if (mOverlay != null) {
                Size size = mCamera2Source.getPreviewSize();
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());
                if (isPortraitMode()) {
                    // Swap width and height sizes when in portrait, since it will be rotated by
                    // 90 degrees
                    mOverlay.setCameraInfo(min/4, max/4, mCamera2Source.getCameraFacing());
                } else {
                    mOverlay.setCameraInfo(max/4, min/4, mCamera2Source.getCameraFacing());
                }
                mOverlay.clear();
            }
            mStartRequested = false;
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mSurfaceAvailable = true;
            mOverlay.bringToFront();
            try {startIfReady();} catch (IOException e) {
                Log.e(TAG, "Could not start camera source.", e);}
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {}
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            mSurfaceAvailable = false;
            return true;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
    };

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;
        if (mCamera2Source != null) {
            Size size = mCamera2Source.getPreviewSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }

        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, childWidth, childHeight);
        }

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Could not start camera source.", e);
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }
}
