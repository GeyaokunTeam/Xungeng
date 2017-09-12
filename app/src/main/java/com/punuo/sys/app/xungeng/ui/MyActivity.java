package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.Toast;

import com.punuo.sys.app.xungeng.util.LogUtil;

/**
 * Created by acer on 2016/11/21.
 */

public class MyActivity extends AppCompatActivity {
    private boolean isLongPressKey;
    private boolean lockLongPressKey;
    private boolean isopen;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                isLongPressKey = true;
                if (!isopen){
                    isopen=true;
                }else{
                    isopen=false;
                }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LogUtil.d("MyActivity", "---->>onKeyDown():keyCode=" + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
                if (isLongPressKey) {
                    isLongPressKey = false;
                    return false;
                } else {
                    if (!isopen) {
                        startActivity(new Intent(MyActivity.this, MyCamera.class));
                    }else{
                        Toast.makeText(this, "无法打开照相机,请关闭手电筒再重试!", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
        }
        return super.onKeyUp(keyCode, event);
    }
}
