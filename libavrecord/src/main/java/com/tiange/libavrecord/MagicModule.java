package com.tiange.libavrecord;

import java.io.File;
import java.io.IOException;
import com.android.tiange.display.VideoEchoDisplay;
import com.android.tiange.encoder.AudioAACEncoder;
import com.android.tiange.encoder.MediaMuxerWrapper;

import android.app.Activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.opengl.GLSurfaceView;

import android.util.Log;
import android.view.OrientationEventListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class MagicModule {
	private static final String TAG = "MagicModule";

	public static final int PARAM_VIDEO_BIT_RATE = 0;//kbps, default 800kbps
	public static final int PARAM_VIDEO_WIDTH = 1;
	public static final int PARAM_VIDEO_HEIGHT = 2;
	public static final int PARAM_VIDEO_FRAME_RATE = 3;
	public static final int PARAM_VIDEO_ORIENTATION = 4;

	public static final int PARAM_AUDIO_SAMPLE_RATE = 10;
	public static final int PARAM_AUDIO_CHANNEL = 11;
	public static final int PARAM_AUDIO_BIT_RATE = 12;

	public static final int PARAM_BEAUTY_LEVEL = 20;
	public static final int PARAM_FILTER_INDEX = 21;




	public static int AVSTATUS_ENCODER_OPEN_FAIL = 0;  //camera open fail
	public static int AVSTATUS_CAMERA_OPEN_FAIL = 1;  //camera open fail
	public static int AVSTATUS_AUDIO_OPEN_FAIL  = 2;  //audio record open fail
	


	public interface IStatusCallbak{
		
		void OnRtmpStatus( int status );
		
		void OnMediaStatus( int status );

		void OnRecordTime(long nTime);//毫秒
		void OnRecordStatus(int nStatus);

		void OnTakePicture(Bitmap bitmap);
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
	private int       mBitrate = 8000;//kbps
	private int       mFps = 20;
	private int 	  mOrientation = 0;


	private int m_nOutWidth  = 0;
	private int m_nOutHeight = 0;
	long mStartTick      = 0;
	long mAudioCurTick   = 0;
	long mVideoCurTick   = 0;
	boolean mbFront = false;
	private int m_nBeautyLevel  = 3;//0-5

	AlbumOrientationEventListener mAlbumOrientationEventListener;

	MediaMuxerWrapper mMixerWrapper = null;
	private AudioAACEncoder mAudioEncorder;
	private IStatusCallbak mStatusCallback;
	public MagicModule(Activity ctx , ViewGroup viewGroup, IStatusCallbak callback, String sSaveFilePath) {
		mContext     = ctx;
		mViewGroup = viewGroup;
		mSaveFilePath = sSaveFilePath;
		mStatusCallback = callback;
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
		SetParam(PARAM_BEAUTY_LEVEL, 3);

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
			case PARAM_BEAUTY_LEVEL:
				m_nBeautyLevel = (int)fValue;
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
			Log.e("MJHTEST", "new MediaMuxerWrapper = " + sSaveFilePath);
			mMixerWrapper = new MediaMuxerWrapper(sSaveFilePath);
			Log.e("MJHTEST", "mMixerWrapper = " + mMixerWrapper);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		MediaMuxerWrapper.IMuxerStatusCallbak mMagicModuleCallback = new MediaMuxerWrapper.IMuxerStatusCallbak() {

			@Override
			public void OnRecordTime(long nTime)//毫秒
			{
				mStatusCallback.OnRecordTime(nTime);
			}
			@Override
			public void OnRecordStatus(int nStatus)
			{
				mStatusCallback.OnRecordStatus(nStatus);
			}
		};

		mMixerWrapper.SetCallback(mMagicModuleCallback);
		mAudioEncorder = new AudioAACEncoder( null );
		mAudioEncorder.setMuxer(mMixerWrapper);
		mAudioEncorder.start();
		mMixerWrapper.SetOrientationHint(mOrientation);
		mVideoEcho.SetRecordingState(true);
		mVideoEcho.SetMixerWapper(mMixerWrapper);
		mVideoEcho.StartRecord();

		mStatusCallback.OnRecordStatus(CommonDef.AVSTATUS_RECORD_START);

	}
	public void StopRecord()
	{

		mAudioEncorder.stop();
		mVideoEcho.StopRecord();
		//mStatusCallback.OnRecordStatus(CommonDef.AVSTATUS_RECORD_STOP);
	}
	public void StartCameraPreview() {

		mGlSurfaceView = new GLSurfaceView(mContext);
		framelayout    = new FrameLayout(mContext);
		if (mViewGroup != null) {
			mViewGroup.addView(framelayout, new LayoutParams(-1, -1));
		} else {
			mContext.addContentView(framelayout, new LayoutParams(-1, -1));
		}

		final VideoEchoDisplay.IVideoCallbak mVideoCallback = new VideoEchoDisplay.IVideoCallbak() {
			@Override
			public void OnMediaFormat(MediaFormat format)
			{

			}
			@Override
			public void OnEncodeStatus(int status)
			{

			}
			@Override
			public void OnH264Data(byte[] data, int len, int keyframe)
			{

			}
			@Override
			public void OnTakePicture(Bitmap bitmap)
			{
				mStatusCallback.OnTakePicture(bitmap);
			}
		};
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
						mVideoCallback, m_nOutWidth, m_nOutHeight, 1920, 1080,
						mBitrate, null);
				mVideoEcho.SetLevel(m_nBeautyLevel);
				mVideoEcho.SetDefaultCamera(mbFront);
				mVideoEcho.SetRecordingState(true);
				mVideoEcho.OnResume();
			}
		});
		
		//mAudioEncorder.start();
	}

	public void CapturePhoto(String sFilePath)//输入为null时回调Ontakepicture
	{
		if (sFilePath != null)
		{
			mVideoEcho.onTakePicture(new File(sFilePath), null, null, mOrientation);
		}
		else
		{
			mVideoEcho.onTakePicture(null, null, null, mOrientation);
		}
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
	
	public int swapCamera( boolean bFront ){
		if (mVideoEcho == null)
		{//未开始预览，设置默认摄像头
			mbFront = bFront;
			return 0;
		}
		if (mMixerWrapper!=null && mMixerWrapper.isStarted())
		{//正在录制，禁止转摄像头
			return -2;
		}

		mVideoEcho.SwapCamera(bFront);
		return 0;
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
	public boolean isCameraPermission() {

		boolean canUse =true;

		Camera mCamera =null;

		try{

			mCamera = Camera.open();

// setParameters 是针对魅族MX5。MX5通过Camera.open()拿到的Camera对象不为null

			Camera.Parameters mParameters = mCamera.getParameters();

			mCamera.setParameters(mParameters);

		}catch(Exception e) {

			canUse =false;

		}

		if(mCamera !=null) {

			mCamera.release();

		}
		return canUse;

	}
	/**
	 * 作用：用户是否同意录音权限
	 *
	 * @return true 同意 false 拒绝
	 */
	public boolean isVoicePermission() {

		try {
			AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT));
			record.startRecording();
			int recordingState = record.getRecordingState();
			if(recordingState == AudioRecord.RECORDSTATE_STOPPED){
				return false;
			}
			record.release();
			return true;
		} catch (Exception e) {
			return false;
		}

	}
}
