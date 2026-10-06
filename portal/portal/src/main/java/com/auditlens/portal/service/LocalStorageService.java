package com.auditlens.portal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;

/**
 * Local stand-in for S3. Locking sets the file read-only at the OS level.
 * This is tamper-evident (the recorded SHA-256 exposes any change), not tamper-proof:
 * an OS administrator can clear the read-only flag. S3 Object Lock in Compliance mode
 * closes that gap in the cloud phase.
 */
@Service
public class LocalStorageService implements StorageService {
    private final Path root;

    public LocalStorageService(@Value("${auditlens.storage-dir}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public String store(int documentId, int version, String fileName, byte[] content) {
        String key = "doc-" + documentId + "/v" + version + "/" + sanitize(fileName);
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store " + key, e);
        }
        return key;
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + key, e);
        }
    }

    @Override
    public void lock(String key) {
        if (!resolve(key).toFile().setReadOnly()) {
            throw new IllegalStateException("Could not set " + key + " read-only");
        }
    }

    @Override
    public boolean isLocked(String key) {
        Path p = resolve(key);
        return Files.exists(p) && !Files.isWritable(p);
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
        return p;
    }

    private static String sanitize(String name) {
        String base = name == null ? "document" : Path.of(name).getFileName().toString();
        String clean = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return clean.isBlank() ? "document" : clean;
    }
}
