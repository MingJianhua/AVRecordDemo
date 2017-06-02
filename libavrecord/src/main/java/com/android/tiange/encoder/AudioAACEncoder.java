package com.android.tiange.encoder;

import java.io.File;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;


public class AudioAACEncoder {

	public interface AudioClient{
		void OnMediaFormat(MediaFormat format);
		void OnAACData( byte[] dataOut , int len );
		
		void OnAudioStatus( int type );
	}
	
	String MIME_TYPE      = "audio/mp4a-latm";
	int KEY_CHANNEL_COUNT = 2;
	int KEY_SAMPLE_RATE   = 44100;
	int KEY_BIT_RATE      = 96000;
	int KEY_AAC_PROFILE   = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
	int WAIT_TIME         = 10000;
	int AUDIO_FORMAT      = AudioFormat.ENCODING_PCM_16BIT;
	int CHANNEL_MODE      = AudioFormat.CHANNEL_IN_STEREO;
	int mReadBufferSize   = 4096;
	
	//private AudioClient mClient = null;
	private Worker      mWorker = null;
	private boolean  isSilent = false;
	private final  String TAG    = "AudioEncoder";
	private byte[] mOutputBytes   = null;
	private boolean isPaused = false;
	private byte[]   mSilentBuffer;
	private MediaMuxerWrapper mMuxerWrapper;
	MediaFormat mMediaFormat;
	int mTrackid;
	MediaCodec mEncoder;
	//private MediaMuxer mMuxer;
	private int mTrackIndex;
	public AudioAACEncoder( AudioClient client ) {
		//mClient = client;
/*
		mMediaFormat = MediaFormat.createAudioFormat(
				MIME_TYPE, KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT);
		mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
		mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
				KEY_AAC_PROFILE);
		mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);

		mEncoder.configure(mMediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
				*/
	}
	public void setMuxer(MediaMuxerWrapper muxerWrapper)
	{
		mMuxerWrapper = muxerWrapper;
		//MediaFormat newFormat = mEncoder.getOutputFormat();
		//mTrackid = mMuxerWrapper.addTrack(newFormat);
	}

	public void SetSilent( boolean silent ){
		isSilent = silent;
	}
	
	public void InitConfig( int bitrate , int samprate, int channel ){
		KEY_BIT_RATE = bitrate;
		KEY_SAMPLE_RATE = samprate;
		KEY_CHANNEL_COUNT = channel;
		if( KEY_CHANNEL_COUNT == 1 ){
			CHANNEL_MODE = AudioFormat.CHANNEL_IN_MONO;
		}else{
			CHANNEL_MODE = AudioFormat.CHANNEL_IN_STEREO;
		}
	}
	
	public void start() {
		if (mWorker == null) {
			mWorker = new Worker();
			mWorker.setRunning(true);
			mWorker.start();
			Log.e("MJHTEST", "AudioAACEncoder mWorker Start");
		}
	}

	public void Pause(){
		isPaused = true;
	}
	
	public void Resume(){
		isPaused = false;
	}
	
	public void stop() {
		if (mWorker != null) {
			mWorker.setRunning(false);
			mWorker = null;
		}
	}

	private class Worker extends Thread 
	{
		
		private boolean       isRunning = false;
		private MediaCodec    mEncoder;
		private AudioRecord   mRecord;
		MediaCodec.BufferInfo mBufferInfo;
		
		private byte[]        mReadBuffer;
		private byte[]        mCacheBuffer;
		int                   mReadedSize= 0;
		
