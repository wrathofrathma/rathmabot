package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MVPAssign extends Command {
    public MVPAssign(CommandService commandService, Message message) {
        this.database = commandService.database;
        this.jda = commandService.jda;
        this.commandService = commandService;
        identifier = "mvpassign";
        childIdentifier = null;
        requiredPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        aliases = new ArrayList<>();
        aliases.add("mvp");
        run(message);

    }
    public MVPAssign(){
        identifier = "mvpassign";
        childIdentifier = null;
        requiredPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        aliases = new ArrayList<>();
        aliases.add("mvp");
    }
    public MessageEmbed createWeeklyRankAssignList(long guildID, Map<Long, Integer> weeklyRoles){
        Guild guild = jda.getGuildById(guildID);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLUE);
        embedBuilder.setDescription("**" + guild.getName() + "'s roles auto-assigned by weekly rank.**");
        String roles = "";
        String levels = "";
        if(weeklyRoles!=null) {
            for (Long key : weeklyRoles.keySet()) {
                Role r = guild.getRoleById(key);
                if (r != null) {
                    roles = roles.concat(r.getName() + "\n");
                    levels = levels.concat(weeklyRoles.get(key).toString() + "\n");
                }
            }
        }
        embedBuilder.addField("Roles", roles, true);
        embedBuilder.addField("Rank Requirement", levels, true);
        return embedBuilder.build();
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
        if(!writePermissions){
            commandService.ongoingCommands.remove(this);
            return;
        }
        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        String[] messageSplit = parsedCommand.split(" ");

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

        /* 3 possible commands
         * mvp add role rank
         * mvp remove role
         * mvp list
         */
        System.out.println("Message Length " + messageSplit.length );
        if(messageSplit.length<=1){
            /* Then they either don't have hte parameters added, or they're listing */
            if(messageSplit.length==0 || messageSplit[0].isEmpty()) {
                if(writePermissions)
                    message.getChannel().sendMessage(createWeeklyRankAssignList(guildID, experienceService.getWeeklyRoles(guildID))).queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
            else if(messageSplit[0].equals("list")){
                if(writePermissions && experienceService.weeklyRoles.containsKey(guildID))
                    message.getChannel().sendMessage(createWeeklyRankAssignList(guildID, experienceService.getWeeklyRoles(guildID))).queue();
                commandService.ongoingCommands.remove(this);
                return;            }
            else if(messageSplit[0].equals("add") || messageSplit[0].equals("remove")){
                if(writePermissions)
                    message.getChannel().sendMessage("Syntax Error: No parameters listed").queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
        }
        /* Logically people will think we're parsing left to right, so an error on the role before the add/remove would probably throw people off. This is just convenience */
        if(!(messageSplit[0].equals("add") || messageSplit[0].equals("remove"))){
            message.getChannel().sendMessage("Syntax Error: no valid sub-commands listed. Please use add/remove/list").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        List<Role> roleList=null;
        /* If we make it this far, we may as well try to get the role now */
        if(message.getMentionedRoles().isEmpty()) {
            roleList = message.getGuild().getRolesByName(messageSplit[1], true);
            if (roleList == null || roleList.size()==0) {
                if (writePermissions)
                    message.getChannel().sendMessage("Error: Role not found").queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
        }
        else{
            roleList = message.getMentionedRoles();
            if(roleList==null || roleList.size()==0){
                if (writePermissions)
                    message.getChannel().sendMessage("Error: Role not found").queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
        }
        if(messageSplit[0].equals("add")){
            /* TODO Add the first thing in the role list */
            /* Now we should make sure the rank requirement is actually a thing */
            if(messageSplit.length<3){
                if(writePermissions)
                    message.getChannel().sendMessage("Syntax Error: not enough arguments.").queue();
            }
            int rankReq = 0;
            try{
                rankReq = Integer.parseInt(messageSplit[2]);
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
            experienceService.setWeeklyRole(guildID, rankReq, roleList.get(0).getIdLong());
            message.getChannel().sendMessage("Successfully added roles to weekly assign list").queue();
        }
        else if(messageSplit[0].equals("remove")){
            /* TODO remove the first thing from the role list */
            List<Long> rolesToRemove = new ArrayList<>();
            for(Role r : roleList)
                rolesToRemove.add(r.getIdLong());
            experienceService.removeWeeklyRole(guildID, rolesToRemove);
            message.getChannel().sendMessage("Successfully removed roles from weekly assign list").queue();
        }
        commandService.ongoingCommands.remove(this);
    }
}
