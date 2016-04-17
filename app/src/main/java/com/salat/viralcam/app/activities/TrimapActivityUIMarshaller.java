package com.salat.viralcam.app.activities;

import android.graphics.Matrix;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.views.DrawTrimapView;

import java.util.HashMap;

/**
 * Created by Marek on 12.04.2016.
 */
class TrimapActivityUIMarshaller {
    private static final String TAG = "UIMarshaller";
    private Animation slideUpFromBottomAnimation;
    private Animation slideDownFromBottomAnimation;
    private Animation slideUpFromTopAnimation;
    private Animation slideDownFromTop;

    static private interface Transition {
        void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState);
    }

    static abstract class AnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private TrimapActivity.State state;
    private TrimapActivity activity;

    private HashMap<Pair<TrimapActivity.State, TrimapActivity.State>, Transition> transitions = new HashMap<>();

    public TrimapActivityUIMarshaller(TrimapActivity.State state, final TrimapActivity activity) {
        this.state = state;
        this.activity = activity;

        slideUpFromTopAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_up_from_top);
        slideDownFromTop = AnimationUtils.loadAnimation(activity, R.anim.slide_down_from_top);
        slideUpFromBottomAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_up_from_bottom);
        slideDownFromBottomAnimation = AnimationUtils.loadAnimation(activity, R.anim.slide_down_from_bottom);

        transitions.put(new Pair<>(TrimapActivity.State.INIT_TRIMAP, TrimapActivity.State.RESULT), new Transition() {
            @Override
            public void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState) {
                activity.processDialog.dismiss();

                if (activity.lastImageResult != null) {
                    // drawTrimapView contains clear matrix with current scale and translation
                    Matrix drawTrimapViewMatrix = activity.drawTrimapView.getImageMatrix();
                    activity.setImageViewBitmapWithMatrix(drawTrimapViewMatrix, activity.imageView, activity.lastImageResult);
                }

                activity.drawTrimapView.setState(DrawTrimapView.TrimapDrawState.DONE);
                activity.drawTrimapView.setVisibility(View.INVISIBLE);

                activity.brushGroup.clearAnimation();
                activity.brushGroup.setVisibility(View.GONE);
                activity.buttonDone.setVisibility(View.GONE);

                activity.editOptionGroup.setVisibility(View.VISIBLE);
                activity.editOptionGroup.startAnimation(slideUpFromBottomAnimation);
            }
        });
        transitions.put(new Pair<>(TrimapActivity.State.EDIT_TRIMAP, TrimapActivity.State.RESULT), transitions.get(new Pair<>(TrimapActivity.State.INIT_TRIMAP, TrimapActivity.State.RESULT)));

        transitions.put(new Pair<>(TrimapActivity.State.RESULT, TrimapActivity.State.EDIT_COLOR), new Transition() {
            @Override
            public void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState) {
                activity.colorSeekBar.clearAnimation();
                activity.colorSeekBar.setVisibility(View.VISIBLE);
                activity.buttonDone.setVisibility(View.VISIBLE);

                activity.editOptionGroup.startAnimation(slideDownFromBottomAnimation);
                activity.colorSeekBar.startAnimation(slideUpFromBottomAnimation);
                activity.editDoneGroup.startAnimation(slideUpFromBottomAnimation);
            }
        });

        transitions.put(new Pair<>(TrimapActivity.State.EDIT_COLOR, TrimapActivity.State.RESULT), new Transition() {
            @Override
            public void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState) {
                activity.editOptionGroup.startAnimation(slideUpFromBottomAnimation);
                activity.colorSeekBar.startAnimation(slideDownFromBottomAnimation);
                activity.colorSeekBar.getAnimation().setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        activity.colorSeekBar.setVisibility(View.GONE);
                        activity.colorSeekBar.getAnimation().setAnimationListener(null);
                        activity.colorSeekBar.clearAnimation();
                    }
                });
                activity.editDoneGroup.startAnimation(slideDownFromBottomAnimation);
            }
        });

        transitions.put(new Pair<>(TrimapActivity.State.RESULT, TrimapActivity.State.EDIT_LIGHT), new Transition() {
            @Override
            public void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState) {
                activity.lightSeekBar.clearAnimation();
                activity.lightSeekBar.setVisibility(View.VISIBLE);
                activity.buttonDone.setVisibility(View.VISIBLE);

                activity.editOptionGroup.startAnimation(slideDownFromBottomAnimation);
                activity.lightSeekBar.startAnimation(slideUpFromBottomAnimation);
                activity.editDoneGroup.startAnimation(slideUpFromBottomAnimation);
            }
        });

        transitions.put(new Pair<>(TrimapActivity.State.EDIT_LIGHT, TrimapActivity.State.RESULT), new Transition() {
            @Override
            public void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState) {
                activity.editOptionGroup.startAnimation(slideUpFromBottomAnimation);
                activity.lightSeekBar.startAnimation(slideDownFromBottomAnimation);
                activity.lightSeekBar.getAnimation().setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        activity.lightSeekBar.setVisibility(View.GONE);
                        activity.lightSeekBar.getAnimation().setAnimationListener(null);
                        activity.lightSeekBar.clearAnimation();
                    }
                });
                activity.editDoneGroup.startAnimation(slideDownFromBottomAnimation);
            }
        });

        transitions.put(new Pair<>(TrimapActivity.State.RESULT, TrimapActivity.State.EDIT_TRIMAP), new Transition() {
            @Override
            public void changeUI(TrimapActivityUIMarshaller marshaller, TrimapActivity.State newState) {
                activity.setImageViewBitmapWithMatrix(activity.drawTrimapView.getImageMatrix(), activity.imageView, activity.foreground);

                activity.drawTrimapView.setState(DrawTrimapView.TrimapDrawState.TUNING);
                activity.drawTrimapView.setVisibility(View.VISIBLE);

                activity.editOptionGroup.startAnimation(slideDownFromBottomAnimation);

                activity.buttonDone.setVisibility(View.VISIBLE);
                activity.brushGroup.setVisibility(View.VISIBLE);
                activity.brushGroup.startAnimation(slideUpFromBottomAnimation);
                activity.editDoneGroup.startAnimation(slideUpFromBottomAnimation);
            }
        });
    }

    public void changeUiByState(TrimapActivity.State newState) {
        Log.i(TAG, "Unsupported state " + state.toString() + " -> " + newState.toString());

        activity.markEdgeSnackbar.dismiss();

        Pair<TrimapActivity.State, TrimapActivity.State> statePair = new Pair<>(state, newState);
        if (!transitions.containsKey(statePair)) {
            Log.e(TAG, "Unsupported state " + state.toString() + " -> " + newState.toString());
            return;
        }

        transitions.get(statePair).changeUI(this, newState);
        state = newState;
    }

    public void hideAll() {
        activity.markEdgeSnackbar.dismiss();
        activity.toolbar.startAnimation(slideUpFromTopAnimation);

        if (state == TrimapActivity.State.EDIT_TRIMAP) {
            activity.brushGroup.startAnimation(slideDownFromBottomAnimation);
            activity.editDoneGroup.startAnimation(slideDownFromBottomAnimation);
        }
    }

    public void showAll() {
        activity.markEdgeSnackbar.dismiss();
        activity.toolbar.startAnimation(slideDownFromTop);

        if (state == TrimapActivity.State.EDIT_TRIMAP) {
            activity.brushGroup.startAnimation(slideUpFromBottomAnimation);
            activity.editDoneGroup.startAnimation(slideUpFromBottomAnimation);
        }
    }

    public TrimapActivity.State state() {
        return state;
    }
}
