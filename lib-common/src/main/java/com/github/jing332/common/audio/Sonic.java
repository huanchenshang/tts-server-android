/* Sonic library
   Copyright 2010, 2011
   Bill Cox
   This file is part of the Sonic Library.

   This file is licensed under the Apache 2.0 license.
*/

package com.github.jing332.common.audio;

import java.nio.ByteBuffer;

// https://github.com/waywardgeek/sonic/blob/master/Sonic.java
public class Sonic {

    private static final int SONIC_MIN_PITCH = 65;
    private static final int SONIC_MAX_PITCH = 400;
    // This is used to down-sample some inputs to improve speed
    private static final int SONIC_AMDF_FREQ = 4000;
    // The number of points to use in the sinc FIR filter for resampling.
    private static final int SINC_FILTER_POINTS = 12;
    private static final int SINC_TABLE_SIZE = 601;

    // Lookup table for windowed sinc function of SINC_FILTER_POINTS points.
    // The code to generate this is in the header comment of sonic.c.
    private static final short sincTable[] = {
            0, 0, 0, 0, 0, 0, 0, -1, -1, -2, -2, -3, -4, -6, -7, -9, -10, -12, -14,
            -17, -19, -21, -24, -26, -29, -32, -34, -37, -40, -42, -44, -47, -48, -50,
            -51, -52, -53, -53, -53, -52, -50, -48, -46, -43, -39, -34, -29, -22, -16,
            -8, 0, 9, 19, 29, 41, 53, 65, 79, 92, 107, 121, 137, 152, 168, 184, 200,
            215, 231, 247, 262, 276, 291, 304, 317, 328, 339, 348, 357, 363, 369, 372,
            374, 375, 373, 369, 363, 355, 345, 332, 318, 300, 281, 259, 234, 208, 178,
            147, 113, 77, 39, 0, -41, -85, -130, -177, -225, -274, -324, -375, -426,
            -478, -530, -581, -632, -682, -731, -779, -825, -870, -912, -951, -989,
            -1023, -1053, -1080, -1104, -1123, -1138, -1149, -1154, -1155, -1151,
            -1141, -1125, -1105, -1078, -1046, -1007, -963, -913, -857, -796, -728,
            -655, -576, -492, -403, -309, -210, -107, 0, 111, 225, 342, 462, 584, 708,
            833, 958, 1084, 1209, 1333, 1455, 1575, 1693, 1807, 1916, 2022, 2122, 2216,
            2304, 2384, 2457, 2522, 2579, 2625, 2663, 2689, 2706, 2711, 2705, 2687,
            2657, 2614, 2559, 2491, 2411, 2317, 2211, 2092, 1960, 1815, 1658, 1489,
            1308, 1115, 912, 698, 474, 241, 0, -249, -506, -769, -1037, -1310, -1586,
            -1864, -2144, -2424, -2703, -2980, -3254, -3523, -3787, -4043, -4291,
            -4529, -4757, -4972, -5174, -5360, -5531, -5685, -5819, -5935, -6029,
            -6101, -6150, -6175, -6175, -6149, -6096, -6015, -5905, -5767, -5599,
            -5401, -5172, -4912, -4621, -4298, -3944, -3558, -3141, -2693, -2214,
            -1705, -1166, -597, 0, 625, 1277, 1955, 2658, 3386, 4135, 4906, 5697, 6506,
            7332, 8173, 9027, 9893, 10769, 11654, 12544, 13439, 14335, 15232, 16128,
            17019, 17904, 18782, 19649, 20504, 21345, 22170, 22977, 23763, 24527,
            25268, 25982, 26669, 27327, 27953, 28547, 29107, 29632, 30119, 30569,
            30979, 31349, 31678, 31964, 32208, 32408, 32565, 32677, 32744, 32767,
            32744, 32677, 32565, 32408, 32208, 31964, 31678, 31349, 30979, 30569,
            30119, 29632, 29107, 28547, 27953, 27327, 26669, 25982, 25268, 24527,
            23763, 22977, 22170, 21345, 20504, 19649, 18782, 17904, 17019, 16128,
            15232, 14335, 13439, 12544, 11654, 10769, 9893, 9027, 8173, 7332, 6506,
            5697, 4906, 4135, 3386, 2658, 1955, 1277, 625, 0, -597, -1166, -1705,
            -2214, -2693, -3141, -3558, -3944, -4298, -4621, -4912, -5172, -5401,
            -5599, -5767, -5905, -6015, -6096, -6149, -6175, -6175, -6150, -6101,
            -6029, -5935, -5819, -5685, -5531, -5360, -5174, -4972, -4757, -4529,
            -4291, -4043, -3787, -3523, -3254, -2980, -2703, -2424, -2144, -1864,
            -1586, -1310, -1037, -769, -506, -249, 0, 241, 474, 698, 912, 1115, 1308,
            1489, 1658, 1815, 1960, 2092, 2211, 2317, 2411, 2491, 2559, 2614, 2657,
            2687, 2705, 2711, 2706, 2689, 2663, 2625, 2579, 2522, 2457, 2384, 2304,
            2216, 2122, 2022, 1916, 1807, 1693, 1575, 1455, 1333, 1209, 1084, 958, 833,
            708, 584, 462, 342, 225, 111, 0, -107, -210, -309, -403, -492, -576, -655,
            -728, -796, -857, -913, -963, -1007, -1046, -1078, -1105, -1125, -1141,
            -1151, -1155, -1154, -1149, -1138, -1123, -1104, -1080, -1053, -1023, -989,
            -951, -912, -870, -825, -779, -731, -682, -632, -581, -530, -478, -426,
            -375, -324, -274, -225, -177, -130, -85, -41, 0, 39, 77, 113, 147, 178,
            208, 234, 259, 281, 300, 318, 332, 345, 355, 363, 369, 373, 375, 374, 372,
            369, 363, 357, 348, 339, 328, 317, 304, 291, 276, 262, 247, 231, 215, 200,
            184, 168, 152, 137, 121, 107, 92, 79, 65, 53, 41, 29, 19, 9, 0, -8, -16,
            -22, -29, -34, -39, -43, -46, -48, -50, -52, -53, -53, -53, -52, -51, -50,
            -48, -47, -44, -42, -40, -37, -34, -32, -29, -26, -24, -21, -19, -17, -14,
            -12, -10, -9, -7, -6, -4, -3, -2, -2, -1, -1, 0, 0, 0, 0, 0, 0, 0
    };

