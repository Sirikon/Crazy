package de.st_ddt.crazyloginfilter;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import de.st_ddt.crazyloginfilter.data.PlayerAccessFilter;
import de.st_ddt.crazyloginfilter.databases.CrazyLoginFilterConfigurationDatabase;
import de.st_ddt.crazyloginfilter.listener.CrazyLoginFilterPlayerListener;
import de.st_ddt.crazyplugin.CrazyPlayerDataPlugin;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandCircumstanceException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandExecutorException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandNoSuchException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandParameterException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandPermissionException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandUsageException;
import de.st_ddt.crazyplugin.exceptions.CrazyException;
import de.st_ddt.crazyplugin.exceptions.CrazyNotImplementedException;
import de.st_ddt.crazyutil.ChatHelper;
import de.st_ddt.crazyutil.ToStringDataGetter;
import de.st_ddt.crazyutil.databases.DatabaseType;

public class CrazyLoginFilter extends CrazyPlayerDataPlugin<PlayerAccessFilter, PlayerAccessFilter>
{

	private static CrazyLoginFilter plugin;
	protected PlayerAccessFilter serverFilter;
	private CrazyLoginFilterPlayerListener playerListener;
	protected String filterNames;
	protected int minNameLength;
	protected int maxNameLength;

	public static CrazyLoginFilter getPlugin()
	{
		return plugin;
	}

	@Override
	protected String getShortPluginName()
	{
		return "clf";
	}

	@Override
	public void onEnable()
	{
		plugin = this;
		registerHooks();
		super.onEnable();
	}

	public void registerHooks()
	{
		this.playerListener = new CrazyLoginFilterPlayerListener(this);
		final PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(playerListener, this);
	}

	@Override
	public void load()
	{
		super.load();
		final ConfigurationSection config = getConfig();
		if (config.getConfigurationSection("serverFilter") == null)
			serverFilter = new PlayerAccessFilter("serverFilter");
		else
			try
			{
				serverFilter = new PlayerAccessFilter(config.getConfigurationSection("serverFilter"));
			}
			catch (final Exception e)
			{
				System.out.println("Invalid Server Access Filter config!");
				serverFilter = new PlayerAccessFilter("serverFilter");
			}
		// PlayerNames
		filterNames = config.getString("filterNames", "false");
		if (filterNames.equals("false"))
			filterNames = ".";
		else if (filterNames.equals("true"))
			filterNames = "[a-zA-Z0-9_]";
		minNameLength = Math.min(Math.max(config.getInt("minNameLength", 3), 1), 16);
		maxNameLength = Math.min(Math.max(config.getInt("maxNameLength", 16), minNameLength), 16);
		// Database
		setupDatabase();
	}

