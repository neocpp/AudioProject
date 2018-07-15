package com.neo.audiokit.tarsor;

import com.neo.audiokit.framework.AudioChain;
import com.neo.audiokit.framework.AudioFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;


/**
 * This class plays a file and sends float arrays to registered AudioProcessor
 * implementors. This class can be used to feed FFT's, pitch detectors, audio players, ...
 * Using a (blocking) audio player it is even possible to synchronize execution of
 * AudioProcessors and sound. This behavior can be used for visualization.
 *
 * @author Joren Six
 */
public class TarsorDispatcher extends AudioChain {
    private final static String TAG = "TarsorDispatcher";

    /**
     * Log messages.
     */
    private static final Logger LOG = Logger.getLogger(be.tarsos.dsp.AudioDispatcher.class.getName());

    /**
     * This buffer is reused again and again to store audio data using the float
     * data type.
     */
    private float[] audioFloatBuffer;

    /**
     * This buffer is reused again and again to store audio data using the byte
     * data type.
     */
    private byte[] audioByteBuffer;

    private byte[] cacheByteBuffer;
    private int cacheOffset;
    private int cacheSize;
    private ByteBuffer outputBuffer;
    private final int ERR_CONTINUE = -2;

    /**
     * A list of registered audio processors. The audio processors are
     * responsible for actually doing the digital signal processing
     */
    private final List<AudioProcessor> audioProcessors;

    /**
     * Converter converts an array of floats to an array of bytes (and vice
     * versa).
     */
    private final TarsosDSPAudioFloatConverter converter;

    private final TarsosDSPAudioFormat format;

    /**
     * The floatOverlap: the number of elements that are copied in the buffer
     * from the previous buffer. Overlap should be smaller (strict) than the
     * buffer size and can be zero. Defined in number of samples.
     */
    private int floatOverlap, floatStepSize;

    /**
     * The overlap and stepsize defined not in samples but in bytes. So it
     * depends on the bit depth. Since the int datatype is used only 8,16,24,...
     * bits or 1,2,3,... bytes are supported.
     */
    private int byteOverlap, byteStepSize;

    /**
     * Position in the stream in bytes. e.g. if 44100 bytes are processed and 16
     * bits per frame are used then you are 0.5 seconds into the stream.
     */
    private long bytesProcessed;


    /**
     * The audio event that is send through the processing chain.
     */
    private AudioEvent audioEvent;

    /**
     * If true then the first buffer is only filled up to buffer size - hop size
     * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000 times
     * zero 0 and 48 actual audio samples. During the next iteration you get
     * mostly zeros and 96 samples.
     */
    private boolean zeroPadFirstBuffer;

    /**
     * If true then the last buffer is zero padded. Otherwise the buffer is
     * shortened to the remaining number of samples. If false then the audio
     * processors must be prepared to handle shorter audio buffers.
     */
    private boolean zeroPadLastBuffer;

    private int inputChannels;

    private MonoChannelToMulti monoChannelToMulti;

    /**
     * Create a new dispatcher from a stream.
     *
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     *                        in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     *                        AudioBufferSize is common (512, 1024) for an FFT.
     */
    public TarsorDispatcher(final TarsosDSPAudioFormat audioFormat, final int audioBufferSize, final int bufferOverlap) {
        // The copy on write list allows concurrent modification of the list while
        // it is iterated. A nice feature to have when adding AudioProcessors while
        // the AudioDispatcher is running.
        audioProcessors = new CopyOnWriteArrayList<AudioProcessor>();

        inputChannels = audioFormat.getChannels();
        if (inputChannels == 2) {
            monoChannelToMulti = new MonoChannelToMulti(inputChannels);
        }

        format = new TarsosDSPAudioFormat(audioFormat.getEncoding(),
                audioFormat.getSampleRate(),
                audioFormat.getSampleSizeInBits(), 1,
                ((audioFormat.getSampleSizeInBits() + 7) / 8),
                audioFormat.getFrameRate(),
                audioFormat.isBigEndian());

        setStepSizeAndOverlap(audioBufferSize, bufferOverlap);

        audioEvent = new AudioEvent(format);
        audioEvent.setFloatBuffer(audioFloatBuffer);
        audioEvent.setOverlap(bufferOverlap);

        converter = TarsosDSPAudioFloatConverter.getConverter(format);

        zeroPadLastBuffer = true;

        outputBuffer = ByteBuffer.allocateDirect(audioByteBuffer.length).order(ByteOrder.nativeOrder());
    }

