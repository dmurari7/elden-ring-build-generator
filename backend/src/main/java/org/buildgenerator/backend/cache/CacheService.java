package org.buildgenerator.backend.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final Path cacheDir;
    private final long ttlMillis;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /*
     * @description: Constructor for CacheService.
     * - Creates cache directory if it does not exist.
     * - Sets TTL for cached files.
     *
     * @param cachePath: Path where cache files are stored.
     * @param ttlHours: Time-to-live for cache files in hours.
     */
    public CacheService(@Value("${app.cache.path:./data/cache}") String cachePath,
                        @Value("${app.cache.ttl.hours:168}") long ttlHours) {

        this.cacheDir = Paths.get(cachePath);
        this.ttlMillis = ttlHours * 60 * 60 * 1000;

        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create cache directory " + cacheDir, e);
        }
    }

    /*
     * @description: Reads raw JSON from cache file by key.
     * - Returns empty if file does not exist or is expired.
     *
     * @param key: Cache key (file name without extension).
     *
     * @return: Optional<String> containing raw JSON if present and valid.
     */
    public Optional<String> readRaw(String key) {
        Path f = cacheDir.resolve(sanitizeFileName(key) + ".json");
        try {
            if (Files.exists(f)) {
                long age = Instant.now().toEpochMilli() - Files.getLastModifiedTime(f).toMillis();
                if (age < ttlMillis) {
                    return Optional.of(Files.readString(f));
                } else {
                    Files.deleteIfExists(f);
                    logger.info("Cache expired for key: {}", key);
                }
            }
        } catch (IOException e) {
            logger.error("Error reading cache for key {}: {}", key, e.getMessage());
        }
        return Optional.empty();
    }

    /*
     * @description: Reads JSON from cache and deserializes to a specific class.
     *
     * @param key: Cache key.
     * @param clazz: Target class type.
     *
     * @return: Optional<T> containing deserialized object if present.
     */
    public <T> Optional<T> read(String key, Class<T> clazz) {
        Optional<String> raw = readRaw(key);
        if (raw.isEmpty()) return Optional.empty();

        try {
            T obj = mapper.readValue(raw.get(), clazz);
            return Optional.of(obj);
        } catch (IOException e) {
            logger.error("Failed to deserialize cache for key {}: {}", key, e.getMessage());
            invalidate(key);
            return Optional.empty();
        }
    }

    /*
     * @description: Reads JSON from cache and deserializes using TypeReference (for generics).
     *
     * @param key: Cache key.
     * @param typeRef: TypeReference of desired object.
     *
     * @return: Optional<T> containing deserialized object if present.
     */
    public <T> Optional<T> read(String key, TypeReference<T> typeRef) {
        Optional<String> raw = readRaw(key);
        if (raw.isEmpty()) return Optional.empty();

        try {
            T obj = mapper.readValue(raw.get(), typeRef);
            return Optional.of(obj);
        } catch (IOException e) {
            logger.error("Failed to deserialize cache with TypeReference for key {}: {}", key, e.getMessage());
            invalidate(key);
            return Optional.empty();
        }
    }

    /*
     * @description: Writes an object to cache under the given key.
     * - Performs atomic write via temporary file.
     *
     * @param key: Cache key.
     * @param obj: Object to serialize and write.
     */
    public void write(String key, Object obj) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();

        try {
            String tmpName = sanitizeFileName(key) + ".json.tmp";
            String finalName = sanitizeFileName(key) + ".json";

            Path tmp = cacheDir.resolve(tmpName);
            Path finalPath = cacheDir.resolve(finalName);

            // Serialize to temp file
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                mapper.writeValue(writer, obj);
            }

            // Atomic move to final file
            try {
                Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException amnse) {
                logger.warn("Atomic move not supported, falling back to regular move for key: {}", key);
                Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            logger.error("Failed to write cache for key {}: {}", key, e.getMessage());
        } finally {
            lock.unlock();
            locks.computeIfPresent(key, (k, l) -> l.hasQueuedThreads() ? l : null);
        }
    }

    /*
     * @description: Removes a cache entry manually.
     *
     * @param key: Cache key to remove.
     */
    public void invalidate(String key) {
        Path f = cacheDir.resolve(sanitizeFileName(key) + ".json");
        try {
            Files.deleteIfExists(f);
            logger.info("Cache invalidated for key: {}", key);
        } catch (IOException e) {
            logger.error("Failed to invalidate cache for key {}: {}", key, e.getMessage());
        }
    }

    /*
     * @description: Sanitizes file names for safe filesystem usage.
     * - Replaces spaces and special characters with underscores.
     *
     * @param s: Raw cache key string.
     *
     * @return: Sanitized, lowercase string suitable as filename.
     */
    private String sanitizeFileName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
