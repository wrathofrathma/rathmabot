package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;

import java.util.ArrayList;
import java.util.List;

public class RankAssignAdd extends Command{
    public RankAssignAdd(CommandService commandService, Message message){
        identifier = "rankassign";
        childIdentifier = "add";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public RankAssignAdd() {
        identifier = "rankassign";
        childIdentifier = "add";
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


        /* .rankassign add role rankreq */
        if(messageSplit.length!=2){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: Incorrect number of parameters").queue();
                commandService.ongoingCommands.remove(this);
                return;
        }

        /* First we need to parse the roles and check if they exist on the server */
        List<Role> rolesByName = message.getGuild().getRolesByName(messageSplit[0], true);
        if(rolesByName==null){
            if(writePermissions)
                message.getChannel().sendMessage("Error: Role not found");
            commandService.ongoingCommands.remove(this);
            return;
        }

        /* Now we should make sure the rank requirement is actually a thing */
        int rankReq = 0;
        try{
            rankReq = Integer.parseInt(messageSplit[1]);
        }
        catch (NumberFormatException e){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No rank requirement specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        /* Cool, now we need to get our ExperienceService to call the method to update the roles */
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService)
                experienceService = (ExperienceService)o;
        }
        if(experienceService==null) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        /* And now we just add all the roles that match by name */
        int addnum = 0;
        for(Role role : rolesByName){
            experienceService.setRankRole(guildID, rankReq, role.getIdLong());
            addnum=addnum+1;
        }
        message.getChannel().sendMessage("Successfully added " + addnum + " roles to the rank based auto assign service.").queue();
        commandService.ongoingCommands.remove(this);
    }
}