    /**
     * Set a new step size and overlap size. Both in number of samples. Watch
     * out with this method: it should be called after a batch of samples is
     * processed, not during.
     *
     * @param audioBufferSize The size of the buffer defines how much samples are processed
     *                        in one step. Common values are 1024,2048.
     * @param bufferOverlap   How much consecutive buffers overlap (in samples). Half of the
     *                        AudioBufferSize is common (512, 1024) for an FFT.
     */
    public void setStepSizeAndOverlap(final int audioBufferSize, final int bufferOverlap) {
        audioFloatBuffer = new float[audioBufferSize];
        floatOverlap = bufferOverlap;
        floatStepSize = audioFloatBuffer.length - floatOverlap;

        audioByteBuffer = new byte[audioFloatBuffer.length * format.getFrameSize()];
        byteOverlap = floatOverlap * format.getFrameSize();
        byteStepSize = floatStepSize * format.getFrameSize();

        cacheByteBuffer = new byte[audioByteBuffer.length * 2];
    }

    /**
     * if zero pad is true then the first buffer is only filled up to  buffer size - hop size
     * E.g. if the buffer is 2048 and the hop size is 48 then you get 2000x0 and 48 filled audio samples
     *
     * @param zeroPadFirstBuffer true if the buffer should be zeroPadFirstBuffer, false otherwise.
     */
    public void setZeroPadFirstBuffer(boolean zeroPadFirstBuffer) {
        this.zeroPadFirstBuffer = zeroPadFirstBuffer;
    }

    /**
     * If zero pad last buffer is true then the last buffer is filled with zeros until the normal amount
     * of elements are present in the buffer. Otherwise the buffer only contains the last elements and no zeros.
     * By default it is set to true.
     *
     * @param zeroPadLastBuffer
     */
    public void setZeroPadLastBuffer(boolean zeroPadLastBuffer) {
        this.zeroPadLastBuffer = zeroPadLastBuffer;
    }


    /**
     * Adds an AudioProcessor to the chain of processors.
     *
     * @param audioProcessor The AudioProcessor to add.
     */
    public void addAudioProcessor(final AudioProcessor audioProcessor) {
        audioProcessors.add(audioProcessor);
        LOG.fine("Added an audioprocessor to the list of processors: " + audioProcessor.toString());
    }

    /**
     * Removes an AudioProcessor to the chain of processors and calls its <code>processingFinished</code> method.
     *
     * @param audioProcessor The AudioProcessor to remove.
     */
    public void removeAudioProcessor(final AudioProcessor audioProcessor) {
        audioProcessors.remove(audioProcessor);
        audioProcessor.processingFinished();
        LOG.fine("Remove an audioprocessor to the list of processors: " + audioProcessor.toString());
    }

