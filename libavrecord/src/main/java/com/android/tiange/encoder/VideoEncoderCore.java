/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tiange.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.android.tiange.display.VideoEchoDisplay.IVideoCallbak;

/**
 * This class wraps up the core components used for surface-input video encoding.
 * <p>
 * Once created, frames are fed to the input surface.  Remember to provide the presentation
 * time stamp, and always call drainEncoder() before swapBuffers() to ensure that the
 * producer side doesn't get backed up.
 * <p>
 * This class is not thread-safe, with one exception: it is valid to use the input surface
 * on one thread, and drain the output on a different thread.
 */
public class VideoEncoderCore {
    private static final String TAG = "VideoEncoderCore";
    private static final boolean VERBOSE = true;

    // TODO: these ought to be configurable as well
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 20;               // 20fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames

    private Surface mInputSurface;
    private MediaCodec mEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private int mTrackIndex;
    private boolean mMuxerStarted;
    
    private IVideoCallbak mDataCallback;
    private MediaMuxerWrapper mMuxerWrapper;
    //private MediaMuxer mMuxer;

    private byte[] mSpsPPSInfo = null; 
    public  byte[] mOutputData = null;
    public  int    mOutputLen  = 0;
    
    boolean isRecordInit = true;
    
    boolean isRecvMediaData = false;
    //alalalalaalall

    public void setMuxer(MediaMuxerWrapper muxerWrapper)
    {
        mMuxerWrapper = muxerWrapper;
    }
    /**
     * Configures encoder and muxer state, and prepares the input Surface.
     */
    @SuppressLint("InlinedApi")
	public VideoEncoderCore(int width, int height, int bitRate, IVideoCallbak outputFile)
            throws IOException {
        mBufferInfo = new MediaCodec.BufferInfo();

        mOutputData = new byte[ width*height * 3 ];
        mDataCallback = outputFile;
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        //if (VERBOSE) Log.d(TAG, "format: " + format);

        Log.e("MJH", "+++++++format OnMediaFormat= " + format);
        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        //mDataCallback.OnMediaFormat(format);
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        try{
        	mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch( Exception e ){
            /*
        	if( mDataCallback!= null ){
        		mDataCallback.OnEncodeStatus( 0 );
        	}
        	*/
        }
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        // Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
        
        if( isRecordInit ){
            //Log.e(TAG, "VideoEncoderCore: "+ outputFile1.toString());

            //mMuxer = new MediaMuxer(getCaptureFile(Environment.DIRECTORY_MOVIES, ".mp4").toString(),
            //    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        }
        mTrackIndex = -1;
        mMuxerStarted = false;
    }

    /*
    public void Start()
    {
        isRecordInit = true;
    }
    public void Stop()
    {
        isRecordInit = false;
    }
    */
    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Releases encoder resources.
     */
    public void release() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }


        if (mMuxerWrapper != null)
        {
            mMuxerWrapper.stop();
            mMuxerWrapper = null;
        }
        /*
        if (mMuxer != null)
        {
            mMuxer.stop();
            mMuxer = null;
        }
        */
    }

    private long mPrevstamp = 0;
    private int  mFpsIndex = 0;
    private int  mOutputFpsNum = 0;
    
