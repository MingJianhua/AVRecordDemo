package com.android.tiange.display;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
//import android.graphics.Matrix;
//import android.opengl.Matrix;

import android.os.Handler;
import android.util.Log;

import com.android.grafika.gles.FullFrameRect;
import com.android.grafika.gles.GlUtil;
import com.android.grafika.gles.Texture2dProgram;
import com.android.tiange.encoder.MediaMuxerWrapper;
import com.android.tiange.encoder.TextureMovieEncoder;
import com.seu.magicfilter.display.MagicDisplay;
import com.seu.magicfilter.filter.base.MagicCameraInputFilter;
import com.seu.magicfilter.filter.helper.MagicFilterParam;
import com.seu.magicfilter.utils.OpenGLUtils;
import com.seu.magicfilter.utils.Rotation;
import com.seu.magicfilter.utils.SaveTask;
import com.seu.magicfilter.utils.TextureRotationUtil;

public class VideoEchoDisplay extends MagicDisplay {


	public interface IVideoCallbak {
		public void OnMediaFormat(MediaFormat format);

		public void OnEncodeStatus(int status);

		public void OnH264Data(byte[] data, int len, int keyframe);
	}

	private MagicCameraInputFilter mCameraInputFilter;

	private SurfaceTexture mSurfaceTexture;
	private TextureMovieEncoder mVideoEncoder;
	private FullFrameRect mFullScreen;

	// the output video size!!! the width must be 16n
	private int mOutWidth;
	private int mOutHeight;

	private float[] mIdentityMatrix;

	public Handler mFpsHandler;

	private RenderFrameBuffer mFboBuffer;
	private CamaraHelper mCameraHelper;

	private boolean isFrontCamera = false;

	private boolean isCameraSwapDown = true;

	private int mBitrate, frameRate = 20;

	private LostStatic mStatic = new LostStatic();

	private int mBeautyLevel = 5;

	private int mRotation = 0;
	//private IVideoCallbak mCallback;

	public VideoEchoDisplay(Context context, GLSurfaceView glSurfaceView,
							IVideoCallbak callback, int nOutWidth, int nOutHeight,
							int mCameraWidth, int mCameraHeight, int bitrate, MediaMuxerWrapper muxerWrapper) {
		super(context, glSurfaceView);
		mBitrate = bitrate;
		mIdentityMatrix = new float[16]; // 1 0 0 0
		android.opengl.Matrix.setIdentityM(mIdentityMatrix, 0); // 0 1 0 0
		// 0 0 1 0
		// 0 0 0 1
//		mOutWidth = nOutHeight;//nOutWidth;
//		mOutHeight = nOutWidth;//nOutHeight;
		mOutWidth = nOutWidth;
		mOutHeight = nOutHeight;
		//mCallback = callback;
		mCameraInputFilter = new MagicCameraInputFilter();
//		SetBeautyLevel( mBeautyLevel );
		mVideoEncoder = new TextureMovieEncoder(callback);
		mFboBuffer = new RenderFrameBuffer();
		mCameraHelper = new CamaraHelper();
		setCaptrueSize(mCameraWidth, mCameraHeight);
		mTextureId = OpenGLUtils.getExternalOESTextureID();
		mRotation = 0;
		//mVideoEncoder.SetMuxer(muxerWrapper);
	}

	public void StartRecord()
	{
		isRealRecordStart = true;
	}
	public void StopRecord()
	{
		isRealRecordStart = false;
		if (mVideoEncoder != null && mVideoEncoder.isRecording()) {
			mVideoEncoder.stopRecording();
		}
	}
	public void SetMixerWapper(MediaMuxerWrapper muxerWrapper)
	{
		mVideoEncoder.SetMuxer(muxerWrapper);
	}
//	public void SetBeautyLevel( int leavel ){
//		mBeautyLevel = leavel;
//		SetLevel( leavel );
//	}
	
