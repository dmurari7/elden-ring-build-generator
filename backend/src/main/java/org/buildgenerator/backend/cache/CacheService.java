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
    * @param:Config path for the cache.
    *
    * @param:The Time-To-Live for the scraped information in hours.
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


}
