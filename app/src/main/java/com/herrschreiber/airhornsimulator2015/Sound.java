package com.herrschreiber.airhornsimulator2015;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

/**
 * Created by alex on 5/6/15.
 */
public class Sound implements TarsosDSPAudioInputStream {
    public static final String TAG = "Sound";
    public static final long TIMEOUT_US = 1000;
    public static final int NO_OUTPUT_COUNTER_LIMIT = 50;
    private final String name;
    private final MediaExtractor extractor;
    private final int channelCount;
    private MediaFormat mediaFormat;
    private String mime;
    private MediaCodec decoder;
    private TarsosDSPAudioFormat audioFormat;
    private AsyncTask<Void, Void, Void> decoderTask;
    private boolean stop;
    private ByteArrayOutputStream byteStream;
    private ByteArrayInputStream byteInput;

    public Sound(String name, AssetFileDescriptor fd) throws IOException {
        this.name = name;

        extractor = new MediaExtractor();
        extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        fd.close();

        if (extractor.getTrackCount() != 1) {
            throw new Error("File does not have one track: " + name);
        }

        mediaFormat = extractor.getTrackFormat(0);
        mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        if (!mime.startsWith("audio/")) {
            throw new Error("Not an audio file: " + name);
        }

        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        audioFormat = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);

        byteStream = new ByteArrayOutputStream();

        Log.i(TAG, "Created sound " + this.toString());
        this.decode();
    }

    private void decode() {
        stop = false;
        decoderTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Sound.this.decoderLoop();
                return null;
            }
        };
        decoderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public String getName() {
        return name;
    }

    public void start() {
        byteInput.reset();
    }

    protected void decoderLoop() {
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        try {
            decoder = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            Log.e(TAG, "Error creating decoder for Sound '" + name + "'", e);
        }
        decoder.configure(mediaFormat, null, null, 0);
        decoder.start();

        extractor.selectTrack(0);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = decoder.getInputBuffers();
        ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        while (!sawOutputEOS && noOutputCounter < NO_OUTPUT_COUNTER_LIMIT && !stop) {
            noOutputCounter++;
            if (!sawInputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = inputBuffers[inputBufIndex];

                    int sampleSize = extractor.readSampleData(dstBuf, 0);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {
                        Log.d(TAG, "Input EOS");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    // can throw illegal state exception (???)
                    decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                } else {
                    Log.e(TAG, "inputBufIndex " + inputBufIndex);
                }
            }
            int res = decoder.dequeueOutputBuffer(info, TIMEOUT_US);
            if (res >= 0) {
                if (info.size > 0) {
                    noOutputCounter = 0;
                }

                ByteBuffer buf = outputBuffers[res];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();

                final byte[] monoChunk;
                if (channelCount == 2) {
                    short[] shorts = new short[chunk.length / 2];
                    short[] monoShorts = new short[shorts.length / 2];
                    ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                    for (int i = 0; i < monoShorts.length; ++i) {
                        short left = shorts[2 * i];
                        short right = shorts[2 * i + 1];
                        int mono = (left + right) / 2;
                        monoShorts[i] = (short) mono;
                    }
                    monoChunk = new byte[monoShorts.length * 2];
                    ByteBuffer.wrap(monoChunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(monoShorts);
                } else if (channelCount == 1) {
                    monoChunk = chunk;
                } else {
                    throw new IllegalStateException("Only supports sounds with 1 or 2 channels");
                }

                if (chunk.length > 0) {
                    try {
                        byteStream.write(monoChunk);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to audio pipe", e);
                        stop = true;
                    }
                }
                decoder.releaseOutputBuffer(res, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "Output EOS");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.getOutputBuffers();
                Log.d(TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = decoder.getOutputFormat();
                Log.d(TAG, "output format has changed to " + oformat);
            } else {
                Log.d(TAG, "dequeueOutputBuffer returned " + res);
            }
        }

        byte[] buffer = byteStream.toByteArray();
        byteInput = new ByteArrayInputStream(buffer);
        Log.d(TAG, "Finished decoding sound " + name);

        releaseResources(true);
        stop = true;
    }

    private void releaseResources(Boolean release) {
        if (decoder != null) {
            if (release) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
        }
    }

    public float getSampleRate() {
        return getFormat().getSampleRate();
    }

    public int getChannels() {
        return getFormat().getChannels();
    }

    @Override
    public String toString() {
        return "SoundPlayer.Sound[" +
                "name='" + name + '\'' +
                ", extractor=" + extractor +
                ", decoder=" + decoder +
                ", mediaFormat=" + mediaFormat + ']';
    }

    @Override
    public long skip(long l) throws IOException {
        return byteInput.skip(l);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return byteInput.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        releaseResources(false);
    }

    @Override
    public TarsosDSPAudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public long getFrameLength() {
        return -1;
    }
}
