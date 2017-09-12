package com.punuo.sys.app.xungeng.video;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.groupvoice.G711;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.ui.ActivityCollector;
import com.punuo.sys.app.xungeng.ui.MyActivity;

import org.opencore.avch264.NativeH264Encoder;

import java.io.IOException;
import java.lang.reflect.Method;

public class H264Sending extends MyActivity implements Callback, PreviewCallback {

    int frameSizeG711 = 160;
    public static boolean G711Running = true;
    public Camera mCamera;
    SurfaceView m_surface;
    SurfaceHolder m_surfaceHolder;
    public static RTPSending rtpsending = null;
    final String TAG = H264Sending.class.getSimpleName();    //取得类名
    boolean isCameraOpen = false;

    /**
     * 手机摄像头的个数
     */
    private int numCamera;
    /**
     * 前置摄像头的Id
     */
    private int cameraId_front;
    /**
     * 后置摄像头的Id
     */
    private int cameraId_back;
    /**
     * 判断前置摄像头是否存在的标志位
     */
    private boolean frontExist = false;
    /**
     * 打包发送的数组大小定义
     */
    byte[] rtppkt = new byte[VideoInfo.divide_length + 2];

    /**
     * Called when the activity is first created.
     */
    @SuppressLint({"NewApi", "NewApi", "NewApi", "NewApi", "NewApi"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        setContentView(R.layout.h264sending);
        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        if (rtpsending != null) {

            rtpsending = null;
        }


        if (rtpsending == null) {
            rtpsending = new RTPSending();
        } //RTP会话的一些参数设置

        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);  //获取一个windowmanager对象

        //自动获取屏幕分辨率
        Display display = manager.getDefaultDisplay();   //获取默认演示对象
        Point screenResolution = new Point(display.getWidth(), display.getHeight());  //取得屏幕分辨率

        Log.e("TAG", "Screen resolution: " + screenResolution);


        m_surface = (SurfaceView) this.findViewById(R.id.h264suf);   //surface建立，按顺序执行 surfaceCreated和surfaceChanged函数，进行编码前演示和解码后播放
        // 得到SurfaceHolder对象
        SurfaceHolder holder = m_surface.getHolder();  //返回m_surface的表示符（句柄）
        //设置回调函数
        holder.addCallback(H264Sending.this);   //添加回调接口
        //设置风格
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        long Ssrc = (VideoInfo.media_info_magic[15] & 0x000000ff) | ((VideoInfo.media_info_magic[14] << 8) & 0x0000ff00) | ((VideoInfo.media_info_magic[13] << 16) & 0x00ff0000) | ((VideoInfo.media_info_magic[12] << 24) & 0xff000000);
        rtpsending.rtpSession2.setSsrc(Ssrc);
        G711Running = true;
        G711_recored();
        VideoInfo.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                G711Running = true;
                G711_recored();
            }
        };
        Log.d("111", "onCreate: ");
        NativeH264Encoder.InitEncoder(PreviewWidth, PreviewHeight, PreviewFrameRate);
    }

    private final int PreviewFrameRate = 10;  //演示帧率
    private final int PreviewWidth = 320;     //水平像素
    private final int PreviewHeight = 240;     //垂直像素


    /**
     * 检测到surface有变化时调用此函数，对视频进行预览
     */
    @Override
    @SuppressLint("NewApi")
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.e("111", "surfaceChanged");
        //获取手机摄像头的个数
        numCamera = Camera.getNumberOfCameras();
        CameraInfo info = new CameraInfo();
        for (int i = 0; i < numCamera; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId_back = i;     //获取后置摄像头的Id
            }
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId_front = i;    //获取前置摄像头的Id
                frontExist = true;
            }
        }
