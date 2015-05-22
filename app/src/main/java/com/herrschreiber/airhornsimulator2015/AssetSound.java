package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import com.google.common.math.DoubleMath;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;

/**
 * Created by alex on 5/6/15.
 */
public class AssetSound extends Sound {
    public static final String AUDIO_PATH = "Sounds";
    public static final String ICON_PATH = "Icons";
    public static final String TAG = "Sound";
    public static final long TIMEOUT_US = 1000;
    public static final int NO_OUTPUT_COUNTER_LIMIT = 50;
    public static final int AUDIO_BUFFER_SIZE = 1024;
    private String path;
    private Context context;
    private Drawable icon;
    private MediaExtractor extractor;
    private int channelCount;
    private MediaFormat mediaFormat;
    private String mime;
    private ByteArrayOutputStream byteStream;
    private double averagePitch;

    public AssetSound(String path, Context context) throws IOException {
        super(nameFromPath(path));
        this.path = path;
        this.context = context;
        String iconPath = new File(ICON_PATH, getName() + ".png").getPath();
        icon = Drawable.createFromStream(context.getAssets().open(iconPath), null);
        if (icon == null) {
            icon = context.getResources().getDrawable(R.drawable.ic_image_audiotrack);
        }

        Log.i(TAG, "Created sound " + this.toString());
    }

    private static String nameFromPath(String path) {
        int pos = path.indexOf('.');
        if (pos != -1) {
            path = path.substring(0, pos);
        }
        return path;
    }

    @Override
    public void init() throws IOException {

        extractor = new MediaExtractor();
        AssetFileDescriptor fd = context.getAssets().openFd(new File(AUDIO_PATH, path).getPath());
        extractor.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
        fd.close();

        if (extractor.getTrackCount() != 1) {
            throw new IllegalArgumentException("File does not have one track: " + getName());
        }

        mediaFormat = extractor.getTrackFormat(0);
        mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        if (!mime.startsWith("audio/")) {
            throw new Error("Not an audio file: " + getName());
        }

        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        setAudioFormat(new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false));
        byteStream = new ByteArrayOutputStream();

        Log.d(TAG, "Decoding sound " + this.getName());
        this.decode();
        Log.d(TAG, "Finished decoding sound " + getName() + ". Duration: " + getDuration());

        Log.d(TAG, "Calculating average pitch for sound sound " + this.getName());
        calculateAveragePitch();
        Log.i(TAG, "Finisehd decoding and calculating average pitch for sound " + getName() +
                ". Average pitch: " + averagePitch);
    }

    protected void decode() {
        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        MediaCodec decoder;
        try {
            decoder = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            Log.e(TAG, "Error creating decoder for Sound '" + getName() + "'", e);
            return;
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
        boolean stop = false;
        while (!sawOutputEOS && noOutputCounter < NO_OUTPUT_COUNTER_LIMIT && !stop) {
            noOutputCounter++;
            if (!sawInputEOS) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = inputBuffers[inputBufIndex];

                    int sampleSize = extractor.readSampleData(dstBuf, 0);

                    long presentationTimeUs = 0;

                    if (sampleSize < 0) {
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

                final byte[] monoChunk = stereoToMono(chunk);

                if (chunk.length > 0) {
                    try {
                        byteStream.write(monoChunk);
                    } catch (IOException e) {
                        Log.e(TAG, "Error writing to buffer stream", e);
                        stop = true;
                    }
                }
                decoder.releaseOutputBuffer(res, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = decoder.getOutputBuffers();
            }
        }

        buffer = byteStream.toByteArray();

        decoder.stop();
        decoder.release();
        decoder = null;
    }

    public double getAveragePitch() {
        return averagePitch;
    }

    private void calculateAveragePitch() {
        final List<Float> pitches = new ArrayList<>();
        this.start();
        AudioDispatcher audioDispatcher = new AudioDispatcher(this, AUDIO_BUFFER_SIZE, 0);
        audioDispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.FFT_YIN,
                this.getSampleRate(), AUDIO_BUFFER_SIZE, new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                if (pitchDetectionResult.isPitched()) {
                    pitches.add(pitchDetectionResult.getPitch());
                }
            }
        }));
        audioDispatcher.run();
        if (pitches.isEmpty()) {
            averagePitch = 120; // just a guess
        } else {
            averagePitch = DoubleMath.mean(pitches);
        }
    }

    private byte[] stereoToMono(byte[] chunk) {
        byte[] monoChunk;
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
        return monoChunk;
    }

    @Override
    public String toString() {
        return "AssetSound{" + "name='" + getName() + '\'' + '}';
    }

    public Drawable getIcon() {
        return icon;
    }
}
