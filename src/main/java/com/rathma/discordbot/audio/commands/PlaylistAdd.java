package com.rathma.discordbot.audio.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.ArrayList;

public class PlaylistAdd extends Command {
    public PlaylistAdd(CommandService commandService, Message message){
        requiredUserPermissions=null;
        requiredPermissions= Permission.MESSAGE_WRITE;
        identifier = "prefix";
        childIdentifier = null;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();

        run(message);
    }

    public PlaylistAdd(){
        requiredUserPermissions=null;
        requiredPermissions=Permission.MESSAGE_WRITE;
        identifier="prefix";
        childIdentifier=null;
        aliases = new ArrayList<>();
    }

    public void run(Message message){
        if(!checkUserPermissions(message)){
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

        if(messageSplit.length>1 && writePermissions){
            message.getChannel().sendMessage("Invalid Syntax\nExample: .prefix").queue();
        }
        else if(writePermissions){
            message.getChannel().sendMessage("Current prefix is: " + commandService.prefixes.get(message.getGuild().getIdLong())).queue();
        }
        commandService.ongoingCommands.remove(this);
    }
}
