package de.omegazirkel.risingworld.bosses;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.risingworld.api.definitions.Definitions;

/** Template-standard, file-backed plugin settings for OZ Bosses. */
public final class PluginSettings {
    public int threshold, interval, npcKill, terrain, pickaxe, hoe, sledgehammer, vegetation, objectDestroy, construction, blueprint;
    public List<Short> types = List.of();
    public int baseHealth, healthPerLevel, initialFollowers, followerEveryLevels, followerHealth, minSpawnDistance, spawnChance;
    public double followersPerOnlinePlayer, bountyPercent;
    public boolean wallet;
    public long discordChannel;

    public static PluginSettings load(Path path) {
        Properties p = properties(path); PluginSettings s = new PluginSettings();
        s.threshold=n(p,"threat.threshold",10000); s.interval=n(p,"threat.checkIntervalMinutes",5)*60; s.npcKill=n(p,"threat.npcKill",50); s.terrain=n(p,"threat.terrainHit",0); s.pickaxe=n(p,"threat.pickaxe",2); s.hoe=n(p,"threat.hoe",1); s.sledgehammer=n(p,"threat.sledgehammer",1); s.vegetation=n(p,"threat.vegetation",2); s.objectDestroy=n(p,"threat.objectDestroy",2); s.construction=n(p,"threat.construction",5); s.blueprint=n(p,"threat.blueprint",10);
        List<Short> ids=new ArrayList<>(); for(String value:p.getProperty("boss.types","bandit,barbarian,dummy,ghoul,skeleton,wolf,wildboar,firewolf,lion").split(",")) try { ids.add(Short.parseShort(value.trim())); } catch(NumberFormatException ignored) { var d=Definitions.getNpcDefinition(value.trim()); if(d!=null) ids.add(d.id); } s.types=ids;
        s.baseHealth=n(p,"boss.baseHealth",1000); s.healthPerLevel=n(p,"boss.healthPerLevel",200); s.initialFollowers=n(p,"boss.initialFollowers",2); s.followersPerOnlinePlayer=d(p,"boss.followersPerOnlinePlayer",0); s.followerEveryLevels=n(p,"boss.followerEveryLevels",5); s.followerHealth=n(p,"boss.followerHealth",250); s.minSpawnDistance=n(p,"boss.minSpawnDistance",80); s.spawnChance=Math.max(0,Math.min(100,n(p,"boss.spawnChance",10))); s.wallet=Boolean.parseBoolean(p.getProperty("wallet.enabled","true")); s.bountyPercent=d(p,"wallet.bountyPercent",50); s.discordChannel=l(p,"discord.channelId",0); return s;
    }
    public static String read(Path path,String key,String fallback) { return properties(path).getProperty(key,fallback); }
    private static Properties properties(Path path) { Properties p=new Properties(); try { if(Files.exists(path)) p.load(Files.newInputStream(path)); } catch(IOException ignored) {} return p; }
    private static int n(Properties p,String key,int fallback) { try{return Integer.parseInt(p.getProperty(key,""+fallback));}catch(NumberFormatException ignored){return fallback;} }
    private static double d(Properties p,String key,double fallback) { try{double value=Double.parseDouble(p.getProperty(key,""+fallback));return Double.isFinite(value)?Math.max(0,value):fallback;}catch(NumberFormatException ignored){return fallback;} }
    private static long l(Properties p,String key,long fallback) { try{return Long.parseLong(p.getProperty(key,""+fallback));}catch(NumberFormatException ignored){return fallback;} }
}
