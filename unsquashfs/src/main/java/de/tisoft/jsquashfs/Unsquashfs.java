package de.tisoft.jsquashfs;

import static org.slieb.throwables.ConsumerWithThrowable.aConsumerThatUnsafelyThrowsUnchecked;

import de.tisoft.jsquashfs.compression.lzo.LzoAvailabilityCheck;
import de.tisoft.jsquashfs.parser.Squashfs;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import picocli.AutoComplete;
import picocli.CommandLine;

// This is a console application, so using System.out is fine
@SuppressWarnings("java:S106")
@CommandLine.Command(
    name = "junsquashfs",
    sortOptions = false,
    versionProvider = Unsquashfs.Version.class,
    subcommands = AutoComplete.GenerateCompletion.class,
    synopsisSubcommandLabel = "")
public class Unsquashfs implements Runnable {

  static class Version implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
      Manifest manifest = new Manifest(getClass().getResourceAsStream("/META-INF/MANIFEST.MF"));
      Attributes attr = manifest.getMainAttributes();
      if (LzoAvailabilityCheck.isLzoAvailable()) {
        return new String[] {
          "junsquashfs version "
              + attr.getValue("Implementation-Version")
              + " ("
              + attr.getValue("Implementation-Build-Date")
              + ") with LZO support",
          "copyright (C) 2022 Markus Heberling <markus@tisoft.de>",
          "",
          "This program is free software; you can redistribute it and/or",
          "modify it under the terms of the GNU General Public License",
          "as published by the Free Software Foundation; either version 2,",
          "or (at your option) any later version.",
          "",
          "This program is distributed in the hope that it will be useful,",
          "but WITHOUT ANY WARRANTY; without even the implied warranty of",
          "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the",
          "GNU General Public License for more details."
        };
      } else {
        return new String[] {
          "junsquashfs version "
              + attr.getValue("Implementation-Version")
              + " ("
              + attr.getValue("Implementation-Build-Date")
              + ")",
          "copyright (C) 2022 Markus Heberling <markus@tisoft.de>",
          "",
          "Permission is hereby granted, free of charge, to any person obtaining a copy",
          "of this software and associated documentation files (the \"Software\"), to deal",
          "in the Software without restriction, including without limitation the rights",
          "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell",
          "copies of the Software, and to permit persons to whom the Software is",
          "furnished to do so, subject to the following conditions:",
          "",
          "The above copyright notice and this permission notice shall be included in all",
          "copies or substantial portions of the Software.",
          "",
          "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR",
          "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,",
          "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE",
          "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER",
          "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,",
          "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE",
          "SOFTWARE."
        };
      }
    }
  }

  @CommandLine.Option(
      names = {"${picocli.version.name.0:--v}", "${picocli.version.name.1:--version}"},
      versionHelp = true,
      descriptionKey = "mixinStandardHelpOptions.version",
      description = "Print version information and exit.")
  private boolean versionRequested;

  @CommandLine.Parameters(paramLabel = "FILESYSTEM", hideParamSyntax = true)
  private String filesystem;

  @CommandLine.Parameters(
      paramLabel = "[files to extract or exclude (with -excludes) or cat (with -cat )]",
      hideParamSyntax = true)
  private String[] files;

  @CommandLine.Option(
      names = {"-d", "-dest"},
      paramLabel = "pathname",
      arity = "1",
      description = "extract to <pathname>, default \"${DEFAULT-VALUE}\"",
      defaultValue = "squashfs-root")
  private String dest;

  @CommandLine.Option(
      names = {"${picocli.help.name.0:--h}", "${picocli.help.name.1:--help}"},
      usageHelp = true,
      descriptionKey = "mixinStandardHelpOptions.help",
      description = "Show this help message and exit.")
  private boolean helpRequested;

  public static void main(String[] args) {
    CommandLine cmd = new CommandLine(new Unsquashfs());
    CommandLine gen = cmd.getSubcommands().get("generate-completion");
    gen.getCommandSpec().usageMessage().hidden(true);
    int exitCode = cmd.execute(args);
    System.exit(exitCode);
  }

  @Override
  public void run() {
    try {
      Squashfs squashfs = Squashfs.fromFile(filesystem);

      squashfs.superblock().versionMajor();

      Map<Long, Squashfs.InodeHeader> inodes =
          squashfs.inodeTable().inodes().inodeHeader().stream()
              .collect(Collectors.toMap(Squashfs.InodeHeader::inodeNumber, Function.identity()));

      Squashfs.InodeHeader rootInode =
          squashfs.superblock().rootInodeRef().inodeTable().inodeHeader();

      recurse(inodes, rootInode, new File(dest));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void recurse(
      Map<Long, Squashfs.InodeHeader> inodes, Squashfs.InodeHeader inodeHeader, File dest)
      throws IOException {
    System.out.println(dest + " " + inodeHeader.inodeNumber() + " " + inodeHeader.type());
    if (inodeHeader.type() == Squashfs.InodeType.BASIC_DIRECTORY) {
      if (!dest.isDirectory() && !dest.mkdir()) {
        throw new IOException("Could not create directory " + dest);
      }
      Squashfs.InodeHeaderBasicDirectory directory =
          (Squashfs.InodeHeaderBasicDirectory) inodeHeader.header();
      directory.dir().directoryHeader().stream()
          .map(Squashfs.DirectoryHeader::directoryEntry)
          .filter(Objects::nonNull)
          .flatMap(List::stream)
          .forEach(
              aConsumerThatUnsafelyThrowsUnchecked(
                  directoryEntry -> {
                    File dir = new File(dest, directoryEntry.name());
                    System.out.println(dir);
                    recurse(
                        inodes,
                        inodes.get(
                            (long) directoryEntry._parent().inodeNumber()
                                + directoryEntry.inodeOffset()),
                        dir);
                  }));
      clearMemory(directory, "dir", "_raw_dir", "directoryTable", "_raw_directoryTable");
    } else if (inodeHeader.type() == Squashfs.InodeType.EXTENDED_DIRECTORY) {
      if (!dest.isDirectory() && !dest.mkdir()) {
        throw new IOException("Could not create directory " + dest);
      }
      Squashfs.InodeHeaderExtendedDirectory directory =
          (Squashfs.InodeHeaderExtendedDirectory) inodeHeader.header();
      directory.dir().directoryHeader().stream()
          .map(Squashfs.DirectoryHeader::directoryEntry)
          .filter(Objects::nonNull)
          .flatMap(List::stream)
          .forEach(
              aConsumerThatUnsafelyThrowsUnchecked(
                  directoryEntry -> {
                    File dir = new File(dest, directoryEntry.name());
                    System.out.println(dir);
                    recurse(
                        inodes,
                        inodes.get(
                            (long) directoryEntry._parent().inodeNumber()
                                + directoryEntry.inodeOffset()),
                        dir);
                  }));
      clearMemory(directory, "dir", "_raw_dir", "directoryTable", "_raw_directoryTable");
    } else if (inodeHeader.type() == Squashfs.InodeType.BASIC_FILE) {
      Squashfs.InodeHeaderBasicFile file = (Squashfs.InodeHeaderBasicFile) inodeHeader.header();
      System.out.println(dest + " size: " + file.fileSize());
      byte[] data = new byte[(int) file.fileSize()];
      int offset = 0;
      for (Squashfs.DataBlock block : file.blocks()) {
        // last block might be not fully used, we may need to cap the length
        final int length = Math.min(block.data().data().length, (int) file.fileSize() - offset);
        System.arraycopy(block.data().data(), 0, data, offset, length);
        offset += length;
      }
      if (file.fragIndex() != 0xFFFFFFFFL) {
        System.arraycopy(
            file.fragment().block().data().data(),
            (int) file.blockOffset(),
            data,
            offset,
            data.length - offset);
        System.out.println(dest + " fragment: " + offset);
      } else {
        System.out.println(dest + " finished: " + offset);
      }
      Files.write(dest.toPath(), data);
      clearMemory(file, "blocks", "_raw_blocks", "fragment");
    } else if (inodeHeader.type() == Squashfs.InodeType.EXTENDED_FILE) {
      Squashfs.InodeHeaderExtendedFile file =
          (Squashfs.InodeHeaderExtendedFile) inodeHeader.header();
      System.out.println(dest + " size: " + file.fileSize());
      byte[] data = new byte[(int) file.fileSize()];
      int offset = 0;
      for (Squashfs.DataBlock block : file.blocks()) {
        // last block might be not fully used, we may need to cap the length
        final int length = Math.min(block.data().data().length, (int) file.fileSize() - offset);
        System.arraycopy(block.data().data(), 0, data, offset, length);
        offset += length;
      }
      if (file.fragIndex() != 0xFFFFFFFFL) {
        System.arraycopy(
            file.fragment().block().data().data(),
            (int) file.blockOffset(),
            data,
            offset,
            data.length - offset);
        System.out.println(dest + " fragment: " + offset);
      } else {
        System.out.println(dest + " finished: " + offset);
      }
      Files.write(dest.toPath(), data);
      clearMemory(file, "blocks", "_raw_blocks", "fragment");
    } else {
      throw new IOException("Unsupported inode type " + inodeHeader.type());
    }
  }

  private static void clearMemory(Object directory, String... fieldNames) {
    // claim back memory, currently only working with reflection
    for (String fieldName : fieldNames) {
      try {
        Field f = directory.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(directory, null);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
