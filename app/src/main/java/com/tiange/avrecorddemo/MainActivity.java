package com.tiange.avrecorddemo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tiange.libavrecord.MagicModule;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class MainActivity extends Activity {

    private FrameLayout mVideoContainer = null;

    private MagicModule mMagicModule = null;

    private Activity mActivity;

    private static final String TAG = "MainActivity";
    private static final String DIR_NAME = "AVRecSample";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
    private static final int MESSAGE_UPDATA_TEXTVIEW_STATUS = 0;
    private static final int MESSAGE_UPDATA_TEXTVIEW_TIME = 1;
    TextView mTextViewRecordStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoContainer =(FrameLayout) findViewById( R.id.videoContainer );

        mActivity = this;

        Button btnStart = (Button) findViewById( R.id.btn_start );
        Button btnStop = (Button) findViewById( R.id.btn_stop );
        Button btnStartRecord = (Button) findViewById( R.id.btn_startrecord );
        Button btnStopRecord = (Button) findViewById( R.id.btn_stoprecord );
        Button btnTakePicture = (Button) findViewById( R.id.btn_takephoto );
        Button btnSwitchCamera = (Button) findViewById( R.id.btn_switchcamera );
        mTextViewRecordStatus = (TextView)findViewById(R.id.textViewRecordStatus);

        btnStart.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMagicModule  = new MagicModule(  mActivity, mVideoContainer , new MagicModule.IStatusCallbak() {
                    @Override
                    public void OnRecordStatus(int nStatus)
                    {
                        if(nStatus == MagicModule.AVSTATUS_RECORD_START)
                        {
                            Message message = new Message();
                            message.what = MESSAGE_UPDATA_TEXTVIEW_STATUS;
                            message.arg1 = 1;
                            mHandler.sendMessage(message);
                        }
                        if(nStatus == MagicModule.AVSTATUS_RECORD_STOP)
                        {
                            Message message = new Message();
                            message.what = MESSAGE_UPDATA_TEXTVIEW_STATUS;
                            message.arg1 = 0;
                            mHandler.sendMessage(message);
                        }
                    }
                    @Override
                    public void OnRecordTime(long nTime)//毫秒
                    {
                        Log.e(TAG, "OnRecordTime = " + nTime);
                        Message message = new Message();
                        message.what = MESSAGE_UPDATA_TEXTVIEW_TIME;
                        message.arg1 = (int)nTime;
                        mHandler.sendMessage(message);
                    }
                    @Override
                    public void OnRtmpStatus(int status) {
                        Log.i( "=====>111111" , "OnRtmpStatus" + status);

                    }

                    @Override
                    public void OnMediaStatus(int status) {
                        Log.i( "=====>222222" , "OnMediaStatus" + status );

                    }
                },  getCaptureFilePath(Environment.DIRECTORY_MOVIES, ".mp4"));
/*
                //默认参数，可以不设，如需改变输出参数，在这里设置
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_WIDTH, 1080);
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_HEIGHT, 1920);
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_BIT_RATE, 8000);
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_FRAME_RATE, 20);
                mMagicModule.SetParam(mMagicModule.PARAM_AUDIO_CHANNEL, 1);
                mMagicModule.SetParam(mMagicModule.PARAM_AUDIO_SAMPLE_RATE, 44100);
*/
                if(!mMagicModule.isCameraPermission())
                {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "请到设置页面开启摄像头权限", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                if (!mMagicModule.isVoicePermission())
                {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "请到设置页面开启麦克风权限", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 100);
                    toast.show();
                }
                mMagicModule.StartCameraPreview();
            }
        } );


        btnStop.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMagicModule.CloseCamera();
                        mMagicModule.DeleteInput();
                    }
                });
        btnTakePicture.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMagicModule.CapturePhoto(getOutputMediaFilePath());
                    }
                });
        btnStartRecord.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMagicModule.StartRecord(getCaptureFilePath(Environment.DIRECTORY_MOVIES, ".mp4"));
                    }
                });
        btnStopRecord.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMagicModule.StopRecord();
                    }
                });
        btnSwitchCamera.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMagicModule == null)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "未启动预览，不能切换摄像头", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return;
                        }
                        int nRet = mMagicModule.swapCamera(false);
                        if (nRet == -1)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "未启动预览，不能切换摄像头", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                        if (nRet == -2)
                        {
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "正在录制中，不能切换摄像头", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
                });


    }
    boolean isSlient = false;
    final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            if(msg.what == MESSAGE_UPDATA_TEXTVIEW_TIME){
                long nTime = msg.arg1;
                mTextViewRecordStatus.setText("Recording:"+ nTime);
            }
            if(msg.what == MESSAGE_UPDATA_TEXTVIEW_STATUS){
                int nStatus = msg.arg1;
                if (nStatus == 0)
                    mTextViewRecordStatus.setText("Stop");
                else if (nStatus == 1)
                    mTextViewRecordStatus.setText("Recording");


            }
        }
    };
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if( mMagicModule != null )
            mMagicModule.onResume();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();

        if( mMagicModule != null )
            mMagicModule.onPause();

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if( mMagicModule != null )
            mMagicModule.onDestroy();
    }
    @Override
    public void onConfigurationChanged(Configuration config) {

        super.onConfigurationChanged(config);

    }
    /**
     * generate output file
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public String getCaptureFilePath(final String type, final String ext) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
        Log.d(TAG, "path=" + dir.toString());
        dir.mkdirs();
        if (dir.canWrite()) {

            return  dir.toString() + File.separator + getDateTimeString() + ext;
        }
        Toast toast = Toast.makeText(getApplicationContext(),
                "请到设置页面开启写文件权限", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 200);
        toast.show();
        return null;
    }

    /**
     * get current date and time as String
     * @return
     */
    private static final String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }

    private static String getOutputMediaFilePath() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MagicCamera");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        String mediaFilePath = mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg";

        return mediaFilePath;
    }

}