    private short inputBuffer[];
    private short outputBuffer[];
    private short pitchBuffer[];
    private short downSampleBuffer[];
    private float speed;
    private float volume;
    private float pitch;
    private float rate;
    private int oldRatePosition;
    private int newRatePosition;
    private boolean useChordPitch;
    private int quality;
    private int numChannels;
    private int inputBufferSize;
    private int pitchBufferSize;
    private int outputBufferSize;
    private int numInputSamples;
    private int numOutputSamples;
    private int numPitchSamples;
    private int minPeriod;
    private int maxPeriod;
    private int maxRequired;
    private int remainingInputToCopy;
    private int sampleRate;
    private int prevPeriod;
    private int prevMinDiff;
    private int minDiff;
    private int maxDiff;

    // Resize the array.
    private short[] resize(
            short[] oldArray,
            int newLength) {
        newLength *= numChannels;
        short[] newArray = new short[newLength];
        int length = oldArray.length <= newLength ? oldArray.length : newLength;

        System.arraycopy(oldArray, 0, newArray, 0, length);
        return newArray;
    }

    // Move samples from one array to another.  May move samples down within an array, but not up.
    private void move(
            short dest[],
            int destPos,
            short source[],
            int sourcePos,
            int numSamples) {
        System.arraycopy(source, sourcePos * numChannels, dest, destPos * numChannels, numSamples * numChannels);
    }

    // Scale the samples by the factor.
    private void scaleSamples(
            short samples[],
            int position,
            int numSamples,
            float volume) {
        // Convert volume to fixed-point, with a 12 bit fraction.
        int fixedPointVolume = (int) (volume * 4096.0f);
        int start = position * numChannels;
        int stop = start + numSamples * numChannels;

        for (int xSample = start; xSample < stop; xSample++) {
            // Convert back from fixed point to 16-bit integer.
            int value = (samples[xSample] * fixedPointVolume) >> 12;
            if (value > 32767) {
                value = 32767;
            } else if (value < -32767) {
                value = -32767;
            }
            samples[xSample] = (short) value;
        }
    }

    // Get the speed of the stream.
    public float getSpeed() {
        return speed;
    }

    // Set the speed of the stream.
    public void setSpeed(
            float speed) {
        this.speed = speed;
    }

    // Get the pitch of the stream.
    public float getPitch() {
        return pitch;
    }

    // Set the pitch of the stream.
    public void setPitch(
            float pitch) {
        this.pitch = pitch;
    }

    // Get the rate of the stream.
    public float getRate() {
        return rate;
    }

    // Set the playback rate of the stream. This scales pitch and speed at the same time.
    public void setRate(
            float rate) {
        this.rate = rate;
        this.oldRatePosition = 0;
        this.newRatePosition = 0;
    }

    // Get the vocal chord pitch setting.
    public boolean getChordPitch() {
        return useChordPitch;
    }

    // Set the vocal chord mode for pitch computation.  Default is off.
    public void setChordPitch(
            boolean useChordPitch) {
        this.useChordPitch = useChordPitch;
    }

    // Get the quality setting.
    public int getQuality() {
        return quality;
    }

    // Set the "quality".  Default 0 is virtually as good as 1, but very much faster.
    public void setQuality(
            int quality) {
        this.quality = quality;
    }

