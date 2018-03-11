package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.ArrayList;

public class ExpFullreset extends Command {
    public ExpFullreset(CommandService commandService, Message message) {
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        identifier = "exp";
        childIdentifier = "fullreset";
        aliases = new ArrayList<>();

        run(message);
    }

    public ExpFullreset() {
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        identifier = "exp";
        childIdentifier = "fullreset";
        aliases = new ArrayList<>();

    }

    public void run(Message message) {
        if(!checkUserPermissions(message)) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        if(!checkBotPermissions(message)) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        boolean writePermissions = checkChannelWritePermission(message.getTextChannel());
        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        String[] messageSplit = parsedCommand.split(" ");

        if(writePermissions)
            message.getChannel().sendMessage("Not implemented yet").queue();
        commandService.ongoingCommands.remove(this);
    }
}
