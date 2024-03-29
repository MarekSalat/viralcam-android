package com.salat.viralcam.app.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.salat.viralcam.app.AnalyticsTrackers;
import com.salat.viralcam.app.BuildConfig;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.util.Constants;

public class HomeScreenActivity extends AppCompatActivity {
    private static final int CAPTURE_SCENE_REQUEST = 42;
    private static final int INTRODUCTION_REQUEST = 43;

    private static final String PRIVATE_PREF = "viralcam_private_pref";
    private static final String VERSION_KEY = "VERSION_KEY";
    private static final String INTRO_HAS_BEEN_SHOWN = "INTRO_HAS_BEEN_SHOWN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AnalyticsTrackers.initialize(this);
        AnalyticsTrackers.tracker().get(AnalyticsTrackers.Target.APP);

        if(shouldShowIntroduction()){
            neverShowIntroAgain();
            neverShowWhatsNewAgain();
            openIntroductionActivity();
        }
        else if (shouldBeWhatsNewShown()) {
            neverShowWhatsNewAgain();
            showWhatsNewDialog();
        } else {
            openCaptureScreenActivity();
        }
    }


    private void openIntroductionActivity() {
        startActivityForResult(new Intent(this, IntroductionActivity.class), INTRODUCTION_REQUEST);
    }

    private void openCaptureScreenActivity() {
        startActivityForResult(new Intent(this, CaptureSceneActivity.class), CAPTURE_SCENE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == INTRODUCTION_REQUEST)
            openCaptureScreenActivity();

        finish();
    }

    private boolean shouldShowIntroduction() {
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);

        boolean wasIntroAlreadyShown = sharedPref.getBoolean(INTRO_HAS_BEEN_SHOWN, false);

        return Constants.ALWAYS_SHOW_INTRODUCTION || !wasIntroAlreadyShown;

    }

    private boolean shouldBeWhatsNewShown() {
        if(getString(R.string.changelog_dialog_text).isEmpty())
            return false;

        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);

        int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);
        int currentVersionNumber = BuildConfig.VERSION_CODE;

        return Constants.ALWAYS_SHOW_WHATS_NEW || currentVersionNumber > savedVersionNumber;

    }

    private void neverShowIntroAgain() {
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(INTRO_HAS_BEEN_SHOWN, true);
        editor.apply();
    }

    private void neverShowWhatsNewAgain() {
        int currentVersionNumber = BuildConfig.VERSION_CODE;
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(VERSION_KEY, currentVersionNumber);
        editor.apply();
    }

    private void showWhatsNewDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.changelog_dialog_title, BuildConfig.VERSION_NAME));
//        dialog.setIcon(getResources().getDrawable(R.drawable.icon));

        WebView webView = new WebView(getApplicationContext());
        webView.loadData(getString(R.string.changelog_dialog_text), "text/html", "utf-8");
        dialog.setView(webView);

        dialog.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                openCaptureScreenActivity();
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                openCaptureScreenActivity();
            }
        });

        dialog.show();
    }
}
