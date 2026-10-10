package com.auditlens.portal.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads prepared CUAD test-split contracts: for each contract, <id>.txt (full text)
 * and <id>.snippets.json (gold clause snippets + the exact model input used in training).
 *
 * The folder is found automatically: the configured path first, then demo_data/cuad in the
 * working directory and in each parent folder. The first folder that actually holds
 * *.snippets.json files wins, so the portal works from any launch directory.
 */
@Component
public class CuadLibrary {
    private static final Logger log = LoggerFactory.getLogger(CuadLibrary.class);
    private static final String SUFFIX = ".snippets.json";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]+");

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Snippets(String title, Map<String, String> clauses,
                           @JsonProperty("model_input") String modelInput) {}

    public record Entry(String id, String title, int clauseCount) {}

    private final Path configured;
    private final ObjectMapper mapper;

    public CuadLibrary(@Value("${auditlens.cuad-dir}") String dir, ObjectMapper mapper) {
        this.configured = Path.of(dir).toAbsolutePath().normalize();
        this.mapper = mapper;
        log.info("CUAD folder candidates (first with contracts wins): {}", candidates());
    }

    private List<Path> candidates() {
        List<Path> c = new ArrayList<>();
        c.add(configured);
        Path p = Path.of("").toAbsolutePath().normalize();
        for (int i = 0; i < 6 && p != null; i++) {
            c.add(p.resolve("demo_data").resolve("cuad"));
            c.add(p.resolve("AuditLens").resolve("demo_data").resolve("cuad"));
            p = p.getParent();
        }
        return c.stream().distinct().toList();
    }

    private static boolean hasContracts(Path d) {
        if (!Files.isDirectory(d)) return false;
        try (Stream<Path> files = Files.list(d)) {
            return files.anyMatch(f -> f.getFileName().toString().endsWith(SUFFIX));
        } catch (IOException e) {
            return false;
        }
    }

    /** The first candidate folder that holds prepared contracts; otherwise the configured path. */
    public Path getDir() {
        for (Path c : candidates()) {
            if (hasContracts(c)) return c;
        }
        return configured;
    }

    public List<Entry> list() {
        Path dir = getDir();
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(SUFFIX))
                    .map(p -> toEntry(dir, p))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Entry::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list {}: {}", dir, e.getMessage());
            return List.of();
        }
    }

    public Snippets snippets(String id) throws IOException {
        return mapper.readValue(resolve(getDir(), id + SUFFIX).toFile(), Snippets.class);
    }

    public byte[] contractText(String id) throws IOException {
        return Files.readAllBytes(resolve(getDir(), id + ".txt"));
    }

    public static int countPresent(Snippets s) {
        if (s.clauses() == null) return 0;
        return (int) RiskRules.RISK_CLAUSES.stream()
                .filter(c -> { String v = s.clauses().get(c); return v != null && !v.isBlank(); })
                .count();
    }

    private Entry toEntry(Path dir, Path p) {
        String name = p.getFileName().toString();
        String id = name.substring(0, name.length() - SUFFIX.length());
        if (!SAFE.matcher(id).matches()) {
            log.warn("Skipping {}: unsupported characters in file name", name);
            return null;
        }
        if (!Files.exists(dir.resolve(id + ".txt"))) {
            log.warn("Skipping {}: matching {}.txt not found", name, id);
            return null;
        }
        try {
            Snippets s = mapper.readValue(p.toFile(), Snippets.class);
            return new Entry(id, s.title() == null || s.title().isBlank() ? id : s.title(), countPresent(s));
        } catch (IOException e) {
            log.warn("Skipping {}: could not parse JSON: {}", name, e.getMessage());
            return null;
        }
    }

    private static Path resolve(Path dir, String fileName) {
        if (!SAFE.matcher(fileName).matches()) throw new IllegalArgumentException("Unknown CUAD contract");
        Path p = dir.resolve(fileName).normalize();
        if (!p.startsWith(dir)) throw new IllegalArgumentException("Unknown CUAD contract");
        return p;
    }
}