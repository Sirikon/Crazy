package de.st_ddt.crazyarena.participants.race;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.st_ddt.crazyarena.arenas.race.RaceArena;
import de.st_ddt.crazyarena.arenas.race.RaceTarget;
import de.st_ddt.crazyarena.participants.Participant;

public class RaceParticipant extends Participant<RaceParticipant, RaceArena>
{

	protected final Location start;
	protected RaceTarget target;

	public RaceParticipant(Player player, RaceArena arena, Location start, RaceTarget target)
	{
		super(player, arena);
		this.start = start;
		this.target = target;
	}

	public Location getStart()
	{
		return start;
	}

	public RaceTarget getTarget()
	{
		return target;
	}

	public void setTarget(RaceTarget target)
	{
		this.target = target;
	}

	public void reachTarget()
	{
		target = target.getNext();
		if (target == null)
			((RaceArena) arena).reachFinish(this);
	}

	@Override
	public void showDetailed(CommandSender target, String chatHeader)
	{
		// EDIT Auto-generated method stub
	}

	@Override
	public String getParameter(int index)
	{
		switch (index)
		{
			case 0:
				return getName();
			case 1:
				return arena.getName();
			case 2:
				return participantType.toString();
			case 3:
				return target.getName();
			case 4:
				return "target.location";
			default:
				return "";
		}
	}

	@Override
	public int getParameterCount()
	{
		return 4;
	}
}
