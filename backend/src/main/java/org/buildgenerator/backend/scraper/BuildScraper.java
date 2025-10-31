package org.buildgenerator.backend.scraper;

import org.buildgenerator.backend.model.Build;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Simple Jsoup-based scraper for Fextralife build pages.
 * - Polite defaults (user-agent, timeout)
 * - Parses <h3 class="bonfire"> blocks and next <ul> items into Build objects
 *
 * NOTE: adapt selectors if site changes. Always test with saved sample HTML.
 */
@Component
public class BuildScraper {
    private static final Logger logger = LoggerFactory.getLogger(BuildScraper.class);

    private static final String DEFAULT_USER_AGENT = "EldenBuildGeneratorBot/1.0 (dharshaan@hotmail.com)";
    private static final int DEFAULT_TIMEOUT_MS = (int) Duration.ofSeconds(10).toMillis();
    private static final int DEFAULT_RETRIES = 3;
    private static final long RETRY_SLEEP_MS = 500L;

    private static final Pattern STAT_PAIRS = Pattern.compile("(\\d{1,3})\\s+([A-Za-z]+)");
    private static final Pattern LEVEL_PATTERN = Pattern.compile("Level\\s*(\\d{1,3})", Pattern.CASE_INSENSITIVE);

    public BuildScraper() {}

    /*
     * @description: Scrape a single page and return all builds found on that page.
     *
     * @param: url - URL of the page to scrape.
     *
     * @return: List of Build objects parsed from the page.
     * @throws IOException if fetching the page fails.
     */
    public List<Build> scrapePageForBuilds(String url) throws IOException {
        Document doc = fetchDocumentWithRetries(url);
        return parseBuildsFromDocument(doc);
    }

    /*
     * @description: Fetch Jsoup Document with retry logic.
     *
     * @param: url - URL to fetch.
     *
     * @return: Jsoup Document object.
     * @throws IOException if fetching fails after retries.
     */
    private Document fetchDocumentWithRetries(String url) throws IOException {
        IOException lastEx = null;
        for (int attempt = 1; attempt <= DEFAULT_RETRIES; attempt++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(DEFAULT_USER_AGENT)
                        .timeout(DEFAULT_TIMEOUT_MS)
                        .get();
            } catch (IOException e) {
                lastEx = e;
                logger.warn("Attempt {} failed to fetch {}: {}", attempt, url, e.getMessage());
                try {
                    Thread.sleep(RETRY_SLEEP_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while retrying fetch", ie);
                }
            }
        }
        throw lastEx != null ? lastEx : new IOException("Unknown fetch error");
    }

    /*
     * @description: Parse all <h3 class="bonfire"> blocks into Build objects.
     *
     * @param: doc - Jsoup Document to parse.
     *
     * @return: List of Build objects.
     */
    private List<Build> parseBuildsFromDocument(Document doc) {
        List<Build> builds = new ArrayList<>();
        Elements headers = doc.select("h3.bonfire");

        for (Element h3 : headers) {
            try {
                Build b = parseBuildFromHeader(h3);
                if (b != null) builds.add(b);
            } catch (Exception ex) {
                logger.error("Failed to parse build block: {}", ex.getMessage());
            }
        }
        return builds;
    }