	public void setCaptrueSize(int desireWidth, int desireHeight) {
		mCameraHelper.mCameraWidth = desireWidth;
		mCameraHelper.mCameraHeight = desireHeight;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glDisable(GL10.GL_DITHER);
		GLES20.glClearColor(0, 0, 0, 0);
		GLES20.glEnable(GL10.GL_CULL_FACE);
		GLES20.glEnable(GL10.GL_DEPTH_TEST);
		MagicFilterParam.initMagicFilterParam(gl);
		
		mCameraInputFilter.init();
		
		if (mFullScreen == null)
			mFullScreen = new FullFrameRect(new Texture2dProgram(
					Texture2dProgram.ProgramType.TEXTURE_2D));
		setFilter(6);
		Log.i("[ VideoEcho ]", "onSurfaceCreated");
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		onFilterChanged();
		Log.i("[ VideoEcho ]", "onSurfaceChanged");
	}

	private boolean isRecordStart = false;
	private boolean isRealRecordStart = false;


	private long mLastTick  = 0;
	private int  mRenderfps = 0;
	private long mPrevSecTick = 0;
	private int  mPrevfps   = 0;
	
	
	public void OnDelete(){

		if (mVideoEncoder != null && mVideoEncoder.isRecording()) {
			mVideoEncoder.stopRecording();
		}

	}
	
	public void OnResume() {
		if (mCameraHelper.mCamera == null) {
			mCameraHelper.openCamera();
		}
		super.onFilterChanged();
	}

