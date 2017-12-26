package com.zef.hyperworks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.zef.hyperworks.interactor.Classifier;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/retrained_graph_x.pb";
    private static final String LABEL_FILE = "file:///android_asset/retrained_labels_x.txt";

    private TextView textViewResult;
    private ImageView imgCheckObj;
    private CameraView cameraView;
    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.cameraView);
        textViewResult = findViewById(R.id.textViewIdentified);
        imgCheckObj = findViewById(R.id.imgCheckObj);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());

        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM);
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);

        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] picture) {
                super.onPictureTaken(picture);
                Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
                textViewResult.setText(results.toString());
            }
        });

        imgCheckObj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.capturePicture();
            }
        });

        //init tensorflow
        initTensorflow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorflow() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = ImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);
                } catch (final Exception e) {
                    throw new RuntimeException("error initialising tensorflow", e);
                }
            }
        });
    }
}
