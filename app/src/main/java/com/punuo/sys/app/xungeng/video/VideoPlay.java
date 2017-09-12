package com.punuo.sys.app.xungeng.video;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.FFmpeg.ffmpeg;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.ui.ActivityCollector;
import com.punuo.sys.app.xungeng.ui.MyActivity;
import com.punuo.sys.app.xungeng.util.LogUtil;

import org.zoolu.sip.message.Message;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.Bind;
import butterknife.ButterKnife;


public class VideoPlay extends MyActivity {
    public static final String TAG = "VideoPlay";
    private final byte[] SPS_DM365_CIF = {0x00, 0x00, 0x00, 0x01, 0x27, 0x64, 0x00, 0x1f, (byte) 0xad, (byte) 0x84, 0x09, 0x26, 0x6e, 0x23, 0x34, (byte) 0x90, (byte) 0x81, 0x24, (byte) 0xcd, (byte) 0xc4, 0x66, (byte) 0x92, 0x10, 0x24, (byte) 0x99, (byte) 0xb8, (byte) 0x8c, (byte) 0xd2, 0x42, 0x04, (byte) 0x93, 0x37, 0x11, (byte) 0x9a, 0x48, 0x40, (byte) 0x92, 0x66, (byte) 0xe2, 0x33, 0x49, 0x08, 0x12, 0x4c, (byte) 0xdc, 0x46, 0x69, 0x21, 0x05, 0x5a, (byte) 0xeb, (byte) 0xd7, (byte) 0xd7, (byte) 0xe4, (byte) 0xfe, (byte) 0xbf, 0x27, (byte) 0xd7, (byte) 0xae, (byte) 0xb5, 0x50, (byte) 0x82, (byte) 0xad, 0x75, (byte) 0xeb, (byte) 0xeb, (byte) 0xf2, 0x7f, 0x5f, (byte) 0x93, (byte) 0xeb, (byte) 0xd7, 0x5a, (byte) 0xab, 0x40, (byte) 0xb0, 0x4b, 0x20};
    private final byte[] PPS_DM365_CIF = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xee, 0x3c, (byte) 0xb0};
    private final byte[] SPS_MOBILE_QCIF = {0x00, 0x00, 0x00, 0x01, 0x27, 0x42, 0x10, 0x09, (byte) 0x96, 0x35, 0x05, (byte) 0x89, (byte) 0xc8};
    private final byte[] PPS_MOBILE_QCIF = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xce, 0x02, (byte) 0xfc, (byte) 0x80};
