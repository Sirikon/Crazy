package de.st_ddt.crazyutil.action;

import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.configuration.ConfigurationSection;

public abstract class ActionList extends Action
{

	protected final ArrayList<Action> actions = new ArrayList<Action>();

	public ActionList(ConfigurationSection config)
	{
		super(config);
		config = config.getConfigurationSection("actions");
		if (config == null)
			return;
		for (String name : config.getKeys(false))
		{
			try
			{
				Action action = Action.load(config.getConfigurationSection(name));
				if (action != null)
					actions.add(action);
			}
			catch (Exception e)
			{
				System.out.println("Error loading condition: " + name);
				e.printStackTrace();
			}
		}
	}

	public ActionList(String name)
	{
		super(name);
	}

	public ActionList(String name, Collection<? extends Action> actions)
	{
		super(name);
		this.actions.addAll(actions);
	}

	public void addAction(Action action)
	{
		this.actions.add(action);
	}

	public void addAllAction(Collection<? extends Action> actions)
	{
		this.actions.addAll(actions);
	}

	public ArrayList<Action> getActions()
	{
		return actions;
	}

	@Override
	public void save(ConfigurationSection config, String path)
	{
		super.save(config, path);
		config.set(path + "actions", null);
		for (Action action : actions)
			action.save(config, path + "actions." + action.getName() + ".");
	}
}
