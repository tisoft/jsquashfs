package de.tisoft.jsquashfs.compression.lzo;

import java.util.Arrays;

import de.tisoft.jsquashfs.compression.Decompressor;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoConstraint;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.lzo_uintp;

/**
 * This file links against LZO and is therefore released under the terms of the GPL
 */
public final class Lzo implements Decompressor {
    public byte[] uncompress(byte[] rawData, int maxSize, boolean padded) {
        byte[] buffer = new byte[maxSize];
        lzo_uintp outLen = new lzo_uintp(buffer.length);
        LzoLibrary.getInstance().newDecompressor(LzoAlgorithm.LZO1X, LzoConstraint.SAFETY)
                .decompress(rawData, 0, rawData.length, buffer, 0, outLen);
        if (outLen.value == maxSize) {
            // maxSize reached, the buffer can be returned as is
            return buffer;
        } else if (padded) {
            // pad the remaining contents of the buffer with 0
            Arrays.fill(buffer, outLen.value, buffer.length - 1, (byte) 0);
            return buffer;
        } else {
            // shrink to actual size
            byte[] bytes = new byte[outLen.value];
            System.arraycopy(buffer, 0, bytes, 0, bytes.length);
            return bytes;
        }
    }
}
