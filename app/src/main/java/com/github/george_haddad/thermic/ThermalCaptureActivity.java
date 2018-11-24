package com.github.george_haddad.thermic;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.flir.flironesdk.Device;
import com.flir.flironesdk.Frame;
import com.flir.flironesdk.FrameProcessor;
import com.flir.flironesdk.RenderedImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

/**
 * @author George Haddad
 */
public class ThermalCaptureActivity extends AppCompatActivity implements Device.Delegate, FrameProcessor.Delegate, Device.StreamDelegate {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.getDefault());
    private Device.TuningState currentTuningState = Device.TuningState.Unknown;
    private FrameProcessor frameProcessor;
    private volatile Device flirDevice;
    private int snapshotRequested = Integer.MAX_VALUE;
    private String snapshotId = null;
    private int snapsTaken = 0;
    GLSurfaceView thermalImageView;
    private File dataStoragePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermal_capture);

        EnumSet<RenderedImage.ImageType> imageSet = EnumSet.of(
                RenderedImage.ImageType.VisibleAlignedRGBA8888Image,
                RenderedImage.ImageType.BlendedMSXRGBA8888Image,
                RenderedImage.ImageType.ThermalLinearFlux14BitImage,
                RenderedImage.ImageType.ThermalRadiometricKelvinImage
        );

        RenderedImage.ImageType defaultImageType = RenderedImage.ImageType.BlendedMSXRGBA8888Image;
        frameProcessor = new FrameProcessor(this, this, imageSet, true);
        frameProcessor.setGLOutputMode(defaultImageType);

        thermalImageView = (GLSurfaceView) findViewById(R.id.thermal_view);
        thermalImageView.setPreserveEGLContextOnPause(true);
        thermalImageView.setEGLContextClientVersion(2);
        thermalImageView.setRenderer(frameProcessor);
        thermalImageView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        thermalImageView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);

        dataStoragePath = getExternalFilesDir(null);
        if (!dataStoragePath.exists()) {
            dataStoragePath.mkdirs();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            Device.startDiscovery(this, this);
        }
        catch(IllegalStateException e){
            //Ignore error if discovery has already started
        }
        catch (SecurityException e){
            Toast.makeText(this, "Please insert FLIR One and select "+getString(R.string.app_name), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        Device.stopDiscovery();
        flirDevice = null;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        thermalImageView.onResume();
        if (flirDevice != null) {
            flirDevice.startFrameStream(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        thermalImageView.onPause();
        if (flirDevice != null){
            flirDevice.stopFrameStream();
        }
    }

    @Override
    public void onTuningStateChanged(Device.TuningState tuningState) {
        currentTuningState = tuningState;
        if (tuningState == Device.TuningState.InProgress){
            runOnUiThread(new Thread(){
                @Override
                public void run() {
                    super.run();
                    findViewById(R.id.tuningProgressBar).setVisibility(View.VISIBLE);
                    findViewById(R.id.tuningTextView).setVisibility(View.VISIBLE);
                }
            });
        }
        else {
            runOnUiThread(new Thread() {
                @Override
                public void run() {
                    super.run();
                    findViewById(R.id.tuningProgressBar).setVisibility(View.GONE);
                    findViewById(R.id.tuningTextView).setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onAutomaticTuningChanged(boolean b) {

    }

    @Override
    public void onDeviceConnected(Device device) {
        flirDevice = device;
        flirDevice.startFrameStream(this);
    }

    @Override
    public void onDeviceDisconnected(Device device) {
        flirDevice.stopFrameStream();
        flirDevice = null;
    }

    @Override
    public void onFrameProcessed(final RenderedImage renderedImage) {
        switch(renderedImage.imageType()) {
            case BlendedMSXRGBA8888Image: {
                if (isSnapshotRequested()) {
                    snapshotRequested++;
                    saveSnapshot(renderedImage);
                }

                break;
            }

            case ThermalRGBA8888Image: {
                if (isSnapshotRequested()) {
                    snapshotRequested++;
                    saveSnapshot(renderedImage);
                }

                break;
            }

            case VisibleAlignedRGBA8888Image: {
                if (isSnapshotRequested()) {
                    snapshotRequested++;
                    saveSnapshot(renderedImage);
                }

                break;
            }

            case ThermalRadiometricKelvinImage: {
                if (isSnapshotRequested()) {
                    snapshotRequested++;
                    saveSnapshot(renderedImage);
                }

                break;
            }

            case ThermalLinearFlux14BitImage: {
                if (isSnapshotRequested()) {
                    snapshotRequested++;
                    saveSnapshot(renderedImage);
                }

                break;
            }

            default: {
                break;
            }
        }
    }

    @Override
    public void onFrameReceived(Frame frame) {
        if (currentTuningState != Device.TuningState.InProgress){
            frameProcessor.processFrame(frame, FrameProcessor.QueuingOption.CLEAR_QUEUED);
            thermalImageView.requestRender();
        }
    }

    private boolean isSnapshotRequested() {
        return snapshotRequested < 4;
    }

    private synchronized void resetSnapsotRequest() {
        if (snapsTaken >= 4) {
            snapshotRequested = Integer.MAX_VALUE;
            snapshotId = null;
            snapsTaken = 0;


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ThermalCaptureActivity.this, "Snap!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void onCaptureButtonClicked(View view) {
        snapshotRequested = 0;
        snapshotId = SIMPLE_DATE_FORMAT.format(new Date());
    }

    private void saveSnapshot(final RenderedImage renderedImage) {
        new SnapshotAsyncTask().execute(renderedImage);
    }

    private class SnapshotAsyncTask extends AsyncTask<RenderedImage, Void, String> {
        final Context context = ThermalCaptureActivity.this;

        @Override
        protected String doInBackground(RenderedImage ... params) {
            RenderedImage renderedImage = params[0];
            RenderedImage.ImageType imgType = renderedImage.imageType();

            StringBuilder sb = new StringBuilder();
            sb.append("Thermic-");

            switch(imgType) {
                case BlendedMSXRGBA8888Image: {
                    sb.append("BlendedMSXRGBA8888-");
                    sb.append(snapshotId);
                    sb.append(".jpg");
                    break;
                }

                case VisibleAlignedRGBA8888Image: {
                    sb.append("VisibleAlignedRGBA8888-");
                    sb.append(snapshotId);
                    sb.append(".jpg");
                    break;
                }

                case ThermalLinearFlux14BitImage: {
                    sb.append("ThermalLinearFlux14bit-");
                    sb.append(snapshotId);
                    sb.append(".dta");
                    break;
                }

                case ThermalRadiometricKelvinImage: {
                    sb.append("ThermalRadiometricKelvin-");
                    sb.append(snapshotId);
                    sb.append(".dta");
                    break;
                }

                default: {
                    break;
                }
            }

            String fileName = sb.toString();
            File pubPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File snapshotFile = new File(dataStoragePath, fileName);

            String result = null;

            try {
                pubPath.mkdirs();
                Frame frame = renderedImage.getFrame();

                switch(imgType) {
                    case BlendedMSXRGBA8888Image: {
                        frame.save(new File(pubPath, fileName), frameProcessor);

                        SystemClock.sleep(50);
                        MediaScannerConnection.scanFile(context,
                                new String[]{new File(pubPath, fileName).toString()}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.d("ThermicApp", "Scanned " + path + ":");
                                        Log.d("ThermicApp", "-> uri=" + uri);
                                    }
                                });

                        snapsTaken++;
                        break;
                    }

                    case ThermalRadiometricKelvinImage: {
                        try {
                            saveRawData(snapshotFile, renderedImage.thermalPixelValues());
                        }
                        catch(IOException ioe) {
                            Log.e("ThermicApp", ioe.getMessage(), ioe);
                        }
                        finally {
                            snapsTaken++;
                        }

                        break;
                    }

                    case VisibleAlignedRGBA8888Image: {
                        try {
                            Bitmap outputBitmap = renderedImage.getBitmap();
                            saveBitmap(outputBitmap, snapshotFile);
                        }
                        catch(IOException ioe) {
                            Log.e("ThermicApp", ioe.getMessage(), ioe);
                        }
                        finally {
                            snapsTaken++;
                        }

                        break;
                    }

                    case ThermalLinearFlux14BitImage: {
                        try {
                            saveRawData(snapshotFile, renderedImage.thermalPixelValues());
                        }
                        catch(IOException ioe) {
                            Log.e("ThermicApp", ioe.getMessage(), ioe);
                        }
                        finally {
                            snapsTaken++;
                        }

                        break;
                    }

                    default: {
                        break;
                    }
                }

                result = "Success";
            }
            catch (IOException ioe) {
                Log.e("ThermicApp", ioe.getMessage(), ioe);
                result = "Fail";
            }
            finally {
                resetSnapsotRequest();
            }

            return result;
        }

        private void saveBitmap(Bitmap bitmap, File bitmapFile) throws IOException {
            FileOutputStream out = null;

            try {
                out = new FileOutputStream(bitmapFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
            }
        }

        private void saveRawData(File fileName, int[] rawData) throws IOException {
            FileOutputStream out = null;
            ByteBuffer byteBuff = ByteBuffer.allocate(rawData.length * 4);
            IntBuffer intBuff = byteBuff.asIntBuffer();
            intBuff.put(rawData);

            try {
                out = new FileOutputStream(fileName);
                out.write(byteBuff.array());
            }
            finally {
                try {
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                }
                catch (IOException e) {}
            }
        }
    }
}