    // Get the scaling factor of the stream.
    public float getVolume() {
        return volume;
    }

    // Set the scaling factor of the stream.
    public void setVolume(
            float volume) {
        this.volume = volume;
    }

    // Allocate stream buffers.
    private void allocateStreamBuffers(
            int sampleRate,
            int numChannels) {
        minPeriod = sampleRate / SONIC_MAX_PITCH;
        maxPeriod = sampleRate / SONIC_MIN_PITCH;
        maxRequired = 2 * maxPeriod;
        inputBufferSize = maxRequired;
        inputBuffer = new short[maxRequired * numChannels];
        outputBufferSize = maxRequired;
        outputBuffer = new short[maxRequired * numChannels];
        pitchBufferSize = maxRequired;
        pitchBuffer = new short[maxRequired * numChannels];
        downSampleBuffer = new short[maxRequired];
        this.sampleRate = sampleRate;
        this.numChannels = numChannels;
        oldRatePosition = 0;
        newRatePosition = 0;
        prevPeriod = 0;
    }

    // Create a sonic stream.
    public Sonic(
            int sampleRate,
            int numChannels) {
        allocateStreamBuffers(sampleRate, numChannels);
        speed = 1.0f;
        pitch = 1.0f;
        volume = 1.0f;
        rate = 1.0f;
        oldRatePosition = 0;
        newRatePosition = 0;
        useChordPitch = false;
        quality = 0;
    }

    // Get the sample rate of the stream.
    public int getSampleRate() {
        return sampleRate;
    }

    // Set the sample rate of the stream.  This will cause samples buffered in the stream to be lost.
    public void setSampleRate(
            int sampleRate) {
        allocateStreamBuffers(sampleRate, numChannels);
    }

    // Get the number of channels.
    public int getNumChannels() {
        return numChannels;
    }

    // Set the num channels of the stream.  This will cause samples buffered in the stream to be lost.
    public void setNumChannels(
            int numChannels) {
        allocateStreamBuffers(sampleRate, numChannels);
    }

    // Enlarge the output buffer if needed.
    private void enlargeOutputBufferIfNeeded(
            int numSamples) {
        if (numOutputSamples + numSamples > outputBufferSize) {
            outputBufferSize += (outputBufferSize >> 1) + numSamples;
            outputBuffer = resize(outputBuffer, outputBufferSize);
        }
    }

    // Enlarge the input buffer if needed.
    private void enlargeInputBufferIfNeeded(
            int numSamples) {
        if (numInputSamples + numSamples > inputBufferSize) {
            inputBufferSize += (inputBufferSize >> 1) + numSamples;
            inputBuffer = resize(inputBuffer, inputBufferSize);
        }
    }

    // Add the input samples to the input buffer.
    private void addFloatSamplesToInputBuffer(
            float samples[],
            int numSamples) {
        if (numSamples == 0) {
            return;
        }
        enlargeInputBufferIfNeeded(numSamples);
        int xBuffer = numInputSamples * numChannels;
        for (int xSample = 0; xSample < numSamples * numChannels; xSample++) {
            inputBuffer[xBuffer++] = (short) (samples[xSample] * 32767.0f);
        }
        numInputSamples += numSamples;
    }

    // Add the input samples to the input buffer.
    private void addShortSamplesToInputBuffer(
            short samples[],
            int numSamples) {
        if (numSamples == 0) {
            return;
        }
        enlargeInputBufferIfNeeded(numSamples);
        move(inputBuffer, numInputSamples, samples, 0, numSamples);
        numInputSamples += numSamples;
    }

    // Add the input samples to the input buffer.
    private void addUnsignedByteSamplesToInputBuffer(
            byte samples[],
            int numSamples) {
        short sample;

        enlargeInputBufferIfNeeded(numSamples);
        int xBuffer = numInputSamples * numChannels;
        for (int xSample = 0; xSample < numSamples * numChannels; xSample++) {
            sample = (short) ((samples[xSample] & 0xff) - 128); // Convert from unsigned to signed
            inputBuffer[xBuffer++] = (short) (sample << 8);
        }
        numInputSamples += numSamples;
    }

    // Add the input samples to the input buffer.  They must be 16-bit little-endian encoded in a byte array.
    private void addBytesToInputBuffer(
            byte inBuffer[],
            int numBytes) {
        int numSamples = numBytes / (2 * numChannels);
        short sample;

        enlargeInputBufferIfNeeded(numSamples);
        int xBuffer = numInputSamples * numChannels;
        for (int xByte = 0; xByte + 1 < numBytes; xByte += 2) {
            sample = (short) ((inBuffer[xByte] & 0xff) | (inBuffer[xByte + 1] << 8));
            inputBuffer[xBuffer++] = sample;
        }
        numInputSamples += numSamples;
    }

