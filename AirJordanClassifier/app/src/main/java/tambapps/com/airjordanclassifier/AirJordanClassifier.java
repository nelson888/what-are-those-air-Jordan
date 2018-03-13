package tambapps.com.airjordanclassifier;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Vector;

class AirJordanClassifier {
    private final static int IMAGE_SIZE = 299; //256;
    private TensorFlowInferenceInterface tensorFlow;
    private Vector<String> labels;
    private final int[] intValues = new int[IMAGE_SIZE *IMAGE_SIZE];
    private float[] floatValues = new float[IMAGE_SIZE * IMAGE_SIZE * 3];
    private final static int IMAGE_MEAN = 0;
    private final static float IMAGE_STD = 255f;
    private final static float THRESHOLD = 0.1f;
    private final static String  INPUT_NAME = "Mul";
    private final static String  OUTPUT_NAME = "final_result";
    private final static String[]  OUTPUT_NAMES = new String[]{OUTPUT_NAME};
    private final float[] outputs;

    AirJordanClassifier(AssetManager assetManager, String modelFile, String classesFile) {
        tensorFlow = new TensorFlowInferenceInterface(assetManager, modelFile);
        final Operation operation = tensorFlow.graphOperation(OUTPUT_NAME);
        final int numClasses = (int) operation.output(0).shape().size(1);
        outputs = new float[numClasses];
        labels = new Vector<>(numClasses);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(assetManager.open(classesFile)))) {
            String line;
            while ((line = br.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Problem reading label file!" , e);
        }
        if (labels.size() != numClasses) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "There is %d classes and %d labels!!", numClasses, labels.size()));
        }
    }

    PriorityQueue<Prediction> classify(Bitmap b) {
        Bitmap bitmap = Bitmap.createScaledBitmap(b, IMAGE_SIZE, IMAGE_SIZE, false);

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[i * 3 + 2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }

        tensorFlow.feed(INPUT_NAME, floatValues, 1, IMAGE_SIZE, IMAGE_SIZE, 3);
        tensorFlow.run(OUTPUT_NAMES, false);
        tensorFlow.fetch(OUTPUT_NAME, outputs);

        PriorityQueue<Prediction> queue = new PriorityQueue<>(labels.size());
        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] > THRESHOLD) {
                queue.add(new Prediction(labels.get(i), outputs[i]));
            }
        }

        return queue;
    }

    class Prediction implements Comparable<Prediction> {
        final String name;
        final float confidence;

        Prediction(String name, float confidence) {
            this.name = name;
            this.confidence = confidence;
        }

        @Override
        public int compareTo(@NonNull Prediction o) {
            return (int) Math.signum(o.confidence - confidence);
        }
    }
}
