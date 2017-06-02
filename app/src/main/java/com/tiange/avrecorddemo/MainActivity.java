package com.tiange.avrecorddemo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

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
//    AlbumOrientationEventListener mAlbumOrientationEventListener;
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

        btnStart.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMagicModule  = new MagicModule(  mActivity, mVideoContainer , new MagicModule.IStatusCallbak() {

                    @Override
                    public void OnRtmpStatus(int status) {
                        Log.i( "=====>111111" , "OnRtmpStatus" + status);
                        if( status != MagicModule.RTMP_CONNECT_SUCCESS ){
                            mMagicModule.CloseCamera();
                            mMagicModule.DeleteInput();
                        }
                    }

                    @Override
                    public void OnMediaStatus(int status) {
                        Log.i( "=====>222222" , "OnMediaStatus" + status );
                    }
                },  getCaptureFile(Environment.DIRECTORY_MOVIES, ".mp4").toString());
/*
                //默认参数，可以不设，如需改变输出参数，在这里设置
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_WIDTH, 1080);
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_HEIGHT, 1920);
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_BIT_RATE, 8000);
                mMagicModule.SetParam(mMagicModule.PARAM_VIDEO_FRAME_RATE, 20);
                mMagicModule.SetParam(mMagicModule.PARAM_AUDIO_CHANNEL, 1);
                mMagicModule.SetParam(mMagicModule.PARAM_AUDIO_SAMPLE_RATE, 44100);
*/
                mMagicModule.Start();
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
                        mMagicModule.CapturePhoto(getOutputMediaFile().toString());
                    }
                });
        btnStartRecord.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMagicModule.StartRecord(getCaptureFile(Environment.DIRECTORY_MOVIES, ".mp4").toString());
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
                        mMagicModule.swapCamera(false);
                    }
                });
//        mAlbumOrientationEventListener = new AlbumOrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL);
//        if (mAlbumOrientationEventListener.canDetectOrientation()) {
//            mAlbumOrientationEventListener.enable();
//        } else {
//            Log.d("chengcj1", "Can't Detect Orientation");
//        }
    }
    boolean isSlient = false;

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
        /*
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i("info", "landscape"); // 横屏
            Point screenSize = new Point();
            getWindowManager().getDefaultDisplay().getSize(screenSize);
            mVideoContainer =(FrameLayout) findViewById( R.id.frameContainer );
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoContainer.getLayoutParams();
            params.width = screenSize.x;
            params.height = screenSize.x * 9 / 16;
            Log.e("MJHTEST", "width = " + params.width + "   params.height = "+ params.height);
            mVideoContainer.setLayoutParams(params);
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i("info", "portrait"); // 竖屏
            Point screenSize = new Point();
            getWindowManager().getDefaultDisplay().getSize(screenSize);
            mVideoContainer =(FrameLayout) findViewById( R.id.frameContainer );
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoContainer.getLayoutParams();
            params.width = screenSize.x;
            params.height = screenSize.x * 16/ 9;
            mVideoContainer.setLayoutParams(params);
            Log.e("MJHTEST", "width = " + params.width + "   params.height = "+ params.height);

        }
*/
    }
    /**
     * generate output file
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static final File getCaptureFile(final String type, final String ext) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
        Log.d(TAG, "path=" + dir.toString());
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, getDateTimeString() + ext);
        }
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

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MagicCamera");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINESE).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

//    private class AlbumOrientationEventListener extends OrientationEventListener {
//        int mOrientation;
//        public AlbumOrientationEventListener(Context context) {
//            super(context);
//        }
//
//        public AlbumOrientationEventListener(Context context, int rate) {
//            super(context, rate);
//        }
//
//        @Override
//        public void onOrientationChanged(int orientation) {
//            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
//                return;
//            }
//
//            //保证只返回四个方向
//            int newOrientation = ((orientation + 45) / 90 * 90) % 360;
//            if (newOrientation != mOrientation) {
//                mOrientation = newOrientation;
//                Log.e("MJHTEST", "mOrientation = " + mOrientation);
//                //返回的mOrientation就是手机方向，为0°、90°、180°和270°中的一个
//            }
//        }
//    }
}