    // Remove input samples that we have already processed.
    private void removeInputSamples(
            int position) {
        int remainingSamples = numInputSamples - position;

        move(inputBuffer, 0, inputBuffer, position, remainingSamples);
        numInputSamples = remainingSamples;
    }

    // Just copy from the array to the output buffer
    private void copyToOutput(
            short samples[],
            int position,
            int numSamples) {
        enlargeOutputBufferIfNeeded(numSamples);
        move(outputBuffer, numOutputSamples, samples, position, numSamples);
        numOutputSamples += numSamples;
    }

    // Just copy from the input buffer to the output buffer.  Return num samples copied.
    private int copyInputToOutput(
            int position) {
        int numSamples = remainingInputToCopy;

        if (numSamples > maxRequired) {
            numSamples = maxRequired;
        }
        copyToOutput(inputBuffer, position, numSamples);
        remainingInputToCopy -= numSamples;
        return numSamples;
    }

    // Read data out of the stream.  Sometimes no data will be available, and zero
    // is returned, which is not an error condition.
    public int readFloatFromStream(
            float samples[],
            int maxSamples) {
        int numSamples = numOutputSamples;
        int remainingSamples = 0;

        if (numSamples == 0) {
            return 0;
        }
        if (numSamples > maxSamples) {
            remainingSamples = numSamples - maxSamples;
            numSamples = maxSamples;
        }
        for (int xSample = 0; xSample < numSamples * numChannels; xSample++) {
            samples[xSample] = (outputBuffer[xSample]) / 32767.0f;
        }
        move(outputBuffer, 0, outputBuffer, numSamples, remainingSamples);
        numOutputSamples = remainingSamples;
        return numSamples;
    }

    // Read short data out of the stream.  Sometimes no data will be available, and zero
    // is returned, which is not an error condition.
    public int readShortFromStream(
            short samples[],
            int maxSamples) {
        int numSamples = numOutputSamples;
        int remainingSamples = 0;

        if (numSamples == 0) {
            return 0;
        }
        if (numSamples > maxSamples) {
            remainingSamples = numSamples - maxSamples;
            numSamples = maxSamples;
        }
        move(samples, 0, outputBuffer, 0, numSamples);
        move(outputBuffer, 0, outputBuffer, numSamples, remainingSamples);
        numOutputSamples = remainingSamples;
        return numSamples;
    }

    // Read unsigned byte data out of the stream.  Sometimes no data will be available, and zero
    // is returned, which is not an error condition.
    public int readUnsignedByteFromStream(
            byte samples[],
            int maxSamples) {
        int numSamples = numOutputSamples;
        int remainingSamples = 0;

        if (numSamples == 0) {
            return 0;
        }
        if (numSamples > maxSamples) {
            remainingSamples = numSamples - maxSamples;
            numSamples = maxSamples;
        }
        for (int xSample = 0; xSample < numSamples * numChannels; xSample++) {
            samples[xSample] = (byte) ((outputBuffer[xSample] >> 8) + 128);
        }
        move(outputBuffer, 0, outputBuffer, numSamples, remainingSamples);
        numOutputSamples = remainingSamples;
        return numSamples;
    }


    public byte[] readBytesFromStream(int maxBytes) {
        int maxSamples = maxBytes / (2 * numChannels);
        int numSamples = numOutputSamples;
        int remainingSamples = 0;

        if (numSamples == 0 || maxSamples == 0) {
            return new byte[0];
        }
        if (numSamples > maxSamples) {
            remainingSamples = numSamples - maxSamples;
            numSamples = maxSamples;
        }
        final int outLength = 2 * numSamples * numChannels;
        byte[] outBuffer = new byte[outLength];
        for (int xSample = 0; xSample < numSamples * numChannels; xSample++) {
            short sample = outputBuffer[xSample];
            outBuffer[xSample << 1] = (byte) (sample & 0xff);
            outBuffer[(xSample << 1) + 1] = (byte) (sample >> 8);
        }
        move(outputBuffer, 0, outputBuffer, numSamples, remainingSamples);
        numOutputSamples = remainingSamples;
        return outBuffer;
    }


    // Read unsigned byte data out of the stream.  Sometimes no data will be available, and zero
    // is returned, which is not an error condition.
    public int readBytesFromStream(ByteBuffer outBuffer, int maxBytes) {
        if (outBuffer == null || maxBytes < 0) {
            throw new IllegalArgumentException("Invalid input parameters.");
        }

        int maxSamples = maxBytes / (2 * numChannels);
        int numSamples = numOutputSamples;
        int remainingSamples = 0;

        if (numSamples == 0 || maxSamples == 0) {
            return 0;
        }

        if (numSamples > maxSamples) {
            remainingSamples = numSamples - maxSamples;
            numSamples = maxSamples;
        }

        // 获取当前 position，以便后续计算写入的字节数
        int initialPosition = outBuffer.position();

        for (int i = 0; i < numSamples * numChannels; i++) {
            short sample = outputBuffer[i]; // 假设 outputBufferArray 是内部的 short[] 数组
            outBuffer.put((byte) (sample & 0xff)); // 低字节
            outBuffer.put((byte) (sample >> 8));   // 高字节
        }

        move(outputBuffer, 0, outputBuffer, numSamples, remainingSamples);
        numOutputSamples = remainingSamples;

        // 计算并返回实际写入的字节数
        return outBuffer.position() - initialPosition;
    }