    /**
     * Reads the next audio block. It tries to read the number of bytes defined
     * by the audio buffer size minus the overlap. If the expected number of
     * bytes could not be read either the end of the stream is reached or
     * something went wrong.
     * <p>
     * The behavior for the first and last buffer is defined by their corresponding the zero pad settings. The method also handles the case if
     * the first buffer is also the last.
     *
     * @return The number of bytes read.
     * @throws IOException When something goes wrong while reading the stream. In
     *                     particular, an IOException is thrown if the input stream has
     *                     been closed.
     */
    private int readNextAudioBlock() {
        assert floatOverlap < audioFloatBuffer.length;

        // Is this the first buffer?
        boolean isFirstBuffer = (bytesProcessed == 0);

        final int offsetInBytes;

        final int offsetInSamples;

        final int bytesToRead;
        //Determine the amount of bytes to read from the stream
        if (isFirstBuffer && !zeroPadFirstBuffer) {
            //If this is the first buffer and we do not want to zero pad the
            //first buffer then read a full buffer
            bytesToRead = audioByteBuffer.length;
            // With an offset in bytes of zero;
            offsetInBytes = 0;
            offsetInSamples = 0;
        } else {
            //In all other cases read the amount of bytes defined by the step size
            bytesToRead = byteStepSize;
            offsetInBytes = byteOverlap;
            offsetInSamples = floatOverlap;
        }

        //Shift the audio information using array copy since it is probably faster than manually shifting it.
        // No need to do this on the first buffer
        if (!isFirstBuffer && audioFloatBuffer.length == floatOverlap + floatStepSize && floatOverlap > 0) {
            System.arraycopy(audioFloatBuffer, floatStepSize, audioFloatBuffer, 0, floatOverlap);
			/*
			for(int i = floatStepSize ; i < floatStepSize+floatOverlap ; i++){
				audioFloatBuffer[i-floatStepSize] = audioFloatBuffer[i];
			}*/
        }

        // Total amount of bytes read
        int totalBytesRead = 0;

        // The amount of bytes read from the stream during one iteration.
        int bytesRead = 0;

        // Is the end of the stream reached?
        boolean endOfStream = false;

        // Always try to read the 'bytesToRead' amount of bytes.
        // unless the stream is closed (stopped is true) or no bytes could be read during one iteration
        while (!endOfStream && totalBytesRead < bytesToRead) {
            bytesRead = readStream(audioByteBuffer, offsetInBytes + totalBytesRead, bytesToRead - totalBytesRead);
            if (bytesRead == -1) {
                // The end of the stream is reached if the number of bytes read during this iteration equals -1
                endOfStream = true;
            } else if (bytesRead == ERR_CONTINUE) {
                return ERR_CONTINUE;
            } else {
                // Otherwise add the number of bytes read to the total
                totalBytesRead += bytesRead;
            }
        }

        if (endOfStream) {
            // Could not read a full buffer from the stream, there are two options:
            if (zeroPadLastBuffer) {
                //Make sure the last buffer has the same length as all other buffers and pad with zeros
                for (int i = offsetInBytes + totalBytesRead; i < audioByteBuffer.length; i++) {
                    audioByteBuffer[i] = 0;
                }
                converter.toFloatArray(audioByteBuffer, offsetInBytes, audioFloatBuffer, offsetInSamples, floatStepSize);
            } else {
                // Send a smaller buffer through the chain.
                byte[] audioByteBufferContent = audioByteBuffer;
                audioByteBuffer = new byte[offsetInBytes + totalBytesRead];
                for (int i = 0; i < audioByteBuffer.length; i++) {
                    audioByteBuffer[i] = audioByteBufferContent[i];
                }
                int totalSamplesRead = totalBytesRead / format.getFrameSize();
                audioFloatBuffer = new float[offsetInSamples + totalBytesRead / format.getFrameSize()];
                converter.toFloatArray(audioByteBuffer, offsetInBytes, audioFloatBuffer, offsetInSamples, totalSamplesRead);


            }
        } else if (bytesToRead == totalBytesRead) {
            // The expected amount of bytes have been read from the stream.
            if (isFirstBuffer && !zeroPadFirstBuffer) {
                converter.toFloatArray(audioByteBuffer, 0, audioFloatBuffer, 0, audioFloatBuffer.length);
            } else {
                converter.toFloatArray(audioByteBuffer, offsetInBytes, audioFloatBuffer, offsetInSamples, floatStepSize);
            }
        }


        // Makes sure AudioEvent contains correct info.
        audioEvent.setFloatBuffer(audioFloatBuffer);
        audioEvent.setOverlap(offsetInSamples);

        return totalBytesRead;
    }

    public TarsosDSPAudioFormat getFormat() {
        return format;
    }

