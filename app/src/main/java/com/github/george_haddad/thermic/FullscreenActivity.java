package com.github.george_haddad.thermic;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.flir.flironesdk.Device;
import com.flir.flironesdk.Frame;
import com.flir.flironesdk.FrameProcessor;
import com.flir.flironesdk.RenderedImage;

import java.nio.ByteBuffer;
import java.util.EnumSet;

/**
 * @author George Haddad
 *
 */
public class FullscreenActivity extends RuntimePermissionsActivity implements Device.Delegate, FrameProcessor.Delegate, Device.StreamDelegate {

    private static final int REQUEST_PERMISSIONS = 20;
    private FrameProcessor frameProcessor;
    Device flirDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!hasCameraPermission()) {
            super.requestAppPermissions(R.string.runtime_permissions_txt, REQUEST_PERMISSIONS, Manifest.permission.CAMERA);
        }

        frameProcessor = new FrameProcessor(this, this, EnumSet.of(RenderedImage.ImageType.BlendedMSXRGBA8888Image));
        setContentView(R.layout.activity_fullscreen);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            Device.startDiscovery(this, this);
        }
        catch(IllegalStateException e){
            // it's okay if we've already started discovery
        }
        catch (SecurityException e){
            Toast.makeText(this, "Please insert FLIR One and select "+getString(R.string.app_name), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onStop() {
        if(hasCameraPermission()) {
            Device.stopDiscovery();
        }

        flirDevice = null;
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(hasCameraPermission()) {
            if (flirDevice != null){
                flirDevice.startFrameStream(this);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(hasCameraPermission()) {
            if (flirDevice != null){
                flirDevice.startFrameStream(this);
            }
        }
    }

    @Override
    public void onTuningStateChanged(Device.TuningState tuningState) {
        Log.i("HeatApp", "Tuning state changed");
    }

    @Override
    public void onAutomaticTuningChanged(boolean b) {
        Log.i("HeatApp", "Automatic Tuning state changed");
    }

    @Override
    public void onDeviceConnected(Device device) {
        flirDevice = device;
        if(hasCameraPermission()) {
            flirDevice.startFrameStream(this);
        }
    }

    @Override
    public void onDeviceDisconnected(Device device) {
        final ImageView imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageBitmap(Bitmap.createBitmap(1,1, Bitmap.Config.ALPHA_8));

        if(hasCameraPermission()) {
            flirDevice.stopFrameStream();
        }

        flirDevice = null;
    }

    @Override
    public void onFrameProcessed(RenderedImage renderedImage) {
        final Bitmap imageBitmap = Bitmap.createBitmap(renderedImage.width(), renderedImage.height(), Bitmap.Config.ARGB_8888);
        imageBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(renderedImage.pixelData()));
        final ImageView imageView = (ImageView)findViewById(R.id.imageView);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(imageBitmap);
            }
        });
    }

    @Override
    public void onFrameReceived(Frame frame) {
        frameProcessor.processFrame(frame);
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        Toast.makeText(this, "Permissions Received.", Toast.LENGTH_LONG).show();
        if(hasCameraPermission()) {
            Device.startDiscovery(this, this);
            if(flirDevice != null) {
                flirDevice.startFrameStream(this);
            }
        }
    }

    private boolean hasCameraPermission() {
        int perm = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        return PackageManager.PERMISSION_GRANTED == perm;
    }
}
