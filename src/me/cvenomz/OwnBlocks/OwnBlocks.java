package me.cvenomz.OwnBlocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class OwnBlocks extends JavaPlugin{

	public String mainDirectory = "plugins" + File.separator + "OwnBlocks";
	public Logger log = Logger.getLogger("Minecraft");
	//private File databaseFile = new File(mainDirectory + File.separator + "");
	public Map<OBBlock, String> database;
	public ArrayList<String> activatedPlayers;
	public Properties properties; 
	public ArrayList<Integer> exclude;
	private FileInputStream fis;
	private FileOutputStream fos;
	private ObjectInputStream obi;
	private ObjectOutputStream obo;
	private File file;
	private File propertiesFile;
	private OwnBlocksBlockListener blockListener;
	private OwnBlocksPlayerListener playerListener;
	private double version = 4.0;
	
	@Override
	public void onDisable() {
		// TODO Auto-generated method stub
		try {
			removeAllPlayers();
			fos = new FileOutputStream(file);
			obo = new ObjectOutputStream(fos);
			obo.writeObject(database);
			obo.close();
			fos.close();
			log.info("[OwnBlocks] Wrote database to file");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
		new File(mainDirectory).mkdirs();
		
		try {
				file = new File(mainDirectory + File.separator + "Database.db");
				if (file.exists())
				{
					fis = new FileInputStream(file);
					obi = new ObjectInputStream(fis);
					database = (Map<OBBlock, String>) obi.readObject();
					log.info("[OwnBlocks] Database read in from file");
					fis.close();
					obi.close();
				}
				else
				{
					log.info("[OwnBlocks] Database does not exist.  Creating initial database...");
					database = new HashMap<OBBlock, String>();
				}
		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		
		if (database != null)
		{
			properties = new Properties();
			propertiesFile = new File(mainDirectory + File.separator + "OwnBlocks.properties");
			try {
				if (!propertiesFile.exists())
					createExamplePropertiesFile();
				properties.load(new FileInputStream(propertiesFile));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.severe("[OwnBlocks] Could not create or read properties file");
					e.printStackTrace();
			}
			exclude = new ArrayList<Integer>();
			readProperties();
			activatedPlayers = new ArrayList<String>();
			addCurrentPlayers();
			blockListener = new OwnBlocksBlockListener(this);
			playerListener = new OwnBlocksPlayerListener(this);
			PluginManager pm = getServer().getPluginManager();
			pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Event.Priority.Normal, this);
			pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Event.Priority.Normal, this);
			log.info("[OwnBlocks] version " + version + " initialized");
		}
		
	}
	
	private void addCurrentPlayers()
	{
		Player[] players = getServer().getOnlinePlayers();
		for (int i=0; i < players.length; i++)
		{
			addPlayer(players[i].getName());
		}
	}
	
	private void removeAllPlayers()
	{
		for (String name : activatedPlayers)
		{
			getServer().getPlayer(name).sendMessage(ChatColor.DARK_PURPLE + "Server is Deactivating OwnBlocks--");
			removePlayer(name);
		}
	}
	
	public void addPlayer(String name)
	{
		if (!activatedPlayers.contains(name))
		{
			activatedPlayers.add(name);
			getServer().getPlayer(name).sendMessage(ChatColor.GREEN + name + ": OwnBlocks activated; Blocks you build will be protected");
		}
	}
	
	public void removePlayer(String name)
	{
		if (activatedPlayers.contains(name))
		{
			activatedPlayers.remove(name);
			getServer().getPlayer(name).sendMessage(ChatColor.AQUA + name + ": OwnBlocks now deactivated");
		}
	}
	
	private void togglePlayer(String name)
	{
		if (activatedPlayers.contains(name))
			removePlayer(name);
		else
			addPlayer(name);
	}
	
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		String commandName = command.getName().toLowerCase();
		
		if (!(sender instanceof Player))
		{
			log.info("OwnBlocks Activated Players: " + activatedPlayers.toString());
			return false;
		}
		Player p = (Player)sender;
		String player = p.getName();
		//log.info("\""+player+"\"");
		if (commandName.equalsIgnoreCase("ownblocks") || commandName.equalsIgnoreCase("ob"))
		{
			togglePlayer(player);
			return true;
		}
		
		return false;
	}
	
	private void createExamplePropertiesFile()
	{
		try {
			propertiesFile.createNewFile();
			PrintWriter pw = new PrintWriter(propertiesFile);
			pw.println("#OwnBlocks Properties File");
			pw.println("\n#to exclude certain items from being protected when a player places them" +
						"\n#regarldess of if they have OwnBlocks activated, enter the ID of the item" +
						"\n#after the exclude key (comma separated; no spaces)" +
						"\n#The example below would exclude Dirt(03) and Sand(12) from being added to the database" +
						"\n#exclude=03,12" +
						"\n#Please Note: changes are not retro-active. In this example, dirt placed before being excluded" +
						"\n#Would still be protected, even after it is added to the 'exclude list'");
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void readProperties()
	{
		String str = properties.getProperty("exclude");
		if (str != null)
		{
			StringTokenizer st = new StringTokenizer(str, ",");
			while (st.hasMoreTokens())
			{
				try {
				exclude.add(Integer.parseInt(st.nextToken()));
				}catch (NumberFormatException e)
				{log.severe("[OwnBlocks] Error reading exclude IDs -- Not an Integer");}
			}
		}
	}
	
	

}
