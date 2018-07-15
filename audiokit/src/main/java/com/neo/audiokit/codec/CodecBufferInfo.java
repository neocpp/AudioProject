
package com.neo.audiokit.codec;
public final class CodecBufferInfo {

    public static final int BUFFER_FLAG_SYNC_FRAME = 1;

    /**
     * This indicates that the (encoded) buffer marked as such contains
     * the data for a key frame.
     */
    public static final int BUFFER_FLAG_KEY_FRAME = 1;

    /**
     * This indicated that the buffer marked as such contains codec
     * initialization / codec specific data instead of media data.
     */
    public static final int BUFFER_FLAG_CODEC_CONFIG = 2;

    /**
     * This signals the end of stream, i.e. no buffers will be available
     * after this, unless of course, {link flush} follows.
     */
    public static final int BUFFER_FLAG_END_OF_STREAM = 4;

    public static final int COLOR_FormatYUV420Flexible            = 0x7F420888;
    public static final int COLOR_FormatSurface                   = 0x7F000789;

    public static final int AACObjectLC         = 2;
    public void set(
            int newOffset, int newSize, long newTimeUs,  int newFlags) {
        offset = newOffset;
        size = newSize;
        presentationTimeUs = newTimeUs;
        flags = newFlags;
    }

    /**
     * The start-offset of the data in the buffer.
     */
    public int offset;

    /**
     * The amount of data (in bytes) in the buffer.  If this is {@code 0},
     * the buffer has no data in it and can be discarded.  The only
     * use of a 0-size buffer is to carry the end-of-stream marker.
     */
    public int size;

    /**
     * The presentation timestamp in microseconds for the buffer.
     * This is derived from the presentation timestamp passed in
     * with the corresponding input buffer.  This should be ignored for
     * a 0-sized buffer.
     */
    public long presentationTimeUs;

    public long decodeTimeUs;


    public int flags;

}
