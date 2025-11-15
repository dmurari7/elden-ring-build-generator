package org.buildgenerator.backend.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.buildgenerator.backend.model.Build;
import org.buildgenerator.backend.scraper.BuildScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/*
 * Domain-specific cache layer for storing and retrieving Elden Ring builds.
 *
 * Purpose:
 * - Keeps BuildServiceImpl clean by handling all caching logic here.
 * - Wraps CacheService with type-safe methods for Build objects.
 * - Integrates scraping + caching so the application fetches builds only once.
 *
 * Responsibilities:
 * - Load all builds from cache if available.
 * - Otherwise scrape them and save them permanently.
 * - Optional: force refresh by deleting old cache entries.
 *
 * Note:
 * - This class never overwrites cache unless explicitly asked to refresh.
 * - Keeps architecture modular: scraper logic stays in BuildScraper,
 *   disk caching stays in CacheService, business logic stays in BuildServiceImpl.
 */
@Service
public class BuildCacheService {

    private static final Logger logger = LoggerFactory.getLogger(BuildCacheService.class);

    // Single canonical key for storing all builds
    private static final String ALL_BUILDS_KEY = "all_builds";

    private final CacheService cacheService;
    private final BuildScraper scraper;

    public BuildCacheService(CacheService cacheService, BuildScraper scraper) {
        this.cacheService = cacheService;
        this.scraper = scraper;
    }

    /*
     * @description: Returns all builds from cache if present.
     *               If cache is empty or expired, triggers a full scrape.
     *
     * @return: List<Build> always non-null.
     */
    public List<Build> getAllBuilds() {
        Optional<List<Build>> cached =
                cacheService.read(ALL_BUILDS_KEY, new TypeReference<List<Build>>() {});

        if (cached.isPresent()) {
            logger.info("Loaded {} builds from cache.", cached.get().size());
            return cached.get();
        }

        logger.info("No valid cached build list found. Scraping fresh data...");
        return refreshBuilds();
    }

    /*
     * @description: Forces a full refresh:
     *               - Deletes old cached build list.
     *               - Scrapes fresh build data.
     *               - Writes new list to cache.
     *
     * @return: Freshly scraped List<Build>.
     */
    public List<Build> refreshBuilds() {
        cacheService.invalidate(ALL_BUILDS_KEY);

        List<Build> builds = scrapeAllSourcePages();

        cacheService.write(ALL_BUILDS_KEY, builds);
        logger.info("Refreshed and cached {} builds.", builds.size());

        return builds;
    }

    /*
     * @description: Scrapes all build sources your application depends on.
     *               - Currently scrapes ONE source.
     *               - Expand this to support multiple pages.
     *
     * @return: Combined list of all scraped builds.
     */
    private List<Build> scrapeAllSourcePages() {
        // You can extend this to support multiple URLs.
        String url = "https://eldenring.wiki.fextralife.com/Build+Calculator"; // Example source page

        try {
            return scraper.scrapePageForBuilds(url);
        } catch (IOException e) {
            logger.error("Failed to scrape builds from {}: {}", url, e.getMessage());
            return Collections.emptyList();
        }
    }

    /*
     * @description: Returns cached JSON string for debugging or external UI.
     *
     * @return: Raw JSON or empty string if no cache is present.
     */
    public String getRawCache() {
        return cacheService.readRaw(ALL_BUILDS_KEY).orElse("");
    }

    /*
     * @description: Overwrites cached build list manually.
     *               Useful when editing builds externally and saving them.
     *
     * @param builds: New list to persist.
     */
    public void writeBuilds(List<Build> builds) {
        cacheService.write(ALL_BUILDS_KEY, builds);
        logger.info("Manually updated build cache: {} builds saved.", builds.size());
    }
}
