package ict.mgame.homesecurity;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";

    private final Context context;
    private final LifecycleOwner lifecycleOwner;
    private final PreviewView viewFinder;
    private final ExecutorService cameraExecutor;

    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ImageAnalysis imageAnalysis;
    private ProcessCameraProvider cameraProvider;

    private boolean isMotionSensorEnabled = false;
    private byte[] previousFrame;
    private static final double MOTION_THRESHOLD = 15.0;
    private static final int MOTION_WINDOW_SAMPLES = 3;
    private int motionSampleCount = 0;
    private double motionAccumulator = 0.0;

    private MotionDetectionListener motionListener;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    public interface MotionDetectionListener {
        void onMotionDetected();
    }

    public CameraManager(Context context, LifecycleOwner lifecycleOwner, PreviewView viewFinder) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.viewFinder = viewFinder;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }

    public void setMotionListener(MotionDetectionListener listener) {
        this.motionListener = listener;
    }

    public void setMotionSensorEnabled(boolean enabled) {
        this.isMotionSensorEnabled = enabled;
        if (!enabled) {
            previousFrame = null;
        }
    }

    public boolean isMotionSensorEnabled() {
        return isMotionSensorEnabled;
    }

    public void switchCamera() {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCamera();
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrameForMotion);

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageCapture, videoCapture, imageAnalysis);
                } catch (Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                    // Fallback without video if failed
                    try {
                        cameraProvider.unbindAll();
                        cameraProvider.bindToLifecycle(
                                lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis);
                    } catch (Exception e) {
                        Log.e(TAG, "Camera initialization failed", e);
                    }
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider failed", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    public void takePhoto(OnPhotoSavedCallback callback) {
        if (imageCapture == null) {
            if (callback != null) callback.onError(new IllegalStateException("ImageCapture not initialized"));
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HomeSecurity-Image");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                context.getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
        ).build();

        try {
            imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            if (callback != null) {
                                callback.onPhotoSaved(outputFileResults.getSavedUri() != null ? outputFileResults.getSavedUri().toString() : null);
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                            if (callback != null) {
                                callback.onError(exception);
                            }
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to take picture", e);
            if (callback != null) callback.onError(e);
        }
    }

    public interface OnPhotoSavedCallback {
        void onPhotoSaved(String uri);
        void onError(Exception e);
    }

    public void toggleVideoRecording(OnVideoRecordingListener listener) {
        if (videoCapture == null) return;

        if (recording != null) {
            recording.stop();
            recording = null;
            return;
        }

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HomeSecurity-Video");
        }

        MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions.Builder(
                context.getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build();

        recording = videoCapture.getOutput()
                .prepareRecording(context, mediaStoreOutputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context), recordEvent -> {
                    if (recordEvent instanceof VideoRecordEvent.Start) {
                        if (listener != null) listener.onRecordingStarted();
                    } else if (recordEvent instanceof VideoRecordEvent.Finalize) {
                        if (!((VideoRecordEvent.Finalize) recordEvent).hasError()) {
                            if (listener != null) listener.onRecordingStopped(((VideoRecordEvent.Finalize) recordEvent).getOutputResults().getOutputUri().toString());
                        } else {
                            recording = null;
                            if (listener != null) listener.onError(((VideoRecordEvent.Finalize) recordEvent).getError());
                        }
                    }
                });
    }

    public interface OnVideoRecordingListener {
        void onRecordingStarted();
        void onRecordingStopped(String uri);
        void onError(int error);
    }

    private void analyzeFrameForMotion(ImageProxy image) {
        if (!isMotionSensorEnabled) {
            image.close();
            return;
        }

        ImageProxy.PlaneProxy yPlane = image.getPlanes()[0];
        ByteBuffer yBuffer = yPlane.getBuffer();
        byte[] currentFrame = new byte[yBuffer.remaining()];
        yBuffer.get(currentFrame);

        if (previousFrame != null && currentFrame.length == previousFrame.length) {
            double difference = calculateMotionDifference(previousFrame, currentFrame);
            motionAccumulator += difference;
            motionSampleCount++;
            if (motionSampleCount >= MOTION_WINDOW_SAMPLES) {
                double avg = motionAccumulator / motionSampleCount;
                if (avg > MOTION_THRESHOLD) {
                    if (motionListener != null) {
                        motionListener.onMotionDetected();
                    }
                }
                motionAccumulator = 0.0;
                motionSampleCount = 0;
            }
        }
        previousFrame = currentFrame;
        image.close();
    }

    private double calculateMotionDifference(byte[] frame1, byte[] frame2) {
        if (frame1.length == 0 || frame2.length == 0) return 0;
        long difference = 0;
        int sampleStep = 8;
        int sampleCount = 0;
        for (int i = 0; i < frame1.length; i += sampleStep) {
            int val1 = frame1[i] & 0xFF;
            int val2 = frame2[i] & 0xFF;
            difference += Math.abs(val2 - val1);
            sampleCount++;
        }
        return sampleCount == 0 ? 0 : (double) difference / sampleCount;
    }

    public void shutdown() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
