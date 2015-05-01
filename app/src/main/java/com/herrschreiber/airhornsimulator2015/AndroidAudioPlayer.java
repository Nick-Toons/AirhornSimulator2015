package com.herrschreiber.airhornsimulator2015;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

/**
 * Created by alex on 4/6/15.
 */
public class AndroidAudioPlayer implements AudioProcessor {
    private static final String TAG = "AndroidAudioPlayer";
    private AudioTrack audioTrack;
    private final TarsosDSPAudioFormat format;

    public AndroidAudioPlayer(TarsosDSPAudioFormat audioFormat, int bufferSize) {
        format = audioFormat;
        int channelConfig;
        if (format.getChannels() == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (format.getChannels() == 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            throw new IllegalArgumentException("Unsupported channel count " + format.getChannels());
        }
        int encoding;
        if (audioFormat.getEncoding() == TarsosDSPAudioFormat.Encoding.PCM_SIGNED ||
                audioFormat.getEncoding() == TarsosDSPAudioFormat.Encoding.PCM_UNSIGNED) {
            if (format.getSampleSizeInBits() == 16) {
                encoding = AudioFormat.ENCODING_PCM_16BIT;
            } else if (format.getSampleRate() == 8) {
                encoding = AudioFormat.ENCODING_PCM_8BIT;
            } else {
                throw new IllegalArgumentException("Unsupported sample size " + format.getSampleSizeInBits());
            }
        } else {
            throw new IllegalArgumentException("Unsupported encoding " + audioFormat.getEncoding());
        }
        encoding = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = Math.max(bufferSize, AudioTrack.getMinBufferSize((int) format.getSampleRate(),
                channelConfig, encoding));
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, (int) format.getSampleRate(),
                channelConfig, encoding, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
        Log.i(TAG, "AndroidAudioPlayer(audioFormat=\"" + audioFormat + "\", bufferSize=\"" + bufferSize + "\")");
        Log.i(TAG, "audioTrack=\"" + audioTrack + "\"");
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        int offset;
        if (audioEvent.getTimeStamp() == 0) {
            offset = 0;
        } else {
            offset = audioEvent.getOverlap();
        }
        int length = audioEvent.getBufferSize() - offset;
        int ret;
        if (format.getSampleSizeInBits() == 16 ) {
            short[] shorts = new short[audioEvent.getBufferSize()];
            ByteBuffer.wrap(audioEvent.getByteBuffer()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ret = audioTrack.write(shorts, offset, length);
        } else {
            ret = audioTrack.write(audioEvent.getByteBuffer(), offset, length);
        }
        if (ret < 0) {
            Log.e(TAG, "AudioTrack.write returned error code " + ret);
        }
        return true;
    }

    @Override
    public void processingFinished() {
        Log.w(TAG, "processingFinished");
        audioTrack.flush();
        audioTrack.stop();
        audioTrack.release();
    }
}