	public void SetFramerate( int fps ){
		frameRate = fps;
	}
	public void SetDefaultCamera(boolean isFront)
	{
		isFrontCamera = isFront;
		isCameraSwapDown = false;
	}
	public void SwapCamera( boolean isFront ){
		isFrontCamera = isFront;
		isCameraSwapDown = false;
		OnPause();
		
		new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep( 500 );
				} catch (InterruptedException e) {
					
				}
				OnResume();
			}
		} ).start();
		
	}
	
	public void SetRecordingState(boolean value) {
		mLastTick  = 0;
		mRenderfps = 0;
		mPrevfps   = 0;
		mRenderfps = 0;
		mPrevSecTick = 0;
		isRecordStart = value;
		mStatic.clear();
	}

	public void OnPause() {
		super.onPause();
		mCameraHelper.releaseCamera();
	}

	public void OnDestroy() {
		mSurfaceTexture = null;
		mCameraHelper.releaseCamera();
		SetRecordingState(false);// = true;
		mGLSurfaceView.requestRender();
	}
	public void onTakePicture(File file, SaveTask.onPictureSaveListener listener, Camera.ShutterCallback shutterCallback, int nRotation){
		//mCameraHelper.setRotation(90);
		mRotation = nRotation;
		mSaveTask = new SaveTask(mContext, file, listener);
		mCameraHelper.takePicture(null, null, mPictureCallback);
	}


	private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback()
	{
		@Override
		public void onShutter(){
			Log.e("MJHTEST", "onShutter");
		}
	};
	private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(final byte[] data,Camera camera) {
			Log.e("MJHTEST", "onPictureTaken mFilters = "+ mFilters);
			mCameraHelper.stopPreview();
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			if (isFrontCamera)
				bitmap = rotateBitmapByDegree(bitmap, 270 - mRotation, false);
			else
				bitmap = rotateBitmapByDegree(bitmap, mRotation + 90, false);
			if(mFilters != null){
				getBitmapFromGL(bitmap, true);
			}else{
				mSaveTask.execute(bitmap);
			}
			mCameraHelper.startPreview();
		}
	};

	protected void onGetBitmapFromGL(Bitmap bitmap){
		mSaveTask.execute(bitmap);
	}

	//丢包索引
	private int mLostIndex = 0;
	//丢包间隔
	private int mLostDelta = 0;

	@Override
	public void onDrawFrame(GL10 gl) {
		if( isCameraSwapDown== false ){
			return;
		}
		
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);

		if (!isRecordStart) {

			if (mVideoEncoder.isRecording()) {
				Log.i("recording is stoped", "*******************************");
				mVideoEncoder.stopRecording();
			}

			if( mFboBuffer != null ){
				mFboBuffer.destroyFramebuffers();
				mFboBuffer = null;
			}
			return;
		}
		final SurfaceTexture mTextrue = mSurfaceTexture;
		if (mSurfaceTexture == null)
			return;
		long mCurrentTick = System.currentTimeMillis();

		mTextrue.updateTexImage();

		float[] mtx = new float[16];
		mTextrue.getTransformMatrix(mtx);
		mCameraInputFilter.setTextureTransformMatrix(mtx);
		if (mFilters == null) {
			mCameraInputFilter.onDrawFrame(mTextureId, mGLCubeBuffer,
					mGLTextureBuffer);
		} else {

			if (isRealRecordStart)
			{
				boolean isVideoRecording = mVideoEncoder.isRecording();

				if (!isVideoRecording) {

					int curBitrate = mBitrate * 1000;
					if (curBitrate > 15000000) {
						curBitrate = 15000000;
					} else if (curBitrate < 100000) {
						curBitrate = 100000;
					}
					mVideoEncoder
							.startRecording(new TextureMovieEncoder.EncoderConfig(
									null, mOutWidth, mOutHeight, curBitrate,
									EGL14.eglGetCurrentContext()));
				}
			}


			int mBuftextureID = mCameraInputFilter.onDrawToTexture(mTextureId);

			// draw the filter textrue to fbo!!!
			mFboBuffer.beginFbo();
			mFilters.onDrawFrame(mBuftextureID, mGLCubeBuffer, mGLTextureBuffer);
			mFboBuffer.endFbo();
			mFullScreen
					.drawFrame(mFboBuffer.mOffscreenTexture, mIdentityMatrix);

			mLostIndex++;
			//每秒钟更新fps操作
			if( mStatic.isTickOut() )
			{
				int mOutputFps = 20;
				if (isRealRecordStart)
				 	mOutputFps = mVideoEncoder.getOutputFPS();

				int deltaFps = mOutputFps - frameRate;

				if( mOutputFps > frameRate )
				{
					//初始化丢帧频率
					if( deltaFps > 5 ){
						if( mLostDelta==0 ){
							mLostDelta = mOutputFps/deltaFps + 2;
						}else{
							mLostDelta --;
						}
					}
//			    Log.i( "output fps ==== " , mOutputFps + "mLostDelta＝＝＝" +mLostDelta + "render fps" + mPrevfps );
				}else if( mOutputFps < frameRate ){
					if( mLostDelta != 0 ){
						if( deltaFps < -4 ){
							mLostDelta += 2;
						}else{
							mLostDelta ++;
						}
					}
				}

				if (mLostDelta > 10 || mLostDelta < 0) {
					mLostDelta = 0;
				}
//				Log.w( "output fps ==== " , "mOutputFps＝＝＝" +mOutputFps + "mLostDelta ==" + mLostDelta  + "mRenderfps" + mPrevfps );
			}
			if( mLostDelta == 0 || mLostIndex <  mLostDelta ){
				if (isRealRecordStart) {
					mVideoEncoder.setTextureId(mFboBuffer.mOffscreenTexture);
					mVideoEncoder.frameAvailable(mIdentityMatrix,
							mTextrue.getTimestamp());
				}
			}else{
//				Log.i("lostDelta=======", " do lost video frame!!! ");
				mLostIndex = 0;
			}
		}

		mRenderfps++;
		// caculate fps
		if (mPrevSecTick != 0 && (mCurrentTick - mPrevSecTick) >= 1000) {
			mPrevSecTick = mCurrentTick;
			mPrevfps = mRenderfps;
//			Log.i("render fps ===>******", "mRenderfps" + mRenderfps);
			mRenderfps = 0;
		}
		if (mPrevSecTick == 0) {
			mPrevSecTick = mCurrentTick;
		}

		long sleepSpan = 0;
		if( mLastTick != 0 ){
			long deltaTick = mCurrentTick - mLastTick;
			int frameDelta =(int)(1000/frameRate);
			
			if( deltaTick < frameDelta ){
				sleepSpan = frameDelta - deltaTick - 5;
			}
		}

