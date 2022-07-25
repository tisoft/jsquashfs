package de.tisoft.jsquashfs;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UnsquashfsTest {
  @TempDir private Path directory;

  public static File targetDir() {
    String relPath =
        UnsquashfsTest.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    return new File(relPath + "../../../test_data");
  }

  private static File[] provideStringsForIsBlank() {
    return targetDir().listFiles((dir, name) -> name.startsWith("sq.img"));
  }

  @ParameterizedTest
  @MethodSource("provideStringsForIsBlank")
  void test(File file) throws Exception {
    int statusCode =
        catchSystemExit(
            () ->
                Unsquashfs.main(new String[] {file.getAbsolutePath(), "-d", directory.toString()}));
    assertThat(statusCode).isZero();
    assertDirectory(new File(targetDir(), "data").toPath(), directory);
  }

  private void assertDirectory(Path source, Path dest) throws IOException {
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
      assertThat(dest).isRegularFile().hasSize(Files.size(source));
      if (!Arrays.equals(digest(source), digest(dest))) {
        assertThat(dest).hasSameBinaryContentAs(source);
      }
    } else {
      fail("Unknown file type for " + source);
    }
  }

  private static byte[] digest(Path path) throws IOException {
    try (FileInputStream fis = new FileInputStream(path.toFile())) {
      MessageDigest md = MessageDigest.getInstance("SHA-256");

      byte[] dataBytes = new byte[1024];

      int nread;
      while ((nread = fis.read(dataBytes)) != -1) {
        md.update(dataBytes, 0, nread);
      }

      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }
}
