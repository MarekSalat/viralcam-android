package com.salat.viralcam.app.activities;

import android.os.Bundle;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.salat.viralcam.app.AnalyticsTrackers;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.SampleSlideFragment;

public class IntroductionActivity extends AppIntro2 {
    @Override
    public void init(Bundle savedInstanceState) {
        AnalyticsTrackers.initialize(this);
        AnalyticsTrackers.tracker().get(AnalyticsTrackers.Target.APP);

        addSlide(AppIntroFragment.newInstance("ViralCam", "Change the reality", R.drawable.viralcam_highres_icon_512x512, 0xFF4a148c));
        addSlide(AppIntroFragment.newInstance("Capture the scene", "Select an image you want to edit. You can also change a transparency by swiping over the image.", R.drawable.ic_camera_white_48dp, 0xFF4a148c));
        addSlide(AppIntroFragment.newInstance("Roughly mark the edge", "Helps better estimate what is the foreground. You do not have to be precise. You can tune it later.", R.drawable.ic_gesture_white_48dp, 0xFF4a148c));
        addSlide(SampleSlideFragment.newInstance(R.layout.introduction_slide_1));
        addSlide(AppIntroFragment.newInstance("Share and profit", "Show your composition to the world.", R.drawable.ic_share_white_48dp, 0xFF4a148c));
//        addSlide(AppIntroFragment.newInstance("", "", R.drawable.placeholder_image, 0xFF4a148c));

        setFadeAnimation();
    }

    @Override
    public void onNextPressed() {

    }

    @Override
    public void onDonePressed() {
        finish();
    }

    @Override
    public void onSlideChanged() {

    }
}
