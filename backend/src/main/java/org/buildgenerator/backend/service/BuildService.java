/*
Date: November 15, 2025
Author: Dharshaan Murari
*/

package org.buildgenerator.backend.service;

import org.buildgenerator.backend.cache.BuildCacheService;
import org.buildgenerator.backend.model.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
 * BuildService provides type-safe access and filtering to Elden Ring builds.
 *
 * Responsibilities:
 * - Retrieve all builds or specific builds by name.
 * - Filter builds by starting class, primary/secondary stats, and build type (magic/melee).
 * - Acts as the business logic layer between BuildController and BuildCacheService.
 */
@Service
public class BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildService.class);

    private final BuildCacheService cacheService;

    public BuildService(BuildCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /*
     * @description: Returns all builds available.
     *
     * @return: List<Build> always non-null.
     */
    public List<Build> getAllBuilds() {
        return cacheService.getAllBuilds();
    }

    /*
     * @description: Retrieve a single build by exact name (case-insensitive).
     *
     * @param name: Name of the build to find.
     * @return: Optional<Build> containing the build if found.
     */
    public Optional<Build> getBuildByName(String name) {
        if (name == null || name.isEmpty()) return Optional.empty();
        return getAllBuilds().stream()
                .filter(b -> b.getName() != null && b.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /*
     * @description: Retrieve builds filtered by starting class (case-insensitive).
     *
     * @param className: Name of the starting class.
     * @return: List<Build> matching the class.
     */
    public List<Build> getBuildsByClass(String className) {
        if (className == null || className.isEmpty()) return Collections.emptyList();
        return getAllBuilds().stream()
                .filter(b -> b.getStartingClass() != null && b.getStartingClass().equalsIgnoreCase(className))
                .collect(Collectors.toList());
    }

    /*
     * @description: Retrieve builds that have a primary stat matching the given stat.
     *
     * @param primaryStat: Stat to filter by (e.g., "Strength", "Intelligence").
     * @return: List<Build> matching the primary stat.
     */
    public List<Build> getBuildsByPrimaryStat(String primaryStat) {
        if (primaryStat == null || primaryStat.isEmpty()) return Collections.emptyList();
        return getAllBuilds().stream()
                .filter(b -> b.getPrimaryStats() != null && b.getPrimaryStats().stream()
                        .anyMatch(s -> s.equalsIgnoreCase(primaryStat)))
                .collect(Collectors.toList());
    }

    /*
     * @description: Retrieve builds that have a secondary stat matching the given stat.
     *
     * @param secondaryStat: Stat to filter by (e.g., "Faith", "Dexterity").
     * @return: List<Build> matching the secondary stat.
     */
    public List<Build> getBuildsBySecondaryStat(String secondaryStat) {
        if (secondaryStat == null || secondaryStat.isEmpty()) return Collections.emptyList();
        return getAllBuilds().stream()
                .filter(b -> b.getSecondaryStats() != null && b.getSecondaryStats().stream()
                        .anyMatch(s -> s.equalsIgnoreCase(secondaryStat)))
                .collect(Collectors.toList());
    }

    /*
     * @description: Retrieve all builds classified as magic builds.
     * - Uses Build.isMagicBuild() helper.
     *
     * @return: List<Build> classified as magic.
     */
    public List<Build> getMagicBuilds() {
        return getAllBuilds().stream()
                .filter(Build::isMagicBuild)
                .collect(Collectors.toList());
    }

    /*
     * @description: Retrieve all builds classified as melee builds.
     * - Uses Build.isMeleeBuild() helper.
     *
     * @return: List<Build> classified as melee.
     */
    public List<Build> getMeleeBuilds() {
        return getAllBuilds().stream()
                .filter(Build::isMeleeBuild)
                .collect(Collectors.toList());
    }

    /*
     * @description: Forces a refresh of all builds by scraping and updating cache.
     *
     * @return: Newly scraped List<Build>.
     */
    public List<Build> refreshBuilds() {
        List<Build> refreshed = cacheService.refreshBuilds();
        logger.info("BuildService: refreshed {} builds.", refreshed.size());
        return refreshed;
    }

    /*
     * @description: Generic filter by a predicate for advanced querying.
     *
     * @param predicate: Condition to filter builds.
     * @return: List<Build> satisfying the predicate.
     */
    public List<Build> filterBuilds(java.util.function.Predicate<Build> predicate) {
        if (predicate == null) return getAllBuilds();
        return getAllBuilds().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }
}

