package com.sample.tracking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import zeusees.tracking.Face;

/**
 * Created by wcliang on 2019/12/17.
 */

public class SavePicAsyncTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "SavePicAsyncTask";
    private byte[] data;//图片字节
    private int mCurrentCameraId;
    private OnTakePicCallBackListener mOnTakePicCallBackListener;
    private Face mFace;

    public SavePicAsyncTask(Face mFace, byte[] data, int mCurrentCameraId,
                            OnTakePicCallBackListener mOnTakePicCallBackListener) {
        this.mFace = mFace;
        this.data = data;
        this.mCurrentCameraId = mCurrentCameraId;
        this.mOnTakePicCallBackListener = mOnTakePicCallBackListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Void... params) {
        return saveToSDCard(data);
    }


    @Override
    protected void onPostExecute(String path) {
        super.onPostExecute(path);
        if (mOnTakePicCallBackListener != null) {
            mOnTakePicCallBackListener.savePicPath(path);

        }
    }

    /**
     * 将拍下来的照片存放在SD卡中
     */
    private String saveToSDCard(byte[] data) {
        Bitmap croppedImage = decodeRegionCrop(data);
        if (croppedImage == null) {

            return "";
        }
        String imagePath = saveToFile(croppedImage);
        croppedImage.recycle();
        return imagePath;
    }

    // 保存图片文件
    private String saveToFile(Bitmap croppedImage) {
        if (croppedImage == null) {

            return "";
        }
        String picPath = "";
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/LBitmaps/");
            if (!dir.exists()) {
                dir.mkdirs();

            }
            String fileName = getCameraPath();
            File outFile = new File(dir, fileName);
            FileOutputStream outputStream = new FileOutputStream(outFile);
            croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            picPath = outFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return picPath;
    }

    private static String getCameraPath() {
        Calendar calendar = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("IMG");
        sb.append(calendar.get(Calendar.YEAR));
        int month = calendar.get(Calendar.MONTH) + 1; // 0~11
        sb.append(month < 10 ? "0" + month : month);
        int day = calendar.get(Calendar.DATE);
        sb.append(day < 10 ? "0" + day : day);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        sb.append(hour < 10 ? "0" + hour : hour);
        int minute = calendar.get(Calendar.MINUTE);
        sb.append(minute < 10 ? "0" + minute : minute);
        int second = calendar.get(Calendar.SECOND);
        sb.append(second < 10 ? "0" + second : second);
        if (!new File(sb.toString() + ".jpg").exists()) {
            return sb.toString() + ".jpg";
        }
        StringBuilder tmpSb = new StringBuilder(sb);
        int indexStart = sb.length();
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            tmpSb.append('(');
            tmpSb.append(i);
            tmpSb.append(')');
            tmpSb.append(".jpg");
            if (!new File(tmpSb.toString()).exists()) {

                break;
            }
            tmpSb.delete(indexStart, tmpSb.length());
        }
        return tmpSb.toString();
    }


    private Bitmap decodeRegionCrop(byte[] data) {
        try {
            Bitmap srcImage = BitmapFactory.decodeByteArray(data, 0, data.length);
            Matrix m = new Matrix();
            m.postScale(1, -1);
            srcImage = Bitmap.createBitmap(srcImage, mFace.left,
                    640 - mFace.bottom, mFace.width, mFace.height, m, true);
            return srcImage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface OnTakePicCallBackListener {
        void savePicPath(String picPath);
    }
}
