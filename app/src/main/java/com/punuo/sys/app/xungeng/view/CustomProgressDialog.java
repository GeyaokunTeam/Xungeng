package com.punuo.sys.app.xungeng.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import com.punuo.sys.app.R;


/**
 * Created by acer on 2016/4/26.
 */
public class CustomProgressDialog extends Dialog {

    public CustomProgressDialog(Context context, int themeResId) {
        super(context, themeResId);
    }
    public CustomProgressDialog(Context context) {
        this(context,  R.style.CustomProgressDialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customprogressdialog);
    }
}