    // Force the sonic stream to generate output using whatever data it currently
    // has.  No extra delay will be added to the output, but flushing in the middle of
    // words could introduce distortion.
    public void flushStream() {
        int remainingSamples = numInputSamples;
        float s = speed / pitch;
        float r = rate * pitch;
        int expectedOutputSamples = numOutputSamples + (int) ((remainingSamples / s + numPitchSamples) / r + 0.5f);

        // Add enough silence to flush both input and pitch buffers.
        enlargeInputBufferIfNeeded(remainingSamples + 2 * maxRequired);
        for (int xSample = 0; xSample < 2 * maxRequired * numChannels; xSample++) {
            inputBuffer[remainingSamples * numChannels + xSample] = 0;
        }
        numInputSamples += 2 * maxRequired;
        writeShortToStream(null, 0);
        // Throw away any extra samples we generated due to the silence we added.
        if (numOutputSamples > expectedOutputSamples) {
            numOutputSamples = expectedOutputSamples;
        }
        // Empty input and pitch buffers.
        numInputSamples = 0;
        remainingInputToCopy = 0;
        numPitchSamples = 0;
    }

    // Return the number of samples in the output buffer
    public int samplesAvailable() {
        return numOutputSamples;
    }

    // If skip is greater than one, average skip samples together and write them to
    // the down-sample buffer.  If numChannels is greater than one, mix the channels
    // together as we down sample.
    private void downSampleInput(
            short samples[],
            int position,
            int skip) {
        int numSamples = maxRequired / skip;
        int samplesPerValue = numChannels * skip;
        int value;

        position *= numChannels;
        for (int i = 0; i < numSamples; i++) {
            value = 0;
            for (int j = 0; j < samplesPerValue; j++) {
                value += samples[position + i * samplesPerValue + j];
            }
            value /= samplesPerValue;
            downSampleBuffer[i] = (short) value;
        }
    }

    // Find the best frequency match in the range, and given a sample skip multiple.
    // For now, just find the pitch of the first channel.
    private int findPitchPeriodInRange(
            short samples[],
            int position,
            int minPeriod,
            int maxPeriod) {
        int bestPeriod = 0, worstPeriod = 255;
        int minDiff = 1, maxDiff = 0;

        position *= numChannels;
        for (int period = minPeriod; period <= maxPeriod; period++) {
            int diff = 0;
            for (int i = 0; i < period; i++) {
                short sVal = samples[position + i];
                short pVal = samples[position + period + i];
                diff += sVal >= pVal ? sVal - pVal : pVal - sVal;
            }
            /* Note that the highest number of samples we add into diff will be less
               than 256, since we skip samples.  Thus, diff is a 24 bit number, and
               we can safely multiply by numSamples without overflow */
            if (diff * bestPeriod < minDiff * period) {
                minDiff = diff;
                bestPeriod = period;
            }
            if (diff * worstPeriod > maxDiff * period) {
                maxDiff = diff;
                worstPeriod = period;
            }
        }
        this.minDiff = minDiff / bestPeriod;
        this.maxDiff = maxDiff / worstPeriod;

        return bestPeriod;
    }

    // At abrupt ends of voiced words, we can have pitch periods that are better
    // approximated by the previous pitch period estimate.  Try to detect this case.
    private boolean prevPeriodBetter(
            int minDiff,
            int maxDiff,
            boolean preferNewPeriod) {
        if (minDiff == 0 || prevPeriod == 0) {
            return false;
        }
        if (preferNewPeriod) {
            if (maxDiff > minDiff * 3) {
                // Got a reasonable match this period
                return false;
            }
            if (minDiff * 2 <= prevMinDiff * 3) {
                // Mismatch is not that much greater this period
                return false;
            }
        } else {
            if (minDiff <= prevMinDiff) {
                return false;
            }
        }
        return true;
    }

