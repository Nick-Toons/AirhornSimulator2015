package com.herrschreiber.airhornsimulator2015;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alex on 2/13/15.
 */
public class SoundPlayer {
    private static final String SOUNDS_PATH = "Sounds";
    private static final String TAG = "SoundPlayer";
    private final List<Sound> sounds;

    public SoundPlayer() {
        sounds = new ArrayList<>();
    }

    public void loadSounds(Context context) throws IOException {
        AssetManager am = context.getAssets();
        String[] soundNames;
        try {
            soundNames = am.list(SOUNDS_PATH);
        } catch (IOException e) {
            throw new IOException("Failed to list sounds while loading sounds", e);
        }
        for (String soundName : soundNames) {
            Log.d("SoundPlayer", "Adding sound " + soundName);
            AssetFileDescriptor fd = am.openFd(new File(SOUNDS_PATH, soundName).getPath());
            Sound sound = null;
            try {
                sound = new Sound(soundName, fd);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error creating sound", e);
            }
            sounds.add(sound);
        }
    }

    public List<Sound> getSounds() {
        return sounds;
    }

    public void playSound(Sound sound) {
        sound.play();
    }

    public static class Sound {
        public static final String TAG = "Sound";
        public static final long TIMEOUT_US = 1000;
        public static final int NO_OUTPUT_COUNTER_LIMIT = 50;
        private final String name;
        private final MediaExtractor extractor;
        private MediaFormat mediaFormat;
        private String mime;
        private MediaCodec decoder;
        private AudioTrack audioTrack;
        private AsyncTask<Void, Void, Void> playTask;
        private boolean stop;

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

            Log.i(TAG, "Created sound " + this.toString());
        }

        public String getName() {
            return name;
        }

        public void play() {
            if (playTask == null || playTask.getStatus() != AsyncTask.Status.RUNNING) {
                playTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Sound.this.decoderLoop();
                        return null;
                    }
                };
                playTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            }
        }

        public void stop() {
            stop = true;
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

            int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int channelConfig;
            if (channelCount == 1) {
                channelConfig = AudioFormat.CHANNEL_OUT_MONO;
            } else if (channelCount == 2) {
                channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
            } else {
                throw new Error("Unsupported channel count: " + channelCount);
            }

            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
            Log.i(TAG, "Created AudioTrack " + audioTrack.toString());

            audioTrack.play();
            extractor.selectTrack(0);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = decoder.getInputBuffers();
            ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int noOutputCounter = 0;
            stop = false;
            while (!sawOutputEOS && noOutputCounter < NO_OUTPUT_COUNTER_LIMIT && !stop) {
                noOutputCounter++;
                if (!sawInputEOS) {
                    int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_US);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = inputBuffers[inputBufIndex];

                        int sampleSize = extractor.readSampleData(dstBuf, 0 /* offset */);

                        long presentationTimeUs = 0;

                        if (sampleSize < 0) {
                            Log.d(TAG, "Input EOS");
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                        }
                        // can throw illegal state exception (???)
                        decoder.queueInputBuffer(inputBufIndex, 0 /* offset */, sampleSize,
                                presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
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
                    if (chunk.length > 0)
                        audioTrack.write(chunk, 0, chunk.length);
                    decoder.releaseOutputBuffer(res, false /* render */);
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

            Log.d(TAG, "Finished playing sound " + name);

            relaxResources(true);

            stop = true;
        }

        private void relaxResources(Boolean release) {
            if (decoder != null) {
                if (release) {
                    decoder.stop();
                    decoder.release();
                    decoder = null;
                }
            }
            if (audioTrack != null) {
                audioTrack.flush();
                audioTrack.release();
                audioTrack = null;
            }
        }

        @Override
        public String toString() {
            return "SoundPlayer.Sound[" +
                    "name='" + name + '\'' +
                    ", extractor=" + extractor +
                    ", decoder=" + decoder +
                    ", mediaFormat=" + mediaFormat + ']';
        }
    }
}
