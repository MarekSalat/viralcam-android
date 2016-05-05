package com.salat.viralcam.app.activities;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.model.DataSetItem;
import com.salat.viralcam.app.model.EvaluationResult;
import com.salat.viralcam.app.model.MattingDataSet;
import com.salat.viralcam.app.util.MattingHelper;

public class MattingEvaluationActivity extends AppCompatActivity {
    private static final String TAG = "MattingEvaluation";
    private View evaluateCpuButton;
    private View evaluateGpuButton;

    private enum EvaluationType {
        CPU,
        GPU,
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matting_evaluation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        evaluateCpuButton = findViewById(R.id.evaluate_cpu_button);
        evaluateGpuButton = findViewById(R.id.evaluate_gpu_button);

        evaluateCpuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runEvaluation(EvaluationType.CPU);
            }
        });

        evaluateGpuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runEvaluation(EvaluationType.GPU);
            }
        });
    }

    private void runEvaluation(final EvaluationType evaluationType) {
        evaluateCpuButton.setEnabled(false);
        evaluateGpuButton.setEnabled(false);
        final TextView logPlaceholder = (TextView) findViewById(R.id.log_placeholder);
        final TextView avgTime = (TextView) findViewById(R.id.avg_time_value);
        final TextView avgTimePerUnknownPixel = (TextView) findViewById(R.id.avg_time_per_unknown_pixel_value);
        final TextView avgMse = (TextView) findViewById(R.id.avg_mse_value);
        final TextView avgSad = (TextView) findViewById(R.id.avg_sad_value);
        final TextView progress = (TextView) findViewById(R.id.progress);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        final MattingDataSet dataSet = MattingDataSet.generateAlphamattingComDataSet(1);

        progressBar.setMax(dataSet.getItems().size());
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);

        Thread task = new Thread() {
            @Override
            public void run() {

                int index = 0;
                for (DataSetItem item : dataSet.getItems().values()) {
                    index++;
                    final EvaluationResult result = performTest(item, evaluationType);
                    dataSet.addResult(item, result);

                    final int finalIndex = index;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (MattingEvaluationActivity.this) {
                                logPlaceholder.setText(logPlaceholder.getText() + "\n\n" + result.toString());
                                avgTime.setText(String.format("%.2f", dataSet.avgComputationTime()));
                                avgTimePerUnknownPixel.setText(String.format("%.2f", dataSet.avgComputationTimePerUnknownPixel()));
                                avgMse.setText(String.format("%.2f", dataSet.avgMse()));
                                avgSad.setText(String.format("%.2f", dataSet.avgSad()));
                                progress.setText(String.format("%.2f%%", finalIndex / (double) dataSet.getItems().size() * 100f));
                                progressBar.setProgress(finalIndex);
                            }
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        evaluateCpuButton.setEnabled(true);
                        evaluateGpuButton.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        progress.setText("0%%");
                    }
                });
            }
        };
        task.start();

    }

    private EvaluationResult performTest(DataSetItem item, EvaluationType evaluation) {
        Bitmap image = MattingHelper.read(item.imagePath, Bitmap.Config.ARGB_8888);
        Bitmap trueAlpha = MattingHelper.convertToAlpha8(
                MattingHelper.read(item.trueAlphaPath, Bitmap.Config.ARGB_8888));
        Bitmap calculatedAlpha = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ALPHA_8);
        Log.i(TAG, item.id + " |> image, true alpha, alpha read");
        Bitmap trimap = MattingHelper.changeTrimapColors(
                MattingHelper.read(item.trimapPath, Bitmap.Config.ARGB_8888), 0xFF808080, Color.TRANSPARENT);
        Log.i(TAG, item.id + " |> trimap color transformed");

        Log.i(TAG, item.id + " |> calculate matte started");
        long startTime = System.currentTimeMillis();{
            if (evaluation == EvaluationType.CPU)
                TrimapActivity.calculateAlphaMask(image, trimap, calculatedAlpha);
            if(evaluation == EvaluationType.GPU){
                ;
            }
        }
        long endTime = System.currentTimeMillis();
        Log.i(TAG, item.id + " |> matte calculated");

        EvaluationResult result = new EvaluationResult(item.id)
                .width(image.getWidth())
                .height(image.getHeight())
                .foregroundPixels(MattingHelper.calculatePixels(trimap, Color.WHITE))
                .backgroundPixels(MattingHelper.calculatePixels(trimap, Color.BLACK))
                .computationTime(endTime - startTime)
                .sumOfAbsoluteDifferences(MattingHelper.calculateSAD(trueAlpha, calculatedAlpha))
                .squaredError(MattingHelper.calculateSE(trueAlpha, calculatedAlpha));
        Log.i(TAG, item.id + " |> result created");
        Log.i(TAG, item.id + " |> " + result.toString());

        MattingHelper.saveImage(calculatedAlpha, item.resultAlphaPath);
        Log.i(TAG, item.id + " |> calculated alpha saved");

        MattingHelper.drawResultImage(trimap, image, calculatedAlpha);
        MattingHelper.saveImage(trimap, item.resultImagePath);
        Log.i(TAG, item.id + " |> image saved saved");

        image.recycle();
        trimap.recycle();
        trueAlpha.recycle();
        calculatedAlpha.recycle();

        return result;
    }
}
