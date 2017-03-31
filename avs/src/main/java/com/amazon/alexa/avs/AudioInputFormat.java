/**
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Amazon Software License (the "License"). You may not use this file
 * except in compliance with the License. A copy of the License is located at
 * <p>
 * http://aws.amazon.com/asl/
 * <p>
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.amazon.alexa.avs;


import javax.sound.sampled.AudioFormat;

public enum AudioInputFormat {
    LPCM(Constants.LPCM_CHUNK_SIZE_BYTES, Constants.LPCM_CHUNK_SIZE_MS, Constants.LPCM_AUDIO_FORMAT, Constants.LPCM_CONTENT_TYPE);

    private final int chunkSizeBytes;
    private final int chunkSizeMs;
    private final AudioFormat audioFormat;
    private final String contentType;

    private AudioInputFormat(final int chunkSizeBytes, final int chunkSizeMs, AudioFormat audioFormat, final String contentType) {
        this.chunkSizeBytes = chunkSizeBytes;
        this.chunkSizeMs = chunkSizeMs;
        this.audioFormat = audioFormat;
        this.contentType = contentType;
    }

    public int getChunkSizeBytes() {
        return chunkSizeBytes;
    }

    public int getChunkSizeMs() {
        return chunkSizeMs;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public String getContentType() {
        return contentType;
    }

    public static final class Constants {
        public static final int LPCM_CHUNK_SIZE_BYTES = 320;
        public static final int LPCM_CHUNK_SIZE_MS = 10;
        public static int sampleRate = 16000;
        public static int sampleSizeInBits = 16;
        public static int channels = 1;
        public static boolean signed = true;
        public static boolean bigEndia = false;
        /**
         * sampleRate - the number of samples per second
         sampleSizeInBits - the number of bits in each sample
         channels - the number of channels (1 for mono, 2 for stereo, and so on)
         signed - indicates whether the data is signed or unsigned
         bigEndian - indicates whether the data for a single sample is stored in big-endian byte order (false means little-endian)
         * */
        public static final AudioFormat LPCM_AUDIO_FORMAT = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndia);
        public static final String LPCM_CONTENT_TYPE = "audio/L16; rate=16000; channels=1";
    }
}