    /*
     * @description: Parse a single <h3 class="bonfire"> element and its following <ul> into a Build object.
     *
     * @param: h3 - header element representing a build.
     *
     * @return: Build object or null if parsing fails.
     */
    private Build parseBuildFromHeader(Element h3) {
        String title = h3.text();
        Build build = new Build();

        // Attempt to extract level from title
        Matcher levelMatcher = LEVEL_PATTERN.matcher(title);
        if (levelMatcher.find()) {
            build.setLevel(levelMatcher.group(1));
        }

        // Find next <ul> sibling within a few elements
        Element sibling = h3.nextElementSibling();
        Element ul = null;
        int steps = 0;
        while (sibling != null && steps < 6) {
            if ("ul".equalsIgnoreCase(sibling.tagName())) {
                ul = sibling;
                break;
            }
            sibling = sibling.nextElementSibling();
            steps++;
        }
        if (ul == null) {
            logger.warn("No <ul> found for build header: {}", title);
            return null;
        }

        Map<String, List<String>> entries = parseListItemsToMap(ul);
        build.setName(title);
        build.setStartingClass(firstOrNull(entries.getOrDefault("class", entries.getOrDefault("Class", Collections.emptyList()))));
        build.setFlaskSpread(firstOrNull(entries.getOrDefault("flask spread", entries.getOrDefault("Flask Spread", Collections.emptyList()))));
        build.setMainWeapon(firstOrNull(entries.getOrDefault("main weapon", entries.getOrDefault("Main Weapon", Collections.emptyList()))));
        build.setOffHandWeapon(firstOrNull(entries.getOrDefault("off-hand weapon", entries.getOrDefault("Off-Hand Weapon", Collections.emptyList()))));
        build.setShield(firstOrNull(entries.getOrDefault("shield", Collections.emptyList())));

        build.setArmourSet(entries.getOrDefault("armor", entries.getOrDefault("Armor", Collections.emptyList())));
        if (build.getArmourSet() == null || build.getArmourSet().isEmpty()) {
            build.setArmourSet(entries.getOrDefault("armour", entries.getOrDefault("Armour", Collections.emptyList())));
        }

        build.setTalismans(entries.getOrDefault("talismans", entries.getOrDefault("Talismans", Collections.emptyList())));
        List<String> altTal = entries.getOrDefault("alternate talismans", entries.getOrDefault("Alternate Talismans", Collections.emptyList()));
        if (!altTal.isEmpty()) {
            List<String> combined = new ArrayList<>(build.getTalismans() == null ? Collections.emptyList() : build.getTalismans());
            combined.addAll(altTal);
            build.setTalismans(combined);
        }

        build.setSkills(entries.getOrDefault("skills", entries.getOrDefault("Skills", Collections.emptyList())));
        build.setSpells(entries.getOrDefault("spells", entries.getOrDefault("Spells", Collections.emptyList())));
        build.setCrystalTears(entries.getOrDefault("crystal tear", entries.getOrDefault("Crystal Tear", Collections.emptyList())));
        build.setGreatRunes(entries.getOrDefault("great runes", entries.getOrDefault("Great Runes", Collections.emptyList())));
        build.setPrimaryStats(entries.getOrDefault("primary stats", entries.getOrDefault("Primary Stats", Collections.emptyList())));
        build.setSecondaryStats(entries.getOrDefault("secondary stats", entries.getOrDefault("Secondary Stats", Collections.emptyList())));

        // Description: often in <p> before or after the <ul>
        Element maybeP = ul.previousElementSibling();
        if (maybeP != null && "p".equalsIgnoreCase(maybeP.tagName())) {
            build.setDescription(maybeP.text());
        } else {
            Element pAfter = ul.nextElementSibling();
            if (pAfter != null && "p".equalsIgnoreCase(pAfter.tagName())) {
                build.setDescription(pAfter.text());
            }
        }

        // Parse numeric stats from nearby <p>
        Element look = ul.nextElementSibling();
        int lookSteps = 0;
        Map<String, Integer> parsedStats = null;
        while (look != null && lookSteps < 10) {
            if ("p".equalsIgnoreCase(look.tagName())) {
                String text = look.text();
                if (text.toLowerCase().contains("at level") || containsStatNumbers(text)) {
                    parsedStats = parseStatsFromText(text);
                    break;
                }
            }
            look = look.nextElementSibling();
            lookSteps++;
        }
        build.setStats(parsedStats != null ? parsedStats : Collections.emptyMap());

        return build;
    }

    /*
     * @description: Parse <ul> <li> elements into label -> list-of-values map.
     *
     * @param: ul - unordered list element containing build details.
     *
     * @return: Map of label -> list of values.
     */
    private Map<String, List<String>> parseListItemsToMap(Element ul) {
        Map<String, List<String>> map = new HashMap<>();
        for (Element li : ul.select("li")) {
            Element strong = li.selectFirst("strong");
            if (strong == null) continue;
            String label = strong.text().replace(":", "").trim();
            strong.remove();
            List<String> values = extractValuesFromLi(li);
            map.put(label, values);
            map.put(label.toLowerCase(Locale.ROOT), values);
        }
        return map;
    }

    /*
     * @description: Extract values from <li> element; prefer link text with absolute URL.
     *
     * @param: li - list item element.
     *
     * @return: List of extracted values.
     */
    private List<String> extractValuesFromLi(Element li) {
        List<String> values = new ArrayList<>();
        Elements links = li.select("a.wiki_link, a");
        if (!links.isEmpty()) {
            for (Element a : links) {
                String text = a.text().trim();
                String url = a.absUrl("href"); // resolves relative URLs automatically
                if (!text.isEmpty()) values.add(text);
                // Optional: store URL somewhere if desired
            }
        } else {
            String t = li.text().trim();
            if (!t.isEmpty()) {
                for (String part : t.split(",|/| and ")) {
                    String s = part.trim();
                    if (!s.isEmpty()) values.add(s);
                }
            }
        }
        return values;
    }

    private String firstOrNull(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    private boolean containsStatNumbers(String text) {
        Matcher m = STAT_PAIRS.matcher(text);
        return m.find();
    }

    private Map<String, Integer> parseStatsFromText(String text) {
        Map<String, Integer> map = new LinkedHashMap<>();
        Matcher m = STAT_PAIRS.matcher(text);
        while (m.find()) {
            try {
                int value = Integer.parseInt(m.group(1));
                String name = m.group(2);
                map.put(normalizeStatName(name), value);
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    private String normalizeStatName(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        switch (s) {
            case "vigor": return "Vigor";
            case "mind": return "Mind";
            case "endurance": return "Endurance";
            case "strength": return "Strength";
            case "dexterity": return "Dexterity";
            case "intelligence": return "Intelligence";
            case "faith": return "Faith";
            case "arcane": return "Arcane";
            default: return capitalize(raw);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
