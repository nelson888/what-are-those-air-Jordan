package tambapps.com.airjordanclassifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    private static int REQUEST_IMAGE_CAPTURE = 1, REQUEST_PICK_FROM_GALLERY = 2;
    private static int CAMERA_PERMISSION = 0;
    ImageView imageView;
    TextView label;
    ProgressBar progressBar;
    private AirJordanClassifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        label = findViewById(R.id.label);
        imageView = findViewById(R.id.photo);
        progressBar = findViewById(R.id.progress);
        classifier = new AirJordanClassifier(getAssets(), "model.pb", "labels.txt");

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Button photoButton = findViewById(R.id.photo_but);
            photoButton.setVisibility(View.GONE);
            Button galleryBut = findViewById(R.id.galery_but);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) galleryBut.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_END);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        }

    }

    public void takePhoto(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        } else {
            startActivityForResult(new Intent(this, PhotoActivity.class), REQUEST_IMAGE_CAPTURE);
        }
    }

    public void takeFromGallery(View v) {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_PICK_FROM_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(new Intent(this, PhotoActivity.class), REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "Camera permission was not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Couldn't get an image", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_PICK_FROM_GALLERY || requestCode == REQUEST_IMAGE_CAPTURE) {
            final Uri bitmapUri;
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                bitmapUri = Uri.fromFile(new File(getFilesDir(), PhotoActivity.TEMP_IMAGE));
            } else {
                bitmapUri = data.getData();
            }

            try {
                classify(MediaStore.Images.Media.getBitmap(getContentResolver(), bitmapUri));
            } catch (IOException e) {
                Toast.makeText(MainActivity.this, "Error while loading image, please retry", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void classify(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
        label.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        new ClassifyTask(this).execute(bitmap);
    }

    private static class ClassifyTask extends AsyncTask<Bitmap, Void, Queue<AirJordanClassifier.Prediction>> {

        @SuppressLint("StaticFieldLeak")
        private MainActivity activity;

        ClassifyTask(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        protected Queue<AirJordanClassifier.Prediction> doInBackground(Bitmap... bitmaps) {

            return activity.classifier.classify(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(Queue<AirJordanClassifier.Prediction> predictions) {
            if (predictions.isEmpty()) {
                activity.label.setText("There is no prediction for this image");
            } else {
                StringBuilder builder = new StringBuilder().append("<font color='red'>");
                for (int i = 0; i < 3 && !predictions.isEmpty(); i++) {
                    AirJordanClassifier.Prediction prediction = predictions.remove();
                    builder.append(prediction.name)
                            .append(": ")
                            .append(String.format(Locale.ENGLISH, "%.3f", prediction.confidence));
                    if (i == 0) {
                        builder.append("</font>");
                    }
                    builder.append("<br/>");
                }
                activity.label.setText(Html.fromHtml(builder.toString()));
            }
            activity.progressBar.setVisibility(View.GONE);
            activity.label.setVisibility(View.VISIBLE);

            this.activity = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new File(getFilesDir(), PhotoActivity.TEMP_IMAGE).delete();
    }
}
