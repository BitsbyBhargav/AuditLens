package com.auditlens.portal.service;

/**
 * Document storage. LocalStorageService is used for the local demo.
 * The cloud phase adds an S3 implementation behind this same interface,
 * where lock() applies S3 Object Lock (Compliance mode) instead of a read-only flag.
 */
public interface StorageService {
    /** Stores a new object. Never overwrites: each version gets its own key. */
    String store(int documentId, int version, String fileName, byte[] content);
    byte[] read(String key);
    /** Applies write protection to the object. */
    void lock(String key);
    boolean isLocked(String key);
}