    /**
     * @return The currently processed number of seconds.
     */
    public float secondsProcessed() {
        return bytesProcessed / (format.getSampleSizeInBits() / 8) / format.getSampleRate() / format.getChannels();
    }

    public void setAudioFloatBuffer(float[] audioBuffer) {
        audioFloatBuffer = audioBuffer;
    }

    @Override
    protected boolean isActive() {
        return audioProcessors.size() > 0;
    }

    @Override
    protected AudioFrame doProcessData(AudioFrame audioFrame) {
        return audioFrame;
    }

    @Override
    public void newDataReady(AudioFrame audioFrame) {
//        RecorderLogUtils.e(TAG, "in size: " + audioFrame.info.size);
        if (!audioFrame.isRawData || !isActive()) {
            deliverData(audioFrame);
            return;
        }

        cacheData(audioFrame);
        consumeData(audioFrame);

//        if (inputChannels != 1) {
//            // TODO 转换数据
//            if (convertCache == null || convertCache.length < audioFrame.info.size) {
//                convertCache = new byte[audioFrame.info.size];
//            }
//            audioFrame.buffer.get(convertCache, 0, audioFrame.info.size);
//
//            int ix = 0;
//            int outSize = audioFrame.info.size;
//            for (int ox = 0; ox < outSize; ) {
//                short left = ((short) ((convertCache[ix++] & 0xFF) | (convertCache[ix++] << 8)));
//                short right = ((short) ((convertCache[ix++] & 0xFF) | (convertCache[ix++] << 8)));
//                short merge = (short) (left / 2 + right / 2);
//
//
//                convertCache[ox + 0] = (byte) merge;
//                convertCache[ox + 1] = (byte) (merge >>> 8);
//
//                merge = ((short) ((convertCache[ox + 0] & 0xFF) | (convertCache[ox + 1] << 8)));
//                float fmerge = merge * (1.0f / 32767.0f);
//                int imerge = (int) (fmerge * 32767.0);
//
//                convertCache[ox + 0] = (byte) imerge;
//                convertCache[ox + 1] = (byte) (imerge >>> 8);
//
//                convertCache[ox + 2] = (byte) imerge;
//                convertCache[ox + 3] = (byte) (imerge >>> 8);
//
//                ox += 4;
//            }
//
//
//            if (!audioFrame.buffer.isReadOnly()) {
//                convertByteBuffer = audioFrame.buffer;
//            } else if (convertByteBuffer == null || convertByteBuffer.capacity() < outSize) {
//                convertByteBuffer = ByteBuffer.allocateDirect(outSize).order(ByteOrder.nativeOrder());
//            }
//
//            convertByteBuffer.clear();
//            convertByteBuffer.put(convertCache, 0, outSize);
//            convertByteBuffer.position(0);
//            convertByteBuffer.limit(outSize);
//            audioFrame.buffer = convertByteBuffer;
//            audioFrame.info.offset = 0;
//            audioFrame.info.size = outSize;
//        }
//
//        deliverData(audioFrame);
    }

    private byte[] convertCache;
    private ByteBuffer convertByteBuffer;

    private void cacheData(AudioFrame audioFrame) {
        checkData(audioFrame);

        if (cacheByteBuffer.length - cacheOffset < audioFrame.info.size) {
            byte[] newBuffer = new byte[audioFrame.info.size + cacheOffset];
            if (cacheOffset > 0) {
                System.arraycopy(cacheByteBuffer, 0, newBuffer, 0, cacheOffset);
            }
            cacheByteBuffer = newBuffer;
        }
        ByteBuffer buffer = audioFrame.buffer;
        buffer.get(cacheByteBuffer, cacheOffset, audioFrame.info.size);
        cacheSize += audioFrame.info.size;
    }