	public void setupDatabase()
	{
		final ConfigurationSection config = getConfig();
		final String saveType = config.getString("database.saveType", "CONFIG").toUpperCase();
		final DatabaseType type = DatabaseType.valueOf(saveType);
		final String tableName = config.getString("database.tableName", "accessfilter");
		config.set("database.tableName", tableName);
		try
		{
			if (type == DatabaseType.CONFIG)
			{
				database = new CrazyLoginFilterConfigurationDatabase(tableName, config, this);
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			database = null;
		}
		finally
		{
			if (database == null)
				broadcastLocaleMessage(true, "crazyloginfilter.warndatabase", "DATABASE.ACCESSWARN", saveType);
			else
			{
				database.loadAllEntries();
				sendLocaleMessage("DATABASE.LOADED", Bukkit.getConsoleSender(), database.getAllEntries().size());
			}
		}
	}

	public void saveConfiguration()
	{
		final ConfigurationSection config = getConfig();
		serverFilter.saveToConfigDatabase(config, "serverFilter", new String[] { "name", "checkIP", "whitelistIP", "ips", "checkConnection", "whitelistConnection", "connections" });
		if (filterNames.equals("."))
			config.set("filterNames", false);
		else
			config.set("filterNames", filterNames);
		config.set("minNameLength", minNameLength);
		config.set("maxNameLength", maxNameLength);
		logger.save(config, "logs.");
		super.saveConfiguration();
	}

	@Override
	public boolean commandMain(final CommandSender sender, final String commandLabel, final String[] args) throws CrazyException
	{
		if (commandLabel.equalsIgnoreCase("create"))
		{
			commandMainCreate(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("show"))
		{
			commandMainShow(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("ip") || commandLabel.equalsIgnoreCase("ips"))
		{
			commandMainIP(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("connection") || commandLabel.equalsIgnoreCase("connections"))
		{
			commandMainConnection(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("delete"))
		{
			commandMainDelete(sender, args);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("serverfilter"))
		{
			if (args.length == 0)
				throw new CrazyCommandUsageException("/crazyloginfilter serverfilter show", "/crazyloginfilter serverfilter ip ...", "/crazyloginfilter serverfilter connection ...");
			final String[] newArgs = ChatHelper.shiftArray(args, 1);
			commandMainServerFilter(sender, args[0].toLowerCase(), newArgs);
			return true;
		}
		if (commandLabel.equalsIgnoreCase("mode"))
		{
			commandMainMode(sender, args);
			return true;
		}
		return false;
	}

	private void commandMainServerFilter(final CommandSender sender, final String commandLabel, final String[] args) throws CrazyException
	{
		if (!sender.hasPermission("crazyloginfilter.admin"))
			throw new CrazyCommandPermissionException();
		if (commandLabel.equalsIgnoreCase("show"))
		{
			commandMainShow(sender, args, serverFilter);
		}
		else if (commandLabel.equalsIgnoreCase("ip") || commandLabel.equalsIgnoreCase("ips"))
		{
			commandMainIP(sender, args, serverFilter);
		}
		else if (commandLabel.equalsIgnoreCase("connection") || commandLabel.equalsIgnoreCase("connections"))
		{
			commandMainConnection(sender, args, serverFilter);
		}
	}

	private void commandMainCreate(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (sender instanceof ConsoleCommandSender)
			throw new CrazyCommandExecutorException(false);
		final Player player = (Player) sender;
		final PlayerAccessFilter data = new PlayerAccessFilter(player);
		data.setCheckIP(false);
		data.setCheckConnection(false);
		database.save(data);
		sendLocaleMessage("COMMMAND.CREATED", sender, player.getName());
	}

	private void commandMainShow(final CommandSender sender, final String[] args) throws CrazyException
	{
		if (sender instanceof ConsoleCommandSender)
			throw new CrazyCommandExecutorException(false);
		final Player player = (Player) sender;
		commandMainShow(sender, args, getPlayerData(player));
	}

	private void commandMainShow(final CommandSender sender, final String[] args, final PlayerAccessFilter data) throws CrazyException
	{
		throw new CrazyNotImplementedException();
	}

	private void commandMainIP(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (sender instanceof ConsoleCommandSender)
			throw new CrazyCommandExecutorException(false);
		final Player player = (Player) sender;
		commandMainIP(sender, args, getPlayerData(player));
	}

	private void commandMainIP(final CommandSender sender, final String[] args, final PlayerAccessFilter data) throws CrazyCommandException
	{
		if (data == null)
			throw new CrazyCommandCircumstanceException("when AccessFilter is created!");
		switch (args.length)
		{
			case 1:
				if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("list"))
				{
					sendListMessage(sender, "COMMMAND.IP.LISTHEAD", 10, 1, data.getIPs(), new ToStringDataGetter());
					return;
				}
				if (args[0].equalsIgnoreCase("check"))
				{
					sendLocaleMessage("COMMMAND.IP.CHECK", sender, data.isCheckIP() ? "True" : "False");
					return;
				}
				if (args[0].equalsIgnoreCase("whitelist"))
				{
					sendLocaleMessage("COMMMAND.IP.WHITELIST", sender, data.isWhitelistIP() ? "True" : "False");
					return;
				}
				break;
			case 2:
				String regex = args[1];
				if (args[0].equalsIgnoreCase("add"))
				{
					data.addIP(regex);
					sendLocaleMessage("COMMAND.IP.ADDED", sender, regex);
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
				if (args[0].equalsIgnoreCase("remove"))
				{
					try
					{
						final int index = Integer.parseInt(regex);
						regex = data.removeIP(index);
					}
					catch (final NumberFormatException e)
					{
						data.removeIP(regex);
					}
					sendLocaleMessage("COMMAND.IP.REMOVED", sender, regex);
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
				if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("list"))
				{
					try
					{
						final int page = Integer.parseInt(args[1]);
						sendListMessage(sender, "COMMMAND.IP.LISTHEAD", 10, page, data.getIPs(), new ToStringDataGetter());
					}
					catch (final NumberFormatException e)
					{
						throw new CrazyCommandParameterException(1, "Integer");
					}
					return;
				}
				if (args[0].equalsIgnoreCase("check"))
				{
					boolean newValue = false;
					if (args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("yes"))
						newValue = true;
					data.setCheckIP(newValue);
					sendLocaleMessage("COMMMAND.IP.CHECK", sender, data.isCheckIP() ? "True" : "False");
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
				if (args[0].equalsIgnoreCase("whitelist"))
				{
					boolean newValue = false;
					if (args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("yes"))
						newValue = true;
					data.setWhitelistIP(newValue);
					sendLocaleMessage("COMMMAND.IP.WHITELIST", sender, data.isWhitelistIP() ? "True" : "False");
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
		}
		if (data == serverFilter)
			throw new CrazyCommandUsageException("/crazyloginfilter serverFilter ip show [Page]", "/crazloginfilter serverFilter ip <add/remove> <Value>", "/crazloginfilter serverFilter ip whitelist [True/False]");
		else
			throw new CrazyCommandUsageException("/crazyloginfilter ip show [Page]", "/crazloginfilter ip <add/remove> <Value>", "/crazloginfilter ip whitelist [True/False]");
	}

	private void commandMainConnection(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (sender instanceof ConsoleCommandSender)
			throw new CrazyCommandExecutorException(false);
		final Player player = (Player) sender;
		commandMainConnection(sender, args, getPlayerData(player));
	}

	private void commandMainConnection(final CommandSender sender, final String[] args, final PlayerAccessFilter data) throws CrazyCommandException
	{
		if (data == null)
			throw new CrazyCommandCircumstanceException("when AccessFilter is created!");
		switch (args.length)
		{
			case 1:
				if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("list"))
				{
					sendListMessage(sender, "COMMMAND.CONNECTION.LISTHEAD", 10, 1, data.getConnections(), new ToStringDataGetter());
					return;
				}
				if (args[0].equalsIgnoreCase("check"))
				{
					sendLocaleMessage("COMMMAND.CONNECTION.CHECK", sender, data.isCheckConnection() ? "True" : "False");
					return;
				}
				if (args[0].equalsIgnoreCase("whitelist"))
				{
					sendLocaleMessage("COMMMAND.CONNECTION.WHITELIST", sender, data.isWhitelistConnection() ? "True" : "False");
					return;
				}
				break;
			case 2:
				String regex = args[1];
				if (args[0].equalsIgnoreCase("add"))
				{
					data.addConnection(regex);
					sendLocaleMessage("COMMAND.CONNECTION.ADDED", sender, regex);
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
				if (args[0].equalsIgnoreCase("remove"))
				{
					try
					{
						final int index = Integer.parseInt(regex);
						regex = data.removeConnection(index);
					}
					catch (final NumberFormatException e)
					{
						data.removeConnection(regex);
					}
					sendLocaleMessage("COMMAND.CONNECTION.REMOVED", sender, regex);
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
				if (args[0].equalsIgnoreCase("show") || args[0].equalsIgnoreCase("list"))
				{
					try
					{
						final int page = Integer.parseInt(args[1]);
						sendListMessage(sender, "COMMMAND.CONNECTION.LISTHEAD", 10, page, data.getConnections(), new ToStringDataGetter());
					}
					catch (final NumberFormatException e)
					{
						throw new CrazyCommandParameterException(1, "Integer");
					}
					return;
				}
				if (args[0].equalsIgnoreCase("check"))
				{
					boolean newValue = false;
					if (args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("yes"))
						newValue = true;
					data.setCheckConnection(newValue);
					sendLocaleMessage("COMMMAND.CONNECTION.CHECK", sender, data.isCheckConnection() ? "True" : "False");
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
				if (args[0].equalsIgnoreCase("whitelist"))
				{
					boolean newValue = false;
					if (args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("yes"))
						newValue = true;
					data.setWhitelistConnection(newValue);
					sendLocaleMessage("COMMMAND.CONNECTION.WHITELIST", sender, data.isWhitelistConnection() ? "True" : "False");
					if (data == serverFilter)
						saveConfiguration();
					else
						database.save(data);
					return;
				}
		}
		if (data == serverFilter)
			throw new CrazyCommandUsageException("/crazyloginfilter serverFilter connection show [Page]", "/crazloginfilter serverFilter connection <add/remove> <Value>", "/crazloginfilter serverFilter connection whitelist [True/False]");
		else
			throw new CrazyCommandUsageException("/crazyloginfilter connection show [Page]", "/crazloginfilter connection <add/remove> <Value>", "/crazloginfilter connection whitelist [True/False]");
	}

	private void commandMainDelete(final CommandSender sender, final String[] args) throws CrazyException
	{
		throw new CrazyNotImplementedException();
	}

	private void commandMainMode(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission("crazyloginfilter.mode"))
			throw new CrazyCommandPermissionException();
		switch (args.length)
		{
			case 2:
				if (args[0].equalsIgnoreCase("filterNames"))
				{
					String newFilter = filterNames;
					newFilter = args[0];
					if (newFilter.equalsIgnoreCase("false") || newFilter.equalsIgnoreCase("0") || newFilter.equalsIgnoreCase("off"))
						newFilter = ".";
					else if (newFilter.equalsIgnoreCase("true") || newFilter.equalsIgnoreCase("1") || newFilter.equalsIgnoreCase("on"))
						newFilter = "[a-zA-Z0-9_]";
					filterNames = newFilter;
					sendLocaleMessage("MODE.CHANGE", sender, "filterNames", filterNames.equals(".") ? "disabled" : filterNames);
					saveConfiguration();
					return;
				}
				else if (args[0].equalsIgnoreCase("minNameLength"))
				{
					int length = minNameLength;
					try
					{
						length = Integer.parseInt(args[1]);
					}
					catch (final NumberFormatException e)
					{
						throw new CrazyCommandParameterException(1, "Integer");
					}
					minNameLength = Math.min(Math.max(length, 1), 16);
					sendLocaleMessage("MODE.CHANGE", sender, "minNameLength", minNameLength + " characters");
					saveConfiguration();
					return;
				}
				else if (args[0].equalsIgnoreCase("maxNameLength"))
				{
					int length = maxNameLength;
					try
					{
						length = Integer.parseInt(args[1]);
					}
					catch (final NumberFormatException e)
					{
						throw new CrazyCommandParameterException(1, "Integer");
					}
					maxNameLength = Math.min(Math.max(length, 1), 16);
					sendLocaleMessage("MODE.CHANGE", sender, "maxNameLength", maxNameLength + " characters");
					saveConfiguration();
					return;
				}
				else if (args[0].equalsIgnoreCase("saveDatabaseOnShutdown"))
				{
					boolean newValue = false;
					if (args[1].equalsIgnoreCase("1") || args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("yes"))
						newValue = true;
					saveDatabaseOnShutdown = newValue;
					sendLocaleMessage("MODE.CHANGE", sender, "saveDatabaseOnShutdown", saveDatabaseOnShutdown ? "True" : "False");
					saveConfiguration();
					return;
				}
				throw new CrazyCommandNoSuchException("Mode", args[0]);
			case 1:
				if (args[0].equalsIgnoreCase("filterNames"))
				{
					sendLocaleMessage("MODE.CHANGE", sender, "filterNames", filterNames.equals(".") ? "disabled" : filterNames);
					return;
				}
				else if (args[0].equalsIgnoreCase("minNameLength"))
				{
					sendLocaleMessage("MODE.CHANGE", sender, "minNameLength", minNameLength + " characters");
					return;
				}
				else if (args[0].equalsIgnoreCase("maxNameLength"))
				{
					sendLocaleMessage("MODE.CHANGE", sender, "maxNameLength", maxNameLength + " characters");
					return;
				}
				else if (args[0].equalsIgnoreCase("saveDatabaseOnShutdown"))
				{
					sendLocaleMessage("MODE.CHANGE", sender, "saveDatabaseOnShutdown", saveDatabaseOnShutdown ? "True" : "False");
					return;
				}
				else if (args[0].equalsIgnoreCase("filterNames"))
				{
					sendLocaleMessage("MODE.CHANGE", sender, "filterNames", filterNames.equals(".") ? "disabled" : filterNames);
					return;
				}
				throw new CrazyCommandNoSuchException("Mode", args[0]);
			default:
				throw new CrazyCommandUsageException("/crazylogin mode <Mode> [Value]");
		}
	}

	public boolean checkIP(final Player player)
	{
		return checkIP(player.getName(), player.getAddress().getAddress().getHostAddress());
	}

	public boolean checkIP(final Player player, final String IP)
	{
		return checkIP(player.getName(), IP);
	}

	public boolean checkIP(final String player, final String IP)
	{
		if (!serverFilter.checkIP(IP))
			return false;
		final PlayerAccessFilter filter = getPlayerData(player);
		if (filter == null)
			return true;
		return filter.checkIP(IP);
	}

	public boolean checkConnection(final Player player)
	{
		return checkConnection(player.getName(), player.getAddress().getHostName());
	}

	public boolean checkConnection(final Player player, final String connection)
	{
		return checkConnection(player.getName(), connection);
	}

	public boolean checkConnection(final String player, final String connection)
	{
		if (!serverFilter.checkConnection(connection))
			return false;
		final PlayerAccessFilter filter = getPlayerData(player);
		if (filter == null)
			return true;
		return filter.checkConnection(connection);
	}

	public PlayerAccessFilter getServerAccessFilter()
	{
		return serverFilter;
	}

	public boolean checkNameChars(final String name)
	{
		return name.matches(filterNames + "*");
	}

	public int getMinNameLength()
	{
		return minNameLength;
	}

	public int getMaxNameLength()
	{
		return maxNameLength;
	}

	public boolean checkNameLength(final String name)
	{
		final int length = name.length();
		if (length < minNameLength)
			return false;
		if (length > maxNameLength)
			return false;
		return true;
	}
}
