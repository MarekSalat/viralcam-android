package fragments;

import android.net.Uri;

public interface CameraFragment {
    interface OnCaptureCompleted {
        void onCaptureComplete(String path, Uri uri);
    }

    void takePicture(OnCaptureCompleted callback);
}
