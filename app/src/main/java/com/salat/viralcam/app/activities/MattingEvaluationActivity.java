package com.salat.viralcam.app.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.computeshader.ComputeShader;
import com.salat.viralcam.app.computeshader.ComputeShaderArgs;
import com.salat.viralcam.app.computeshader.ComputeShaderResultCallback;
import com.salat.viralcam.app.fragments.ComputeShaderFragment;
import com.salat.viralcam.app.matting.AlphaMattingComputeShader;
import com.salat.viralcam.app.model.DataSetItem;
import com.salat.viralcam.app.model.EvaluationResult;
import com.salat.viralcam.app.model.EvaluationType;
import com.salat.viralcam.app.model.MattingDataSet;
import com.salat.viralcam.app.util.MattingHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class MattingEvaluationActivity extends AppCompatActivity implements ComputeShaderFragment.OnFragmentEvents {
    private static final String TAG = "MattingEvaluation";
    private static final String FRAGMENT = "CSFragment";
    private View evaluateCpuButton;
    private View evaluateGpuButton;
    private Spinner imageScaleSpinner;
    private ArrayAdapter<String> imageScaleAdapter;
    private Thread evaluationTask;
    private View cancelButton;
    private ProgressBar progressBar;
    private TextView progress;
    private boolean stopEvaluation = false;

    @Override
    public ComputeShader createComputeShader() {
        return new AlphaMattingComputeShader(getResources());
    }

    @Override
    public void onComputeShaderFragmentStart(ComputeShaderFragment fragment) {
        //
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
        cancelButton = findViewById(R.id.cancel_evaluation_button);

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

        String[] arrayScaleSpinner = new String[5];
        for (int i = 0; i < 5; i++ ) {
            arrayScaleSpinner[i] = String.format("%dx", (int) Math.pow(2, i));
        }
        imageScaleSpinner = (Spinner) findViewById(R.id.image_scale_spinner);
        imageScaleAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, arrayScaleSpinner);
        imageScaleSpinner.setAdapter(imageScaleAdapter);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentByTag(FRAGMENT);

        if (fragment == null) {
            fragment = ComputeShaderFragment.newInstance();

            fragment.setRetainInstance(false);
            getFragmentManager().beginTransaction()
                    .replace(R.id.compute_shader_fragment_container, fragment, FRAGMENT)
                    .commit();
        } else {
            onComputeShaderFragmentStart((ComputeShaderFragment) fragment);
        }

        progress = (TextView) findViewById(R.id.progress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(evaluationTask == null)
                    return;
                cancelButton.setEnabled(false);
                stopEvaluation = true;
            }
        });
    }

    private void runEvaluation(final EvaluationType evaluationType) {
        stopEvaluation = false;
        evaluateCpuButton.setEnabled(false);
        evaluateGpuButton.setEnabled(false);
        cancelButton.setEnabled(true);

        final int scale = imageScaleSpinner.getSelectedItemId() > 0 ? (int) Math.pow(2, imageScaleSpinner.getSelectedItemId()) : 1;
        final TextView logPlaceholder = (TextView) findViewById(R.id.log_placeholder);
        logPlaceholder.setText("");
        final TextView avgTime = (TextView) findViewById(R.id.avg_time_value);
        final TextView avgTimePerUnknownPixel = (TextView) findViewById(R.id.avg_time_per_unknown_pixel_value);
        final TextView avgMse = (TextView) findViewById(R.id.avg_mse_value);
        final TextView avgSad = (TextView) findViewById(R.id.avg_sad_value);
        progress = (TextView) findViewById(R.id.progress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        final int trimapSetVersion = 2;
        final MattingDataSet dataSet = MattingDataSet.generateAlphamattingComDataSet(trimapSetVersion, evaluationType, scale);

        progressBar.setMax(dataSet.getItems().size());
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);

        evaluationTask = new Thread() {
            @Override
            public void run() {
                int index = 0;
                for (final DataSetItem item : dataSet.getItems().values()) {
                    if(stopEvaluation)
                        break;

                    index++;

                    final int finalIndex = index;
                    final EvaluationResult result = performTest(item, evaluationType, scale);

                    dataSet.addResult(item, result);

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

                String csvFileName = new File(
                    Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                    MattingDataSet.AlphamattingComDataSet.getResultAlphaPath(0, evaluationType, scale, trimapSetVersion)
                ).getParent() + ".csv";

                try {
                    File csvFile = new File(csvFileName);
                    if(!csvFile.exists())
                        csvFile.createNewFile();

                    FileWriter out = new FileWriter(csvFile, false);
                    String csv = dataSet.csv(',');
                    out.write(csv);
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "save csv", e);
                }


                evaluationTask = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelButton.setEnabled(false);
                        evaluateCpuButton.setEnabled(true);
                        evaluateGpuButton.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        progress.setText("0%%");
                    }
                });
            }
        };
        evaluationTask.start();
    }

    final Object lock = new Object();

    private EvaluationResult performTest(final DataSetItem item, EvaluationType evaluation, final int scale) {
        final Bitmap image = MattingHelper.read(item.imagePath, Bitmap.Config.ARGB_8888, scale);
        final Bitmap trueAlpha = MattingHelper.convertToAlpha8(
                MattingHelper.read(item.trueAlphaPath, Bitmap.Config.ARGB_8888, scale));
        final Bitmap calculatedAlpha = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ALPHA_8);
        Log.i(TAG, item.id + " |> image, true alpha, alpha read");
        final Bitmap trimap = MattingHelper.changeTrimapColors(
                MattingHelper.read(item.trimapPath, Bitmap.Config.ARGB_8888, scale), 0xFF808080, Color.TRANSPARENT);
        Log.i(TAG, item.id + " |> trimap color transformed");

        Log.i(TAG, item.id + " |> calculate matte started");

        switch (evaluation) {
            case CPU:
                final long startTime = System.currentTimeMillis();
                TrimapActivity.calculateAlphaMask(image, trimap, calculatedAlpha);

                EvaluationResult result = getEvaluationResult(
                        item,
                        image,
                        trueAlpha,
                        calculatedAlpha,
                        trimap,
                        System.currentTimeMillis() - startTime,
                        scale
                );
                return result;
            case GPU:
                FragmentManager fm = getFragmentManager();
                ComputeShaderFragment fragment = (ComputeShaderFragment) fm.findFragmentByTag(FRAGMENT);

                final EvaluationResult[] testResult = {null};
                fragment.compute(new AlphaMattingComputeShader.Args(image, trimap, calculatedAlpha, new ComputeShaderResultCallback() {
                    @Override
                    public void success(ComputeShader shader, ComputeShaderArgs _args) {
                        AlphaMattingComputeShader.Args args = (AlphaMattingComputeShader.Args) _args;
                        EvaluationResult result = getEvaluationResult(
                                item,
                                args.image,
                                trueAlpha,
                                args.alpha,
                                args.trimap,
                                (long) ((args.duration - args.readAlphaBufferDuration - args.alphaCopyDuration) / 1e6),
                                scale
                        );
                        testResult[0] = result;

                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }

                    @Override
                    public void error(ComputeShader shader, ComputeShaderArgs args, Exception exception) {
                        exception.printStackTrace();
                        Log.e(TAG, "compute.error", exception);

                        testResult[0] = new EvaluationResult(item.id);
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                }));

                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return testResult[0];
                }
        }
        return null;
    }

    @NonNull
    private EvaluationResult getEvaluationResult(DataSetItem item, Bitmap image, Bitmap trueAlpha, Bitmap calculatedAlpha, Bitmap trimap, long duration, int scale) {
        Log.i(TAG, item.id + " |> matte calculated");

        EvaluationResult result = new EvaluationResult(item.id)
                .width(image.getWidth())
                .height(image.getHeight())
                .foregroundPixels(MattingHelper.calculatePixels(trimap, Color.WHITE))
                .backgroundPixels(MattingHelper.calculatePixels(trimap, Color.BLACK))
                .computationTime(duration)
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
