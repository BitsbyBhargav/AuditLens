package com.auditlens.portal.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads prepared CUAD test-split contracts: for each contract, <id>.txt (full text)
 * and <id>.snippets.json (gold clause snippets + the exact model input used in training).
 */
@Component
public class CuadLibrary {
    private static final String SUFFIX = ".snippets.json";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]+");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippets(String title, Map<String, String> clauses,
                           @JsonProperty("model_input") String modelInput) {}

    public record Entry(String id, String title, int clauseCount) {}

    private final Path dir;
    private final ObjectMapper mapper;

    public CuadLibrary(@Value("${auditlens.cuad-dir}") String dir, ObjectMapper mapper) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
        this.mapper = mapper;
    }

    public Path getDir() { return dir; }

    public List<Entry> list() {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(SUFFIX))
                    .map(this::toEntry)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Entry::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public Snippets snippets(String id) throws IOException {
        return mapper.readValue(resolve(id + SUFFIX).toFile(), Snippets.class);
    }

    public byte[] contractText(String id) throws IOException {
        return Files.readAllBytes(resolve(id + ".txt"));
    }

    public static int countPresent(Snippets s) {
        if (s.clauses() == null) return 0;
        return (int) RiskRules.RISK_CLAUSES.stream()
                .filter(c -> { String v = s.clauses().get(c); return v != null && !v.isBlank(); })
                .count();
    }

    private Entry toEntry(Path p) {
        String name = p.getFileName().toString();
        String id = name.substring(0, name.length() - SUFFIX.length());
        if (!SAFE.matcher(id).matches() || !Files.exists(dir.resolve(id + ".txt"))) return null;
        try {
            Snippets s = mapper.readValue(p.toFile(), Snippets.class);
            return new Entry(id, s.title() == null || s.title().isBlank() ? id : s.title(), countPresent(s));
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolve(String fileName) {
        if (!SAFE.matcher(fileName).matches()) throw new IllegalArgumentException("Unknown CUAD contract");
        Path p = dir.resolve(fileName).normalize();
        if (!p.startsWith(dir)) throw new IllegalArgumentException("Unknown CUAD contract");
        return p;
    }
}