//		Log.i("=====>",  "sleepSpan ===> " + sleepSpan );
		if( sleepSpan > 0 ){
			try {
				Thread.sleep(sleepSpan);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mLastTick = System.currentTimeMillis();
	}

	private OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(SurfaceTexture surfaceTexture) {
			mGLSurfaceView.requestRender();
		}
	};

	protected void onFilterChanged() {
		super.onFilterChanged();
		// the surface display size
		mCameraInputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
		mCameraInputFilter.onOutputSizeChanged(mSurfaceWidth, mSurfaceHeight);
		if (mFilters != null) {
			mCameraInputFilter.initCameraFrameBuffer(mSurfaceWidth,
					mSurfaceHeight);
			if( mFboBuffer != null )
			    mFboBuffer.prepareFramebuffer(mSurfaceWidth, mSurfaceHeight);
			mFullScreen.getProgram().setTexSize(mSurfaceWidth, mSurfaceHeight);
		} else
			mCameraInputFilter.destroyFramebuffers();
	}

	private void adjustPosition(int orientation, boolean flipHorizontal,
			boolean flipVertical) {

		Rotation mRotation = Rotation.fromInt(orientation);
		float[] textureCords = TextureRotationUtil.getRotation(mRotation,
				flipHorizontal, flipVertical);
		mGLTextureBuffer.clear();
		mGLTextureBuffer.put(textureCords).position(0);
	}

	private class CamaraHelper {

		private int mCameraWidth;
		private int mCameraHeight;
		private Camera mCamera;

		private void releaseCamera() {
			if (mCamera != null) {
				mCamera.setPreviewCallback(null);
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
				mSurfaceTexture = null;
			}
		}

		private void openCamera() {
			mGLSurfaceView.queueEvent(new Runnable() {
				@Override
				public void run() {
					
					mSurfaceTexture = new SurfaceTexture(mTextureId);
					mSurfaceTexture
							.setOnFrameAvailableListener(mOnFrameAvailableListener);
					Camera.CameraInfo info = new Camera.CameraInfo();

					int numCameras = Camera.getNumberOfCameras();
					int cameraId = 0;
					
					for (int i = 0; i < numCameras; i++) {
						Camera.getCameraInfo(i, info);
						
						if( isFrontCamera ){
							if( info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ){
								cameraId = i;
								break;
							}
						}else{
							if( info.facing != Camera.CameraInfo.CAMERA_FACING_FRONT ){
								cameraId = i;
								break;
							}
						}
					}

					try{
				    	mCamera = Camera.open( cameraId );
					}catch( Exception e ){
						//mCallback.OnEncodeStatus( 1 );
						return;
					}
					
//					if (mCamera == null) {
//						// Log.d(TAG,
//						// "No front-facing camera found; opening default");
//						mCamera = Camera.open(); // opens first back-facing
//													// camera
//					}
					
					if (mCamera == null) {
						throw new RuntimeException("Unable to open camera");
					}

					Camera.Parameters parms = mCamera.getParameters();
					// List<Size> sss= parms.getSupportedPreviewSizes();
					com.seu.magicfilter.camera.CameraUtils.choosePreviewSize(
							parms, mCameraWidth, mCameraHeight);
					//mCamera.setDisplayOrientation(90);
					parms.setRecordingHint(true);
					List<String> focusModes = parms.getSupportedFocusModes();
					if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
						parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
					mCamera.setParameters(parms);



					Camera.Size mCameraPreviewSize = parms.getPreviewSize();
					Size size = mCameraPreviewSize;
					int orientation = info.orientation;


					mImageWidth = size.width;
					mImageHeight = size.height;
					if (orientation == 90 || orientation == 270) {
						mImageWidth = size.height;
						mImageHeight = size.width;
					}
					Log.e("MJHTEST", "orientation = " + orientation);

					
					CameraInfo cameraInfo = new CameraInfo();
					Camera.getCameraInfo(cameraId, cameraInfo);
					
					boolean flipHorizontal = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT ? true : false;
					adjustPosition(orientation, flipHorizontal, !flipHorizontal);
					try {
						mCamera.setPreviewTexture(mSurfaceTexture);
						mCamera.startPreview();
						mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					isCameraSwapDown = true;
				}
			});
		}
		private void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
									   Camera.PictureCallback jpegCallback){
			mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
		}
		private void setRotation(int rotation){
			Camera.Parameters params = mCamera.getParameters();
			params.setRotation(rotation);
			mCamera.setParameters(params);
		}
		private void startPreview()
		{
			mCamera.startPreview();
		}
		private void stopPreview()
		{
			mCamera.stopPreview();
		}
	}

	/**
	 * 将图片按照某个角度进行旋转
	 *
	 * @param bm
	 *            需要旋转的图片
	 * @param degree
	 *            旋转角度
	 * @return 旋转后的图片
	 */
	public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree, boolean bFlip) {

		if(degree >= 360)
			degree -= 360;
		if(degree < 0)
			degree += 360;
		Bitmap returnBm = null;

		// 根据旋转角度，生成旋转矩阵
		android.graphics.Matrix matrix = new android.graphics.Matrix();

		matrix.postRotate(degree);

		try {
			// 将原始图片按照旋转矩阵进行旋转，并得到新的图片
			returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
					bm.getHeight(), matrix, true);
		} catch (OutOfMemoryError e) {
		}
		if (returnBm == null) {
			returnBm = bm;
		}
		if (bm != returnBm) {
			bm.recycle();
		}
		return returnBm;
	}



	private class LostStatic{
		public long mCurrentTick = 0;
		
		public boolean isTickOut(){
			long currentTick = System.currentTimeMillis();
			if( mCurrentTick == 0 ){
				mCurrentTick = currentTick;
			}
			if( currentTick - mCurrentTick >= 1000 ){
				mCurrentTick = currentTick;
				return true;
			}
			return false;
		}
		
		public void clear(){
			mCurrentTick = 0;
		}
		
	}
	// private

	// fbo class wrap

	private class RenderFrameBuffer {

		private int mFboWidth;
		private int mFboHeight;
		// FBO ,recording magicfiler textrue!!!
		private int mOffscreenTexture = -1;
		private int mFramebuffer = -1;
		private int[] mFrameBuffers = null;
		private int[] mFrameBufferTextures = null;

		public void destroyFramebuffers() {
			if (mFrameBufferTextures != null) {
				GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
				mFrameBufferTextures = null;
			}
			if (mFrameBuffers != null) {
				GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
				mFrameBuffers = null;
			}
			mFboWidth = -1;
			mFboHeight = -1;
		}

		/**
		 * Prepares the off-screen framebuffer.
		 */
		public void prepareFramebuffer(int width, int height) {
			if (width == 0 || height == 0) {
				return;
			}
			if (mFboWidth == width && mFboWidth == height
					&& mFrameBuffers != null) {
				return;
			}
			if (mFrameBuffers != null) {
				destroyFramebuffers();
			}

			GlUtil.checkGlError("prepareFramebuffer start");
			// int[] values = new int[1];
			mFrameBufferTextures = new int[1];
			GLES20.glGenTextures(1, mFrameBufferTextures, 0);
			GlUtil.checkGlError("glGenTextures");
			mOffscreenTexture = mFrameBufferTextures[0]; // expected > 0
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTexture);
			GlUtil.checkGlError("glBindTexture " + mOffscreenTexture);
			// Create texture storage.
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width,
					height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			// Set parameters. We're probably using non-power-of-two dimensions,
			// so
			// some values may not be available for use.
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
					GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GlUtil.checkGlError("glTexParameter");
			// Create framebuffer object and bind it.
			mFrameBuffers = new int[1];
			GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
			GlUtil.checkGlError("glGenFramebuffers");
			mFramebuffer = mFrameBuffers[0]; // expected > 0
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
			GlUtil.checkGlError("glBindFramebuffer " + mFramebuffer);
			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
					GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
					mOffscreenTexture, 0);
			GlUtil.checkGlError("glFramebufferTexture2D");

			// See if GLES is happy with all this.
			int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
			if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
				throw new RuntimeException("Framebuffer not complete, status="
						+ status);
			}

			// Switch back to the default framebuffer.
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			GlUtil.checkGlError("prepareFramebuffer done");

			Log.i("[ video echo ]", "fbo buffer inited!!!");
		}

		public void beginFbo() {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFramebuffer);
		}

		public void endFbo() {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		}

	}

}
