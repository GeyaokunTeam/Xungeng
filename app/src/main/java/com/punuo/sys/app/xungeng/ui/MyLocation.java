package com.punuo.sys.app.xungeng.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.model.PointInfo;
import com.punuo.sys.app.xungeng.receiver.ProximityAlertReciever;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.pointInfoList;

/**
 * Created by acer on 2016/11/9.
 */
public class MyLocation extends MyActivity implements LocationSource, AMapLocationListener, View.OnClickListener {
    @Bind(R.id.mapview)
    MapView mapview;
    @Bind(R.id.xungeng)
    Button xungeng;
    @Bind(R.id.distance)
    TextView distanceText;
    @Bind(R.id.mapType)
    ToggleButton mapType;
    @Bind(R.id.text)
    TextView text;
    @Bind(R.id.change_kuqu)
    Button changeKuqu;
    private AMap aMap = null;
    public AMapLocationClient mapLocationClient;
    public AMapLocationClientOption mapLocationClientOption;
    private OnLocationChangedListener mListener;
    private double latit;
    private double longit;
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    String GEOFENCE_BROADCAST_ACTION = "com.punuo.sys.app.broadcast";
    ProximityAlertReciever proximityAlertReciever;
    private float[] distance;//距离
    private int isArrived[] = new int[pointInfoList.size()];//判断进出区域标志位
    private boolean isStart = false;//开始巡更标志位
    private boolean isFirstLoc = true;//第一次定位标志位
    private BitmapDescriptor bdA;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mylocation);
        ButterKnife.bind(this);
        mapview.onCreate(savedInstanceState);
        xungeng.setOnClickListener(this);
        mapType.setOnClickListener(this);
        changeKuqu.setOnClickListener(this);
        if (aMap == null) {
            aMap = mapview.getMap();
        }
        proximityAlertReciever = new ProximityAlertReciever();
        setUpMap();
        IntentFilter fliter = new IntentFilter(GEOFENCE_BROADCAST_ACTION);
        registerReceiver(proximityAlertReciever, fliter);
        new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < pointInfoList.size(); i++) {
                    LatLng llA = new LatLng(pointInfoList.get(i).getLat(), pointInfoList.get(i).getLang());
                    bdA = BitmapDescriptorFactory.fromResource(getResources().getIdentifier("icon_mark" + pointInfoList.get(i).getId(), "drawable", getPackageName()));
                    MarkerOptions ooA = new MarkerOptions().position(llA).icon(bdA)
                            .zIndex(7).draggable(true);
                    aMap.addMarker(ooA);
                }
            }
        }.start();
        SipInfo.points=new int[pointInfoList.size()];
    }

    private void setUpMap() {
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.icon_myloction));
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18f));
    }

    public void addCircle(LatLng latLng, int radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(radius);
        circleOptions.strokeWidth(1);
        circleOptions.strokeColor(Color.argb(0, 255, 0, 0));
        circleOptions.fillColor(Color.argb(50, 0, 0, 255));
        aMap.addCircle(circleOptions);
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mapLocationClient == null) {
            mapLocationClient = new AMapLocationClient(this);
            mapLocationClientOption = new AMapLocationClientOption();
            //设置定位监听
//            mapLocationClientOption.setOnceLocation(true);
            mapLocationClientOption.setInterval(1000);
            mapLocationClientOption.setNeedAddress(true);
            mapLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mapLocationClient.setLocationOption(mapLocationClientOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mapLocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mapLocationClient != null) {
            mapLocationClient.stopLocation();
            mapLocationClient.onDestroy();
        }
        mapLocationClient = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapview.onDestroy();
        mListener = null;
        if (mapLocationClient != null) {
            mapLocationClient.stopLocation();
            mapLocationClient.onDestroy();
        }
        mapLocationClient = null;
        unregisterReceiver(proximityAlertReciever);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapview.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (!isStart) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "正在巡更中,请结束巡更后退出", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(aMapLocation); // 显示系统小蓝点
                latit = aMapLocation.getLatitude();
                longit = aMapLocation.getLongitude();
                LatLng endlatlng = new LatLng(latit, longit);
                Intent intent = new Intent(GEOFENCE_BROADCAST_ACTION);
                if (isStart) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endlatlng, 18.0f));
                    distance = new float[pointInfoList.size()];
                    StringBuilder stringBuffer = new StringBuilder();
                    stringBuffer.append("距离:");
                    for (PointInfo pointInfo : pointInfoList) {
                        distance[pointInfo.getId() - 1] = AMapUtils.calculateLineDistance(new LatLng(pointInfo.getLat(), pointInfo.getLang()), endlatlng);
                        stringBuffer.append("\n" + pointInfo.getId() + ":" + (int) distance[pointInfo.getId() - 1] + "m");
                    }
                    distanceText.setText(stringBuffer.toString());
                    distanceText.setTextSize(16);
                    check(intent);
                }
                if (isFirstLoc) {
                    isFirstLoc = false;
                    switch (SipInfo.addr_code) {
                        case 1:
                            LatLng l1 = new LatLng(30.5418258946, 120.2093495598);
                            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(l1, 18.0f));
                            break;
                        case 2:
                            LatLng l2 = new LatLng(30.0034655895, 120.6289052442);
                            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(l2, 18.0f));
                            break;
                        case 3:
                            LatLng l3 = new LatLng(30.0437680353, 121.9993533262);
                            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(l3, 18.0f));
                            break;
                        case 4:
                            LatLng l4 = new LatLng(28.9862064997, 118.9877029118);
                            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(l4, 18.0f));
                            break;
                    }

                }
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.xungeng:
                if (!isStart) {
                    isStart = true;
                    text.setText("正在巡更......");
                    xungeng.setText("结束巡更");
                    changeKuqu.setEnabled(false);
                } else {
                    isStart = false;
                    xungeng.setText("开始巡更");
                    text.setText(null);
                    changeKuqu.setEnabled(true);
                }
                break;
            case R.id.mapType:
                if (mapType.isChecked()) {
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                } else {
                    aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                }
                break;
            case R.id.change_kuqu:
                final Spinner spinner = new Spinner(this);
                String[] arr = {"德清", "越州", "舟山", "衢州"};
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arr);
                spinner.setAdapter(arrayAdapter);
                dialog = new AlertDialog.Builder(this)
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
                break;
        }
    }

    private void check(Intent intent) {
        for (PointInfo pointInfo : pointInfoList) {
            int id = pointInfo.getId();
            if (distance[id - 1] < 80) {
                if (isArrived[id - 1] == 0) {
                    //发送GPS信息
                    intent.putExtra("id", id);
                    intent.putExtra("status", 1);
                    sendBroadcast(intent);
                    isArrived[id - 1] = 1;
                }
            } else {
                if (isArrived[id - 1] == 1) {
                    //发送GPS信息
                    intent.putExtra("id", id);
                    intent.putExtra("status", 2);
                    sendBroadcast(intent);
                    isArrived[id - 1] = 3;
                }
            }
        }
    }
}
