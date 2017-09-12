package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.camera.FileOperateUtil;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MakeSmallVideo extends MyActivity implements SurfaceHolder.Callback {

    @Bind(R.id.surface_video)
    SurfaceView surfaceVideo;
    @Bind(R.id.time)
    Chronometer time;
    @Bind(R.id.record)
    ImageButton record;
    @Bind(R.id.returnback)
    Button returnback;


    private final String TAG = "MakeSmallVideo";
    private Handler handler = new Handler();
    /**
     * 录像状态标志位
     */
    private boolean isRecording;
    /**
     * 小视频路径
     */
    private String smallMoviePath;
    /**
     * 摄像头
     */
    private Camera mCamera;
    /**
     * 录制工具
     */
    private MediaRecorder mRecorder;
    /**
     * 输出文件
     */
    private File recordOutput;
    /**
     * 控制小视频长度不超过8秒
     */
    private int len=8;
    /**
     * 计时器
     */
    private Timer timer = new Timer();
    private int isSend=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        SipInfo.instance=this;
        setContentView(R.layout.activity_make_small_video);
        ButterKnife.bind(this);
        //打开摄像头
        try {
            if (mCamera == null) {
                mCamera = Camera.open();
            }
        } catch (RuntimeException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        //添加回调函数
        surfaceVideo.getHolder().addCallback(this);
        surfaceVideo.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //设置分辨率
        surfaceVideo.getHolder().setFixedSize(1280, 720);

        mRecorder = new MediaRecorder();

    }


    @OnClick({R.id.returnback, R.id.record})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.returnback:
                finish();
                break;
            case R.id.record:
                if (!isRecording) {
                    record.setBackgroundResource(R.drawable.btn_shutter_recording);
                    returnback.setVisibility(View.GONE);
                    startRecording();
                    time.setBase(SystemClock.elapsedRealtime());
                    time.start();
                } else {
                    record.setBackgroundResource(R.drawable.btn_shutter_record);
                    stopRecording();
                    time.stop();
                    time.setBase(SystemClock.elapsedRealtime());
                    returnback.setVisibility(View.VISIBLE);
                }
                break;
        }
    }
    TimerTask task=new TimerTask() {
        @Override
        public void run() {
            len--;
            if (len==-1){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                       record.callOnClick();
                    }
                });
            }
        }
    };
    private void startRecording() {
        if (!isRecording) {
            try {
                initializeRecorder();
                isRecording = true;
                mRecorder.start();
                //重置计时
                len=8;
                timer.schedule(task,1000,1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {

        if (isRecording) {
            timer.cancel();
            mRecorder.stop();
            mRecorder.reset();
            mCamera.lock();
            isRecording = false;
            try {
                saveThumbnail();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent videoShow=new Intent(MakeSmallVideo.this,VideoShow.class);
            videoShow.putExtra("smallVideoPath",smallMoviePath);
            startActivityForResult(videoShow,isSend);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==isSend){
            if (resultCode==RESULT_OK){
                String smallVideoPath=data.getStringExtra("smallVideoPath");
                Intent intent = new Intent();
                intent.putExtra("smallvideopath", smallVideoPath);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Bitmap saveThumbnail() throws FileNotFoundException, IOException {
        if (smallMoviePath != null) {
            //创建缩略图,该方法只能获取384X512的缩略图，舍弃，使用源码中的获取缩略图方法
            //			Bitmap bitmap=ThumbnailUtils.createVideoThumbnail(mRecordPath, Thumbnails.MINI_KIND);
            Bitmap bitmap = getVideoThumbnail(smallMoviePath);

            if (bitmap != null) {
                String mThumbnailFolder = FileOperateUtil.getFolderPath(this, FileOperateUtil.TYPE_THUMBNAIL, "Camera");
                File folder = new File(mThumbnailFolder);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                File file = new File(smallMoviePath);
                file = new File(folder + File.separator + file.getName().replace(".mp4", ".jpg"));
                //存图片小图
                BufferedOutputStream bufferos = new BufferedOutputStream(new FileOutputStream(file));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bufferos);
                bufferos.flush();
                bufferos.close();
                return bitmap;
            }
        }
        return null;
    }

    /**
     * 获取帧缩略图，根据容器的高宽进行缩放
     *
     * @param filePath
     * @return
     */
    public Bitmap getVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime(-1);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (bitmap == null)
            return null;
        // Scale down the bitmap if it's too large.
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int pWidth = surfaceVideo.getWidth();// 容器宽度
        int pHeight = surfaceVideo.getHeight();//容器高度
        //获取宽高跟容器宽高相比较小的倍数，以此为标准进行缩放
        float scale = Math.min((float) width / pWidth, (float) height / pHeight);
        int w = Math.round(scale * pWidth);
        int h = Math.round(scale * pHeight);
        bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);
        return bitmap;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.stopPreview();
            mCamera.setPreviewDisplay(holder);
            initCamera();
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    initCamera();
                    mCamera.cancelAutoFocus();
                }
            }
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        closeCamera();
    }

    private void initCamera() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        Camera.Size s = parameters.getPreferredPreviewSizeForVideo();
        parameters.setPreviewSize(1280, 720);  // 部分定制手机，无法正常识别该方法// 。
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        setDispaly(parameters, mCamera);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.cancelAutoFocus();// 如果要实现连续的自动对焦，这一句必须加上
    }

    //控制图像的正确显示方向
    private void setDispaly(Camera.Parameters parameters, Camera camera) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            setDisplayOrientation(camera, 90);
        } else {
            parameters.setRotation(90);
        }
    }

    //实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", int.class);
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, i);
            }
        } catch (Exception e) {
            Log.e(TAG, "图像出错");
        }
    }

    private void initializeRecorder() throws IllegalStateException, IOException {
        mCamera.unlock();
        mRecorder.setCamera(mCamera);
        mRecorder.setOrientationHint(90);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        String path = FileOperateUtil.getFolderPath(this, FileOperateUtil.TYPE_VIDEO, "Camera");
        File directory = new File(path);
        if (!directory.exists())
            directory.mkdirs();
        smallMoviePath = path + File.separator + "video" + FileOperateUtil.createFileNmae(".mp4");
        recordOutput = new File(smallMoviePath);
        if (recordOutput.exists()) {
            recordOutput.delete();
        }
        CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        mRecorder.setProfile(cpHigh);
        mRecorder.setOutputFile(recordOutput.getAbsolutePath());
        mRecorder.setPreviewDisplay(surfaceVideo.getHolder().getSurface());
        mRecorder.setMaxDuration(50000);
        mRecorder.prepare();
    }

    private void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
