package com.auditlens.portal.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {
    private Hashing() {}

    public static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String shortForm(String hash) {
        return hash == null ? "" : hash.substring(0, Math.min(12, hash.length())) + "…";
    }
}
