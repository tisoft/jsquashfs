package de.tisoft.jsquashfs.compression;

public interface Decompressor {
    byte[] uncompress(byte[] raw_data, int maxSize, boolean padded);
}
