package org.buildgenerator.backend.cache;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CacheService {

    private final Path cacheDir;
    private final long ttlMillis;

    //Jackson mapper used for JSON serialization/deserialization
    private final ObjectMapper mapper = new ObjectMapper();

    //simple per-key locks to avoid concurrent writes interfering with each other
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /*
    * @description: Constructor for the cache.
    * Used to create the cache directory as well.
    *
    * @param cachePath:Config path for the cache.
    *
    * @param ttlHours:The Time-To-Live for the scraped information in hours.
    */
    public CacheService(@Value("${app.cache.path:./data/cache}") String cachePath,
                        @Value("${app.cache.ttl.hours:168}") long ttlHours) {

        this.cacheDir = Paths.get(cachePath);
        this.ttlMillis = ttlHours * 60 * 60 * 1000;

        //if the creation of the cache directory fails, a runtime exception is thrown for faster failure.
        try{
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create cache directory" + cacheDir, e);
        }
    }

    /*
    * @description: Reads cached JSON using key (minus the file extension).
    * Returns empty if the JSON isn't there or expired.
    *
    * @param key: key to retrieve JSON file to be read.
    */
    public Optional<String> readRaw(String key) {
        Path f = cacheDir.resolve(sanitizeFileName(key) + ".json");
        try {
            if (Files.exists(f)) {
                long age = Instant.now().toEpochMilli() - Files.getLastModifiedTime(f).toMillis();

                if (age < ttlMillis) {
                    return Optional.of(Files.readString(f));
                } else {
                    try {
                        Files.deleteIfExists(f);
                    } catch (IOException ignore) {}
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /*
    * @description: Reads and deserializes the JSON into the target class
    *
    * @param key: key to retrieve JSON file to be read.
    *
    * @param clazz: stores the converted JSON string as a generic object whose class is determined at runtime.
    * */
    public <T> Optional<T> read(String key, Class<T> clazz) {
        Optional<String> raw = readRaw(key);

        if (raw.isEmpty()) {return Optional.empty();}
        try {
            T obj = mapper.readValue(raw.get(), clazz);
            return Optional.of(obj);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(cacheDir.resolve(sanitizeFileName(key) + ".json"));
            } catch (IOException ignore) {}

            e.printStackTrace();
            return Optional.empty();
        }
    }

    /*
    * @description: Writes object to cache under atomic key and overwrites the existing file.
    *
    * @param key: key of selected build to be written
    *
    * @param obj: content of scraped website for selected build
    * */
    public void write(String key, Object obj) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();

        try {
            String tmpName = sanitizeFileName(key) + ".json.tmp";
            String finalName = sanitizeFileName(key) + ".json";

            Path tmp = cacheDir.resolve(tmpName);
            Path finalPath = cacheDir.resolve(finalName);

            // serialize to temp file
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                mapper.writeValue(writer, obj);
            }

            // atomically move to final file (REPLACE_EXISTING)
            Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException amnse) {

            // fall back to non-atomic move if not supported on filesystem
            try {
                Path tmp = cacheDir.resolve(sanitizeFileName(key) + ".json.tmp");
                Path finalPath = cacheDir.resolve(sanitizeFileName(key) + ".json");
                Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();

            // optional: remove lock if no longer needed to prevent map growth
            locks.computeIfPresent(key, (k, l) -> l.hasQueuedThreads() ? l : null);
        }

    }

    /*
     * @description: Removes selected cache entry. Used for testing/manual invalidation.
     *
     * @param key: key of cached JSON file to remove.
     */
    public void invalidate(String key) {
        Path f = cacheDir.resolve(sanitizeFileName(key) + ".json");
        try { Files.deleteIfExists(f); } catch (IOException e) { e.printStackTrace(); }
    }


    /*
    * @description: Used to clean up any spaces and slashes in the cached JSON files.
    *
    * @param s: Name of the cached file.
    * */
    private String sanitizeFileName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }
}
