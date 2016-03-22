package com.salat.viralcam.app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MattingEvaluationActivity extends AppCompatActivity {

    private static final String TAG = "MattingEvaluation";

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matting_evaluation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final View evaluateCpuButton = findViewById(R.id.evaluate_cpu_button);
        final View evaluateGpuButton = findViewById(R.id.evaluate_gpu_button);
        evaluateGpuButton.setEnabled(false);

        evaluateCpuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                evaluateCpuButton.setEnabled(false);
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
                            final EvaluationResult result = performTest(item);
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
                                progress.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                                progress.setText("0%%");
                            }
                        });
                    }
                };
                task.start();
            }
        });
    }

    private EvaluationResult performTest(DataSetItem item) {
        Bitmap image = read(item.imagePath, Bitmap.Config.ARGB_8888);
        Bitmap trueAlpha = convertToAlpha8(read(item.trueAlphaPath, Bitmap.Config.ARGB_8888));
        Bitmap calculatedAlpha = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ALPHA_8);
        Log.i(TAG, item.id + " |> image, true alpha, alpha read");
        Bitmap trimap = changeTrimapColors(read(item.trimapPath, Bitmap.Config.ARGB_8888), 0xFF808080, Color.TRANSPARENT);
        Log.i(TAG, item.id + " |> trimap color transformed");

        Log.i(TAG, item.id + " |> calculate matte started");
        long startTime = System.currentTimeMillis();{
            TrimapActivity.calculateAlphaMask(image, trimap, calculatedAlpha);
        }
        long endTime = System.currentTimeMillis();
        Log.i(TAG, item.id + " |> matte calculated");

        EvaluationResult result = new EvaluationResult(item.id)
                .width(image.getWidth())
                .height(image.getHeight())
                .foregroundPixels(calculatePixels(trimap, Color.WHITE))
                .backgroundPixels(calculatePixels(trimap, Color.BLACK))
                .computationTime(endTime - startTime)
                .sumOfAbsoluteDifferences(calculateSAD(trueAlpha, calculatedAlpha))
                .squaredError(calculateSE(trueAlpha, calculatedAlpha));
        Log.i(TAG, item.id + " |> result created");
        Log.i(TAG, item.id + " |> " + result.toString());

        saveImage(calculatedAlpha, item.resultAlphaPath);
        Log.i(TAG, item.id + " |> calculated alpha saved");

        drawResultImage(trimap, image, calculatedAlpha);
        saveImage(trimap, item.resultImagePath);
        Log.i(TAG, item.id + " |> image saved saved");

        image.recycle();
        trimap.recycle();
        trueAlpha.recycle();
        calculatedAlpha.recycle();

        return result;
    }

    private Bitmap convertToAlpha8(Bitmap image) {
        Bitmap alpha = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ALPHA_8);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int value = Color.green(image.getPixel(x, y));
                alpha.setPixel(x, y, Color.argb(value, value, value, value));
            }
        }

        image.recycle();
        image = null;

        return alpha;
    }

    private Bitmap changeTrimapColors(Bitmap trimap, int originalUnknown, int currentUnknown) {
        Bitmap newTrimap = trimap.copy(Bitmap.Config.ARGB_8888, true);
        trimap.recycle();
        trimap = null;
        newTrimap.setHasAlpha(true);

        for (int x = 0; x < newTrimap.getWidth(); x++) {
            for (int y = 0; y < newTrimap.getHeight(); y++) {
                if(newTrimap.getPixel(x, y) == originalUnknown)
                    newTrimap.setPixel(x, y, currentUnknown);
            }
        }
        return newTrimap;
    }

    private Bitmap read(String imagePath, Bitmap.Config inPreferredConfig){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = inPreferredConfig;

        return BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + imagePath, options);
    }

    private int calculatePixels(Bitmap trimap, int color) {
        int count = 0;
        for (int x = 0; x < trimap.getWidth(); x++) {
            for (int y = 0; y < trimap.getHeight(); y++) {
                if(trimap.getPixel(x, y) == color)
                    count++;
            }
        }

        return count;
    }

    private long calculateSE(Bitmap trueAlpha, Bitmap calculatedAlpha) {
        long mse = 0;
        for (int x = 0; x < trueAlpha.getWidth(); x++) {
            for (int y = 0; y < trueAlpha.getHeight(); y++) {
                mse += Math.pow(getDelta(
                        trueAlpha.getPixel(x, y),
                        calculatedAlpha.getPixel(x, y)), 2);
            }
        }

        return mse;
    }

    private long calculateSAD(Bitmap trueAlpha, Bitmap calculatedAlpha) {
        long sad = 0;
        for (int x = 0; x < trueAlpha.getWidth(); x++) {
            for (int y = 0; y < trueAlpha.getHeight(); y++) {
                sad += Math.abs(getDelta(
                        trueAlpha.getPixel(x, y),
                        calculatedAlpha.getPixel(x, y)));
            }
        }

        return sad;
    }

    private int getDelta(int x, int y) {
        return (Color.alpha(x)) - Color.alpha(y);
    }

    private void drawResultImage(Bitmap canvasBitmap, Bitmap image, Bitmap calculatedAlpha) {
        Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawColor(Color.GREEN);

        Paint tempPaint = new Paint();
        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(tempPaint);

        // Draw masked foreground. Result is foreground with transparent border.
        canvas.drawBitmap(calculatedAlpha, 0, 0, null);
        tempPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(image, 0, 0, tempPaint);
    }

    private void saveImage(Bitmap image, String path) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path);

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(file);
            (image.getConfig() == Bitmap.Config.ALPHA_8 ? image.copy(Bitmap.Config.ARGB_8888, false) : image)
                    .compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.e(TAG, "Something get wrong: " + e.toString());
            Log.e(TAG, "on file: " + file.toString());
        } finally {
            try {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }
    }
}