//    private final byte[] SPS_MOBILE_CIF = {0x00, 0x00, 0x00, 0x01, 0x67, 0x42, 0x00, 0x29, (byte) 0x8d, (byte) 0x8d, 0x40, (byte) 0xb0, 0x4b, 0x40, 0x3c, 0x22, 0x11, 0x4e};
//    private final byte[] PPS_MOBILE_CIF = {0x00, 0x00, 0x00, 0x01, 0x68, (byte) 0xca, 0x43, (byte) 0xc8};
    private final byte[] SPS_MOBILE_CIF = {0x00, 0x00, 0x00, 0x01, 0x27, 0x42, 0x10, 0x09, (byte) 0x96, 0x35, 0x02, (byte) 0x83, (byte) 0xf2};
    private final byte[] PPS_MOBILE_CIF = {0x00, 0x00, 0x00, 0x01, 0x28, (byte) 0xce, 0x02, (byte) 0xfc, (byte) 0x80};
    @Bind(R.id.surfaceView)
    SurfaceView surfaceView;
    private ffmpeg mFFmpeg = new ffmpeg();
    private SurfaceHolder surfaceHolder;
    private byte[] mPixel = new byte[VideoInfo.width * VideoInfo.height * 2];
    private ByteBuffer buffer = ByteBuffer.wrap(mPixel);
    private Bitmap videoBit = Bitmap.createBitmap(VideoInfo.width, VideoInfo.height, Bitmap.Config.RGB_565);
    private int getNum = 0;
    Timer timer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        setContentView(R.layout.activity_video_play);
        ButterKnife.bind(this);
        surfaceHolder = surfaceView.getHolder();
        playVideo();
        timer.schedule(task, 0, 2000);
    }

    TimerTask task = new TimerTask() {

        @Override
        public void run() {
            if (VideoInfo.isrec == 0) {
                closeVideo();
            } else if (VideoInfo.isrec == 2) {
                VideoInfo.isrec = 0;
            }
        }
    };

    private void closeVideo() {
        Message bye = SipMessageFactory.createByeRequest(SipInfo.sipUser, SipInfo.toDev, SipInfo.user_from);
        SipInfo.sipUser.sendMessage(bye);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        timer.cancel();
        VideoInfo.isrec = 1;
        SipInfo.decoding = false;
        VideoInfo.rtpVideo.removeParticipant();
        VideoInfo.sendActivePacket.stopThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    mFFmpeg.Destroy();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        VideoInfo.rtpVideo.endSession();
        VideoInfo.track.stop();
    }

    private void playVideo() {
        new Thread(Video).start();
    }

    Runnable Video = new Runnable() {
        @Override
        public void run() {
            mFFmpeg.init(VideoInfo.width, VideoInfo.height);
            switch (VideoInfo.videoType) {
                case 2:
                    mFFmpeg.DecoderNal(SPS_DM365_CIF, 78, mPixel);
                    mFFmpeg.DecoderNal(PPS_DM365_CIF, 8, mPixel);
                    break;
                case 3:
                    mFFmpeg.DecoderNal(SPS_MOBILE_QCIF, 13, mPixel);
                    mFFmpeg.DecoderNal(PPS_MOBILE_QCIF, 9, mPixel);
                    break;
                case 4:
                    mFFmpeg.DecoderNal(SPS_MOBILE_CIF, 13, mPixel);
                    mFFmpeg.DecoderNal(PPS_MOBILE_CIF, 9, mPixel);
                    break;
                default:
                    break;
            }
            while (SipInfo.decoding) {
                if (SipInfo.isConnected) {
                    byte[] nal = VideoInfo.nalBuffers[getNum].getReadableNalBuf();
                    if (nal != null) {
                        LogUtil.i(TAG, "nalLen:" + nal.length);
                        try {
                            int iTemp = mFFmpeg.DecoderNal(nal, nal.length, mPixel);
                            if (iTemp > 0) {
                                doSurfaceDraw();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    VideoInfo.nalBuffers[getNum].readLock();
                    VideoInfo.nalBuffers[getNum].cleanNalBuf();
                    getNum++;
                    if (getNum == 200) {
                        getNum = 0;
                    }
                }
            }
        }
    };

    private void doSurfaceDraw() {
        videoBit.copyPixelsFromBuffer(buffer);
        buffer.position(0);
        LogUtil.i(TAG, "doSurfaceDraw");
        int surfaceViewWidth = surfaceView.getWidth();
        int surfaceViewHeight = surfaceView.getHeight();
        int bmpWidth = videoBit.getWidth();
        int bmpHeight = videoBit.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth;
        float scaleHeight;
//        scaleWidth = (float) surfaceViewWidth / bmpWidth;
//        scaleHeight = (float) surfaceViewHeight / bmpHeight;
        matrix.postScale(3, 3);
//        matrix.setTranslate((float) bmpWidth/2,(float)bmpHeight/2);
//        matrix.preRotate(90,(float) bmpWidth/2,(float)bmpHeight/2);
        Bitmap resizeBmp = Bitmap.createBitmap(videoBit, 0, 0, bmpWidth, bmpHeight, matrix, true);
        resizeBmp = adjustPhotoRotation(resizeBmp, 90);
        Canvas canvas = surfaceHolder.lockCanvas();
        canvas.drawBitmap(resizeBmp, 0, 0, null);
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    Bitmap adjustPhotoRotation(Bitmap bm, final int orientationDegree) {
        Matrix m = new Matrix();
        m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
        float targetX, targetY;
        if (orientationDegree == 90) {
            targetX = bm.getHeight();
            targetY = 0;
        } else {
            targetX = bm.getHeight();
            targetY = bm.getWidth();
        }
        final float[] values = new float[9];
        m.getValues(values);
        float x1 = values[Matrix.MTRANS_X];
        float y1 = values[Matrix.MTRANS_Y];
        m.postTranslate(targetX - x1, targetY - y1);
        Bitmap bm1 = Bitmap.createBitmap(bm.getHeight(), bm.getWidth(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        Canvas canvas = new Canvas(bm1);
        canvas.drawBitmap(bm, m, paint);
        return bm1;
    }

    @Override
    public void onBackPressed() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("是否结束监控?")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeVideo();
                    }
                })
                .setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
    }


}