		@Override
		public void run() {
			Log.e("MJHTEST", "AudioAACEncoder mWorker Run");
			if (!prepare()) {
				Log.d(TAG, "音频编码器初始化失败");
				isRunning = false;
			}
			
			while (isRunning) {
				int num = mRecord.read(mReadBuffer, 0, mReadBufferSize);
//				Log.d(TAG, "buffer = " + mReadBuffer.toString() + ", num = " + num);
				if( isPaused ){
					try {
						Thread.sleep( 200 );
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
				mReadedSize = num;
				while (mReadedSize > 0) {
					encode();
				}
			}
			release();
		}

		public void setRunning(boolean run) {
			isRunning = run;
		}

		/**
		 * 释放资源
		 */
		private void release() {
			if (mEncoder != null) {
				mEncoder.stop();
				mEncoder.release();
			}
			if (mRecord != null) {
				if( mRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING )
					mRecord.stop();
				mRecord.release();
				mRecord = null;
			}
			/*
			if (mMuxer != null) {
				// TODO: stop() throws an exception if you haven't fed it any data.  Keep track
				//       of frames submitted, and don't call stop() if we haven't written anything.
				mMuxer.stop();
				mMuxer.release();
				mMuxer = null;
			}
			*/

			if (mMuxerWrapper != null)
			{
				mMuxerWrapper.stop();
				mMuxerWrapper = null;
			}

		}

		/**
		 * 连接服务端，编码器配置
		 * 
		 * @return true配置成功，false配置失败
		 */
		private boolean prepare() {
			Log.e("MJHTEST", "AudioAACEncoder mWorker prepare");
			try {
				mBufferInfo = new MediaCodec.BufferInfo();
				mMediaFormat = MediaFormat.createAudioFormat(
						MIME_TYPE, KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT);
				mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
				mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
						KEY_AAC_PROFILE);
				mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);

				mEncoder.configure(mMediaFormat, null, null,
						MediaCodec.CONFIGURE_FLAG_ENCODE);
				//mClient.OnMediaFormat(mMediaFormat);
				mEncoder.start();

			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			
			int minBufferSize = AudioRecord.getMinBufferSize(KEY_SAMPLE_RATE,
					CHANNEL_MODE, AUDIO_FORMAT);
			int mRecvSize = (((minBufferSize-1)/4096) + 1 )*4096;
			mReadBufferSize = mRecvSize;
			
			mReadBuffer   = new byte[ mReadBufferSize*2  ];
			mCacheBuffer  = new byte[ mReadBufferSize*2 ];
			
			mReadedSize  = 0;
			mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					KEY_SAMPLE_RATE, CHANNEL_MODE, AUDIO_FORMAT,
					mReadBufferSize );
			
			mSilentBuffer = new byte[mReadBufferSize];
			for( int i= 0; i < mReadBufferSize ; i ++ ){
				mSilentBuffer[ i ] = 0;
			}
			
			try{
			   mRecord.startRecording();
			}catch( Exception e ){
				//mClient.OnAudioStatus( 2 );
			}
			return true;
		}

		private void encode(  )
		{
			int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);

			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = mEncoder.getInputBuffers()[ inputBufferIndex ];
				inputBuffer.clear();
				
				int remainSize = inputBuffer.remaining();
				
				byte[] encodebyte = null;

				if (remainSize < mReadedSize) {
					encodebyte = new byte[remainSize];
					System.arraycopy(mReadBuffer, 0, encodebyte, 0, remainSize);
					mReadedSize -= remainSize;

					System.arraycopy(mReadBuffer, remainSize, mCacheBuffer, 0,
							mReadedSize);
					byte[] refBuffer = mReadBuffer;
					mReadBuffer = mCacheBuffer;
					mCacheBuffer = refBuffer;
				} else {
					encodebyte = new byte[mReadedSize];
					System.arraycopy(mReadBuffer, 0, encodebyte, 0, mReadedSize);
					mReadedSize = 0;
				}
				
				if( isSilent ){
					System.arraycopy( mSilentBuffer , 0, encodebyte, 0, encodebyte.length);
				}
				
				inputBuffer.put( encodebyte );
				inputBuffer.limit( encodebyte.length );
				
				mEncoder.queueInputBuffer(inputBufferIndex, 0, encodebyte.length,
						System.nanoTime(), 0);
			}

			int outputBufferIndex = mEncoder
					.dequeueOutputBuffer(mBufferInfo, 0);
			if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// should happen before receiving buffers, and should only happen once

				MediaFormat newFormat = mEncoder.getOutputFormat();
				Log.d(TAG, "encoder output format changed: " + newFormat);

				// now that we have the Magic Goodies, start the muxer


                    //mTrackIndex = mMuxer.addTrack(newFormat);
                    // mMuxer.start();

				mTrackIndex = mMuxerWrapper.addTrack(newFormat);
				mMuxerWrapper.start();


			}
			while (outputBufferIndex >= 0) 
			{
				ByteBuffer outputBuffer = mEncoder
						.getOutputBuffers()[outputBufferIndex];
				int length = mBufferInfo.size;
				if (mOutputBytes == null || mOutputBytes.length < length) {
					mOutputBytes = new byte[length];
				}
				
				outputBuffer.get(mOutputBytes, 0, mBufferInfo.size);
				/*
				if( mClient !=null ){
					mClient.OnAACData(mOutputBytes, length);
				}
				*/
				mBufferInfo.presentationTimeUs = getPTSUs();
				//mMuxer.writeSampleData(mTrackIndex, outputBuffer,
				//mBufferInfo);
				Log.d(TAG, "writeSampleData : " + mBufferInfo.toString());


				mMuxerWrapper.writeSampleData(mTrackIndex, outputBuffer,
						mBufferInfo);

				prevOutputPTSUs = mBufferInfo.presentationTimeUs;
				mEncoder.releaseOutputBuffer(outputBufferIndex, false);
				outputBufferIndex = mEncoder
						.dequeueOutputBuffer(mBufferInfo, 0);
			}
		}

		/**
		 * previous presentationTimeUs for writing
		 */
		private long prevOutputPTSUs = 0;
		/**
		 * get next encoding presentationTimeUs
		 * @return
		 */
		protected long getPTSUs() {
			long result = System.nanoTime() / 1000L;
			// presentationTimeUs should be monotonic
			// otherwise muxer fail to write
			if (result < prevOutputPTSUs)
				result = (prevOutputPTSUs - result) + result;
			return result;
		}
	}
}
