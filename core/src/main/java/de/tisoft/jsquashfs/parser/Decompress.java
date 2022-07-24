package de.tisoft.jsquashfs.parser;

import de.tisoft.jsquashfs.compression.gzip.Gz;
import de.tisoft.jsquashfs.compression.lz4.Lz4;
import de.tisoft.jsquashfs.compression.lzo.LzoAvailabilityCheck;
import de.tisoft.jsquashfs.compression.xz.Xz;
import de.tisoft.jsquashfs.compression.zstd.Zstd;
import io.kaitai.struct.CustomDecoder;

public class Decompress implements CustomDecoder {
    private final boolean compressed;
    private final Squashfs.Compressor compressor;
    private final boolean padded;
    private final int maxSize;

    public Decompress(boolean compressed, Squashfs.Compressor compressor, long maxSize, boolean padded) {
        if (maxSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Can't handle maxSize: " + maxSize);
        }
        this.compressed = compressed;
        this.compressor = compressor;
        this.maxSize = (int) maxSize;
        this.padded = padded;
    }

    public byte[] decode(byte[] rawData) {
        if(rawData.length==0){
            // length 0 means, this is a sparse block, containing only 0s
            return new byte[maxSize];
        }
        if (compressed) {
            switch (compressor) {
                case ZLIB: {
                    return new Gz().uncompress(rawData, maxSize, padded);
                }
                case LZ4: {
                    return new Lz4().uncompress(rawData, maxSize, padded);
                }
                case LZO: {
                    if (LzoAvailabilityCheck.isLzoAvailable()) {
                        return LzoAvailabilityCheck.getLzoDecompressor().uncompress(rawData, maxSize, padded);
                    } else {
                        throw new IllegalArgumentException("Missing LZO dependency");
                    }
                }
                case XZ: {
                    return new Xz().uncompress(rawData, maxSize, padded);
                }
                case ZSTD: {
                    return new Zstd().uncompress(rawData, maxSize, padded);
                }
                default:
                    throw new IllegalArgumentException("Unsupported compression: " + compressor);
            }
        } else {
            return rawData;
        }
    }

}
