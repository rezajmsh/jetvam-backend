package ir.jetvam.modules.origination.service;

import java.util.UUID;

/**
 * Isolates application document persistence from the origination domain.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface ApplicationDocumentStorage {

    StoredDocument store(UUID applicationId, String originalFilename, byte[] content);

    void delete(String storageKey);

    record StoredDocument(String storageKey, String checksumSha256, long sizeBytes) {
    }
}
