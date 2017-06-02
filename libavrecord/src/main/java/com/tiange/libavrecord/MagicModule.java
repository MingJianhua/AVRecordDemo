package com.tiange.libavrecord;

import java.io.File;
import java.io.IOException;
import com.android.tiange.display.VideoEchoDisplay;
import com.android.tiange.encoder.AudioAACEncoder;
import com.android.tiange.encoder.MediaMuxerWrapper;

import android.app.Activity;

import android.content.Context;
import android.hardware.SensorManager;
import android.media.MediaFormat;
import android.opengl.GLSurfaceView;

import android.util.Log;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class MagicModule {
	public static final int PARAM_VIDEO_BIT_RATE = 0;//kbps, default 800kbps
	public static final int PARAM_VIDEO_WIDTH = 1;
	public static final int PARAM_VIDEO_HEIGHT = 2;
	public static final int PARAM_VIDEO_FRAME_RATE = 3;
	public static final int PARAM_VIDEO_ORIENTATION = 4;

	public static final int PARAM_AUDIO_SAMPLE_RATE = 10;
	public static final int PARAM_AUDIO_CHANNEL = 11;
	public static final int PARAM_AUDIO_BIT_RATE = 12;



	public static int AVSTATUS_ENCODER_OPEN_FAIL = 0;  //camera open fail
	public static int AVSTATUS_CAMERA_OPEN_FAIL = 1;  //camera open fail
	public static int AVSTATUS_AUDIO_OPEN_FAIL  = 2;  //audio record open fail
	
	public static int RTMP_CONNECT_FAIL        = 3;  // rtmp connect fail
	public static int RTMP_CONNECT_SUCCESS     = 4;  // rtmp connection success 
	
	public static int RTMP_CONNECTION_CLOSED   = 5;  // rtmp last connection
	
	
	public interface IStatusCallbak{
		
		void OnRtmpStatus( int status );
		
		void OnMediaStatus( int status );
		
	}
	
	int STATUS_STOP 	= 0;
	int STATUS_RUN 		= 1;
	int STATUS_PAUSE 	= 2;
	
	
	Activity          mContext = null;
	GLSurfaceView     mGlSurfaceView = null;
	
	ViewGroup         mViewGroup;
	VideoEchoDisplay  mVideoEcho;
	FrameLayout       framelayout;
	String 			  mSaveFilePath;
	private int       mBitrate = 800;//kbps
	private int       mFps = 20;
	private int 	  mOrientation = 0;


	private int m_nOutWidth  = 0;
	private int m_nOutHeight = 0;
	long mStartTick      = 0;
	long mAudioCurTick   = 0;
	long mVideoCurTick   = 0;
	AlbumOrientationEventListener mAlbumOrientationEventListener;

	MediaMuxerWrapper mMixerWrapper = null;
	private AudioAACEncoder mAudioEncorder;

	public MagicModule(Activity ctx , ViewGroup viewGroup, IStatusCallbak callback, String sSaveFilePath) {
		mContext     = ctx;
		mViewGroup = viewGroup;
		mSaveFilePath = sSaveFilePath;
		/*
		try
		{
			mMixerWrapper = new MediaMuxerWrapper(mSaveFilePath);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		*/
		mAlbumOrientationEventListener = new AlbumOrientationEventListener(mContext, SensorManager.SENSOR_DELAY_NORMAL);
		if (mAlbumOrientationEventListener.canDetectOrientation()) {
			mAlbumOrientationEventListener.enable();
		} else {
			Log.d("chengcj1", "Can't Detect Orientation");
		}
		SetDefaultParams();
		//Start();
	}
	public void SetDefaultParams()
	{
		SetParam(PARAM_VIDEO_WIDTH, 1080);
		SetParam(PARAM_VIDEO_HEIGHT, 1920);
		SetParam(PARAM_VIDEO_BIT_RATE, 8000);
		SetParam(PARAM_VIDEO_FRAME_RATE, 20);
		SetParam(PARAM_AUDIO_CHANNEL, 1);
		SetParam(PARAM_AUDIO_SAMPLE_RATE, 44100);
	}
	public void SetParam(int nParamID, float fValue)
	{
		switch (nParamID)
		{
			case PARAM_VIDEO_WIDTH:
				m_nOutWidth = (int) fValue;
				m_nOutWidth = ((m_nOutWidth + 8) / 16) * 16;
				break;
			case PARAM_VIDEO_HEIGHT:
				m_nOutHeight = (int)fValue;
				break;
			case PARAM_VIDEO_FRAME_RATE:
				mFps = (int)fValue;
				if( mVideoEcho!= null ){
					mVideoEcho.SetFramerate(mFps);
				}
				break;
			case PARAM_VIDEO_BIT_RATE:
				mBitrate = (int)fValue;
				break;
			case PARAM_VIDEO_ORIENTATION:
				break;
			case PARAM_AUDIO_CHANNEL:
				break;
			case PARAM_AUDIO_SAMPLE_RATE:
				break;
			case PARAM_AUDIO_BIT_RATE:
				break;
			default:
				break;
		}

	}
	public float GetPrarm(int nParamID)
	{
		float fValue = 0;
		switch (nParamID)
		{
			case PARAM_VIDEO_WIDTH:
				fValue = m_nOutWidth;
				break;
			case PARAM_VIDEO_HEIGHT:
				fValue = m_nOutHeight;
				break;
			case PARAM_VIDEO_FRAME_RATE:
				fValue = mFps;
				break;
			case PARAM_VIDEO_BIT_RATE:
				fValue = mBitrate;
				break;
			case PARAM_VIDEO_ORIENTATION:
				break;
			case PARAM_AUDIO_CHANNEL:
				break;
			case PARAM_AUDIO_SAMPLE_RATE:
				break;
			case PARAM_AUDIO_BIT_RATE:
				break;
			default:
				break;
		}
		return fValue;

	}
	public void StartRecord(String sSaveFilePath)
	{

		Log.e("MJHTEST", "StartRecord sSaveFilePath = " + sSaveFilePath);
		try
		{
			mMixerWrapper = new MediaMuxerWrapper(sSaveFilePath);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}


		mAudioEncorder = new AudioAACEncoder( null );
		mAudioEncorder.setMuxer(mMixerWrapper);
		mAudioEncorder.start();
		mMixerWrapper.SetOrientationHint(mOrientation);
		mVideoEcho.SetRecordingState(true);
		mVideoEcho.SetMixerWapper(mMixerWrapper);
		mVideoEcho.StartRecord();
	}
	public void StopRecord()
	{
		mAudioEncorder.stop();
		mVideoEcho.StopRecord();
	}
	public void Start() {

		mGlSurfaceView = new GLSurfaceView(mContext);
		framelayout    = new FrameLayout(mContext);
		if (mViewGroup != null) {
			mViewGroup.addView(framelayout, new LayoutParams(-1, -1));
		} else {
			mContext.addContentView(framelayout, new LayoutParams(-1, -1));
		}

		//mAudioEncorder = new AudioAACEncoder( null );
		//mAudioEncorder.setMuxer(mMixerWrapper);
		//mMixerWrapper.SetOrientationHint(mOrientation);
		framelayout.post(new Runnable() {
			@Override
			public void run() {

				float width = framelayout.getWidth();
				float height = framelayout.getHeight();

				float viewHeght = width / 9f * 16;

				LayoutParams cameraoutparams = new LayoutParams((int) width,
						(int) viewHeght);
				framelayout.addView(mGlSurfaceView, cameraoutparams);
				if (viewHeght < height) {
					mGlSurfaceView.setY((height - viewHeght) / 2);
				}
				mVideoEcho = new VideoEchoDisplay(mContext, mGlSurfaceView,
						null, m_nOutWidth, m_nOutHeight, 1920, 1080,
						mBitrate, null);
				mVideoEcho.SetRecordingState(true);
				mVideoEcho.OnResume();
			}
		});
		
		//mAudioEncorder.start();
	}

	public void CapturePhoto(String sFilePath)
	{
		mVideoEcho.onTakePicture(new File(sFilePath), null, null, mOrientation);
	}
	public void DeleteInput(){
		if( framelayout == null )
			return;
		
		final FrameLayout mLayout = framelayout;
		framelayout = null;
		
		mLayout.post( new Runnable() {
			@Override
			public void run() {
				mLayout.removeAllViews();
				ViewGroup paraView =(ViewGroup) mLayout.getParent();
				paraView.removeView(mLayout);
				mVideoEcho.OnDestroy();
				mVideoEcho.OnDelete();
				mVideoEcho = null;
				framelayout = null;
			}
		} );
	}

	/*
	public void setFramerate( int fps ){
		mDefaultFps = fps;
		if( mVideoEcho!= null ){
			mVideoEcho.SetFramerate(mDefaultFps);
		}
	}
	*/
	public void CloseCamera()
	{
		onDestroy();
	}
	
	public void onPause()
	{
		if( mVideoEcho != null ){
			mVideoEcho.OnPause();;
		}

		if( mAudioEncorder != null ){
			mAudioEncorder.Pause();
		}

	}
	
	public void swapCamera( boolean bFront ){
		if( mVideoEcho != null )
			mVideoEcho.SwapCamera(bFront);
	}
	
	public void onResume()
	{
		if( mVideoEcho != null ){
			mVideoEcho.OnResume();
		}

		if( mAudioEncorder != null ){
			mAudioEncorder.Resume();
		}

	}
	
	public void setSlient( boolean vlaue ){

		if( mAudioEncorder != null )
			mAudioEncorder.SetSilent(vlaue);

	}
	
	public void onDestroy()
	{
		mVideoCurTick = mStartTick = mAudioCurTick = 0;
		if( mVideoEcho != null ){
			mVideoEcho.OnDestroy();
		}

		if( mAudioEncorder != null ){
			mAudioEncorder.stop();
		}

	}
	private class AlbumOrientationEventListener extends OrientationEventListener {
		public AlbumOrientationEventListener(Context context) {
			super(context);
		}

		public AlbumOrientationEventListener(Context context, int rate) {
			super(context, rate);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
				return;
			}

			//保证只返回四个方向
			int newOrientation = ((orientation + 45) / 90 * 90) % 360;
			if (newOrientation != mOrientation) {
				mOrientation = newOrientation;
				Log.e("MJHTEST", "mOrientation = " + mOrientation);
				//返回的mOrientation就是手机方向，为0°、90°、180°和270°中的一个
			}
		}
	}
}
