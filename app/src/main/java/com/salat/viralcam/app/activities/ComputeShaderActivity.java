package com.salat.viralcam.app.activities;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.salat.viralcam.app.R;
import com.salat.viralcam.app.computeshader.ComputeShader;
import com.salat.viralcam.app.computeshader.ComputeShaderArgs;
import com.salat.viralcam.app.computeshader.ComputeShaderResultCallback;
import com.salat.viralcam.app.fragments.ComputeShaderFragment;
import com.salat.viralcam.app.matting.AlphaMattingComputeShader;
import com.salat.viralcam.app.model.MattingDataSet;
import com.salat.viralcam.app.util.MattingHelper;

public class ComputeShaderActivity extends AppCompatActivity implements ComputeShaderFragment.OnFragmentEvents {
    private static final String TAG = "ComputeShaderActivity";
    private static final String FRAGMENT = "COMPUTE_SHADER_FRAGMENT";
    public static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private Spinner imageSpinner;
    private ArrayAdapter<String> imageAdapter;
    private ArrayAdapter<String> imageScaleAdapter;
    private Spinner imageScaleSpinner;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_compute_shader);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String[] arrayImageSpinner = new String[27];
        for (int i = 0; i < 27; i++) {
            arrayImageSpinner[i] = String.format("GT%02d", i+1);
        }
        imageSpinner = (Spinner) findViewById(R.id.image_id_spinner);
        imageAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, arrayImageSpinner);
        imageSpinner.setAdapter(imageAdapter);


        String[] arrayScaleSpinner = new String[5];
        for (int i = 0; i < 5; i++ ) {
            arrayScaleSpinner[i] = String.format("%dx", (int) Math.pow(2, i));
        }
        imageScaleSpinner = (Spinner) findViewById(R.id.image_scale_spinner);
        imageScaleAdapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, arrayScaleSpinner);
        imageScaleSpinner.setAdapter(imageScaleAdapter);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Run forest run" , Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();

                FragmentManager fm = getFragmentManager();
                Fragment fragment = fm.findFragmentByTag(FRAGMENT);

                if (fragment == null) {
                    fragment = ComputeShaderFragment.newInstance();

                    fragment.setRetainInstance(false);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.container, fragment, FRAGMENT)
                            .commit();
                } else {
                    onComputeShaderFragmentStart((ComputeShaderFragment) fragment);
                }
            }
        });
//        fab.performClick();
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fab.performClick();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                fab.performClick();
            }
        };
        imageSpinner.setOnItemSelectedListener(listener);
