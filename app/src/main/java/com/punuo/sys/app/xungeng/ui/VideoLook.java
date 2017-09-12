package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.os.Bundle;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.view.FullScreenVideoView;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/11/15.
 */

public class VideoLook extends MyActivity {
    @Bind(R.id.play)
    FullScreenVideoView play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videolook);
        ButterKnife.bind(this);
        Intent i=getIntent();
        String path=i.getStringExtra("Path");
        play.setVideoPath(path);
        play.start();
    }
}
