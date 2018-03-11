package com.rathma.discordbot.core.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.ArrayList;

public class PrefixSet extends Command {

    public PrefixSet(CommandService commandService, Message message){
        requiredPermissions=null;
        defaultRequiredUserPermission=Permission.MANAGE_SERVER;
        identifier = "prefix";
        childIdentifier = "set";
        this.commandService=commandService;
        this.jda=commandService.jda;
        this.database=commandService.database;
        aliases = new ArrayList<>();
        run(message);
    }

    // Only used when in the list of commands to cycle through in CommandService.
    public PrefixSet(){
        requiredPermissions=null;
        defaultRequiredUserPermission=Permission.MANAGE_SERVER;
        identifier = "prefix";
        childIdentifier = "set";
        aliases = new ArrayList<>();
    };

    /* Internal parsing */
    public void run(Message message){
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

        /* Assuming index 0 & 1 are the prefix, identifier and child identifier */
        /* Let's make sure they actually set an identifier */
        if (parsedCommand.isEmpty() && writePermissions) {
            message.getChannel().sendMessage("No prefix specified.").queue();
        }
        else {
            commandService.updatePrefix(message.getGuild().getIdLong(), parsedCommand);
            if(writePermissions) {
                message.getChannel().sendMessage("Prefix successfully set to: " + parsedCommand).queue();
            }
        }
        commandService.ongoingCommands.remove(this);
    }
}
