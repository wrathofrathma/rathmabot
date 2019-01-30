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

public class ExpRoleRatio extends Command {
    public ExpRoleRatio(CommandService commandService, Message message) {
        identifier = "exp";
        childIdentifier = "roleratio";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();
        run(message);
    }

    public ExpRoleRatio() {
        identifier = "exp";
        childIdentifier = "roleratio";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        aliases = new ArrayList<>();

    }

    public MessageEmbed buildRoleList(long guildID){
        List<Object> listeners =  jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService = (ExperienceService)o;
            }
        }

        Guild guild = jda.getGuildById(guildID);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLUE);
        embedBuilder.setDescription("**" + guild.getName() + "'s experience altering roles.**");
        String roles = "";
    /* TODO change this to role ratio map */
        Map<Long, List<Long>> rolesMap = experienceService.blockedRoles;
        if(!rolesMap.containsKey(guildID)) {
            embedBuilder.addField("Roles", roles, true);
            return embedBuilder.build();
        }
        for(Long roleID : rolesMap.get(guildID)){
            Role r = guild.getRoleById(roleID);
            if(r!=null){
                roles = roles.concat(r.getName() + "\n");
            }
        }
        embedBuilder.addField("Roles", roles, true);
        return embedBuilder.build();
    }

    public Role getMentionedRole(long guildID, String message){
        Guild guild = jda.getGuildById(guildID);
        for(Role role : guild.getRolesByName(message, true)){
            return role;
        }
        return null;
    }

    public void run(Message message) {
        if (!checkUserPermissions(message)) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        if (!checkBotPermissions(message)) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        boolean writePermissions = checkChannelWritePermission(message.getTextChannel());
        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        String[] messageSplit = parsedCommand.split(" ");

        if (messageSplit.length < 1) {
            if (writePermissions)
                message.getChannel().sendMessage("Syntax Error: Too few arguments.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        if (messageSplit[0].equals("list")) {
            /* List */
            if (writePermissions)
                message.getChannel().sendMessage(buildRoleList(guildID)).queue();
            commandService.ongoingCommands.remove(this);
        }
        else if (messageSplit.length > 1) {
            List<Object> listeners =  jda.getRegisteredListeners();
            ExperienceService experienceService=null;
            for(Object o : listeners){
                if(o instanceof ExperienceService){
                    experienceService = (ExperienceService)o;
                }
            }
            if(experienceService==null){
                commandService.ongoingCommands.remove(this);
                return;
            }
            if (messageSplit[0].equals("add")) {
                List<Role> roleList = message.getMentionedRoles();
                if (roleList == null || roleList.isEmpty()) {
                    Role role = getMentionedRole(guildID, parsedCommand.substring(messageSplit[0].length() + 1));
                    if (role != null) {
                        experienceService.setIgnoreRole(role);
                        message.getChannel().sendMessage("Added role to roleblock list").queue();
                        commandService.ongoingCommands.remove(this);
                        return;
                    }
                    else {
                        if (writePermissions)
                            message.getChannel().sendMessage("Error: No role found").queue();
                        commandService.ongoingCommands.remove(this);
                        return;
                    }
                }
                else {
                    experienceService.setIgnoreRole(roleList);
                    commandService.ongoingCommands.remove(this);
                    return;
                }
            }
            else if (messageSplit[0].equals("remove")) {
                List<Role> roleList = message.getMentionedRoles();
                if (roleList == null || roleList.isEmpty()) {
                    Role role = getMentionedRole(guildID, parsedCommand.substring(messageSplit[0].length() + 1));
                    if (role != null) {
                        experienceService.removeIgnoreRoles(role);
                        message.getChannel().sendMessage("Removed role from roleblock list").queue();
                        commandService.ongoingCommands.remove(this);
                        return;
                    }
                    else {
                        if (writePermissions)
                            message.getChannel().sendMessage("Error: No role found").queue();
                        commandService.ongoingCommands.remove(this);
                        return;
                    }
                }
                else {
                    experienceService.removeIgnoreRoles(roleList);
                    commandService.ongoingCommands.remove(this);
                    return;
                }
            }
            else {
                if (writePermissions)
                    message.getChannel().sendMessage("Syntax Error: Incorrect number of arguments").queue();
                commandService.ongoingCommands.remove(this);
            }
        }
        if(commandService.ongoingCommands.contains(this))
            commandService.ongoingCommands.remove(this);
    }

}