    // Find the pitch period.  This is a critical step, and we may have to try
    // multiple ways to get a good answer.  This version uses AMDF.  To improve
    // speed, we down sample by an integer factor get in the 11KHz range, and then
    // do it again with a narrower frequency range without down sampling
    private int findPitchPeriod(
            short samples[],
            int position,
            boolean preferNewPeriod) {
        int period, retPeriod;
        int skip = 1;

        if (sampleRate > SONIC_AMDF_FREQ && quality == 0) {
            skip = sampleRate / SONIC_AMDF_FREQ;
        }
        if (numChannels == 1 && skip == 1) {
            period = findPitchPeriodInRange(samples, position, minPeriod, maxPeriod);
        } else {
            downSampleInput(samples, position, skip);
            period = findPitchPeriodInRange(downSampleBuffer, 0, minPeriod / skip,
                    maxPeriod / skip);
            if (skip != 1) {
                period *= skip;
                int minP = period - (skip << 2);
                int maxP = period + (skip << 2);
                if (minP < minPeriod) {
                    minP = minPeriod;
                }
                if (maxP > maxPeriod) {
                    maxP = maxPeriod;
                }
                if (numChannels == 1) {
                    period = findPitchPeriodInRange(samples, position, minP, maxP);
                } else {
                    downSampleInput(samples, position, 1);
                    period = findPitchPeriodInRange(downSampleBuffer, 0, minP, maxP);
                }
            }
        }
        if (prevPeriodBetter(minDiff, maxDiff, preferNewPeriod)) {
            retPeriod = prevPeriod;
        } else {
            retPeriod = period;
        }
        prevMinDiff = minDiff;
        prevPeriod = period;
        return retPeriod;
    }

    // Overlap two sound segments, ramp the volume of one down, while ramping the
    // other one from zero up, and add them, storing the result at the output.
    private void overlapAdd(
            int numSamples,
            int numChannels,
            short out[],
            int outPos,
            short rampDown[],
            int rampDownPos,
            short rampUp[],
            int rampUpPos) {
        for (int i = 0; i < numChannels; i++) {
            int o = outPos * numChannels + i;
            int u = rampUpPos * numChannels + i;
            int d = rampDownPos * numChannels + i;
            for (int t = 0; t < numSamples; t++) {
                out[o] = (short) ((rampDown[d] * (numSamples - t) + rampUp[u] * t) / numSamples);
                o += numChannels;
                d += numChannels;
                u += numChannels;
            }
        }
    }

    // Overlap two sound segments, ramp the volume of one down, while ramping the
    // other one from zero up, and add them, storing the result at the output.
    private void overlapAddWithSeparation(
            int numSamples,
            int numChannels,
            int separation,
            short out[],
            int outPos,
            short rampDown[],
            int rampDownPos,
            short rampUp[],
            int rampUpPos) {
        for (int i = 0; i < numChannels; i++) {
            int o = outPos * numChannels + i;
            int u = rampUpPos * numChannels + i;
            int d = rampDownPos * numChannels + i;
            for (int t = 0; t < numSamples + separation; t++) {
                if (t < separation) {
                    out[o] = (short) (rampDown[d] * (numSamples - t) / numSamples);
                    d += numChannels;
                } else if (t < numSamples) {
                    out[o] = (short) ((rampDown[d] * (numSamples - t) + rampUp[u] * (t - separation)) / numSamples);
                    d += numChannels;
                    u += numChannels;
                } else {
                    out[o] = (short) (rampUp[u] * (t - separation) / numSamples);
                    u += numChannels;
                }
                o += numChannels;
            }
        }
    }

    // Just move the new samples in the output buffer to the pitch buffer
    private void moveNewSamplesToPitchBuffer(
            int originalNumOutputSamples) {
        int numSamples = numOutputSamples - originalNumOutputSamples;

        if (numPitchSamples + numSamples > pitchBufferSize) {
            pitchBufferSize += (pitchBufferSize >> 1) + numSamples;
            pitchBuffer = resize(pitchBuffer, pitchBufferSize);
        }
        move(pitchBuffer, numPitchSamples, outputBuffer, originalNumOutputSamples, numSamples);
        numOutputSamples = originalNumOutputSamples;
        numPitchSamples += numSamples;
    }

    // Remove processed samples from the pitch buffer.
    private void removePitchSamples(
            int numSamples) {
        if (numSamples == 0) {
            return;
        }
        move(pitchBuffer, 0, pitchBuffer, numSamples, numPitchSamples - numSamples);
        numPitchSamples -= numSamples;
    }

    // Change the pitch.  The latency this introduces could be reduced by looking at
    // past samples to determine pitch, rather than future.
    private void adjustPitch(
            int originalNumOutputSamples) {
        int period, newPeriod, separation;
        int position = 0;

        if (numOutputSamples == originalNumOutputSamples) {
            return;
        }
        moveNewSamplesToPitchBuffer(originalNumOutputSamples);
        while (numPitchSamples - position >= maxRequired) {
            period = findPitchPeriod(pitchBuffer, position, false);
            newPeriod = (int) (period / pitch);
            enlargeOutputBufferIfNeeded(newPeriod);
            if (pitch >= 1.0f) {
                overlapAdd(newPeriod, numChannels, outputBuffer, numOutputSamples, pitchBuffer,
                        position, pitchBuffer, position + period - newPeriod);
            } else {
                separation = newPeriod - period;
                overlapAddWithSeparation(period, numChannels, separation, outputBuffer, numOutputSamples,
                        pitchBuffer, position, pitchBuffer, position);
            }
            numOutputSamples += newPeriod;
            position += period;
        }
        removePitchSamples(position);
    }

