package tdif.colordetected;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SeekBar.OnSeekBarChangeListener, View.OnClickListener {


    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "OpenCV not loaded");
        } else {
            Log.d("OpenCV", "OpenCV loaded");
        }
    }


    private static final int NUM_CODES = 10;

    // HSV colour bounds
    private static final Scalar COLOR_BOUNDS[][] = {
            {new Scalar(0, 0, 0), new Scalar(180, 250, 50)},    // black
            {new Scalar(0, 90, 10), new Scalar(15, 250, 100)},    // brown
            {new Scalar(0, 0, 0), new Scalar(0, 0, 0)},         // red (defined by two bounds)
            {new Scalar(4, 100, 100), new Scalar(9, 250, 150)},   // orange
            {new Scalar(20, 130, 100), new Scalar(30, 250, 160)}, // yellow
            {new Scalar(45, 50, 60), new Scalar(72, 250, 150)},   // green
            {new Scalar(80, 50, 50), new Scalar(106, 250, 150)},  // blue
            {new Scalar(130, 40, 50), new Scalar(155, 250, 150)}, // purple
            {new Scalar(0, 0, 50), new Scalar(180, 50, 80)},       // gray
            {new Scalar(0, 0, 90), new Scalar(180, 15, 140)}      // white
    };

    // red wraps around in HSV, so we need two ranges
    private static Scalar LOWER_RED1 = new Scalar(0, 65, 100);
    private static Scalar UPPER_RED1 = new Scalar(2, 250, 150);
    private static Scalar LOWER_RED2 = new Scalar(171, 65, 50);
    private static Scalar UPPER_RED2 = new Scalar(180, 250, 150);

    private SparseIntArray _locationValues = new SparseIntArray(4);

    private JavaCameraView cameraView;
    SeekBar seekBar;
    Button btnGuide;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        btnGuide = (Button) findViewById(R.id.btnGuide);
        btnGuide.setOnClickListener(this);
        seekBar = (SeekBar) findViewById(R.id.CameraZoomControls);
        seekBar.setOnSeekBarChangeListener(this);
        cameraView = (JavaCameraView) findViewById(R.id.cameraview);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCameraIndex(0);//0 for rear and 1 for front
        
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();
    }
    public void onClick(View v) {
        Intent i = new Intent(this, Guide.class);

        startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    public void onResume()
    {
        super.onResume();
        cameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Toast toast = Toast.makeText(MainActivity.this,width+"/"+height,Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //รับค่าจากกล้อง
        Mat imgMat = inputFrame.rgba();
        int cols = imgMat.cols();
        int rows = imgMat.rows();

        //สร้างรูปอีกรูปจากรูปเดิม
        Mat subMat = imgMat.submat(rows / 2, rows / 2 + 30, cols / 2 - 50, cols / 2 + 50);

        Mat filteredMat = new Mat();
        Imgproc.cvtColor(subMat, subMat, Imgproc.COLOR_RGB2BGR);
        Imgproc.bilateralFilter(subMat, filteredMat, 5, 80, 80);
        Imgproc.cvtColor(filteredMat, filteredMat, Imgproc.COLOR_BGR2HSV);

        findLocations(filteredMat);
        if (_locationValues.size() >= 3) {
            // recover the resistor value by iterating through the centroid locations
            // in an ascending manner and using their associated colour values
            int k_tens = _locationValues.keyAt(0);
            int k_units = _locationValues.keyAt(1);
            int k_power = _locationValues.keyAt(2);

            int value = 10 * _locationValues.get(k_tens) + _locationValues.get(k_units);
            value *= Math.pow(10, _locationValues.get(k_power));

            String valueStr;
            if (value >= 1e3 && value < 1e6)
                valueStr = String.valueOf(value / 1e3) + " KOhm";
            else if (value >= 1e6)
                valueStr = String.valueOf(value / 1e6) + " MOhm";
            else
                valueStr = String.valueOf(value) + " Ohm";

            if (value <= 1e9)
                Imgproc.putText(imgMat, valueStr, new Point(10, 100), Core.FONT_HERSHEY_COMPLEX,
                        2, new Scalar(255, 0, 0, 255), 3);
        }
        Scalar color = new Scalar(255, 0, 0, 255);
        Imgproc.line(imgMat, new Point(cols / 2 - 50, rows / 2), new Point(cols / 2 + 50, rows / 2), color, 2);
        return imgMat;
    }
    // find contours of colour bands and the x-coords of their centroids
    private void findLocations(Mat searchMat) {
        _locationValues.clear();
        SparseIntArray areas = new SparseIntArray(4);

        for (int i = 0; i < NUM_CODES; i++) {
            Mat mask = new Mat();
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();

            if (i == 2) {
                // combine the two ranges for red
                Core.inRange(searchMat, LOWER_RED1, UPPER_RED1, mask);
                Mat rmask2 = new Mat();
                Core.inRange(searchMat, LOWER_RED2, UPPER_RED2, rmask2);
                Core.bitwise_or(mask, rmask2, mask);
            } else
                Core.inRange(searchMat, COLOR_BOUNDS[i][0], COLOR_BOUNDS[i][1], mask);

            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            for (int contIdx = 0; contIdx < contours.size(); contIdx++) {
                int area;
                if ((area = (int) Imgproc.contourArea(contours.get(contIdx))) > 20) {
                    Moments M = Imgproc.moments(contours.get(contIdx));
                    int cx = (int) (M.get_m10() / M.get_m00());

                    // if a colour band is split into multiple contours
                    // we take the largest and consider only its centroid
                    boolean shouldStoreLocation = true;
                    for (int locIdx = 0; locIdx < _locationValues.size(); locIdx++) {
                        if (Math.abs(_locationValues.keyAt(locIdx) - cx) < 10) {
                            if (areas.get(_locationValues.keyAt(locIdx)) > area) {
                                shouldStoreLocation = false;
                                break;
                            } else {
                                _locationValues.delete(_locationValues.keyAt(locIdx));
                                areas.delete(_locationValues.keyAt(locIdx));
                            }
                        }
                    }

                    if (shouldStoreLocation) {
                        areas.put(cx, area);
                        _locationValues.put(cx, i);
                    }
                }
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
