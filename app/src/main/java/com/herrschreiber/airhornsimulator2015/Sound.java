package com.herrschreiber.airhornsimulator2015;

import android.os.Parcelable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;

/**
 * Created by alex on 5/16/15.
 */
public abstract class Sound implements TarsosDSPAudioInputStream {
    protected byte[] buffer = null;
    private String name;
    private TarsosDSPAudioFormat audioFormat;
    private ByteArrayInputStream byteStream = null;
    private boolean hasInitialized = false;

    public Sound(String name) {
        this.name = name;
    }

    protected abstract void init() throws IOException;

    public void initialize() throws IOException {
        if (!hasInitialized()) {
            init();
            hasInitialized = true;
        }
    }

    public boolean hasInitialized() {
        return hasInitialized;
    }

    public void start() {
        if (byteStream == null) {
            if (buffer == null) {
                throw new IllegalStateException("Tried to start a sound without a populated sound buffer");
            } else {
                byteStream = new ByteArrayInputStream(buffer);
            }
        }
        byteStream.reset();
    }

    public String getName() {
        return name;
    }

    @Override
    public long skip(long l) throws IOException {
        return byteStream.skip(l);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return byteStream.read(buffer, offset, length);
    }

    @Override
    public void close() throws IOException {
        byteStream.close();
    }

    @Override
    public TarsosDSPAudioFormat getFormat() {
        return audioFormat;
    }

    @Override
    public long getFrameLength() {
        return buffer.length / getFormat().getFrameSize();
    }

    public float getSampleRate() {
        return getFormat().getSampleRate();
    }

    public int getChannels() {
        return getFormat().getChannels();
    }

    public double getDuration() {
        return getFrameLength() / getFormat().getFrameRate();
    }

    public void setAudioFormat(TarsosDSPAudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }
}