    // Approximate the sinc function times a Hann window from the sinc table.
    private int findSincCoefficient(int i, int ratio, int width) {
        int lobePoints = (SINC_TABLE_SIZE - 1) / SINC_FILTER_POINTS;
        int left = i * lobePoints + (ratio * lobePoints) / width;
        int right = left + 1;
        int position = i * lobePoints * width + ratio * lobePoints - left * width;
        int leftVal = sincTable[left];
        int rightVal = sincTable[right];

        return ((leftVal * (width - position) + rightVal * position) << 1) / width;
    }

    // Return 1 if value >= 0, else -1.  This represents the sign of value.
    private int getSign(int value) {
        return value >= 0 ? 1 : -1;
    }

    // Interpolate the new output sample.
    private short interpolate(
            short in[],
            int inPos,  // Index to first sample which already includes channel offset.
            int oldSampleRate,
            int newSampleRate) {
        // Compute N-point sinc FIR-filter here.  Clip rather than overflow.
        int i;
        int total = 0;
        int position = newRatePosition * oldSampleRate;
        int leftPosition = oldRatePosition * newSampleRate;
        int rightPosition = (oldRatePosition + 1) * newSampleRate;
        int ratio = rightPosition - position - 1;
        int width = rightPosition - leftPosition;
        int weight, value;
        int oldSign;
        int overflowCount = 0;

        for (i = 0; i < SINC_FILTER_POINTS; i++) {
            weight = findSincCoefficient(i, ratio, width);
            /* printf("%u %f\n", i, weight); */
            value = in[inPos + i * numChannels] * weight;
            oldSign = getSign(total);
            total += value;
            if (oldSign != getSign(total) && getSign(value) == oldSign) {
                /* We must have overflowed.  This can happen with a sinc filter. */
                overflowCount += oldSign;
            }
        }
        /* It is better to clip than to wrap if there was a overflow. */
        if (overflowCount > 0) {
            return Short.MAX_VALUE;
        } else if (overflowCount < 0) {
            return Short.MIN_VALUE;
        }
        return (short) (total >> 16);
    }

    // Change the rate.
    private void adjustRate(
            float rate,
            int originalNumOutputSamples) {
        int newSampleRate = (int) (sampleRate / rate);
        int oldSampleRate = sampleRate;
        int position;
        int N = SINC_FILTER_POINTS;

        // Set these values to help with the integer math
        while (newSampleRate > (1 << 14) || oldSampleRate > (1 << 14)) {
            newSampleRate >>= 1;
            oldSampleRate >>= 1;
        }
        if (numOutputSamples == originalNumOutputSamples) {
            return;
        }
        moveNewSamplesToPitchBuffer(originalNumOutputSamples);
        // Leave at least N pitch samples in the buffer
        for (position = 0; position < numPitchSamples - N; position++) {
            while ((oldRatePosition + 1) * newSampleRate > newRatePosition * oldSampleRate) {
                enlargeOutputBufferIfNeeded(1);
                for (int i = 0; i < numChannels; i++) {
                    outputBuffer[numOutputSamples * numChannels + i] = interpolate(pitchBuffer,
                            position * numChannels + i, oldSampleRate, newSampleRate);
                }
                newRatePosition++;
                numOutputSamples++;
            }
            oldRatePosition++;
            if (oldRatePosition == oldSampleRate) {
                oldRatePosition = 0;
                if (newRatePosition != newSampleRate) {
                    System.out.printf("Assertion failed: newRatePosition != newSampleRate\n");
                    assert false;
                }
                newRatePosition = 0;
            }
        }
        removePitchSamples(position);
    }


    // Skip over a pitch period, and copy period/speed samples to the output
    private int skipPitchPeriod(
            short samples[],
            int position,
            float speed,
            int period) {
        int newSamples;

        if (speed >= 2.0f) {
            newSamples = (int) (period / (speed - 1.0f));
        } else {
            newSamples = period;
            remainingInputToCopy = (int) (period * (2.0f - speed) / (speed - 1.0f));
        }
        enlargeOutputBufferIfNeeded(newSamples);
        overlapAdd(newSamples, numChannels, outputBuffer, numOutputSamples, samples, position,
                samples, position + period);
        numOutputSamples += newSamples;
        return newSamples;
    }

