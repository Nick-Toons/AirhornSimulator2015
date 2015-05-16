package com.herrschreiber.airhornsimulator2015;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * Created by alex on 4/6/15.
 */
public class AndroidAudioPlayer implements AudioProcessor {
    private static final String TAG = "AndroidAudioPlayer";
    private final int bufferSize;
    private final TarsosDSPAudioFormat format;
    private AudioTrack audioTrack;
    private int channelConfig;
    private int encoding;

    public AndroidAudioPlayer(TarsosDSPAudioFormat audioFormat, int bufferSize) {
        format = audioFormat;
        channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        encoding = AudioFormat.ENCODING_PCM_16BIT;
        this.bufferSize = Math.max(bufferSize, AudioTrack.getMinBufferSize((int) format.getSampleRate(),
                channelConfig, encoding));
        Log.i(TAG, "AndroidAudioPlayer(audioFormat=\"" + audioFormat + "\", bufferSize=\"" + this.bufferSize + "\")");
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        if (audioTrack == null) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, (int) format.getSampleRate(),
                    channelConfig, encoding, bufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
            Log.i(TAG, "audioTrack=\"" + audioTrack + "\"");
        }
        short[] buffer = new short[audioEvent.getBufferSize()];
        ByteBuffer.wrap(audioEvent.getByteBuffer()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(buffer);
        Log.i(TAG, "overlap: " + audioEvent.getOverlap() + ", bufferSize: " + audioEvent.getBufferSize());
        int offset = audioEvent.getOverlap();
        int size = audioEvent.getBufferSize() - offset;
        Log.i(TAG, "length: " + buffer.length + ", offset: " + offset + ", size: " + size);
        int ret = audioTrack.write(buffer, offset, size);
        if (ret < 0) {
            Log.e(TAG, "AudioTrack.write returned error code " + ret);
        }
        return true;
    }

    @Override
    public void processingFinished() {
        Log.w(TAG, "processingFinished");
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }
}