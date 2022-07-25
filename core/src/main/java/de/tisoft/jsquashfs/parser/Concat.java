package de.tisoft.jsquashfs.parser;

import io.kaitai.struct.CustomDecoder;
import java.util.List;
import java.util.stream.Collectors;

public class Concat implements CustomDecoder {
  private final List<Squashfs.Metablock> metablocks;

  public Concat(Squashfs.MetablockList metablockList) {
    this.metablocks = metablockList.metablock();
  }

  public Concat(List<Squashfs.MetablockReference> metablockReferenceList) {
    this.metablocks =
        metablockReferenceList.stream()
            .map(Squashfs.MetablockReference::metablock)
            .collect(Collectors.toList());
  }

  public byte[] decode(byte[] rawData) {
    byte[] data = new byte[0];
    for (Squashfs.Metablock metablock : metablocks) {
      byte[] blockData = metablock.data().data();
      byte[] combinedDate = new byte[data.length + blockData.length];
      System.arraycopy(data, 0, combinedDate, 0, data.length);
      System.arraycopy(blockData, 0, combinedDate, data.length, blockData.length);
      data = combinedDate;
    }
    return data;
  }
}
