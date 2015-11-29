package fragments;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.salat.viralcam.jellybeansupportlibrary.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CameraOldVersionsFragment extends Fragment implements CameraFragment {
    private static String TAG = "fragments.CameraOldVersionsFragment";

    public static class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private static boolean DEBUGGING = true;
        private static final String LOG_TAG = "CameraPreviewSample";
        private static final String CAMERA_PARAM_ORIENTATION = "orientation";
        private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
        private static final String CAMERA_PARAM_PORTRAIT = "portrait";
        protected Activity mActivity;
        private SurfaceHolder mHolder;
        protected Camera mCamera;
        protected List<Camera.Size> mPreviewSizeList;
        protected List<Camera.Size> mPictureSizeList;
        protected Camera.Size mPreviewSize;
        protected Camera.Size mPictureSize;
        private int mSurfaceChangedCallDepth = 0;
        private int mCameraId;
        private LayoutMode mLayoutMode;
        private int mCenterPosX = -1;
        private int mCenterPosY;

        PreviewReadyCallback mPreviewReadyCallback = null;

        public static enum LayoutMode {
            FitToParent, // Scale to the size that no side is larger than the parent
            NoBlank // Scale to the size that no side is smaller than the parent
        };

        public interface PreviewReadyCallback {
            public void onPreviewReady();
        }

        /**
         * State flag: true when surface's layout size is set and surfaceChanged()
         * process has not been completed.
         */
        protected boolean mSurfaceConfiguring = false;

        public CameraPreview(Activity activity, int cameraId, LayoutMode mode) {
            super(activity);
            mActivity = activity;
            mLayoutMode = mode;
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                if (Camera.getNumberOfCameras() > cameraId) {
                    mCameraId = cameraId;
                } else {
                    mCameraId = 0;
                }
            } else {
                mCameraId = 0;
            }

            try {
                releaseCameraAndPreview();
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } catch (Exception e) {
                Log.e(TAG, "failed to open Camera. " + e.toString() + ": " + e.getMessage());
                e.printStackTrace();
                Toast.makeText(activity, "Camera cannot be opened. Please check you have granted all permission needed.", Toast.LENGTH_LONG).show();
                activity.finish();
            }

            Camera.Parameters cameraParams = mCamera.getParameters();
            mPreviewSizeList = cameraParams.getSupportedPreviewSizes();
            mPictureSizeList = cameraParams.getSupportedPictureSizes();
        }

        private void releaseCameraAndPreview() {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mSurfaceChangedCallDepth++;
            doSurfaceChanged(width, height);
            mSurfaceChangedCallDepth--;
        }

        private void doSurfaceChanged(int width, int height) {
            mCamera.stopPreview();

            Camera.Parameters cameraParams = mCamera.getParameters();
            boolean portrait = isPortrait();

            // The code in this if-statement is prevented from executed again when surfaceChanged is
            // called again due to the change of the layout size in this if-statement.
            if (!mSurfaceConfiguring) {
                Camera.Size previewSize = determinePreviewSize(portrait, width, height);
                Camera.Size pictureSize = determinePictureSize(previewSize);
                if (DEBUGGING) { Log.v(LOG_TAG, "Desired Preview Size - w: " + width + ", h: " + height); }
                mPreviewSize = previewSize;
                mPictureSize = pictureSize;
                mSurfaceConfiguring = adjustSurfaceLayoutSize(previewSize, portrait, width, height);
                // Continue executing this method if this method is called recursively.
                // Recursive call of surfaceChanged is very special case, which is a path from
                // the catch clause at the end of this method.
                // The later part of this method should be executed as well in the recursive
                // invocation of this method, because the layout change made in this recursive
                // call will not trigger another invocation of this method.
                if (mSurfaceConfiguring && (mSurfaceChangedCallDepth <= 1)) {
                    return;
                }
            }

            configureCameraParameters(cameraParams, portrait);
            mSurfaceConfiguring = false;

            try {
                mCamera.startPreview();
            } catch (Exception e) {
                Log.w(LOG_TAG, "Failed to start preview: " + e.getMessage());

                // Remove failed size
                mPreviewSizeList.remove(mPreviewSize);
                mPreviewSize = null;

                // Reconfigure
                if (mPreviewSizeList.size() > 0) { // prevent infinite loop
                    surfaceChanged(null, 0, width, height);
                } else {
                    Toast.makeText(mActivity, "Can't start preview", Toast.LENGTH_LONG).show();
                    Log.w(LOG_TAG, "Gave up starting preview");
                }
            }

            if (null != mPreviewReadyCallback) {
                mPreviewReadyCallback.onPreviewReady();
            }
        }

        /**
         * @param portrait
         * @param reqWidth must be the value of the parameter passed in surfaceChanged
         * @param reqHeight must be the value of the parameter passed in surfaceChanged
         * @return Camera.Size object that is an element of the list returned from Camera.Parameters.getSupportedPreviewSizes.
         */
        protected Camera.Size determinePreviewSize(boolean portrait, int reqWidth, int reqHeight) {
            // Meaning of width and height is switched for preview when portrait,
            // while it is the same as user's view for surface and metrics.
            // That is, width must always be larger than height for setPreviewSize.
            int reqPreviewWidth; // requested width in terms of camera hardware
            int reqPreviewHeight; // requested height in terms of camera hardware
            if (portrait) {
                reqPreviewWidth = reqHeight;
                reqPreviewHeight = reqWidth;
            } else {
                reqPreviewWidth = reqWidth;
                reqPreviewHeight = reqHeight;
            }

            if (DEBUGGING) {
                Log.v(LOG_TAG, "Listing all supported preview sizes");
                for (Camera.Size size : mPreviewSizeList) {
                    Log.v(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
                }
                Log.v(LOG_TAG, "Listing all supported picture sizes");
                for (Camera.Size size : mPictureSizeList) {
                    Log.v(LOG_TAG, "  w: " + size.width + ", h: " + size.height);
                }
            }

            // Adjust surface size with the closest aspect-ratio
            float reqRatio = ((float) reqPreviewWidth) / reqPreviewHeight;
            float curRatio, deltaRatio;
            float deltaRatioMin = Float.MAX_VALUE;
            Camera.Size retSize = null;
            for (Camera.Size size : mPreviewSizeList) {
                curRatio = ((float) size.width) / size.height;
                deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    retSize = size;
                }
            }

            return retSize;
        }

        protected Camera.Size determinePictureSize(Camera.Size previewSize) {
            Camera.Size retSize = null;
            for (Camera.Size size : mPictureSizeList) {
                if (size.equals(previewSize)) {
                    return size;
                }
            }

            if (DEBUGGING) { Log.v(LOG_TAG, "Same picture size not found."); }

            // if the preview size is not supported as a picture size
            float reqRatio = ((float) previewSize.width) / previewSize.height;
            float curRatio, deltaRatio;
            float deltaRatioMin = Float.MAX_VALUE;
            for (Camera.Size size : mPictureSizeList) {
                curRatio = ((float) size.width) / size.height;
                deltaRatio = Math.abs(reqRatio - curRatio);
                if (deltaRatio < deltaRatioMin) {
                    deltaRatioMin = deltaRatio;
                    retSize = size;
                }
            }

            return retSize;
        }

        protected boolean adjustSurfaceLayoutSize(Camera.Size previewSize, boolean portrait,
                                                  int availableWidth, int availableHeight) {
            float tmpLayoutHeight, tmpLayoutWidth;
            if (portrait) {
                tmpLayoutHeight = previewSize.width;
                tmpLayoutWidth = previewSize.height;
            } else {
                tmpLayoutHeight = previewSize.height;
                tmpLayoutWidth = previewSize.width;
            }

            float factH, factW, fact;
            factH = availableHeight / tmpLayoutHeight;
            factW = availableWidth / tmpLayoutWidth;
            if (mLayoutMode == LayoutMode.FitToParent) {
                // Select smaller factor, because the surface cannot be set to the size larger than display metrics.
                if (factH < factW) {
                    fact = factH;
                } else {
                    fact = factW;
                }
            } else {
                if (factH < factW) {
                    fact = factW;
                } else {
                    fact = factH;
                }
            }

            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)this.getLayoutParams();

            int layoutHeight = (int) (tmpLayoutHeight * fact);
            int layoutWidth = (int) (tmpLayoutWidth * fact);
            if (DEBUGGING) {
                Log.v(LOG_TAG, "Preview Layout Size - w: " + layoutWidth + ", h: " + layoutHeight);
                Log.v(LOG_TAG, "Scale factor: " + fact);
            }

            boolean layoutChanged;
            if ((layoutWidth != this.getWidth()) || (layoutHeight != this.getHeight())) {
                layoutParams.height = layoutHeight;
                layoutParams.width = layoutWidth;
                if (mCenterPosX >= 0) {
                    layoutParams.topMargin = mCenterPosY - (layoutHeight / 2);
                    layoutParams.leftMargin = mCenterPosX - (layoutWidth / 2);
                }
                this.setLayoutParams(layoutParams); // this will trigger another surfaceChanged invocation.
                layoutChanged = true;
            } else {
                layoutChanged = false;
            }

            return layoutChanged;
        }

        /**
         * @param x X coordinate of center position on the screen. Set to negative value to unset.
         * @param y Y coordinate of center position on the screen.
         */
        public void setCenterPosition(int x, int y) {
            mCenterPosX = x;
            mCenterPosY = y;
        }

        protected void configureCameraParameters(Camera.Parameters cameraParams, boolean portrait) {
            int angle = getCameraDisplayOrientation(mActivity, mCameraId, mCamera);

            cameraParams.setRotation(angle);
            mCamera.setDisplayOrientation(angle);

            cameraParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            cameraParams.setPictureSize(mPictureSize.width, mPictureSize.height);

            if (cameraParams.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                cameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }

            if (DEBUGGING) {
                Log.v(LOG_TAG, "Preview Actual Size - w: " + mPreviewSize.width + ", h: " + mPreviewSize.height);
                Log.v(LOG_TAG, "Picture Actual Size - w: " + mPictureSize.width + ", h: " + mPictureSize.height);
            }

            mCamera.setParameters(cameraParams);
        }

        public static int getCameraDisplayOrientation(Activity activity,
                                                       int cameraId, android.hardware.Camera camera) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }

            return result;
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stop();
        }

        public void stop() {
            if (null == mCamera) {
                return;
            }
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        public boolean isPortrait() {
            return (mActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        }

        public void setOneShotPreviewCallback(Camera.PreviewCallback callback) {
            if (null == mCamera) {
                return;
            }
            mCamera.setOneShotPreviewCallback(callback);
        }

        public void setPreviewCallback(Camera.PreviewCallback callback) {
            if (null == mCamera) {
                return;
            }
            mCamera.setPreviewCallback(callback);
        }

        public Camera.Size getPreviewSize() {
            return mPreviewSize;
        }

        public void setOnPreviewReady(PreviewReadyCallback cb) {
            mPreviewReadyCallback = cb;
        }
    }


    private CameraPreview mPreview;
    private RelativeLayout mLayout;

    public CameraOldVersionsFragment() {
        // Required empty public constructor
    }

    public static CameraOldVersionsFragment newInstance() {
        return new CameraOldVersionsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_old_versions, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLayout = (RelativeLayout) view.findViewById(R.id.camera_view); // new RelativeLayout(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Set the second argument by your choice.
        // Usually, 0 for back-facing camera, 1 for front-facing camera.
        mPreview = new CameraPreview(getActivity(), 0, CameraPreview.LayoutMode.FitToParent);
        RelativeLayout.LayoutParams previewLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        mLayout.addView(mPreview, 0, previewLayoutParams);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPreview.stop();
        mLayout.removeView(mPreview); // This is necessary.
        mPreview = null;
    }

    @Override
    public void takePicture(final OnCaptureCompleted callback) {
        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;

                String time = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(new Date());

                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "JPEG_" + time + ".jpg");

                String path = file.getPath();
                Uri uri = Uri.fromFile(file);

                try {
                    outStream = new FileOutputStream(path);
                    outStream.write(data);
                    outStream.close();
                    Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
                } catch (Exception e) {
                    Log.d(TAG, "Image saving failed " + e.toString() + ": " + e.getMessage());
                    e.printStackTrace();
                    path = null;
                    uri = null;
                } finally {
                    callback.onCaptureComplete(path, uri);
                }
                Log.d(TAG, "onPictureTaken - jpeg");
            }
        };


        try {
            mPreview.mCamera.takePicture(null, null, jpegCallback);
        } catch (Exception e) {
            callback.onCaptureComplete(null, null);
            Log.e(TAG, "takePicture failed" + e.toString());
            Toast.makeText(getActivity(), "Capturing failed. Have you tried to turn it off and on again?", Toast.LENGTH_SHORT).show();
        }
    }
}