    public int getOutputFPS(){
    	return mOutputFpsNum;
    }
    
    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */
    public void drainEncoder(boolean endOfStream) {
        if (mMuxerWrapper.GetStartedCount() == 0)
            return;
    	int m_useLength = 0;
        final int TIMEOUT_USEC = 10000;
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        while (true) 
        {
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                if( isRecordInit ){

                    mTrackIndex = mMuxerWrapper.addTrack(newFormat);
                    mMuxerWrapper.start();

/*
                    mTrackIndex = mMuxer.addTrack(newFormat);
                     mMuxer.start();
*/
                }
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else { 	
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                //get config information about h264 ,sps_pps info
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                	byte[] outData = new byte[ mBufferInfo.size ];
            		encodedData.get(outData);	
					ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
					if (spsPpsBuffer.getInt() == 0x00000001) {
						mSpsPPSInfo = new byte[outData.length];
						System.arraycopy(outData, 0, mSpsPPSInfo, 0,
								outData.length);
						Log.d(TAG, "get sps pps buffer success!!!"
								+ outData.length);
					} else {
						Log.d(TAG, "get sps pps buffer fail!!!");
					}
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                	isRecvMediaData = true;
                	byte[] outData = new byte[ mBufferInfo.size ];
                	
					// adjust the ByteBuffer values to match BufferInfo (not
					// needed?)
					encodedData.position(mBufferInfo.offset);
					encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
					encodedData.get(outData);
					 
					if( mOutputData.length < mBufferInfo.size ){
						mOutputData = new byte[ mBufferInfo.size ];
					}
					System.arraycopy(outData, 0, mOutputData, m_useLength,
							mBufferInfo.size);
					
					m_useLength += mBufferInfo.size;
					
					if( isRecordInit ){
						encodedData.position(mBufferInfo.offset);
                        //Log.e(TAG, "mMuxer.writeSampleData size = " + mBufferInfo.size);
                        mMuxerWrapper.writeSampleData(mTrackIndex, encodedData,
                                mBufferInfo);
                        //mMuxer.writeSampleData(mTrackIndex, encodedData,
					    // mBufferInfo);
					}
					// Log.d(TAG, "sent " + mBufferInfo.size + " m_useLength=" +
					// m_useLength);
					if (VERBOSE) {
						Log.d(TAG, "sent " + mBufferInfo.size
								+ " bytes to muxer, ts="
								+ mBufferInfo.presentationTimeUs);
					}
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
        
        if( isRecvMediaData )
        {            
//        	int index = 0;
            while( true ){
            	int result = ParseData( mOutputData , m_useLength );
//            	index ++;
            	//get last video frame
            	if( result == m_useLength )
            	{
            		DeliverData( mOutputData , m_useLength );
            		break;
            	}else //has more than one frame!!!
            	{
            		byte[] remian = new byte[ m_useLength -result  ];
            		System.arraycopy(mOutputData, result, remian, 0 , remian.length);
            		DeliverData( mOutputData , result );
//            		Log.i("", index + "*************************get remain data!!!!!!***************************" + result + " == " + m_useLength);
            		System.arraycopy(remian, 0, mOutputData, 0, remian.length);
            		m_useLength = remian.length;
				}
            }
        }
    }
    
    //deliver data to server
	private void DeliverData(byte[] mOutputData, int m_useLength) {
		byte[] byteOutput = mOutputData;
		int m_keyFlag = 0;
		if (mOutputData[4] == 0x65 || mOutputData[4] == 0x25) {
			byteOutput = new byte[m_useLength + mSpsPPSInfo.length];
			m_keyFlag = 1;
			System.arraycopy(mSpsPPSInfo, 0, byteOutput, 0, mSpsPPSInfo.length);
			System.arraycopy(mOutputData, 0, byteOutput, mSpsPPSInfo.length, m_useLength);
			m_useLength += mSpsPPSInfo.length;
		}
		if (mDataCallback != null && m_useLength != 0) {
			isRecvMediaData = false;
			
			long currentTimeStamp = System.currentTimeMillis();
			if( mPrevstamp == 0 ){
				mPrevstamp = currentTimeStamp;
			}
			
			mFpsIndex ++;
			if( currentTimeStamp - mPrevstamp >= 500 ){
				mOutputFpsNum = mFpsIndex*2;
				mFpsIndex = 0;
				mPrevstamp = currentTimeStamp;
			}
			
			//mDataCallback.OnH264Data(byteOutput, m_useLength, m_keyFlag);
		}
	}

	//look for next nalu data
	private int ParseData(byte[] dataIn, int length) {
		int index = 4;
		while (index + 4 <= length) {
			if (dataIn[index] == 0 && dataIn[index + 1] == 0 && dataIn[index + 2] == 1) {
				break;
			}
			if (dataIn[index] == 0 && dataIn[index + 1] == 0 && dataIn[index + 2] == 0 && dataIn[index + 3] == 1) {
				break;
			}
			index++;
		}
		if (index + 4 >= length) {
			return length;
		}
		return index;
	}
    /**
     * generate output file
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public File getCaptureFile(final String type, final String ext) {
        String DIR_NAME = "AVRecSample";

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
    private String getDateTimeString() {
        SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }
}
