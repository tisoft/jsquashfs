package de.tisoft.jsquashfs.compression.lz4;

import de.tisoft.jsquashfs.compression.Decompressor;
import java.util.Arrays;
import net.jpountz.lz4.LZ4Factory;

public class Lz4 implements Decompressor {
  @Override
  public byte[] uncompress(byte[] rawData, int maxSize, boolean padded) {
    final byte[] dest = new byte[maxSize];
    int len = LZ4Factory.fastestInstance().safeDecompressor().decompress(rawData, dest);
    if (len == maxSize) {
      // maxSize reached, the buffer can be returned as is
      return dest;
    } else if (padded) {
      // pad the remaining contents of the buffer with 0
      Arrays.fill(dest, len, dest.length - 1, (byte) 0);
      return dest;
    } else {
      // shrink to actual size
      byte[] bytes = new byte[len];
      System.arraycopy(dest, 0, bytes, 0, bytes.length);
      return bytes;
    }
  }
}
