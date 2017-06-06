package com.android.tiange.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaMuxerWrapper.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class MediaMuxerWrapper {

	public interface IStatusCallbak{
		void OnRecordTime(long nTime);//毫秒
	}
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "MediaMuxerWrapper";

	private static final String DIR_NAME = "AVRecSample";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

	private String mOutputPath;
	private final MediaMuxer mMediaMuxer;	// API >= 18
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private IStatusCallbak mStatusCallback;

	long mStartTick      = 0;
	long mAudioCurTick   = 0;
	long mVideoCurTick   = 0;
	//private MediaEncoder mVideoEncoder, mAudioEncoder;

	/**
	 * Constructor
	 * @param ext extension of output file
	 * @throws IOException
	 */
	public MediaMuxerWrapper(String filepath) throws IOException {

		Log.v(TAG, "new mMediaMuxer");

		mMediaMuxer = new MediaMuxer(filepath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		if (mMediaMuxer != null)
		{
			Log.v(TAG, "mMediaMuxer NOT NULL = " + mMediaMuxer);
		}
		else
		{
			Log.v(TAG, "mMediaMuxer IS NULL + " + mMediaMuxer);
		}
		mEncoderCount = 2;
		mStatredCount = 0;
		mIsStarted = false;
	}

	public void SetCallback(IStatusCallbak callbak)
	{
		mStatusCallback = callbak;
	}
	public String getOutputPath() {
		return mOutputPath;
	}
	public void SetOrientationHint(int nDegrees)
	{
		if (mMediaMuxer != null)
		{
			Log.e("MJHTEST", "setOrientationHint nDegrees = " + nDegrees);
			mMediaMuxer.setOrientationHint(nDegrees);
		}

	}
	/*
	public void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

	public void stopRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.stopRecording();
		mVideoEncoder = null;
		if (mAudioEncoder != null)
			mAudioEncoder.stopRecording();
		mAudioEncoder = null;
	}
*/
	public synchronized boolean isStarted() {
		return mIsStarted;
	}

//**********************************************************************
//**********************************************************************
	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
	 */
	/*package*/
	/*
	void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoder) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		} else if (encoder instanceof MediaAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}
*/
	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/ synchronized public boolean start() {
		Log.e(TAG,  "start:");
		mVideoCurTick = mStartTick = mAudioCurTick = 0;
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			Log.e(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	*/
	/*package*/ synchronized public void stop() {
		if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			mMediaMuxer.stop();
			mMediaMuxer.release();
			mIsStarted = false;
			Log.e(TAG,  "MediaMuxer stopped:");
		}
		mVideoCurTick = mStartTick = mAudioCurTick = 0;

	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ synchronized int addTrack(final MediaFormat format) {
		if (mIsStarted)
			throw new IllegalStateException("muxer already started");
		final int trackIx = mMediaMuxer.addTrack(format);
		Log.e(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
		return trackIx;
	}
	public int GetStartedCount()
	{
		return mStatredCount;
	}
	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/ synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mIsStarted) {
			mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
			if (mStartTick == 0 && trackIndex == 0)
			{
				mStartTick = bufferInfo.presentationTimeUs;

			}
			if (trackIndex == 0)
			{
				mStatusCallback.OnRecordTime((bufferInfo.presentationTimeUs - mStartTick)/1000);
			}
			//Log.e(TAG, "bufferInfo.presentationTimeUs = " + bufferInfo.presentationTimeUs);
		}
	}

//**********************************************************************
//**********************************************************************
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

}
