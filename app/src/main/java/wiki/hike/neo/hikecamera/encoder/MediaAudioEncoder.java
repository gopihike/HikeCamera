package wiki.hike.neo.hikecamera.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaAudioEncoder.java
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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class MediaAudioEncoder extends MediaEncoder {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "MediaAudioEncoder";

	private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int BIT_RATE = 64000;
	public static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
	public static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec
	private static String mfilterAudioPath = null;

    private AudioThread mAudioThread = null;
	public ArrayList<SampleData> mSampleQueue;
	public Mp3Decoder mp3decoder;
	public boolean isDecoded = false;
	int mIndexCountRequested = 0;

	public MediaAudioEncoder(MediaMuxerWrapper muxer, MediaEncoderListener listener, String filterAudioPath) {
		super(muxer, listener);
		Log.d("constructor","constructorrrrrrrrr");
		if(!TextUtils.isEmpty(filterAudioPath)) {
            mfilterAudioPath = filterAudioPath;
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected void prepare() throws IOException {
		if (DEBUG) Log.v(TAG, "prepare:");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;
        // prepare MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
		if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
		if (DEBUG) Log.i(TAG, "format: " + audioFormat);
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
        	try {
        		mListener.onPrepared(this);
        	} catch (final Exception e) {
        		Log.e(TAG, "prepare:", e);
        	}
        }
	}

    @Override
	protected void startRecording() {
		super.startRecording();
		// create and execute audio capturing thread using internal mic
		if(!TextUtils.isEmpty(mfilterAudioPath)) {
			mSampleQueue = new ArrayList<>();
			mp3decoder = new Mp3Decoder(mSampleQueue);
			mp3decoder.start();
		}
		if (mAudioThread == null) {
	        mAudioThread = new AudioThread();
			mAudioThread.start();
		}
	}

	@Override
    protected void release() {
		mAudioThread = null;
		mp3decoder = null;
        mfilterAudioPath = null;
		super.release();
    }

	private static final int[] AUDIO_SOURCES = new int[] {
		MediaRecorder.AudioSource.MIC,
		MediaRecorder.AudioSource.DEFAULT,
		MediaRecorder.AudioSource.CAMCORDER,
		MediaRecorder.AudioSource.VOICE_COMMUNICATION,
		MediaRecorder.AudioSource.VOICE_RECOGNITION,
	};

	/**
	 * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
	 * and write them to the MediaCodec encoder
	 */
    private class AudioThread extends Thread {
    	@Override
    	public void run() {
    		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    		try {
				final int min_buffer_size = AudioRecord.getMinBufferSize(
					SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
				int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
				if (buffer_size < min_buffer_size)
					buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

				AudioRecord audioRecord = null;
				for (final int source : AUDIO_SOURCES) {
					try {
						audioRecord = new AudioRecord(
							source, SAMPLE_RATE,
							AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
	    	            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
	    	            	audioRecord = null;
					} catch (final Exception e) {
						audioRecord = null;
					}
					if (audioRecord != null) break;
				}
				if (audioRecord != null) {
		            try {
						if (mIsCapturing) {
		    				if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
							final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
							ByteBuffer mp3buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
			                int readBytes;
			                audioRecord.startRecording();
			                try {
					    		for (; mIsCapturing && !mRequestStop && !mIsEOS ;) {
					    			// read audio data from internal mic
									if(!TextUtils.isEmpty(mfilterAudioPath)){
										mp3buf.clear();
										byte[] mp3data = read();
										if (mp3data != null) {
											// set audio data to encoder
											mp3buf.put(mp3data);
											mp3buf.flip();
											Log.d("Dataaaaaa",""+mp3data.length);
											encode(mp3buf,SAMPLES_PER_FRAME,getPTSUs());
											frameAvailableSoon();
										}
									}
									else{
										buf.clear();
										readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
										if (readBytes > 0) {
											// set audio data to encoder
											Log.d("Data",""+readBytes);
											buf.position(readBytes);
											buf.flip();
											encode(buf, readBytes, getPTSUs());
											frameAvailableSoon();
										}
									}
					    		}
								Log.d("stopped","stoppeddddddddddddddddd");
			    				frameAvailableSoon();
			                } finally {
			                	audioRecord.stop();
			                }
		            	}
		            } finally {
		            	audioRecord.release();
		            }
				} else {
					Log.e(TAG, "failed to initialize AudioRecord");
				}
    		} catch (final Exception e) {
    			Log.e(TAG, "AudioThread#run", e);
    		}
			if (DEBUG) Log.v(TAG, "AudioThread:finished");
    	}

		public byte[] read() {

			if (isDecoded && (mIndexCountRequested == mSampleQueue.size())){
				mIndexCountRequested = 0;
				Log.d("indexreset","indexreset");
			}


			if (mSampleQueue.size() > mIndexCountRequested) {
				Log.d("queuesize",""+mSampleQueue.size()+" "+mIndexCountRequested);
				SampleData Data = mSampleQueue.get(mIndexCountRequested);
				mIndexCountRequested++;
				return Data.buffer;
			}

			return null;
		}
    }

	/**
	 * select the first codec that match a specific MIME type
	 * @param mimeType
	 * @return
	 */
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
		if (DEBUG) Log.v(TAG, "selectAudioCodec:");

		MediaCodecInfo result = null;
		// get the list of available codecs
		final int numCodecs = MediaCodecList.getCodecCount();
		LOOP:	for (int i = 0; i < numCodecs; i++) {
			final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {	// skipp decoder
				continue;
			}
			final String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
				if (types[j].equalsIgnoreCase(mimeType)) {
					if (result == null) {
						result = codecInfo;
						break LOOP;
					}
				}
			}
		}
		return result;
	}

	public static class SampleData{
		byte[] buffer;
		int size;
		public SampleData(byte[] buffer,int size){
			this.buffer=buffer;
			this.size=size;
		}
	}

	class Mp3Decoder extends Thread {

		private static final String MIME_TYPE = "audio/mpeg";
		private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
		private static final int BIT_RATE = 64000;
		public static final int SAMPLES_PER_FRAME = 1024;    // AAC, bytes/frame/channel
		public static final int FRAMES_PER_BUFFER = 25;    // AAC, frame/buffer/sec
		private static final int SAMPLES_PER_FRAME_AUDIO = 2304;
		private String TAG = "Mp3Decoder";
		public ArrayList<SampleData> queue;
		public boolean stop=false;


		public Mp3Decoder(ArrayList<SampleData> bufQueue) {
			this.queue = bufQueue;
		}


		@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
		@Override
		public void run() {

			//Thread.currentThread().setPriority(MAX_PRIORITY);
			MediaExtractor extractor = new MediaExtractor();
			try {
				extractor.setDataSource(mfilterAudioPath);


				MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 2);
				//format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
				//format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

				MediaCodec codec = MediaCodec.createDecoderByType(MIME_TYPE);

				codec.configure(format, null, null, 0);
				codec.start();

				ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
				ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();


				Log.d("filepathhhhh",""+mfilterAudioPath);

				extractor.selectTrack(0);

				MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
				boolean inputEos = false;
				boolean outputEos = false;
				long timeoutUs = 5000;
				int index = 0 ;
				byte[] prev=null;
				byte[] first ;
				int start_index=0;
				int rem;


				while (!outputEos && !mRequestStop) {
					Log.v(TAG, "encoding... " + inputEos + ", " + outputEos);
					if (!inputEos) {

						int inputBufIndex = codec.dequeueInputBuffer(timeoutUs);

						if (inputBufIndex >= 0) {
							ByteBuffer buf = codecInputBuffers[inputBufIndex];

							int sampleSize = extractor.readSampleData(buf, 0);


							long presentationTimeUs = 0;
							if (sampleSize < 0) {
								Log.d(TAG, "input EOS.");
								inputEos = true;
								sampleSize = 0;
							} else {
								presentationTimeUs = extractor.getSampleTime();
							}

							codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs,
									inputEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
							if (!inputEos) {
								extractor.advance();
							}
						}
					}

					int res = codec.dequeueOutputBuffer(info, timeoutUs);
					Log.d("dsttttttttt", "" + res + " " + info.size);

					if (res >= 0) {
						int outputBufIndex = res;
						ByteBuffer buf = codecOutputBuffers[outputBufIndex];
						byte[] dst = new byte[info.size];
						int oldPosition = buf.position();


						buf.get(dst);
						buf.position(oldPosition);

						byte[] dst1 = new byte[dst.length / 2];

						Log.d("sizessss", dst.length + " " + dst1.length);


						for (int i = 0; i < dst1.length; i = i + 2) {
							dst1[i] = dst[2 * i];
							dst1[i + 1] = dst[2 * i + 1];
						}

						rem = dst1.length;

						if(prev != null){
							start_index = 1024-prev.length;
							byte[] temp= Arrays.copyOfRange(dst1,0,start_index);
							ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
							tempStream.write(prev);
							tempStream.write(temp);
							queue.add(new MediaAudioEncoder.SampleData(tempStream.toByteArray(),dst1.length));
							index++;
							rem = dst1.length-start_index;
							prev = null;
						}

						while(rem >= 1024){
							first = Arrays.copyOfRange(dst1,start_index,start_index+1024);
							queue.add(new MediaAudioEncoder.SampleData(first,dst1.length));
							index++;

							start_index = start_index+1024;
							rem = rem - start_index;
						}
						if(rem==0){
							prev = null;
						}
						else{
							prev = Arrays.copyOfRange(dst1,start_index,dst1.length);
						}

						codec.releaseOutputBuffer(outputBufIndex, false);

						if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
							Log.d(TAG, "output eos.");
							outputEos = true;
						}
					} else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						Log.d(TAG, "output buffer changed.");
						codecOutputBuffers = codec.getOutputBuffers();
					}
				}
				isDecoded = true;
				Log.d("stopped","stopped");
				codec.stop();
				codec.release();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
