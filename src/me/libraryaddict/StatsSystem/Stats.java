package me.libraryaddict.StatsSystem;

public class Stats
{
	private String player;
	private int wins;
	private int timesPlayed;
	private int kills;
	private String savedKit;
	private int currentKills;
	private int deaths;
	private int highestKillStreak;
	private long timeLogged;
	private long startLog = 0L;
	private boolean isLogging = false;
	
	public Stats(String PlayerName, int Wins, int TimesPlayed, int Kills, int HighestKillStreak, String SavedKits, long timeLogged)
	{
		this.player = PlayerName;
		this.wins = Wins;
		this.timesPlayed = TimesPlayed;
		this.kills = Kills;
		this.highestKillStreak = HighestKillStreak;
		this.savedKit = SavedKits;
		this.timeLogged = timeLogged;
	}
	
	public int getHighestKillStreak()
	{
		return this.highestKillStreak;
	}
	
	public void setHighestKillStreak(int newHigh)
	{
		this.highestKillStreak = newHigh;
	}
	
	public int getLosses()
	{
		return this.timesPlayed - this.wins;
	}
	
	public String getName()
	{
		return this.player;
	}
	
	public int getDeaths()
	{
		return this.deaths;
	}
	
	public int getWins()
	{
		return this.wins;
	}
	
	public int getTimesPlayed()
	{
		return this.timesPlayed;
	}
	
	public int getKills()
	{
		return this.kills;
	}
	
	public int getCurrentKills()
	{
		return this.currentKills;
	}
	
	public void addKill()
	{
		this.currentKills += 1;
	}
	
	public String getSavedKit()
	{
		return this.savedKit;
	}
	
	public void setWins(int newWins)
	{
		this.wins = newWins;
	}
	
	public void addTimesPlayed()
	{
		this.timesPlayed += 1;
	}
	
	public void setKills(int newKills)
	{
		this.kills = newKills;
	}
	
	public void setSavedKit(String newSavedKit)
	{
		this.savedKit = newSavedKit;
	}
	
	public void setDeaths(int deaths)
	{
		this.deaths = deaths;
	}
	
	public String getTime(long time)
	{
		String word = "%d days, %d hours, %d minutes".replaceFirst("%d", (int) Math.floor(time / 86400L) + "").replaceFirst("%d", (int) Math.floor(time / 3600L) % 24 + "").replaceFirst("%d", (int) Math.floor(time / 60L) % 60 + "");
		return word;
	}
	
	public void startTimeLog()
	{
		if (this.isLogging)
			return;
		this.startLog = (System.currentTimeMillis() / 1000L);
		this.isLogging = true;
	}
	
	public void endTimeLog()
	{
		if (!this.isLogging)
			return;
		this.timeLogged += System.currentTimeMillis() / 1000L - this.startLog;
		this.isLogging = false;
	}
	
	public long getLogged()
	{
		endTimeLog();
		startTimeLog();
		return this.timeLogged;
	}
}