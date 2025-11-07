package org.buildgenerator.backend.scraper;

import org.buildgenerator.backend.cache.CacheService;
import org.buildgenerator.backend.model.Build;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

class BuildScraperTest {

    public static void main(String[] args) throws IOException {
        // Load local HTML file
        File file = new File("C:\\Users\\dhars\\projects\\elden-ring-build-generator\\sample-build.html");
        Document doc = Jsoup.parse(file, "UTF-8", "https://fextralife.com/");

        // Create a mock or temporary CacheService for testing
        CacheService cacheService = new CacheService("./data/test-cache", 168); // 168 hours TTL
        BuildScraper scraper = new BuildScraper(cacheService);

        // Directly parse builds from the local document
        List<Build> builds = scraper.parseBuildsFromDocument(doc);

        // Print out parsed builds for verification
        for (Build b : builds) {
            System.out.println("Build Name: " + b.getName());
            System.out.println("Level: " + b.getLevel());
            System.out.println("Starting Class: " + b.getStartingClass());
            System.out.println("Main Weapon: " + b.getMainWeapon());
            System.out.println("Off-Hand Weapon: " + b.getOffHandWeapon());
            System.out.println("Shield: " + b.getShield());
            System.out.println("Armour Set: " + b.getArmourSet());
            System.out.println("Talismans: " + b.getTalismans());
            System.out.println("Skills: " + b.getSkills());
            System.out.println("Spells: " + b.getSpells());
            System.out.println("Crystal Tears: " + b.getCrystalTears());
            System.out.println("Great Runes: " + b.getGreatRunes());
            System.out.println("Primary Stats: " + b.getPrimaryStats());
            System.out.println("Secondary Stats: " + b.getSecondaryStats());
            System.out.println("Stats: " + b.getStats());
            System.out.println("Description: " + b.getDescription());
            System.out.println("--------------------------------------------------");
        }
    }
}