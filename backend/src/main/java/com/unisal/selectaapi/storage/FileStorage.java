package com.unisal.selectaapi.storage;

import java.util.UUID;

public interface FileStorage {
    StoredFile store(String directory, String fileName, byte[] content, String contentType);
    StoredFile storeResume(UUID userId, byte[] content);
    byte[] readResume(String key);

    /** Guarda um documento no bucket privado. A chave resultante fica em {@code private-documents/{directory}/{fileName}}. */
    StoredFile storePrivate(String directory, String fileName, byte[] content, String contentType);

    byte[] readPrivate(String key);

    void deleteQuietly(String key);
}