    private void checkData(AudioFrame audioFrame) {
        if (inputChannels != 1) {
            // TODO 转换数据
            if (convertCache == null || convertCache.length < audioFrame.info.size) {
                convertCache = new byte[audioFrame.info.size];
            }
            audioFrame.buffer.get(convertCache, 0, audioFrame.info.size);

            int ix = 0;
            int outSize = audioFrame.info.size / 2;
            for (int ox = 0; ox < outSize; ) {
                short left = ((short) ((convertCache[ix++] & 0xFF) | (convertCache[ix++] << 8)));
                short right = ((short) ((convertCache[ix++] & 0xFF) | (convertCache[ix++] << 8)));
                short merge = (short) (left / 2 + right / 2);

                convertCache[ox++] = (byte) merge;
                convertCache[ox++] = (byte) (merge >>> 8);
            }

            if (!audioFrame.buffer.isReadOnly()) {
                convertByteBuffer = audioFrame.buffer;
            } else if (convertByteBuffer == null || convertByteBuffer.capacity() < outSize) {
                convertByteBuffer = ByteBuffer.allocateDirect(outSize).order(ByteOrder.nativeOrder());
            }

            convertByteBuffer.clear();
            convertByteBuffer.put(convertCache, 0, outSize);
            convertByteBuffer.position(0);
            convertByteBuffer.limit(outSize);
            audioFrame.buffer = convertByteBuffer;
            audioFrame.info.offset = 0;
            audioFrame.info.size = outSize;
        }
    }

    private long lastTimeUs = 0;

    private void consumeData(AudioFrame audioFrame) {
        long timeUs = audioFrame.info.presentationTimeUs;
        long duration = timeUs - lastTimeUs;

        int bytesRead = readNextAudioBlock();
        audioEvent.setBytesProcessed(bytesProcessed);

        long startUs = lastTimeUs;
        while (bytesRead != ERR_CONTINUE) {

            for (final AudioProcessor processor : audioProcessors) {
                if (!processor.process(audioEvent)) {
                    //skip to the next audio processors if false is returned.
                    break;
                }
            }

            if (monoChannelToMulti != null) {
                monoChannelToMulti.process(audioEvent);
            }

            //Update the number of bytes processed;
            bytesProcessed += bytesRead;
            audioEvent.setBytesProcessed(bytesProcessed);

            // 传送数据
            byte[] byteBuffer = audioEvent.getByteBuffer();
            int overlapInByte = audioEvent.getOverlap() * ((format.getSampleSizeInBits() + 7) / 8) * inputChannels;
            int stepSizeInByte = byteBuffer.length - overlapInByte;
            if (!audioFrame.buffer.isReadOnly() && stepSizeInByte <= audioFrame.buffer.capacity()) {
                outputBuffer = audioFrame.buffer;
            } else if (stepSizeInByte > outputBuffer.capacity()) {
                outputBuffer = ByteBuffer.allocateDirect(stepSizeInByte).order(ByteOrder.nativeOrder());
            }
            outputBuffer.clear();
            outputBuffer.put(byteBuffer, overlapInByte, stepSizeInByte);
            outputBuffer.limit(stepSizeInByte);
            outputBuffer.position(0);
            audioFrame.buffer = outputBuffer;
            audioFrame.info.offset = 0;
            audioFrame.info.size = outputBuffer.limit();
            float ratio = stepSizeInByte * 1f / byteBuffer.length;
            audioFrame.info.presentationTimeUs = startUs + (long) (duration * ratio);
            startUs = audioFrame.info.presentationTimeUs;
//            RecorderLogUtils.e(TAG, "deliver size: " + audioFrame.info.size + "; time:" + audioFrame.info.presentationTimeUs);
            deliverData(audioFrame);

            // TODO
            bytesRead = readNextAudioBlock();
            audioEvent.setOverlap(floatOverlap);
        }

        lastTimeUs = timeUs;
    }

    private int readStream(byte[] data, int offset, int len) {
        if (len > cacheSize) {
            // 结束，等待新数据
            System.arraycopy(cacheByteBuffer, cacheOffset, cacheByteBuffer, 0, cacheSize);
            cacheOffset = 0;

            return ERR_CONTINUE;
        }

        System.arraycopy(cacheByteBuffer, cacheOffset, data, offset, len);
        cacheOffset += len;
        cacheSize -= len;

        return len;
    }

    @Override
    public void release() {

    }
}

