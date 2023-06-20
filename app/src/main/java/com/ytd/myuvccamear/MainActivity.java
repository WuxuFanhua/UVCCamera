package com.ytd.myuvccamear;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.utils.PermissionCheck;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.UVCCameraTextureView;
import com.ytd.myuvccamear.databinding.ActivityMainBinding;

public final class MainActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent {
    private static final boolean DEBUG = true;    // FIXME set false when production
    private static final String TAG = "MainActivity";

    private static final float[] BANDWIDTH_FACTORS = {0.5f, 0.5f};

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;

    private UVCCameraHandler mHandlerR;
    private CameraViewInterface mUVCCameraViewR;
    private ImageButton mCaptureButtonR;
    private Surface mRightPreviewSurface;

    private UVCCameraHandler mHandlerL;
    private CameraViewInterface mUVCCameraViewL;
    private ImageButton mCaptureButtonL;
    private Surface mLeftPreviewSurface;

    private ActivityMainBinding binding;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    private static final String[] NEEDED_PERMISSIONS =
        new String[] {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
//        binding.RelativeLayout1.setOnClickListener(mOnClickListener);
        initView();
    }

    /**
     * 权限检查
     *
     * @param neededPermissions 需要的权限
     * @return 是否全部被允许
     */
    protected boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &=
                ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isAllGranted = true;
        for (int grantResult : grantResults) {
            isAllGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
        }
        afterRequestPermission(requestCode, isAllGranted);
    }

    /**
     * 请求权限的回调
     *
     * @param requestCode 请求码
     * @param isAllGranted 是否全部被同意
     */
    private void afterRequestPermission(int requestCode, boolean isAllGranted) {
        Log.d("dd", "afterRequestPermission");
        initView();
    }

    private void initView() {
        mUVCCameraViewL = binding.cameraViewL;
        mUVCCameraViewL.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        ((UVCCameraTextureView) mUVCCameraViewL).setOnClickListener((v) -> {
            if (mHandlerL != null) {
                if (!mHandlerL.isOpened()) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    mHandlerL.close();
                    setCameraButton();
                }
            }
        });
        mCaptureButtonL = binding.captureButtonL;
        mCaptureButtonL.setOnClickListener((v) -> {
            if (mHandlerL != null) {
                if (mHandlerL.isOpened()) {
                    if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                        if (!mHandlerL.isRecording()) {
                            mCaptureButtonL.setColorFilter(0xffff0000);    // turn red
                            mHandlerL.startRecording();
                        } else {
                            mCaptureButtonL.setColorFilter(0);    // return to default color
                            mHandlerL.stopRecording();
                        }
                    }
                }
            }
        });
        mCaptureButtonL.setVisibility(View.INVISIBLE);
        mHandlerL = UVCCameraHandler.createHandler(this, mUVCCameraViewL, UVCCamera.DEFAULT_PREVIEW_WIDTH,
            UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[0]);

        mUVCCameraViewR = binding.cameraViewR;
        mUVCCameraViewR.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        ((UVCCameraTextureView) mUVCCameraViewR).setOnClickListener((v) -> {
            if (mHandlerR != null) {
                if (!mHandlerR.isOpened()) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    mHandlerR.close();
                    setCameraButton();
                }
            }
        });
        mCaptureButtonR = binding.captureButtonR;
        mCaptureButtonR.setOnClickListener((v) -> {
            if (mHandlerR != null) {
                if (mHandlerR.isOpened()) {
                    if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                        if (!mHandlerR.isRecording()) {
                            mCaptureButtonR.setColorFilter(0xffff0000);    // turn red
                            mHandlerR.startRecording();
                        } else {
                            mCaptureButtonR.setColorFilter(0);    // return to default color
                            mHandlerR.stopRecording();
                        }
                    }
                }
            }
        });
        mCaptureButtonR.setVisibility(View.INVISIBLE);
        mHandlerR = UVCCameraHandler.createHandler(this, mUVCCameraViewR, UVCCamera.DEFAULT_PREVIEW_WIDTH,
            UVCCamera.DEFAULT_PREVIEW_HEIGHT, BANDWIDTH_FACTORS[1]);

    }

    /**
     * 外部ストレージへの書き込みパーミッションが有るかどうかをチェック
     * なければ説明ダイアログを表示する
     *
     * @return true 外部ストレージへの書き込みパーミッションが有る
     */
    protected boolean checkPermissionWriteExternalStorage() {
        if (!PermissionCheck.hasWriteExternalStorage(this)) {
//            MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE,
//                R.string.permission_title, R.string.permission_ext_storage_request,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
            return false;
        }
        return true;
    }

    /**
     * 録音のパーミッションが有るかどうかをチェック
     * なければ説明ダイアログを表示する
     *
     * @return true 録音のパーミッションが有る
     */
    protected boolean checkPermissionAudio() {
        if (!PermissionCheck.hasAudio(this)) {
//            MessageDialogFragmentV4.showDialog(this, REQUEST_PERMISSION_AUDIO_RECORDING,
//                R.string.permission_title, R.string.permission_audio_recording_request,
//                new String[]{Manifest.permission.RECORD_AUDIO});
            return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        if (mUVCCameraViewR != null) {
            mUVCCameraViewR.onResume();
        }
        if (mUVCCameraViewL != null) {
            mUVCCameraViewL.onResume();
        }
    }

    @Override
    protected void onStop() {
        mHandlerR.close();
        if (mUVCCameraViewR != null) {
            mUVCCameraViewR.onPause();
        }
        mHandlerL.close();
        mCaptureButtonR.setVisibility(View.INVISIBLE);
        if (mUVCCameraViewL != null) {
            mUVCCameraViewL.onPause();
        }
        mCaptureButtonL.setVisibility(View.INVISIBLE);
        mUSBMonitor.unregister();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mHandlerR != null) {
            mHandlerR = null;
        }
        if (mHandlerL != null) {
            mHandlerL = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraViewR = null;
        mCaptureButtonR = null;
        mUVCCameraViewL = null;
        mCaptureButtonL = null;
        super.onDestroy();
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener =
        new USBMonitor.OnDeviceConnectListener() {
            @Override
            public void onAttach(final UsbDevice device) {
                if (DEBUG) {
                    Log.v(TAG, "onAttach:" + device);
                }
                Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock,
                                  final boolean createNew) {
                if (DEBUG) {
                    Log.v(TAG, "onConnect:" + device);
                }
                if (!mHandlerL.isOpened()) {
                    mHandlerL.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewL.getSurfaceTexture();
                    mHandlerL.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonL.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (!mHandlerR.isOpened()) {
                    mHandlerR.open(ctrlBlock);
                    final SurfaceTexture st = mUVCCameraViewR.getSurfaceTexture();
                    mHandlerR.startPreview(new Surface(st));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mCaptureButtonR.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

            @Override
            public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
                if (DEBUG) {
                    Log.v(TAG, "onDisconnect:" + device);
                }
                if ((mHandlerL != null) && !mHandlerL.isEqual(device)) {
                    mCaptureButtonL.post(new Runnable() {
                        @Override
                        public void run() {
                            mHandlerL.close();
                            if (mLeftPreviewSurface != null) {
                                mLeftPreviewSurface.release();
                                mLeftPreviewSurface = null;
                            }
                            setCameraButton();
                        }
                    });
                } else if ((mHandlerR != null) && !mHandlerR.isEqual(device)) {
                    mCaptureButtonL.post(new Runnable() {
                        @Override
                        public void run() {
                            mHandlerR.close();
                            if (mRightPreviewSurface != null) {
                                mRightPreviewSurface.release();
                                mRightPreviewSurface = null;
                            }
                            setCameraButton();
                        }
                    });
                }
            }

            @Override
            public void onDettach(final UsbDevice device) {
                if (DEBUG) {
                    Log.v(TAG, "onDettach:" + device);
                }
                Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel(final UsbDevice device) {
                if (DEBUG) {
                    Log.v(TAG, "onCancel:");
                }
            }
        };

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setCameraButton();
                }
            });
        }
    }

    private void setCameraButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ((mHandlerL != null) && !mHandlerL.isOpened() && (mCaptureButtonL != null)) {
                    mCaptureButtonL.setVisibility(View.INVISIBLE);
                }
                if ((mHandlerR != null) && !mHandlerR.isOpened() && (mCaptureButtonR != null)) {
                    mCaptureButtonR.setVisibility(View.INVISIBLE);
                }
            }
        });
    }
}