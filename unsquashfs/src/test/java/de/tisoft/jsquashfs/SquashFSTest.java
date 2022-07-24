package de.tisoft.jsquashfs;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.tisoft.jsquashfs.parser.Squashfs;
import io.kaitai.struct.ByteBufferKaitaiStream;
import io.kaitai.struct.RandomAccessFileKaitaiStream;
import org.junit.jupiter.api.Test;

class SquashFSTest {

    public File targetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../../test_data");
        return targetDir;
    }

    @Test
    void testGzip() throws IOException {
        test("sq.img.gzip");
    }

    @Test
    void testLzo() throws IOException {
        test("sq.img.lzo");
    }

    @Test
    void testXz() throws IOException {
        test("sq.img.xz");
    }

    private void test(String name) throws IOException {
        Squashfs squashfs = new Squashfs(new RandomAccessFileKaitaiStream(new File(targetDir(), name).getAbsolutePath()));


        Map<Long, Squashfs.InodeHeader> inodes = squashfs.inodeTable().inodes().inodeHeader().stream().collect(Collectors.toMap(Squashfs.InodeHeader::inodeNumber, Function.identity()));

        Squashfs.InodeHeader rootInode = squashfs.superblock().rootInodeRef().inodeTable().inodeHeader();

        Main.recurse(inodes, rootInode, "");

   //     Assertions.assertThat(new File()).ishasSameContentAs();
    }
}