package com.punuo.sys.app.xungeng.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.util.SHA1;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/9/28.
 */

public class ChangPWD extends MyActivity implements View.OnClickListener, SipUser.ChangePWDListener {


    @Bind(R.id.old_password)
    EditText oldPassword;
    @Bind(R.id.new_password)
    EditText newPassword;
    @Bind(R.id.confirm_new_password)
    EditText confirmNewPassword;
    @Bind(R.id.change)
    Button change;
    @Bind(R.id.back)
    ImageButton back;
    @Bind(R.id.title)
    TextView title;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changepassword);
        ButterKnife.bind(this);
        change.setOnClickListener(this);
        back.setOnClickListener(this);
        SipInfo.sipUser.setChangePWDListener(this);
        title.setText("修改密码");
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.change:
                String oldPwd = oldPassword.getText().toString();
                String newPwd = newPassword.getText().toString();
                String confrimNewPwd = confirmNewPassword.getText().toString();
                if (TextUtils.isEmpty(oldPwd)) {
                    Toast.makeText(this, "旧密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(newPwd)) {
                    Toast.makeText(this, "新密码不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(confrimNewPwd)) {
                    Toast.makeText(this, "请再次输入新密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!oldPwd.equals(SipInfo.password)) {
                    Toast.makeText(this, "旧密码输入错误", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPwd.equals(confrimNewPwd)) {
                    Toast.makeText(this, "两个密码不一样", Toast.LENGTH_SHORT).show();
                    return;
                }
                SHA1 sha1 = SHA1.getInstance();
                String oldpassword = sha1.hashData(SipInfo.salt + oldPwd);
                String newpassword = sha1.hashData(SipInfo.salt + newPwd);
                SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser,
                        SipInfo.user_to, SipInfo.user_from, BodyFactory.cretePasswordChange(oldpassword, newpassword)));
                break;
            default:
                break;
        }
    }

    /**
     * 强制关闭软键盘
     */
    public static void closeKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onChangePWD(int i) {
        if (i == 1) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
                }
            });

            finish();
        } else if (i == 0) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "密码修改失败,请重试!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
