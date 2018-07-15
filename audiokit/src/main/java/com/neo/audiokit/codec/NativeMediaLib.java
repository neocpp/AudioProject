package com.neo.audiokit.codec;


import android.view.Surface;

import java.nio.ByteBuffer;

public class NativeMediaLib {
    static {
        System.loadLibrary("native-lib");
    }
    //混音native接口
    public static native  long createMixAudio(int dstSampleRate,int dstBytePerSample,int dstChannelNum,float firstAudioWeight,float secondAudioWeight);
    public static native  int setFirstAudioFormat(long handle,int sampleRate,int bytePerSample,int channelNum);
    public static native  int setSecondAudioFormat(long handle,int sampleRate,int bytePerSample,int channelNum);
    public static native  int sendFirstAudioData(long handle,byte[]data,int dataSize);
    public static native  int sendSecondAudioData(long handle,byte[]data,int dataSize);
    public static native  int getResultAuidoData(long handle,byte[]dstData,int isEnd);
    public static native  int closeMixAudio(long handle);

    //mp4 Muxer接口
    public static native long createMp4Muxer();
    public static native int  setVideoConfig(long handle,int enable,int aiWidth,int aiHeight,int aiBitrate);
    public static native int  setAudioConfig(long handle,int enable,int sampleRate,int channelNum,int aiBitRate);
    public static native int  openFile(long handle,String filename);
    public static native int  setMetadata(long handle,String key,String value);
    public static native int  writeSampleData(long handle,int trackType,byte[]src, int srcLen,long  pts, long dts, long duration,int isKeyFrame);
    public static native int  closeFile(long handle,int aiWidth,int aiHeight,byte[] videoCodecData,int videoCodecDataSize,byte[] audioCodecData,int audioCodecDataSize);
    public static native int  destroyMp4Muxer(long handle);

    //mp4 Reader native interface
    public static native long mp4ReaderCreate();
    //open file and get file media info method
    public static native int mp4ReaderOpenFile(long handle,String path);
    public static native int mp4ReaderHasAudioTrack(long handle);
    public static native int mp4ReaderHasVideoTrack(long handle);
    public static native int mp4ReaderGetWidth(long handle);
    public static native int mp4ReaderGetHeight(long handle);
    public static native int mp4ReaderGetRotation(long handle);
    public static native long mp4ReaderGetDuration(long handle);
    public static native long mp4ReaderGetAudioDuration(long handle);
    public static native int mp4ReaderGetChannelNum(long handle);
    public static native int mp4ReaderGetSampleRate(long handle);
    public static native String mp4ReaderGetAudioMimetype(long handle);
    public static native byte[] mp4ReaderGetSPSData(long handle);
    public static native byte[] mp4ReaderGetPPSData(long handle);
    public static native byte[] mp4ReaderGetAudioConfigData(long handle);
    //read frame and get frame info method
    public static native int mp4ReaderReadPacket(long handle,ByteBuffer byteBuffer,int size);
    public static native long mp4ReaderGetFramePts(long handle);
    public static native long mp4ReaderGetFrameDts(long handle);
    public static native int mp4ReaderGetFrameTrackType(long handle);
    public static native int mp4ReaderGetFrameFlag(long handle);
    //seek method
    public static native int mp4ReaderSeekTo(long handle,long timeUs);
    //close file and free buffer
    public static native int mp4ReaderCloseFile(long handle);
    public static native int mp4ReaderDestroy(long handle);

    //audio decoder native interface
    public static native long audioDecoderCreate();
    public static native int audioDecoderOpenDecode(long handle,int sampleRate,int channelNum,int codeID,byte[]extData,int extDataSize);
    public static native int audioDecoderDecodeFrame(long handle,ByteBuffer inByteBuffer,int inSize,long inPts);
    public static native ByteBuffer audioDecoderGetFrameOutBuffer(long handle);
    public static native long audioDecoderGetFramePts(long handle);
    public static native int audioDecoderCloseDecode(long handle);
    public static native int audioDecoderDestory(long handle);

