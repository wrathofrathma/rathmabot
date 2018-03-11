package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.entities.Message;

import java.util.ArrayList;
import java.util.List;


public class ExpRatio extends Command {
    public ExpRatio(CommandService commandService, Message message){
        this.database = commandService.database;
        this.jda = commandService.jda;
        this.commandService = commandService;
        identifier = "exp";
        childIdentifier = "ratio";
        defaultRequiredUserPermission=null;
        requiredPermissions=null;
        aliases = new ArrayList<>();

        run(message);
    }
    public ExpRatio() {
        identifier = "exp";
        childIdentifier = "ratio";
        defaultRequiredUserPermission=null;
        requiredPermissions=null;
        aliases = new ArrayList<>();

    }

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
        if(!writePermissions) {
            commandService.ongoingCommands.remove(this);
            return;
        }

        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        commandService.ongoingCommands.remove(this);
        if(!parsedCommand.isEmpty()){
            message.getChannel().sendMessage("Syntax Error: too many parameters").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        List<Object> listeners = jda.getRegisteredListeners();
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                double ratio = ((ExperienceService) o).getGuildExpRatio(message.getGuild().getIdLong());
                message.getChannel().sendMessage("Experience ratio: " + ratio).queue();
            }
        }
        commandService.ongoingCommands.remove(this);
    }
}
