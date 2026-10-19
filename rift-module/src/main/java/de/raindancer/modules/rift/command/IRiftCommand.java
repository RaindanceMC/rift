package de.raindancer.modules.rift.command;

import io.papermc.paper.command.brigadier.BasicCommand;

/**
 * A command belonging to this module. Built during Paper's bootstrap, so it holds a {@code Supplier} and looks its
 * module up only when run. The host wraps every one in {@code ModuleCommands.guarded}.
 */
public interface IRiftCommand extends BasicCommand {

    String describe();
}
