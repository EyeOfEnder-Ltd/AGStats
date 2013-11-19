package me.libraryaddict.StatsSystem;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StatsSaveThread extends Thread {
    ConcurrentLinkedQueue<Stats> quitQueue = new ConcurrentLinkedQueue();
    StatsSystem plugin;
    public Connection con = null;

    public StatsSaveThread(StatsSystem stats) {
        this.plugin = stats;
    }

    public void SQLdisconnect() {
        try {
            System.out.println("[StatsSaveThread] Disconnecting from MySQL database...");
            this.con.close();
        } catch (SQLException ex) {
            System.err.println("[StatsSaveThread] Error while closing the connection...");
        } catch (NullPointerException ex) {
            System.err.println("[StatsSaveThread] Error while closing the connection...");
        }
    }

    public void SQLconnect() {
        try {
            System.out.println("[StatsSaveThread] Connecting to MySQL database...");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + this.plugin.SQL_HOST + "/" + this.plugin.SQL_DATA;
            this.con = DriverManager.getConnection(conn, this.plugin.SQL_USER, this.plugin.SQL_PASS);
        } catch (ClassNotFoundException ex) {
            System.err.println("[StatsSaveThread] No MySQL driver found!");
        } catch (SQLException ex) {
            System.err.println("[StatsSaveThread] Error while fetching MySQL connection!");
        } catch (Exception ex) {
            System.err.println("[StatsSaveThread] Unknown error while fetchting MySQL connection.");
        }
    }

    public void run() {
        System.out.println("Stats Save Thread started");
        SQLconnect();
        while (true) {
            if (this.quitQueue.peek() != null) {
                Stats stats = (Stats) this.quitQueue.poll();
                String savedKit = stats.getSavedKit();
                if (savedKit == null) savedKit = "null";
                String statement = "UPDATE AGStats SET Wins='" + stats.getWins() + "', GamesPlayed='" + stats.getTimesPlayed() + "', Kills='" + stats.getKills() + "', HighestKillStreak='"
                        + stats.getHighestKillStreak() + "', SavedKit='" + savedKit + "', TimeLogged='" + stats.getLogged() + "' WHERE `Name` = '" + stats.getName() + "' ;";
                try {
                    Statement stamt = this.con.createStatement();
                    stamt.executeUpdate(statement);
                    stamt.close();
                } catch (SQLException ex) {
                    System.err.println("[StatsSaveThread] Error while doing statement: " + statement);
                    System.err.println("[StatsSaveThread] MySql error: " + ex.getMessage());
                } catch (NullPointerException ex) {
                    System.err.println("[StatsSaveThread] Error while doing statement: " + statement);
                    System.err.println("[StatsSaveThread] Error while performing a query. (NullPointerException)");
                }
            }
            if (this.quitQueue.peek() == null) try {
                Thread.currentThread();
                Thread.sleep(1000L);
            } catch (InterruptedException localInterruptedException) {
            }
        }
    }
}