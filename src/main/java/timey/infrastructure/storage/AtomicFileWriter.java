package timey.infrastructure.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes a file through a temporary sibling and atomically replaces the target when supported. */
final class AtomicFileWriter {
    private AtomicFileWriter() {
    }

    static void write(Path target, String temporaryFilePrefix, ContentWriter contentWriter) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporaryFile = Files.createTempFile(target.toAbsolutePath().getParent(), temporaryFilePrefix, ".tmp");
        try {
            contentWriter.write(temporaryFile);
            moveIntoPlace(temporaryFile, target);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static void moveIntoPlace(Path temporaryFile, Path target) throws IOException {
        try {
            Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    interface ContentWriter {
        void write(Path temporaryFile) throws IOException;
    }
}
