package com.sample.tracking;

import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import zeusees.tracking.Face;
import zeusees.tracking.FaceTracking;


public class FaceOverlapFragment extends CameraOverlapFragment {

    private static final int MESSAGE_DRAW_POINTS = 100;

    private FaceTracking mMultiTrack106 = null;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private byte mNv21Data[];
    private byte[] mTmpBuffer;

    private int frameIndex = 0;

    private Paint mPaint;
    private final Object lockObj = new Object();
    private boolean mIsPaused = false;

    @SuppressLint("NewApi")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mNv21Data = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        mTmpBuffer = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        frameIndex = 0;
        mPaint = new Paint();
        mPaint.setColor(Color.rgb(57, 138, 243));
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Style.FILL);
        mHandlerThread = new HandlerThread("DrawFacePointsThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DRAW_POINTS) {
                    synchronized (lockObj) {
                        if (!mIsPaused) {
                            handleDrawPoints();
                        }
                    }
                }
            }
        };
        this.setPreviewCallback(new PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (mNv21Data) {
                    System.arraycopy(data, 0, mNv21Data, 0, data.length);
                }

                mHandler.removeMessages(MESSAGE_DRAW_POINTS);
                mHandler.sendEmptyMessage(MESSAGE_DRAW_POINTS);
            }
        });
        return view;
    }

    @SuppressLint("SdCardPath")
    private void handleDrawPoints() {

        synchronized (mNv21Data) {
            System.arraycopy(mNv21Data, 0, mTmpBuffer, 0, mNv21Data.length);
        }

        boolean frontCamera = (CameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT);

        if (null == mMultiTrack106) {
            mMultiTrack106 = new FaceTracking("/sdcard/LBFaceTracking/models");
        }

        if (frameIndex == 0) {
            mMultiTrack106.FaceTrackingInit(mTmpBuffer, PREVIEW_HEIGHT, PREVIEW_WIDTH);
        } else {
            mMultiTrack106.Update(mTmpBuffer, PREVIEW_HEIGHT, PREVIEW_WIDTH);
        }
        frameIndex += 1;

        List<Face> faceActions = mMultiTrack106.getTrackingInfo();
        if (faceActions != null) {

            if (!mOverlap.getHolder().getSurface().isValid()) {
                return;
            }

            Canvas canvas = mOverlap.getHolder().lockCanvas();
            if (canvas == null)
                return;

            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            canvas.setMatrix(getMatrix());
            Log.d("camera orientation: ", String.valueOf(mCameraInfo.orientation));
            boolean rotate270 = mCameraInfo.orientation == 270;
            for (Face r : faceActions) {
                Rect rect = new Rect(PREVIEW_HEIGHT - r.left, r.top, PREVIEW_HEIGHT - r.right, r.bottom);
                PointF[] points = new PointF[106];
                for (int i = 0; i < 106; i++) {
                    points[i] = new PointF(r.landmarks[i * 2], r.landmarks[i * 2 + 1]);
                }
                for (PointF point : points) {
                    if (rotate270) {
                        point.x = PREVIEW_HEIGHT - point.x;
                    }
                }

                STUtils.drawFaceRect(canvas, rect, PREVIEW_HEIGHT, PREVIEW_WIDTH, frontCamera);
                STUtils.drawPoints(canvas, mPaint, points, PREVIEW_HEIGHT, PREVIEW_WIDTH, frontCamera);

            }
            mOverlap.getHolder().unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (lockObj) {
            if (mMultiTrack106 != null) {
                mMultiTrack106 = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mIsPaused = false;
    }

    @Override
    public void onPause() {
        mHandler.removeMessages(MESSAGE_DRAW_POINTS);
        mIsPaused = true;
        super.onPause();
    }


}
