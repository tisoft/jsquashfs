package de.tisoft.jsquashfs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class UnsquashfsTest {
    @TempDir
    private Path directory;

    public File targetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        File targetDir = new File(relPath + "../../../test_data");
        return targetDir;
    }

    @Test
    void testGzip() throws Exception {
        test("sq.img.gzip");
    }

    @Test
    void testLz4() throws Exception {
        test("sq.img.lz4");
    }

    @Test
    void testLzma() throws Exception {
        test("sq.img.lzma");
    }

    @Test
    void testLzo() throws Exception {
        test("sq.img.lzo");
    }

    @Test
    void testXz() throws Exception {
        test("sq.img.xz");
    }

    @Test
    void testZstd() throws Exception {
        test("sq.img.zstd");
    }

    private void test(String name) throws Exception {
        int statusCode = catchSystemExit(() -> Unsquashfs.main(new String[]{new File(targetDir(), name).getAbsolutePath(), "-d", directory.toString()}));
        assertThat(statusCode).isZero();
        assertDirectory(new File(targetDir(), "data").toPath(), directory);
    }

    private void assertDirectory(Path source, Path dest) {
        System.out.println("Checking " + dest);
        File sourceFile = source.toFile();
        File destFile = dest.toFile();
        if (Files.isDirectory(source)) {
            assertThat(dest).isDirectory();
            assertThat(destFile.list()).containsExactlyInAnyOrder(sourceFile.list());
            for (String s : sourceFile.list()) {
                assertDirectory(new File(sourceFile, s).toPath(), new File(destFile, s).toPath());
            }
        } else if (Files.isRegularFile(source)) {
            assertThat(dest).isRegularFile();
            if (!Arrays.equals(digest(source), digest(dest))) {
                assertThat(dest).hasSameBinaryContentAs(source);
            }
        } else {
            fail("Unknown file type for " + source);
        }
    }

    private static byte[] digest(Path path) {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] dataBytes = new byte[1024];

            int nread;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }

            return md.digest();
        } catch (IOException | NoSuchAlgorithmException e) {
            fail("Can't calculate digest for " + path, e);
            // never reached
            throw new RuntimeException(e);
        }
    }
}