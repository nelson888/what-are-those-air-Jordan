package tambapps.com.airjordanclassifier;

import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.FileOutputStream;

public class PhotoActivity extends AppCompatActivity implements Camera.PictureCallback {

    private Camera camera;
    public final static String TEMP_IMAGE = "temp.jpg";
    private byte[] image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        hideSystemUI();
        try {
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(params);
        }
        catch (Exception e){
            setResult(RESULT_CANCELED);
            finish();
        }

        CameraPreview mPreview = new CameraPreview(this, camera);
        FrameLayout preview = findViewById(R.id.camera_preview_container);
        preview.addView(mPreview);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        image = data;
    }

    public void takePicture(View view){
        findViewById(R.id.keep_picture_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.button_capture).setVisibility(View.GONE);
        camera.takePicture(null, null, this);
    }

    public void keepPicture(View v) {
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(TEMP_IMAGE, Context.MODE_PRIVATE);
            outputStream.write(image);
            outputStream.close();
        } catch (Exception e) {
            Toast.makeText(this, "Failed saving image", Toast.LENGTH_SHORT).show();
            retryPicture(null);
            e.printStackTrace();
        }
        setResult(RESULT_OK);
        finish();
    }

    public void retryPicture(View view) {
        findViewById(R.id.keep_picture_layout).setVisibility(View.GONE);
        findViewById(R.id.button_capture).setVisibility(View.VISIBLE);
        camera.startPreview();
    }

    private void hideSystemUI() {
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(flags);

        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags );
                }
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
