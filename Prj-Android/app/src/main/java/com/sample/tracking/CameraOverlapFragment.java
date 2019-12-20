package com.sample.tracking;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;


public class CameraOverlapFragment extends Fragment {

    protected Camera mCamera = null;
    protected CameraInfo mCameraInfo = null;
    protected int mCameraInit = 0;
    protected SurfaceView mSurfaceView = null;
    protected SurfaceView mOverlap = null;
    protected SurfaceHolder mSurfaceHolder = null;

    Camera.PreviewCallback mPreviewCallback;
    Matrix matrix = new Matrix();
    final int PREVIEW_WIDTH = 640;
    final int PREVIEW_HEIGHT = 480;

    int CameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_overlap, container, false);

        mSurfaceView = view.findViewById(R.id.surfaceViewCamera);
        mOverlap = view.findViewById(R.id.surfaceViewOverlap);
        mOverlap.setZOrderOnTop(true);
        mOverlap.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                matrix.setScale(width / (float) PREVIEW_HEIGHT, height / (float) PREVIEW_WIDTH);
                initCamera();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera = null;
                openCamera(CameraFacing);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (null != mCamera) {
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }
                mCameraInit = 0;
            }
        });

        return view;
    }

    private void openCamera(int CameraFacing) {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraFacing) {
                try {
                    mCamera = Camera.open(i);
                    mCameraInfo = info;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    mCamera = null;
                    continue;
                }
                break;
            }
        }
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            initCamera();
        } catch (Exception ex) {
            if (null != mCamera) {
                mCamera.release();
                mCamera = null;
            }
        }
    }

    private class CameraSizeComparator implements Comparator<Size> {
        @Override
        public int compare(Size o1, Size o2) {
            return Integer.compare(o1.width, o2.width);
        }
    }

    private void initCamera() {
        mCameraInit = 1;
        if (null != mCamera) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                List<String> flashModes = parameters.getSupportedFlashModes();
                if (flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }

                CameraSizeComparator comparator = new CameraSizeComparator();
                List<Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                Collections.sort(previewSizes, comparator);
                for (Size previewSize : previewSizes) {
                    if (previewSize.width >= 640) {
                        parameters.setPreviewSize(previewSize.width, previewSize.height);
                        break;
                    }
                }

                List<Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
                Collections.sort(pictureSizes, comparator);
                for (Size pictureSize : pictureSizes) {
                    if (pictureSize.width >= 640) {
                        parameters.setPictureSize(pictureSize.width, pictureSize.height);
                        break;
                    }
                }
                if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    parameters.set("orientation", "portrait");
                    parameters.set("rotation", 90);
                    int orientation = 360 - mCameraInfo.orientation;
                    mCamera.setDisplayOrientation(orientation);
                } else {
                    parameters.set("orientation", "landscape");
                    mCamera.setDisplayOrientation(0);
                }

                mCamera.setParameters(parameters);
                mCamera.setPreviewCallback(this.mPreviewCallback);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.mPreviewCallback = previewCallback;
        if (mCamera != null) {
            mCamera.setPreviewCallback(previewCallback);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCameraInit == 1 && mCamera == null) {
            openCamera(CameraFacing);
        }
    }

    @Override
    public void onPause() {

        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    public Matrix getMatrix() {
        return matrix;
    }

}
