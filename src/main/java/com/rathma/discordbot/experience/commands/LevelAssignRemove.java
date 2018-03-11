package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;
import java.util.List;

public class LevelAssignRemove extends Command{
    public LevelAssignRemove(CommandService commandService, Message message) {
        identifier = "levelassign";
        childIdentifier = "remove";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();


        run(message);
    }

    public LevelAssignRemove() {
        identifier = "levelassign";
        childIdentifier = "remove";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
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

        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());
        String[] messageSplit = parsedCommand.split(" ");

        if(messageSplit.length<1){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No role specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        /* Need the listener for everything */
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService = null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService = (ExperienceService)o;
            }
        }
        if(experienceService==null){
            commandService.ongoingCommands.remove(this);
            return;
        }

        /* Let's find out whether we have easy to use mentioned roles, or have to parse */
        List<Role> mentionedRoles = message.getMentionedRoles();
        List<Role> roles=null;
        if(mentionedRoles.size()>0){
            roles=mentionedRoles;
        }
        else{
            roles = message.getGuild().getRolesByName(parsedCommand, true);
        }

        if(roles==null || roles.size()==0){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No role specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        List<Long> roleIDs = new ArrayList<>();
        for(Role r : roles){
            roleIDs.add(r.getIdLong());
        }
        experienceService.removeLevelRoles(guildID, roleIDs);
        if(writePermissions)
            message.getChannel().sendMessage("Successfully removed roles from level based auto assign.").queue();
        commandService.ongoingCommands.remove(this);
    }

}
