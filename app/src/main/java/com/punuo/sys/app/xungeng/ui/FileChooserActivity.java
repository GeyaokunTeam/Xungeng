package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.FileChooserAdapter;
import com.punuo.sys.app.xungeng.file.FileInfo;
import com.punuo.sys.app.xungeng.util.LogUtil;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/6/3.
 */
public class FileChooserActivity extends MyActivity {
    @Bind(R.id.back)
    ImageButton back;
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.gvFileChooser)
    GridView gvFileChooser;
    @Bind(R.id.upload)
    Button upload;
    @Bind(R.id.imgBackFolder)
    Button imgBackFolder;
    private String TAG = "FileChooserActivity";
    private int lastposition = -1;

    private String mSdcardRootPath;  //sdcard 根路径
    private String mLastFilePath;    //当前显示的路径

    private ArrayList<FileInfo> mFileLists;
    private FileChooserAdapter mAdatper;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filechooser_show);
        ButterKnife.bind(this);
        mSdcardRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        back.setOnClickListener(mClickListener);
        back.setOnClickListener(mClickListener);
        imgBackFolder.setOnClickListener(mClickListener);
        upload.setOnClickListener(mClickListener);
        gvFileChooser.setEmptyView(findViewById(R.id.tvEmptyHint));
        gvFileChooser.setOnItemClickListener(mItemClickListener);
        setGridViewAdapter(mSdcardRootPath);

    }

    //配置适配器
    private void setGridViewAdapter(String filePath) {
        updateFileItems(filePath);
        mAdatper = new FileChooserAdapter(this, mFileLists);
        gvFileChooser.setAdapter(mAdatper);

    }

    //根据路径更新数据，并且通知Adatper数据改变
    private void updateFileItems(String filePath) {
        mLastFilePath = filePath;
        title.setText(mLastFilePath);

        if (mFileLists == null)
            mFileLists = new ArrayList<FileInfo>();
        if (!mFileLists.isEmpty())
            mFileLists.clear();

        File[] files = folderScan(filePath);
        if (files == null)
            return;

        for (int i = 0; i < files.length; i++) {
            if (files[i].isHidden())  // 不显示隐藏文件
                continue;

            String fileAbsolutePath = files[i].getAbsolutePath();
            String fileName = files[i].getName();
            boolean isDirectory = false;
            if (files[i].isDirectory()) {
                isDirectory = true;
            }
            FileInfo fileInfo = new FileInfo(fileAbsolutePath, fileName, isDirectory);
            mFileLists.add(fileInfo);
        }
        //When first enter , the object of mAdatper don't initialized
        if (mAdatper != null)
            mAdatper.notifyDataSetChanged();  //重新刷新
    }

    //获得当前路径的所有文件
    private File[] folderScan(String path) {
        File file = new File(path);
        File[] files = file.listFiles();
        return files;
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imgBackFolder:
                    backProcess();
                    break;
                case R.id.back:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
                case R.id.upload:
                    if (lastposition != -1) {
                        FileInfo fileInfo = mFileLists.get(lastposition);
                        Intent intent = new Intent();
                        intent.putExtra("FilePath", fileInfo.getFilePath());
                        LogUtil.d(TAG,fileInfo.getFilePath());
                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                        Toast.makeText(FileChooserActivity.this, "请选择要上传的文件", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

            FileInfo fileInfo = mAdatper.getItem(position);
            if (fileInfo.isDirectory()) {   //点击项为文件夹, 显示该文件夹下所有文件
                updateFileItems(fileInfo.getFilePath());
                lastposition = -1;
            } else {
                FileInfo file = mFileLists.get(position);
                if (file.isSelected()) {
                    file.setSelected(false);
                    lastposition = -1;
                } else {
                    file.setSelected(true);
                    if (lastposition >= 0) {
                        mFileLists.get(lastposition).setSelected(false);
                    }
                    lastposition = position;
                }
                mAdatper.notifyDataSetChanged();


            }
        }
    };

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode()
                == KeyEvent.KEYCODE_BACK) {
            backProcess();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //返回上一层目录的操作
    public void backProcess() {
        //判断当前路径是不是sdcard路径,如果不是，则返回到上一层。
        if (lastposition >= 0) {
            mFileLists.get(lastposition).setSelected(false);
        }
        if (!mLastFilePath.equals(mSdcardRootPath)) {
            File thisFile = new File(mLastFilePath);
            String parentFilePath = thisFile.getParent();
            updateFileItems(parentFilePath);
            lastposition=-1;
        } else {   //是sdcard路径 ，直接结束
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
