package de.tisoft.jsquashfs.compression.lzma;

import de.tisoft.jsquashfs.compression.Decompressor;
import org.tukaani.xz.LZMAInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

public class Lzma implements Decompressor {
    @Override
    public byte[] uncompress(byte[] rawData, int maxSize, boolean padded) {
        try {
            final LZMAInputStream lzmaInputStream = new LZMAInputStream(new ByteArrayInputStream(rawData));
            lzmaInputStream.enableRelaxedEndCondition();
            byte[] bytes = lzmaInputStream.readAllBytes();
            if (bytes.length == maxSize || !padded) {
                // maxSize reached or no padding needed, the buffer can be returned as is
                return bytes;
            } else {
                // pad the remaining contents of the buffer with 0
                byte[] paddedBytes = new byte[maxSize];
                System.arraycopy(paddedBytes, 0, bytes, 0, bytes.length);
                Arrays.fill(paddedBytes, bytes.length, paddedBytes.length - 1, (byte) 0);
                return paddedBytes;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