//        imageScaleSpinner.setOnItemSelectedListener(listener);
    }

    @Override
    public ComputeShader createComputeShader() {
        //return new AddVectorsComputeShader(getResources());
        return new AlphaMattingComputeShader(getResources());
    }

    @Override
    public void onComputeShaderFragmentStart(final ComputeShaderFragment fragment) {
        final ImageView imageView = (ImageView) findViewById(R.id.imageView2);
        final ImageView trimapView = (ImageView) findViewById(R.id.imageView3);
        final ImageView alphaView = (ImageView) findViewById(R.id.imageView4);
        final ImageView trueAlphaView = (ImageView) findViewById(R.id.imageView5);
        fab.setEnabled(false);

        if (imageView == null || trimapView == null || alphaView == null || trueAlphaView == null)
            return;

        final int SCALE = imageScaleSpinner.getSelectedItemId() > 0 ? (int) Math.pow(2, imageScaleSpinner.getSelectedItemId()) : 1;
        final int ID = imageSpinner.getSelectedItemId() > 0 ? (int) imageSpinner.getSelectedItemId() : 1;
        final int VERSION = 1;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //        final int SIZE = 1024; //* invocationAttempt*invocationAttempt;
//        //invocationAttempt++;
//        IntBuffer input1buffer = IntBuffer.allocate(SIZE);
//        for (int i = 0; i < SIZE; i++) {
//            input1buffer.put(i, 1027);
//        }
//
//        IntBuffer input2Buffer = IntBuffer.allocate(SIZE);
//        for (int i = 0; i < SIZE; i++) {
//            input2Buffer.put(i, 3039);
//        }
//
//        final long start = System.currentTimeMillis();
//        fragment.compute(AddVectorsComputeShader.createArgs(input1buffer, input2Buffer, new ComputeShaderResultCallback() {
//            @Override
//            public void success(ComputeShader shader, ComputeShaderArgs args) {
//                long end = System.currentTimeMillis();
//                IntBuffer c = AddVectorsComputeShader.getResultFromArgs(args);
//                Log.e(TAG, String.format(
//                        "Result (%d) [%d, ... %d, %d, ... %d] in %d [ms]",
//                        SIZE, c.get(0), c.get(SIZE / 2), c.get(SIZE / 2 + 1), c.get(SIZE - 1), end - start));
//            }
//
//            @Override
//            public void error(ComputeShader shader, ComputeShaderArgs args, Exception exception) {
//                Log.e(TAG, exception.toString());
//            }
//        }));

//        Bitmap image = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
//        Bitmap trimap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
//        Bitmap alpha = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ALPHA_8);
//
//        Canvas canvas = new Canvas(image);
//        canvas.drawColor(Color.RED);
//
//        Paint paint = new Paint();
//        paint.setStyle(Paint.Style.FILL);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
//        paint.setColor(Color.MAGENTA);
//        paint.setAlpha(0x77);
//        canvas.drawRect(0, HEIGHT/2, WIDTH, HEIGHT, paint);
//
//        canvas.setBitmap(trimap);
//        canvas.drawColor(Color.BLACK);
//        paint.setColor(Color.WHITE);
//        canvas.drawRect(0, HEIGHT/2, WIDTH, HEIGHT, paint);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        canvas.drawRect(0, HEIGHT/2 - 10, WIDTH, HEIGHT/2 + 10, paint);
//        canvas.drawCircle(WIDTH/2, HEIGHT/2, 28, paint);
//
//        canvas.setBitmap(alpha);
//        canvas.drawColor(Color.BLACK);
                final Bitmap image = MattingHelper.read(
                        MattingDataSet.AlphamattingComDataSet.getImagePath(ID), Bitmap.Config.ARGB_8888, SCALE);
                final Bitmap trueAlpha = MattingHelper.convertToAlpha8(MattingHelper.read(
                                MattingDataSet.AlphamattingComDataSet.getTrueAlphaPath(ID), Bitmap.Config.ARGB_8888, SCALE));
                final Bitmap alpha = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ALPHA_8);
                final Bitmap trimap = MattingHelper.changeTrimapColors(
                        MattingHelper.read(MattingDataSet.AlphamattingComDataSet.getTrimapPath(ID, VERSION), Bitmap.Config.ARGB_8888, SCALE), 0xFF808080, Color.TRANSPARENT);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(image);
                        trimapView.setImageBitmap(trimap);
                        alphaView.setImageBitmap(alpha);
                        trueAlphaView.setImageBitmap(trueAlpha);
                    }
                });

                final long start = System.currentTimeMillis();
                fragment.compute(new AlphaMattingComputeShader.Args(image, trimap, alpha, new ComputeShaderResultCallback() {
                    @Override
                    public void success(ComputeShader shader, ComputeShaderArgs _args) {
                        final AlphaMattingComputeShader.Args args = (AlphaMattingComputeShader.Args) _args;
                        final long end = System.currentTimeMillis();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String message = String.format("Duration %d [ms]", end - start);
                                Log.i(TAG, message);
                                Toast.makeText(ComputeShaderActivity.this, message, Toast.LENGTH_SHORT).show();

                                alphaView.setImageBitmap(args.alpha);
                                fab.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void error(ComputeShader shader, ComputeShaderArgs args, Exception exception) {

                    }
                }));
            }
        });
        thread.start();
    }
}
