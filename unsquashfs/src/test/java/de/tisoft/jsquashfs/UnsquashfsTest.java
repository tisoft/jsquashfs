package de.tisoft.jsquashfs;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ginsberg.junit.exit.ExpectSystemExit;
import com.ginsberg.junit.exit.ExpectSystemExitWithStatus;
import de.tisoft.jsquashfs.parser.Squashfs;
import io.kaitai.struct.RandomAccessFileKaitaiStream;
import org.junit.jupiter.api.Test;

@ExpectSystemExitWithStatus(0)
class UnsquashfsTest {

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
    void testLz4() throws IOException {
        test("sq.img.lz4");
    }

    @Test
    void testLzma() throws IOException {
        test("sq.img.lzma");
    }

    @Test
    void testLzo() throws IOException {
        test("sq.img.lzo");
    }

    @Test
    void testXz() throws IOException {
        test("sq.img.xz");
    }

    @Test
    void testZstd() throws IOException {
        test("sq.img.zstd");
    }

    private void test(String name) throws IOException {
        Unsquashfs.main(new String[]{new File(targetDir(), name).getAbsolutePath()});


   //     Assertions.assertThat(new File()).ishasSameContentAs();
    }
}