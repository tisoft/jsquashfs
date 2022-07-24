package de.tisoft.jsquashfs.compression.lz4;

import de.tisoft.jsquashfs.compression.Decompressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

public class Lz4 implements Decompressor {
    @Override
    public byte[] uncompress(byte[] rawData, int maxSize, boolean padded) {
            final byte[] dest = new byte[maxSize];
            LZ4Factory.fastestJavaInstance().fastDecompressor().decompress(rawData, dest);

            return dest;
    }
}
