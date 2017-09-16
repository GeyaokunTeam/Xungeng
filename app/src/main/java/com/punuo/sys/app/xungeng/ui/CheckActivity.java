package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.model.PointInfo;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.zxing.android.CaptureActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.zoolu.sip.message.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.punuo.sys.app.xungeng.sip.SipInfo.devId;
import static com.punuo.sys.app.xungeng.sip.SipInfo.pointInfoList;
import static com.punuo.sys.app.xungeng.sip.SipInfo.userId;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class CheckActivity extends Activity {

    ListView pointList;
    TextView title;
    TextView change;
    ImageButton back;
    Button xungeng;
    PointAdapter pointAdapter;
    private static final int REQUEST_CODE_SCAN = 0x0000;
    private static final String DECODED_CONTENT_KEY = "codedContent";
    private static final String DECODED_BITMAP_KEY = "codedBitmap";
    int currentId = -1;
    boolean isStart=false;
    Handler myHander=new Handler();

    CheckListener mCheckListener=new CheckListener() {
        @Override
        public void OnQueryTurn() {
            myHander.post(new Runnable() {
                @Override
                public void run() {
                    isStart = true;
                    title.setText("巡更列表(巡更中)");
                    xungeng.setText("结束巡更");
                    change.setEnabled(false);
                }
            });
        }

        @Override
        public void OnCheck(final int i) {
            myHander.post(new Runnable() {
                @Override
                public void run() {
                    if (i==0) {
                        Toast.makeText(CheckActivity.this, "打卡成功", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(CheckActivity.this, "打卡失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.check_layout);
        SipInfo.sipUser.setmCheckListener(mCheckListener);
        initView();
    }

    private void initView() {
        pointList = (ListView) findViewById(R.id.point_list);
        xungeng= (Button) findViewById(R.id.xungeng);
        title = (TextView) findViewById(R.id.title);
        change = (TextView) findViewById(R.id.change);
        back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
        xungeng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isStart){
                    Message queryTurn=SipMessageFactory.createNotifyRequest(SipInfo.sipUser,SipInfo.user_to,SipInfo.user_from,
                            BodyFactory.createQueryGPSLatestGpdInfo(SipInfo.devId));
                    SipInfo.sipUser.sendMessage(queryTurn);
                }else {
                    isStart = false;
                    xungeng.setText("开始巡更");
                    title.setText("巡更列表");
                    change.setEnabled(true);
                }
            }
        });
        change.setVisibility(View.VISIBLE);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Spinner spinner = new Spinner(CheckActivity.this);
                String[] arr = {"德清", "越州", "舟山", "衢州"};
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(CheckActivity.this, android.R.layout.simple_list_item_1, arr);
                spinner.setAdapter(arrayAdapter);
                AlertDialog dialog = new AlertDialog.Builder(CheckActivity.this)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                        /*请求巡更点信息*/
                                finish();
                                SipInfo.addr_code = spinner.getSelectedItemPosition() + 1;
                                SipInfo.pointInfoListbd09.clear();
                                SipInfo.pointInfoList.clear();
                                SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipUser, SipInfo.user_to,
                                        SipInfo.user_from, BodyFactory.createPointsQuery(SipInfo.addr_code)));
                            }
                        })
                        .setNegativeButton("取消", null)
                        .create();
                dialog.setTitle("请选择库区");
                dialog.setView(spinner);
                dialog.show();
            }
        });
        title.setText("巡更列表");
        pointAdapter = new PointAdapter(CheckActivity.this, mOnClickListener);
        pointList.setAdapter(pointAdapter);
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isStart) {
                PointInfo pointInfo = (PointInfo) v.getTag();
                if (pointInfo.isCheck()) return;
                currentId = pointInfo.getId();
                Intent intent = new Intent(CheckActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }else {
                myHander.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CheckActivity.this,"请先开始巡更",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(DECODED_CONTENT_KEY);
                content="{"+content+"}";
                try {
                    JSONObject jo = new JSONObject(content);
                    int areaCode = jo.optInt("area_code");
                    int id = jo.optInt("id");
                    double lat = jo.optDouble("lat");
                    double lon = jo.optDouble("lon");
                    if (currentId == id) {
                        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                        Date curDate=  new Date(System.currentTimeMillis());
                        String date=formatter.format(curDate);
                        SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser,SipInfo.user_to,SipInfo.user_from,
                                BodyFactory.creatGPSInfoBody(userId, areaCode, lon, lat, System.currentTimeMillis() / 1000, id, 0,devId,Integer.parseInt(SipInfo.turn)+1,date)));
                        PointInfo pointInfo = new PointInfo();
                        pointInfo.setId(id);
                        int index = SipInfo.pointInfoList.indexOf(pointInfo);
                        if (index != -1) {
                            pointInfoList.get(index).setCheck(true);
                        }
                        pointAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "请检查当前您所要打卡的巡更点是否匹配当前点", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        showDialog();
    }

    public void showDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("注意")
                .setMessage("确定要结束当前轮次的巡更吗？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        isStart=false;
                        CheckActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }

    public class PointAdapter extends BaseAdapter {
        private Context mContext;
        View.OnClickListener onClickListener;

        public PointAdapter(Context context, View.OnClickListener onClickListener) {
            mContext = context;
            this.onClickListener = onClickListener;
        }

        @Override
        public int getCount() {
            return pointInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            return pointInfoList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.layout_point_item, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            PointInfo pointInfo = pointInfoList.get(position);
            if (pointInfo != null) {
                viewHolder.text2.setText(pointInfo.getName());
                viewHolder.text1.setText("第" + pointInfo.getId() + "号点");
                viewHolder.daka.setTag(pointInfo);
                viewHolder.daka.setOnClickListener(onClickListener);
                if (pointInfo.isCheck()) {
                    viewHolder.daka.setText("已打卡");
                } else {
                    viewHolder.daka.setText("打卡");
                }
            }
            return convertView;
        }

        class ViewHolder {
            TextView text1;
            TextView text2;
            Button daka;

            ViewHolder(View view) {
                text1 = (TextView) view.findViewById(R.id.text1);
                text2 = (TextView) view.findViewById(R.id.text2);
                daka = (Button) view.findViewById(R.id.daka);
            }
        }
    }

    public interface CheckListener{
        public void OnQueryTurn();

        public void OnCheck(int i);
    }
}
