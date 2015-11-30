package com.salat.viralcam.app.activities;

import com.github.clans.fab.FloatingActionButton;
import com.salat.viralcam.app.BuildConfig;
import com.salat.viralcam.app.R;
import com.salat.viralcam.app.fragments.CameraLollipopFragment;
import com.salat.viralcam.app.util.BitmapLoader;
import com.salat.viralcam.app.util.Constants;
import com.salat.viralcam.app.views.ImageWithMask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.IOException;

import fragments.CameraFragment;
import fragments.CameraOldVersionsFragment;

public class HomeScreenActivity extends AppCompatActivity {
    private static final int CAPTURE_SCENE_RESULT = 42;

    private static final String PRIVATE_PREF = "viralcam_private_pref";
    private static final String VERSION_KEY = "VERSION_KEY";
    private static final String INTRO_HAS_BEEN_SHOWN = "INTRO_HAS_BEEN_SHOWN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (shouldBeWhatsNewShown()) {
            showWhatsNewDialog();
        }
//        else if(shouldShowIntroduction()){
//            openIntroductionActivity();
//        }
        else {
            openCaptureScreenActivity();
        }

        setContentView(R.layout.activity_home_screen);
    }

    private boolean shouldShowIntroduction() {
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);

        boolean wasIntroAlreadyShown = sharedPref.getBoolean(INTRO_HAS_BEEN_SHOWN, false);

        if (Constants.ALWAYS_SHOW_INTRODUCTION || !wasIntroAlreadyShown) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(INTRO_HAS_BEEN_SHOWN, true);
            editor.apply();

            return true;
        }

        return false;
    }

    private void openIntroductionActivity() {
        startActivityForResult(new Intent(this, IntroductionActivity.class), CAPTURE_SCENE_RESULT);
    }

    private void openCaptureScreenActivity() {
        startActivityForResult(new Intent(this, CaptureSceneActivity.class), CAPTURE_SCENE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    private boolean shouldBeWhatsNewShown() {
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);

        int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);
        int currentVersionNumber = BuildConfig.VERSION_CODE;

        if (Constants.ALWAYS_SHOW_WHATS_NEW || currentVersionNumber > savedVersionNumber) {
            showWhatsNewDialog();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(VERSION_KEY, currentVersionNumber);
            editor.apply();

            return true;
        }

        return false;
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
