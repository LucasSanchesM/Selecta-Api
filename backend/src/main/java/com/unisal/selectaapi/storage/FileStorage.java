package com.unisal.selectaapi.storage;

import java.util.UUID;

public interface FileStorage {
    StoredFile store(String directory, String fileName, byte[] content, String contentType);
    StoredFile storeResume(UUID userId, byte[] content);
    byte[] readResume(String key);
    void deleteQuietly(String key);
}
