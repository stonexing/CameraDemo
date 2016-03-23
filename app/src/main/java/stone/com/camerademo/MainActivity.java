package stone.com.camerademo;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements Animation.AnimationListener {
    ImageButton btn_capture;
    SurfaceView sufaceView;
    SurfaceHolder mSurfaceHolder;
    Camera mCamera;
    int screenWidth;
    int screenHeight;


    RelativeLayout view_focus_layout;
    ImageView view_focus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        btn_capture = (ImageButton)findViewById(R.id.button_capture);
        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //拍照按钮
            }
        });

        sufaceView = (SurfaceView)findViewById(R.id.camera_sufaceView);
        mSurfaceHolder = sufaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback(){
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera(holder);
            }
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mCamera != null)
                {
                    mCamera.stopPreview();

                    mCamera.release();
                    mCamera = null;

                }
            }
        });

        view_focus_layout = (RelativeLayout)findViewById(R.id.focus_view_container);
        view_focus = (ImageView)findViewById(R.id.focus_view);
        sufaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_UP){
                    return true;
                }
                if (mCamera != null) {
                    mCamera.cancelAutoFocus();

                    float x = event.getX();
                    float y = event.getY();
                    view_focus_layout.setX(x - view_focus_layout.getWidth() / 2);
                    view_focus_layout.setY(y - view_focus_layout.getHeight() / 2);

                    view_focus.setAlpha(1.0f);
                    final Animation animationSet = AnimationUtils.loadAnimation(MainActivity.this, R.animator.focus_touch_animation);
                    view_focus.startAnimation(animationSet);


                    Camera.Parameters parameters = mCamera.getParameters();
//                    parameters.setFocusMode(Parameters.FOCUS_MODE_MACRO);
                    final List<String> focusModes = parameters.getSupportedFocusModes();

                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                    }
                    else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                    }
                    else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                    }
                    else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
                    {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }

                    Rect focusRect = calculateTapArea(x, y, 1f);
                    Log.i("=====", focusRect.toString() + parameters.getMaxNumMeteringAreas());
                    Rect meteringRect = calculateTapArea(x, y, 1.5f);
                    if (parameters.getMaxNumFocusAreas() > 0) {
                        List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
                        focusAreas.add(new Camera.Area(focusRect, 1000));
                        parameters.setFocusAreas(focusAreas);
                    }
                    if (parameters.getMaxNumMeteringAreas() > 0) {
                        List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                        meteringAreas.add(new Camera.Area(meteringRect, 1000));

                        parameters.setMeteringAreas(meteringAreas);
                    }
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {

                            if(success){
                                view_focus.setImageResource(R.drawable.focus_focused);
                                AnimationSet animation = (AnimationSet)AnimationUtils.loadAnimation(MainActivity.this, R.animator.focus_detch_animation);
                                animation.getAnimations().get(0).setAnimationListener(MainActivity.this);
                                view_focus.startAnimation(animation);
                                Log.i("-------", "聚焦成功");
                            }else{
                                Log.i("-------", "聚焦失败");
                            }
                        }
                    });

                    mCamera.setErrorCallback(new Camera.ErrorCallback() {
                        @Override
                        public void onError(int error, Camera camera) {
                            Toast.makeText(MainActivity.this, "Error ID:" + error, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return true;
            }
        });
    }



    private void initCamera(SurfaceHolder holder)
    {
        try{
            mCamera = Camera.open(0);
            mCamera.setDisplayOrientation(90);
//            view_focus.setBackgroundDrawable(getResources().getDrawable(R.drawable.focus_focusing));

            Camera.Parameters parameters = mCamera.getParameters();


            parameters.setPictureFormat(ImageFormat.JPEG);

            parameters.set("jpeg-quality", 100);

            List<Camera.Size> prevszize = parameters.getSupportedPreviewSizes();
            Camera.Size prevSize = prevszize.get(0);
            for (int i = 1; i < prevszize.size(); i++) {
                Camera.Size size = (Camera.Size) prevszize.get(i);
                if (size.width * size.height > prevSize.width *prevSize.height){
                    prevSize = size;
                }
            }
            parameters.setPreviewSize(prevSize.width, prevSize.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            List<Camera.Size> pszize = parameters.getSupportedPictureSizes();
            Camera.Size sizeMax = pszize.get(0);
            for (int i = 1; i < pszize.size(); i++) {
                Camera.Size size = (Camera.Size) pszize.get(i);
                if (size.width * size.height > sizeMax.width *sizeMax.height){
                    sizeMax = size;
                }
            }
            parameters.setPictureSize(sizeMax.width, sizeMax.height);


            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();


        }catch (Exception e){
        }
    }


    private Rect calculateTapArea(float x, float y, float coefficient) {
        Log.i("=======", ""+x+"---"+y);
        float focusAreaSize = 100;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();

        float centerX =  ((x / getResolution().width -0.5f) * 1000);
        float centerY = ((y / getResolution().height -0.5f) * 1000);

        float left = clamp(centerX - areaSize / 2, -1000, 1000);
        float top = clamp(centerY - areaSize / 2, -1000, 1000);
        float right = clamp(left + areaSize, -1000, 1000);
        float bottom = clamp(top + areaSize, -1000, 1000);

        return new Rect(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
    }



    public Camera.Size getResolution() {
        Camera.Parameters params = mCamera.getParameters();
        Camera.Size s = params.getPreviewSize();
        return s;
    }

    private float clamp(float x, float min, float max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public void onAnimationStart(Animation animation){


    }

    public void onAnimationEnd(Animation animation){
        view_focus.setAlpha(0.0f);
        view_focus.setImageResource(R.drawable.focus_focusing);
    }

    public void onAnimationRepeat(Animation animation){

    }
}
