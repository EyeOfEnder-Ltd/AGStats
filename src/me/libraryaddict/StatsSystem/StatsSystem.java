package me.libraryaddict.StatsSystem;

import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentHashMap;

import me.libraryaddict.AGKits.Kit;
import me.libraryaddict.AGKits.KitsApi;
import me.libraryaddict.Hungergames.Events.GameStartEvent;
import me.libraryaddict.Hungergames.Events.PlayerKilledEvent;
import me.libraryaddict.Hungergames.Events.PlayerWinEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class StatsSystem extends JavaPlugin implements Listener {
    protected ConcurrentHashMap<String, Stats> playerStats = new ConcurrentHashMap<String, Stats>();
    public String SQL_USER;
    public String SQL_PASS;
    public String SQL_DATA;
    public String SQL_HOST;
    private StatsSaveThread saveThread;
    private StatsLoadThread loadThread;
    private KitsApi kits = new KitsApi();
    private Config config;

    public void onEnable() {
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(this, this);
        config = new Config(this);
        SQL_USER = config.getUsername();
        SQL_PASS = config.getPassword();
        SQL_HOST = config.getHost();
        SQL_DATA = config.getDatabase();
        this.loadThread = new StatsLoadThread(this);
        this.saveThread = new StatsSaveThread(this);
        this.loadThread.start();
        this.saveThread.start();
    }

    public Stats getStats(String name) {
        return (Stats) this.playerStats.get(name);
    }

    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers())
            p.kickPlayer("Bye bye!, Game shutdown for weird reasons!");
        while (this.loadThread.joinQueue.size() > 0) {
            try {
                System.out.println("Waiting for stats join queue to finish");
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (Stats stats : this.playerStats.values())
            this.saveThread.quitQueue.add(stats);
        this.playerStats.clear();
        while (this.saveThread.quitQueue.size() > 0) {
            try {
                System.out.println("Waiting for stats quit queue to finish");
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.loadThread.terminate();
        this.saveThread.terminate();
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        this.loadThread.joinQueue.add(event.getPlayer().getName());
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        Stats stats = (Stats) this.playerStats.remove(event.getPlayer().getName());
        if (stats != null) this.saveThread.quitQueue.add(stats);
    }

    @EventHandler
    public void onDeath(PlayerKilledEvent event) {
        if (event.getKiller() != null) {
            Stats stats = (Stats) this.playerStats.get(event.getKiller().getName());
            if (stats != null) {
                stats.addKill();
                stats.setKills(stats.getKills() + 1);
                if (stats.getCurrentKills() % 5 == 0) event.getKiller().sendMessage(ChatColor.RED + "Kill streak of " + stats.getCurrentKills() + "!");
                if (stats.getCurrentKills() > stats.getHighestKillStreak()) {
                    event.getKiller().sendMessage(ChatColor.RED + "You just broke your previous killstreak record! " + stats.getCurrentKills() + " kills!");
                    stats.setHighestKillStreak(stats.getCurrentKills());
                }
            }
        }
    }

    @EventHandler
    public void onWin(PlayerWinEvent event) {
        Stats stats = (Stats) this.playerStats.get(event.getWinner().getName());
        if (stats != null) stats.setWins(stats.getWins() + 1);
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Stats stats = (Stats) this.playerStats.get(p.getName());
            if (stats != null) stats.addTimesPlayed();
        }
    }

    public void loadedStats(Stats stats) {
        if (stats.getSavedKit() != null) if (this.kits.allowedKit(Bukkit.getPlayerExact(stats.getName()), this.kits.getKit(stats.getSavedKit()))) {
            boolean loaded = this.kits.setKit(stats.getName(), stats.getSavedKit());
            if (loaded) Bukkit.getPlayerExact(stats.getName()).sendMessage(ChatColor.RED + "Loaded saved kit: " + this.kits.getKit(stats.getSavedKit()).getName());
            else
                Bukkit.getPlayerExact(stats.getName()).sendMessage(ChatColor.RED + "Failed to load saved kit: " + this.kits.getKit(stats.getSavedKit()).getName());
        } else {
            stats.setSavedKit(null);
            Bukkit.getPlayerExact(stats.getName()).sendMessage(ChatColor.RED + "No permission to use kit: " + this.kits.getKit(stats.getSavedKit()).getName());
            Bukkit.getPlayerExact(stats.getName()).sendMessage(ChatColor.RED + "Removed saved kit: " + this.kits.getKit(stats.getSavedKit()).getName());
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Stats stats = (Stats) this.playerStats.get(sender.getName());
        if (stats == null) {
            sender.sendMessage(ChatColor.RED + "Your stats have not loaded yet");
            return true;
        }
        Player p = Bukkit.getPlayerExact(sender.getName());
        if (cmd.getName().equalsIgnoreCase("savekit")) if (args.length > 0) {
            Kit kit = this.kits.getKit(args[0]);
            if (kit == null) {
                p.sendMessage(ChatColor.RED + "That kit does not exist!");
                return true;
            }
            if (this.kits.allowedKit(p, kit)) {
                stats.setSavedKit(kit.getSafeName());
                p.sendMessage(ChatColor.RED + "Saved kit " + kit.getName() + ChatColor.RESET + ChatColor.RED + "!");
            } else {
                p.sendMessage(ChatColor.RED + "You do not own that kit!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You must define a kit name");
        }

        if (cmd.getName().equalsIgnoreCase("stats")) {
            if (args.length > 0) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null) {
                    sender.sendMessage(ChatColor.RED + "That player does not exist");
                    return true;
                }
                stats = (Stats) this.playerStats.get(player.getName());
                if (stats == null) p.sendMessage(ChatColor.RED + player.getName() + "'s stats has not loaded yet");
            }
            stats.endTimeLog();
            stats.startTimeLog();
            sender.sendMessage(ChatColor.DARK_AQUA + "-- Now displaying " + stats.getName() + " stats --");
            sender.sendMessage(ChatColor.BLUE + "Wins: " + ChatColor.AQUA + stats.getWins());
            sender.sendMessage(ChatColor.BLUE + "Losses: " + ChatColor.AQUA + stats.getLosses());
            sender.sendMessage(ChatColor.BLUE + "Games played: " + ChatColor.AQUA + (stats.getWins() + stats.getLosses()));
            sender.sendMessage(ChatColor.BLUE + "Total kills: " + ChatColor.AQUA + stats.getKills());
            sender.sendMessage(ChatColor.BLUE + "Best kill streak: " + ChatColor.AQUA + stats.getHighestKillStreak());
            sender.sendMessage(ChatColor.BLUE + "Current kill streak: " + ChatColor.AQUA + stats.getCurrentKills());
            sender.sendMessage(ChatColor.BLUE + "Time logged: " + ChatColor.AQUA + stats.getTime(stats.getLogged()));
            sender.sendMessage(ChatColor.BLUE + "Wins/Losses: " + ChatColor.AQUA + new DecimalFormat("#.##").format(stats.getWins() / stats.getLosses()));
            sender.sendMessage(ChatColor.BLUE + "Kills per game: " + ChatColor.AQUA + new DecimalFormat("#.##").format(stats.getKills() / (stats.getLosses() + stats.getWins())));
            sender.sendMessage(ChatColor.BLUE + "Time per game: " + ChatColor.AQUA
                    + stats.getTime(stats.getLogged() / (stats.getLosses() + stats.getWins())).replace("0 days, ", "").replace("0 hours, ", ""));
            sender.sendMessage(ChatColor.DARK_AQUA + "-- End of " + stats.getName() + "'s stats --");
        }
        return true;
    }
}