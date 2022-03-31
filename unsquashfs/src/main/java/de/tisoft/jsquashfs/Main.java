package de.tisoft.jsquashfs;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.tisoft.jsquashfs.parser.Squashfs;
import picocli.CommandLine;

// This is a console application, so using System.out is fine
@SuppressWarnings("java:S106")
@CommandLine.Command(name = "ASCIIArt", version = "ASCIIArt 1.0", sortOptions = false)
public class Main implements Runnable{

    @CommandLine.Option(names = {"${picocli.version.name.0:--v}", "${picocli.version.name.1:---version}"}, versionHelp = true, descriptionKey = "mixinStandardHelpOptions.version",
            description = "Print version information and exit.")
    private boolean versionRequested;


    @CommandLine.Parameters(paramLabel = "FILESYSTEM", hideParamSyntax = true)
    private String filesystem;


    @CommandLine.Parameters(paramLabel = "[files to extract or exclude (with -excludes) or cat (with -cat )]", hideParamSyntax = true)
    private String[] files;

    @CommandLine.Option(names = { "-s", "--font-size" }, description = "Font size")
    int fontSize = 19;


    @CommandLine.Option(names = {"${picocli.help.name.0:--h}", "${picocli.help.name.1:---help}"}, usageHelp = true, descriptionKey = "mixinStandardHelpOptions.help",
            description = "Show this help message and exit.")
    private boolean helpRequested;

    public static void main(String[] args) throws IOException {

        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }


    @Override
    public void run() {
        try {
            Squashfs squashfs = Squashfs.fromFile(filesystem);

            squashfs.superblock().versionMajor();

            Map<Long, Squashfs.InodeHeader> inodes = squashfs.inodeTable().inodes().inodeHeader().stream().collect(Collectors.toMap(Squashfs.InodeHeader::inodeNumber, Function.identity()));

            Squashfs.InodeHeader rootInode = squashfs.superblock().rootInodeRef().inodeTable().inodeHeader();

            recurse(inodes, rootInode, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void recurse(Map<Long, Squashfs.InodeHeader> inodes, Squashfs.InodeHeader inodeHeader, String prefix) {
        System.out.println(prefix + inodeHeader.inodeNumber() + " " + inodeHeader.type());
        if (inodeHeader.type() == Squashfs.InodeType.BASIC_DIRECTORY) {
            Squashfs.InodeHeaderBasicDirectory directory = (Squashfs.InodeHeaderBasicDirectory) inodeHeader.header();
            directory.dir().directoryHeader().stream()
                    .map(Squashfs.DirectoryHeader::directoryEntry)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .forEach(directoryEntry -> {
                        System.out.println(prefix + directoryEntry.name());
                        recurse(inodes, inodes.get((long) directoryEntry._parent().inodeNumber() + directoryEntry.inodeOffset()), prefix + " ");
                    });
            clearMemory(directory, "dir", "_raw_dir", "directoryTable", "_raw_directoryTable");
        } else if (inodeHeader.type() == Squashfs.InodeType.EXTENDED_DIRECTORY) {
            Squashfs.InodeHeaderExtendedDirectory directory = (Squashfs.InodeHeaderExtendedDirectory) inodeHeader.header();
            directory.dir().directoryHeader().stream()
                    .map(Squashfs.DirectoryHeader::directoryEntry)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .forEach(directoryEntry -> {
                        System.out.println(prefix + directoryEntry.name());
                        recurse(inodes, inodes.get((long) directoryEntry._parent().inodeNumber() + directoryEntry.inodeOffset()), prefix + " ");
                    });
            clearMemory(directory, "dir", "_raw_dir", "directoryTable", "_raw_directoryTable");
        } else if (inodeHeader.type() == Squashfs.InodeType.BASIC_FILE) {
            Squashfs.InodeHeaderBasicFile file = (Squashfs.InodeHeaderBasicFile) inodeHeader.header();
            System.out.println(prefix + "size: " + file.fileSize());
            byte[] data = new byte[(int) file.fileSize()];
            int offset = 0;
            for (Squashfs.DataBlock block : file.blocks()) {
                //last block might be not fully used, we may need to cap the length
                final int length = Math.min(block.data().data().length, (int) file.fileSize() - offset);
                System.arraycopy(block.data().data(), 0, data, offset, length);
                offset += length;
            }
            if (file.fragIndex() != 0xFFFFFFFFL) {
                System.arraycopy(file.fragment().block().data().data(), (int) file.blockOffset(), data, offset, data
                        .length - offset);
                System.out.println(prefix + "fragment: " + offset);
            } else {
                System.out.println(prefix + "finished: " + offset);
            }
            clearMemory(file, "blocks", "_raw_blocks", "fragment");
        } else {
            throw new IllegalArgumentException("Unsupported inode type " + inodeHeader.type());
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
