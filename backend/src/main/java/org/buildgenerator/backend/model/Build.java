/*
Date: October 13, 2025
Author: Dharshaan Murari
*/

package org.buildgenerator.backend.model;

import java.util.List;
import java.util.Map;

/*
Build class serves as a model/data container for the build data I will obtain from the Scraper.
It allows me to represent each attribute of each build (weapon, armour, etc.) as Java objects that the service layer can manipulate.
*/
public class Build {

    private String name;
    private String level;
    private String description;
    private String startingClass;
    private String flaskSpread;
    private String mainWeapon;
    private String offHandWeapon;
    private String shield;

    private List<String> armourSet;
    private List<String> talismans;
    private List<String> skills;
    private List<String> spells;
    private List<String>  crystalTears;
    private List<String> greatRunes;

    private Map<String, Integer> stats;

    /*These stats are used to group builds together,
    so that each group of builds can be served based on the user's preferences*/
    private List<String> primaryStats;
    private List<String> secondaryStats;

    //default constructor as required by Jackson
    public Build() {}

    //Full constructor with params of all instance variables
    public Build(String name,
                 String level,
                 String description,
                 String startingClass,
                 String flaskSpread,
                 String mainWeapon,
                 String offHandWeapon,
                 String shield,
                 List<String> armourSet,
                 List<String> talismans,
                 List<String> skills,
                 List<String> spells,
                 List<String> crystalTears,
                 List<String> greatRunes,
                 Map<String, Integer> stats,
                 List<String> primaryStats,
                 List<String> secondaryStats) {

        this.name = name;
        this.level = level;
        this.description = description;
        this.startingClass = startingClass;
        this.flaskSpread = flaskSpread;
        this.mainWeapon = mainWeapon;
        this.offHandWeapon = offHandWeapon;
        this.shield = shield;
        this.armourSet = armourSet;
        this.talismans = talismans;
        this.skills = skills;
        this.spells = spells;
        this.crystalTears = crystalTears;
        this.greatRunes = greatRunes;
        this.stats = stats;
        this.primaryStats = primaryStats;
        this.secondaryStats = secondaryStats;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    //getters and setters for each instance variable
    public String getStartingClass() {
        return startingClass;
    }

    public void setStartingClass(String startingClass) {
        this.startingClass = startingClass;
    }

    public String getFlaskSpread() {
        return flaskSpread;
    }

    public void setFlaskSpread(String flaskSpread) {
        this.flaskSpread = flaskSpread;
    }

    public String getMainWeapon() {
        return mainWeapon;
    }

    public void setMainWeapon(String mainWeapon) {
        this.mainWeapon = mainWeapon;
    }

    public String getOffHandWeapon() {
        return offHandWeapon;
    }

    public void setOffHandWeapon(String offHandWeapon) {
        this.offHandWeapon = offHandWeapon;
    }

    public String getShield() {
        return shield;
    }

    public void setShield(String shield) {
        this.shield = shield;
    }

    public List<String> getArmourSet() {
        return armourSet;
    }

    public void setArmourSet(List<String> armourSet) {
        this.armourSet = armourSet;
    }

    public List<String> getTalismans() {
        return talismans;
    }

    public void setTalismans(List<String> talismans) {
        this.talismans = talismans;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getSpells() {
        return spells;
    }

    public void setSpells(List<String> spells) {
        this.spells = spells;
    }

    public List<String> getCrystalTears() {
        return crystalTears;
    }

    public void setCrystalTears(List<String> crystalTears) {
        this.crystalTears = crystalTears;
    }

    public List<String> getGreatRunes() {
        return greatRunes;
    }

    public void setGreatRunes(List<String> greatRunes) {
        this.greatRunes = greatRunes;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public void setStats(Map<String, Integer> stats) {
        this.stats = stats;
    }

    public List<String> getPrimaryStats() {
        return primaryStats;
    }

    public void setPrimaryStats(List<String> primaryStats) {
        this.primaryStats = primaryStats;
    }

    public List<String> getSecondaryStats() {
        return secondaryStats;
    }

    public void setSecondaryStats(List<String> secondaryStats) {
        this.secondaryStats = secondaryStats;
    }

    //classification helper methods for the service layer

    /*
    * @description: helper method used to help classify whether
    * a build mainly uses magic based on whether it has a primary stat
    * of Intelligence or Faith.
    *
    * @return: returns true if it has Int or Faith, and false if not
    * */
    public boolean isMagicBuild() {
        return primaryStats.contains("Intelligence") || primaryStats.contains("Faith");
    }

    /*
     * @description: helper method used to help classify whether
     * a build mainly uses melee based on whether it has a primary stat
     * of Strength or Dexterity.
     *
     * @return: returns true if it has Str or Dex, and false if not
     * */
    public boolean isMeleeBuild() {
        return primaryStats.contains("Strength") || primaryStats.contains("Dexterity");
    }
}
