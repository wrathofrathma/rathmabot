package com.rathma.discordbot.experience.commands;


import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

import java.util.ArrayList;
import java.util.List;

public class SetExpRatio extends Command {
    public SetExpRatio(CommandService commandService, Message message){
        this.database = commandService.database;
        this.jda = commandService.jda;
        this.commandService = commandService;
        identifier = "set";
        childIdentifier = "exp_ratio";
        requiredPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        aliases = new ArrayList<>();

        run(message);
    }


    public SetExpRatio() {
        identifier = "set";
        childIdentifier = "exp_ratio";
        requiredPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
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
        String[] messageSplit = message.getContentRaw().split(" ");
        if(messageSplit.length<3){
            /* No ratio specified */
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: Ratio not specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        try{
            float expRatio = Float.parseFloat(messageSplit[2]);
            List<Object> listeners = jda.getRegisteredListeners();
            for(Object o : listeners){
                if(o instanceof ExperienceService){
                    ((ExperienceService) o).setGuildExpRatio(message.getGuild().getIdLong(), expRatio);
                    if(writePermissions)
                        message.getChannel().sendMessage("Succesfully set guild exp ratio to: " + expRatio).queue();
                }
            }
        }
        catch (NumberFormatException e){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No number specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        commandService.ongoingCommands.remove(this);
    }
}
