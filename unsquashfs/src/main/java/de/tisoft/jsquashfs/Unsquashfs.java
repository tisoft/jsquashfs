package de.tisoft.jsquashfs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.tisoft.jsquashfs.parser.Squashfs;
import org.slieb.throwables.ConsumerWithThrowable;
import picocli.CommandLine;

import static org.slieb.throwables.ConsumerWithThrowable.aConsumerThatUnsafelyThrowsUnchecked;

// This is a console application, so using System.out is fine
@SuppressWarnings("java:S106")
@CommandLine.Command(name = "unsquashfs", sortOptions = false)
public class Unsquashfs implements Runnable{

    @CommandLine.Option(names = {"${picocli.version.name.0:--v}", "${picocli.version.name.1:--version}"}, versionHelp = true, descriptionKey = "mixinStandardHelpOptions.version",
            description = "Print version information and exit.")
    private boolean versionRequested;


    @CommandLine.Parameters(paramLabel = "FILESYSTEM", hideParamSyntax = true)
    private String filesystem;


    @CommandLine.Parameters(paramLabel = "[files to extract or exclude (with -excludes) or cat (with -cat )]", hideParamSyntax = true)
    private String[] files;

    @CommandLine.Option(names = {"-d", "-dest"}, paramLabel = "pathname", arity = "1", description = "extract to <pathname>, default \"squashfs-root\"", defaultValue = "squashfs-root")
    private String dest;


    @CommandLine.Option(names = {"${picocli.help.name.0:--h}", "${picocli.help.name.1:---help}"}, usageHelp = true, descriptionKey = "mixinStandardHelpOptions.help",
            description = "Show this help message and exit.")
    private boolean helpRequested;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Unsquashfs()).execute(args);
        System.exit(exitCode);
    }


    @Override
    public void run() {
        try {
            Squashfs squashfs = Squashfs.fromFile(filesystem);

            squashfs.superblock().versionMajor();

            Map<Long, Squashfs.InodeHeader> inodes = squashfs.inodeTable().inodes().inodeHeader().stream().collect(Collectors.toMap(Squashfs.InodeHeader::inodeNumber, Function.identity()));

            Squashfs.InodeHeader rootInode = squashfs.superblock().rootInodeRef().inodeTable().inodeHeader();

            recurse(inodes, rootInode, new File(dest));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void recurse(Map<Long, Squashfs.InodeHeader> inodes, Squashfs.InodeHeader inodeHeader, File dest) throws IOException {
        System.out.println(dest + " " + inodeHeader.inodeNumber() + " " + inodeHeader.type());
        if (inodeHeader.type() == Squashfs.InodeType.BASIC_DIRECTORY) {
            if (!dest.isDirectory() && !dest.mkdir()) {
                throw new IOException("Could not create directory " + dest);
            }
            Squashfs.InodeHeaderBasicDirectory directory = (Squashfs.InodeHeaderBasicDirectory) inodeHeader.header();
            directory.dir().directoryHeader().stream()
                    .map(Squashfs.DirectoryHeader::directoryEntry)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .forEach(aConsumerThatUnsafelyThrowsUnchecked(directoryEntry -> {
                        File dir = new File(dest, directoryEntry.name());
                        System.out.println(dir);
                        recurse(inodes, inodes.get((long) directoryEntry._parent().inodeNumber() + directoryEntry.inodeOffset()), dir);
                    }));
            clearMemory(directory, "dir", "_raw_dir", "directoryTable", "_raw_directoryTable");
        } else if (inodeHeader.type() == Squashfs.InodeType.EXTENDED_DIRECTORY) {
            if (!dest.isDirectory() && !dest.mkdir()) {
                throw new IOException("Could not create directory " + dest);
            }
            Squashfs.InodeHeaderExtendedDirectory directory = (Squashfs.InodeHeaderExtendedDirectory) inodeHeader.header();
            directory.dir().directoryHeader().stream()
                    .map(Squashfs.DirectoryHeader::directoryEntry)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .forEach(aConsumerThatUnsafelyThrowsUnchecked(directoryEntry -> {
                        File dir = new File(dest, directoryEntry.name());
                        System.out.println(dir);
                        recurse(inodes, inodes.get((long) directoryEntry._parent().inodeNumber() + directoryEntry.inodeOffset()), dir);
                    }));
            clearMemory(directory, "dir", "_raw_dir", "directoryTable", "_raw_directoryTable");
        } else if (inodeHeader.type() == Squashfs.InodeType.BASIC_FILE) {
            Squashfs.InodeHeaderBasicFile file = (Squashfs.InodeHeaderBasicFile) inodeHeader.header();
            System.out.println(dest + " size: " + file.fileSize());
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
                System.out.println(dest + " fragment: " + offset);
            } else {
                System.out.println(dest + " finished: " + offset);
            }
            Files.write(dest.toPath(), data);
            clearMemory(file, "blocks", "_raw_blocks", "fragment");
        } else if (inodeHeader.type() == Squashfs.InodeType.EXTENDED_FILE) {
            Squashfs.InodeHeaderExtendedFile file = (Squashfs.InodeHeaderExtendedFile) inodeHeader.header();
            System.out.println(dest + " size: " + file.fileSize());
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