    // Insert a pitch period, and determine how much input to copy directly.
    private int insertPitchPeriod(
            short samples[],
            int position,
            float speed,
            int period) {
        int newSamples;

        if (speed < 0.5f) {
            newSamples = (int) (period * speed / (1.0f - speed));
        } else {
            newSamples = period;
            remainingInputToCopy = (int) (period * (2.0f * speed - 1.0f) / (1.0f - speed));
        }
        enlargeOutputBufferIfNeeded(period + newSamples);
        move(outputBuffer, numOutputSamples, samples, position, period);
        overlapAdd(newSamples, numChannels, outputBuffer, numOutputSamples + period, samples,
                position + period, samples, position);
        numOutputSamples += period + newSamples;
        return newSamples;
    }

    // Resample as many pitch periods as we have buffered on the input.  Return 0 if
    // we fail to resize an input or output buffer.  Also scale the output by the volume.
    private void changeSpeed(
            float speed) {
        int numSamples = numInputSamples;
        int position = 0, period, newSamples;

        if (numInputSamples < maxRequired) {
            return;
        }
        do {
            if (remainingInputToCopy > 0) {
                newSamples = copyInputToOutput(position);
                position += newSamples;
            } else {
                period = findPitchPeriod(inputBuffer, position, true);
                if (speed > 1.0) {
                    newSamples = skipPitchPeriod(inputBuffer, position, speed, period);
                    position += period + newSamples;
                } else {
                    newSamples = insertPitchPeriod(inputBuffer, position, speed, period);
                    position += newSamples;
                }
            }
        } while (position + maxRequired <= numSamples);
        removeInputSamples(position);
    }

    // Resample as many pitch periods as we have buffered on the input.  Scale the output by the volume.
    private void processStreamInput() {
        int originalNumOutputSamples = numOutputSamples;
        float s = speed / pitch;
        float r = rate;

        if (!useChordPitch) {
            r *= pitch;
        }
        if (s > 1.00001 || s < 0.99999) {
            changeSpeed(s);
        } else {
            copyToOutput(inputBuffer, 0, numInputSamples);
            numInputSamples = 0;
        }
        if (useChordPitch) {
            if (pitch != 1.0f) {
                adjustPitch(originalNumOutputSamples);
            }
        } else if (r != 1.0f) {
            adjustRate(r, originalNumOutputSamples);
        }
        if (volume != 1.0f) {
            // Adjust output volume.
            scaleSamples(outputBuffer, originalNumOutputSamples, numOutputSamples - originalNumOutputSamples,
                    volume);
        }
    }

    // Write floating point data to the input buffer and process it.
    public void writeFloatToStream(
            float samples[],
            int numSamples) {
        addFloatSamplesToInputBuffer(samples, numSamples);
        processStreamInput();
    }

    // Write the data to the input stream, and process it.
    public void writeShortToStream(
            short samples[],
            int numSamples) {
        addShortSamplesToInputBuffer(samples, numSamples);
        processStreamInput();
    }

    // Simple wrapper around sonicWriteFloatToStream that does the unsigned byte to short
    // conversion for you.
    public void writeUnsignedByteToStream(
            byte samples[],
            int numSamples) {
        addUnsignedByteSamplesToInputBuffer(samples, numSamples);
        processStreamInput();
    }

    // Simple wrapper around sonicWriteBytesToStream that does the byte to 16-bit LE conversion.
    public void writeBytesToStream(
            byte inBuffer[],
            int numBytes) {
        addBytesToInputBuffer(inBuffer, numBytes);
        processStreamInput();
    }

    // This is a non-stream oriented interface to just change the speed of a sound sample
    public static int changeFloatSpeed(
            float samples[],
            int numSamples,
            float speed,
            float pitch,
            float rate,
            float volume,
            boolean useChordPitch,
            int sampleRate,
            int numChannels) {
        Sonic stream = new Sonic(sampleRate, numChannels);

        stream.setSpeed(speed);
        stream.setPitch(pitch);
        stream.setRate(rate);
        stream.setVolume(volume);
        stream.setChordPitch(useChordPitch);
        stream.writeFloatToStream(samples, numSamples);
        stream.flushStream();
        numSamples = stream.samplesAvailable();
        stream.readFloatFromStream(samples, numSamples);
        return numSamples;
    }

    /* This is a non-stream oriented interface to just change the speed of a sound sample */
    public int sonicChangeShortSpeed(
            short samples[],
            int numSamples,
            float speed,
            float pitch,
            float rate,
            float volume,
            boolean useChordPitch,
            int sampleRate,
            int numChannels) {
        Sonic stream = new Sonic(sampleRate, numChannels);

        stream.setSpeed(speed);
        stream.setPitch(pitch);
        stream.setRate(rate);
        stream.setVolume(volume);
        stream.setChordPitch(useChordPitch);
        stream.writeShortToStream(samples, numSamples);
        stream.flushStream();
        numSamples = stream.samplesAvailable();
        stream.readShortFromStream(samples, numSamples);
        return numSamples;
    }
}