//		if(frontExist){
//		    mCamera = Camera.open(cameraId_front);
//			DisplayToast("打开前置摄像头");
//    }
        if (mCamera != null)  //没有背面摄像头的情况
        {
            mCamera.setPreviewCallback(null);//must do this，停止接收回叫信号
            mCamera.stopPreview();   //停止捕获和绘图
            mCamera.release();   //断开与摄像头的连接，并释放摄像头资源
            mCamera = null;
            isCameraOpen=false;
        }
        if (!isCameraOpen) {
            try {
                mCamera = Camera.open(cameraId_back);
                isCameraOpen=true;
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        DisplayToast("打开后置摄像头");
        //}
        try {
            mCamera.setPreviewDisplay(m_surfaceHolder);//设置surface进行实时演示，m_surfaceHolder表示在surface上演示的位置，null表示清楚
            //设置回叫函数，每个演示帧的时候都被呼叫
            //参数this是回叫信号对象接收每个演示帧的拷贝，null则停止接收回叫信号。
            mCamera.setPreviewCallback(this);
            mCamera.setDisplayOrientation(90);
            //set camera
            Camera.Parameters parame = mCamera.getParameters();    //获取配置参数对象

            parame.setPreviewFrameRate(PreviewFrameRate);    //设置Camera的演示帧率
            parame.setPreviewSize(PreviewWidth, PreviewHeight);    //设置屏幕分辨率
            //android2.3.3以后无需下步
            mCamera.setParameters(parame);
            //通过SurfaceView显示取景画面
            //开始对演示帧进行捕获和绘图到surface
            mCamera.startPreview();
            // 自动对焦
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        Camera.Parameters parameters = camera.getParameters();
                        parameters.setPictureFormat(PixelFormat.JPEG);
                        //parameters.setPictureSize(surfaceView.getWidth(), surfaceView.getHeight());  // 部分定制手机，无法正常识别该方法。
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
                        camera.setParameters(parameters);
                        camera.startPreview();
                        camera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
                    }
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }


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
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Log.e("Came_e", "图像出错");
        }
    }

    /**
     * 此函数在surface第一次创建时立即执行，可对surface的一些参数进行设置
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e("111", "surfaceCreated");
        m_surfaceHolder = holder;   //参数设置
    }

    /**
     * 在一个surface被销毁之前调用
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("", "surfaceDestroyed");

    }

    final byte[] head = {0x00, 0x00, 0x00, 0x01};
    int[] frame = new int[PreviewWidth * PreviewHeight];  //新建帧数组，大小等于分辨率长*宽
    static long time = System.currentTimeMillis();   //以毫秒形式返回当前系统时间

    /**
     * onPreviewFrame是一个回调函数，当预览画面演示第一个预览帧时调用，在事件线程open()被使用的时候呼叫，播放解码视频
     *
     * @param data:表示之前演示帧 camera：摄像头对象
     */
    /**
     * g711采集编码线程
     */
    private void G711_recored() {
        new Thread(G711_encode).start();
    }

    Runnable G711_encode = new Runnable() {
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            AudioRecord record = getAudioRecord();
            //int frame_size = 160;
            short[] audioData = new short[frameSizeG711];
            byte[] encodeData = new byte[frameSizeG711];
            int numRead = 0;

            while (G711Running) {
                numRead = record.read(audioData, 0, frameSizeG711);
                if (numRead <= 0) continue;
                calc2(audioData, 0, numRead);
                //进行pcmu编码
                G711.linear2ulaw(audioData, 0, encodeData, numRead);
                rtpsending.rtpSession2.payloadType(0x45);
                rtpsending.rtpSession2.sendData(encodeData);
            }
            record.stop();
            record.release();
            Log.v("zlj", "G711_encode stopped!");
        }
    };

    void calc2(short[] lin, int off, int len) {
        int i, j;

        for (i = 0; i < len; i++) {
            j = lin[i + off];
            lin[i + off] = (short) (j >> 1);
        }
    }

    /**
     * 取得音频采集对象引用
     */
    private AudioRecord getAudioRecord() {
        int samp_rate = 8000;
        int min = AudioRecord.getMinBufferSize(samp_rate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.e(TAG, "min buffer size:" + min);

        AudioRecord record = null;
        record = new AudioRecord(
                MediaRecorder.AudioSource.MIC,//the recording source
                samp_rate, //采样频率，一般为8000hz/s
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                min);
        record.startRecording();
        return record;
    }
    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {    //演示帧演示后调用
        byte[] encodeResult = NativeH264Encoder.EncodeFrame(data, time);  //进行编码，将编码结果存放进数组
        time += 1000 / PreviewFrameRate;    //计算出一帧所耗费的时间，单位为毫秒
        int encodeState = NativeH264Encoder.getLastEncodeStatus();//获取最后的编码状态，0——表示成功！！
        Log.e(TAG, "onPreviewFrame: " + SipInfo.isConnected);
        if (SipInfo.isConnected) {
            if (VideoInfo.endView) {    //收到BYE命令，关闭当前视频采集功能，重新回到注册之后的等待邀请界面
                VideoInfo.nalfirst = 0; //0表示未收到首包，1表示收到
                VideoInfo.index = 0;
                VideoInfo.query_response = false;
                VideoInfo.endView = false;
                G711Running = false;
//				rtpsending.rtpSession1.endSession();
                H264Sending.this.finish();
            }
            if (encodeState == 0 && encodeResult.length > 0) {
                Log.e(TAG, "encode len:" + encodeResult.length);//打印编码结果的长度
                setSSRC_PAYLOAD();
                DivideAndSendNal(encodeResult);
            }
        } else {
            VideoInfo.nalfirst = 0; //0表示未收到首包，1表示收到
            VideoInfo.index = 0;
            VideoInfo.query_response = false;
            G711Running = false;
            H264Sending.this.finish();
        }
//			}
//		}.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoInfo.handler=null;
        if (mCamera != null)  //没有背面摄像头的情况
        {
            mCamera.setPreviewCallback(null);//must do this，停止接收回叫信号
            mCamera.stopPreview();   //停止捕获和绘图
            mCamera.release();   //断开与摄像头的连接，并释放摄像头资源
            mCamera = null;
            isCameraOpen=false;
        }
        NativeH264Encoder.DeinitEncoder();
        rtpsending = null;
        Log.d("111", "onDestroy: ");
        ActivityCollector.removeActivity(this);
    }

    /**
     * 设置ssrc与payload
     */
    public void setSSRC_PAYLOAD() {
        byte msg[] = new byte[20];
        long Ssrc = 0;
        msg[0] = 0x00;
        msg[1] = 0x01;
        msg[2] = 0x00;
        msg[3] = 0x10;
        try {
            System.arraycopy(VideoInfo.media_info_magic, 0, msg, 4, 16);  //生成RTP心跳保活包，即在Info.media_info_megic之前再加上0x00 0x01 0x00 0x10
        } catch (Exception e) {
            Log.d("ZR", "System.arraycopy failed!");
        }
        rtpsending.rtpSession1.payloadType(0x7a);    //设置RTP包的负载类型为0x7a

        //取Info.media_info_megic的后四组设为RTP的同步源码（Ssrc）
        Ssrc = (VideoInfo.media_info_magic[15] & 0x000000ff) | ((VideoInfo.media_info_magic[14] << 8) & 0x0000ff00) | ((VideoInfo.media_info_magic[13] << 16) & 0x00ff0000) | ((VideoInfo.media_info_magic[12] << 24) & 0xff000000);
        rtpsending.rtpSession1.setSsrc(Ssrc);
        for (int i = 0; i < 2; i++) {
            rtpsending.rtpSession1.sendData(msg);
        }
    }

    /**
     * 分片、发送方法
     */
    public void DivideAndSendNal(byte[] h264) {

        if (h264.length > 0) {  //有数据才进行分片发送操作
            if (h264.length > VideoInfo.divide_length) {
                VideoInfo.dividingFrame = true;
                VideoInfo.status = true;
                VideoInfo.firstPktReceived = false;
                VideoInfo.pktflag = 0;

                while (VideoInfo.status) {
                    if (!VideoInfo.firstPktReceived) {  //首包
                        sendFirstPacket(h264);
                    } else {
                        if (h264.length - VideoInfo.pktflag > VideoInfo.divide_length) {  //中包
                            sendMiddlePacket(h264);
                        } else {   //末包
                            sendLastPacket(h264);
                        }
                    } //end of 首包
                }//end of while
            } else {   //不分片包
                sendCompletePacket(h264);
            }
        }
    }

    /**
     * 发送首包
     */
    public void sendFirstPacket(byte[] h264) {
        Log.d("H264Sending", "发送首包");
        rtppkt[0] = (byte) (h264[0] & 0xe0);
        rtppkt[0] = (byte) (rtppkt[0] + 0x1c);
        rtppkt[1] = (byte) (0x80 + (h264[0] & 0x1f));
        try {
            System.arraycopy(h264, 0, rtppkt, 2, VideoInfo.divide_length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        VideoInfo.pktflag = VideoInfo.pktflag + VideoInfo.divide_length;
        VideoInfo.firstPktReceived = true;
        //设置RTP包的负载类型为0x62
        rtpsending.rtpSession1.payloadType(0x62);
        //发送打包数据
        rtpsending.rtpSession1.sendData(rtppkt);   //发送打包数据
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送中包
     */
    public void sendMiddlePacket(byte[] h264) {
        Log.d("H264Sending", "发送中包");
        rtppkt[0] = (byte) (h264[0] & 0xe0);
        rtppkt[0] = (byte) (rtppkt[0] + 0x1c);
        rtppkt[1] = (byte) (0x00 + (h264[0] & 0x1f));

        try {
            System.arraycopy(h264, VideoInfo.pktflag, rtppkt, 2, VideoInfo.divide_length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        VideoInfo.pktflag = VideoInfo.pktflag + VideoInfo.divide_length;
        //设置RTP包的负载类型为0x62
        rtpsending.rtpSession1.payloadType(0x62);
        //发送打包数据
        rtpsending.rtpSession1.sendData(rtppkt);   //发送打包数据   //发送打包数据
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送末包
     */
    public static void sendLastPacket(byte[] h264) {
        Log.d("H264Sending", "发送末包");
        byte[] rtppktLast = new byte[h264.length - VideoInfo.pktflag + 2];
        rtppktLast[0] = (byte) (h264[0] & 0xe0);
        rtppktLast[0] = (byte) (rtppktLast[0] + 0x1c);
        rtppktLast[1] = (byte) (0x40 + (h264[0] & 0x1f));
        try {
            System.arraycopy(h264, VideoInfo.pktflag, rtppktLast, 2, h264.length - VideoInfo.pktflag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //设置RTP包的负载类型为0x62
        rtpsending.rtpSession1.payloadType(0x62);
        //发送打包数据
        rtpsending.rtpSession1.sendData(rtppktLast);   //发送打包数据  //发送打包数据
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        VideoInfo.status = false;  //打包组包结束，下一步进行解码
        VideoInfo.dividingFrame = false;  //一帧分片打包完毕，时间戳改下一帧
    }

    /**
     * 发送完整包
     */
    public void sendCompletePacket(byte[] h264) {
        Log.d("H264Sending", "发送单包");
        //设置RTP包的负载类型为0x62
        rtpsending.rtpSession1.payloadType(0x62);
        //发送打包数据
        rtpsending.rtpSession1.sendData(h264);   //发送打包数据   //发送打包数据
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    protected static final int MENU_RTPSEND = Menu.FIRST;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_RTPSEND, 0, "RTP包发送");
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_RTPSEND:
                DisplayToast("RTP包发送" + VideoInfo.pktNumber);
                break;

        }
        return true;
    }

    public void DisplayToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {

    }
}