    //audio encoder native interface
    public static native long audioEncoderCreate();
    public static native int audioEncoderOpenEncode(long handle,int sampleRate,int channelNum,int bitsrate);
    public static native byte[] audioEncoderGetCsd0(long handle);
    public static native int audioEncoderEncodeFrame(long handle,Object inst,ByteBuffer inByteBuffer,int inSize,long inPts);
    public static native int audioEncoderCloseEncode(long handle);
    public static native int audioEncoderDestory(long handle);

    //video decoder native interface
    public static native long videoDecoderCreate();
    public static native int videoDecoderOpenDecode(long handle,int aiWidth,int aiHeight,int aiType,Surface surface);
    public static native int videoDecoderSetSPS(long handle,byte[]data,int dataSize);
    public static native int videoDecoderSetPPS(long handle,byte[]data,int dataSize);
    public static native int videoDecoderDecodeFrame(long handle,Object inst,ByteBuffer inByteBuffer,int inSize,long inPts);
    public static native int videoDecoderCloseDecode(long handle,Object inst);
    public static native int videoDecoderDestory(long handle);

    //video encoder native interface
    public static native long videoEncoderCreate();
    public static native int videoEncoderOpenEncode(long handle,int aiWidth,int aiHeight,int bitrate,int IGap);
    public static native byte[] videoEncoderGetSPS(long handle);
    public static native byte[] videoEncoderGetPPS(long handle);
    public static native int videoEncoderSendData(long handle,ByteBuffer inByteBuffer,int inSize,long inPts);
    public static native void videoEncoderInitReadPixel(long handle,int width,int height,int apiLevel);
    public static native void videoEncoderBind(long handle);
    public static native void videoEncoderNotifyReadPixel(long handle,long timeUs);
    public static native void videoEncoderUnBind(long handle);
    public static native void videoEncoderUnInitReadPixel(long handle);
    public static native int videoEncoderEncodeFrame(long handle,Object inst);
    public static native int videoEncoderCloseEncode(long handle);
    public static native int videoEncoderDestory(long handle);

    //sonic native interface
    public static native long sonicInitNative(int sampleRate, int channels);
    // When done with sound processing, it's best to call this method to clean up memory.
    public static native void sonicCloseNative(long sonicID);
    public static native void sonicFlushNative(long sonicID);
    // Note that changing the sonicsample rate or num channels will cause a flush.
    public static native void sonicSetSampleRateNative(long sonicID, int newSampleRate);
    public static native int sonicGetSampleRateNative(long sonicID);
    public static native void sonicSetNumChannelsNative(long sonicID, int newNumChannels);
    public static native int sonicGetNumChannelsNative(long sonicID);
    public static native void sonicSetPitchNative(long sonicID, float newPitch);
    public static native float sonicGetPitchNative(long sonicID);
    public static native void sonicSetSpeedNative(long sonicID, float newSpeed);
    public static native float sonicGetSpeedNative(long sonicID);
    public static native void sonicSetRateNative(long sonicID, float newRate);
    public static native float sonicGetRateNative(long sonicID);
    public static native void sonicSetChordPitchNative(long sonicID, boolean useChordPitch);
    public static native boolean sonicGetChordPitchNative(long sonicID);
    public static native boolean sonicPutBytesNative(long sonicID, byte[] buffer, int lenBytes);
    public static native int sonicReceiveBytesNative(long sonicID, byte[] ret, int lenBytes);
    public static native int sonicAvailableBytesNative(long sonicID);
    public static native void sonicSetVolumeNative(long sonicID, float newVolume);
    public static native float sonicGetVolumeNative(long sonicID);

    //for native test
    public static native  String stringFromJNI(Object object);
    public static native void saveRGBA(ByteBuffer byteBuffer,int width,int height);
}
