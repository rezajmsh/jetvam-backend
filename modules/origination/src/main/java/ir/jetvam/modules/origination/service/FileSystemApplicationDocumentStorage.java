package ir.jetvam.modules.origination.service;

import ir.jetvam.common.validation.Preconditions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Development document-storage adapter backed by a configured local directory.
 * Production object storage can replace this adapter without changing domain services.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public class FileSystemApplicationDocumentStorage implements ApplicationDocumentStorage {

    private final Path root;

    public FileSystemApplicationDocumentStorage(
            @Value("${jetvam.origination.documents.root:./data/application-documents}") String root
    ) {
        this.root = Path.of(Preconditions.requireText(root, "documentStorageRoot")).toAbsolutePath().normalize();
    }

    @Override
    public StoredDocument store(UUID applicationId, String originalFilename, byte[] content) {
        Preconditions.requireNonNull(applicationId, "applicationId");
        Preconditions.requireText(originalFilename, "originalFilename");
        Preconditions.require(content != null && content.length > 0, "Document content must not be empty");
        String extension = safeExtension(originalFilename);
        String storageKey = applicationId + "/" + UUID.randomUUID() + extension;
        Path target = resolve(storageKey);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredDocument(storageKey, sha256(content), content.length);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store application document", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete application document", exception);
        }
    }

    private Path resolve(String storageKey) {
        Path resolved = root.resolve(Preconditions.requireText(storageKey, "storageKey")).normalize();
        Preconditions.require(resolved.startsWith(root), "Invalid document storage key");
        return resolved;
    }

    private static String safeExtension(String filename) {
        String leaf = Path.of(filename).getFileName().toString();
        int dot = leaf.lastIndexOf('.');
        if (dot < 0 || dot == leaf.length() - 1) {
            return "";
        }
        String extension = leaf.substring(dot).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
