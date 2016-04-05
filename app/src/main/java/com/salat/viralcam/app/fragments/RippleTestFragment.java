package com.salat.viralcam.app.fragments;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;

import java.io.IOException;

/**
 * A placeholder fragment containing a simple view.
 */
public class RippleTestFragment extends Fragment {

    public RippleTestFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ripple_test, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Uri backgroundUri = Constants.getUriFromResource(getResources(), R.raw.panda);
        Bitmap background = getBitmap(backgroundUri);
        final ImageView imageView = (ImageView) view.findViewById(R.id.image_view);
        imageView.setImageBitmap(background);
    }

    private Bitmap getBitmap(Uri imageUri) {
        Bitmap result = null;
        try {
            result = BitmapLoader.load(getActivity().getContentResolver(), imageUri, Constants.IMAGE_OPTIMAL_WIDTH, Constants.IMAGE_OPTIMAL_HEIGHT);
        } catch (IOException e) {

        }
        return result;
    }
}
