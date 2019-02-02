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
/*
 * Functions to be covered
 * * isting roles and their ratios
 * * removing roles from the ratio list
 * * adding roles to the ratio list

 * Edits to make to the experienceservice


 */
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

    // TODO - This needs to be changed to generate a MessageEmbed containing the current roles and their ratios
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
        String ratios = "";
        Map<Long, Map<Long, Double>> expRatioMap = experienceService.expRatioRoles;
        for(Long key : expRatioMap.get(guildID).keySet()){
            Role r = guild.getRoleById(key);
            if(r!=null){
                roles = roles.concat(r.getName() + "\n");
                ratios = ratios.concat(expRatioMap.get(guildID).get(key) + "\n");
            }
        }

        embedBuilder.addField("Roles", roles, true);
        embedBuilder.addField("Role Ratio", ratios, true);
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
        // Command expected input : exp roleratio <command> number/role role
        // Post parse command possibilities
        // add double role
        // remove role
        // list
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
                double ratio = 0;
                if(messageSplit.length > 2){
                    // TODO - catch whatever numberical throw it does
                    try{
                        ratio = Double.parseDouble(messageSplit[1]);
                    }
                    catch (NumberFormatException e){
                        if(writePermissions){
                            message.getChannel().sendMessage("Hey you dumby, that's not a number.").queue();
                            commandService.ongoingCommands.remove(this);
                            return;
                        }
                    }
                }
                else{
                    if(writePermissions)
                        message.getChannel().sendMessage("Error: Not enough parameters and fuck descriptive error messages.").queue();
                    commandService.ongoingCommands.remove(this);
                    return;
                }

                List<Role> roleList = message.getMentionedRoles();
                if (roleList == null || roleList.isEmpty()) {
                    Role role = getMentionedRole(guildID, parsedCommand.substring(messageSplit[0].length() + 1));
                    if (role != null) {
                        experienceService.setExpRatioRole(role, ratio);
                        message.getChannel().sendMessage("Added role to exp ratio modifier list").queue();
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
                    experienceService.setExpRatioRole(roleList.get(0), ratio);
                    message.getChannel().sendMessage("Added role to exp ratio modifier list").queue();
                    commandService.ongoingCommands.remove(this);
                    return;
                }
            }
            else if (messageSplit[0].equals("remove")) {
                List<Role> roleList = message.getMentionedRoles();
                if (roleList == null || roleList.isEmpty()) {
                    Role role = getMentionedRole(guildID, parsedCommand.substring(messageSplit[0].length() + 1));
                    if (role != null) {
                        experienceService.removeExpRatioRole(role);
                        message.getChannel().sendMessage("Removed role from exp ratio modifier list").queue();
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
                    experienceService.removeExpRatioRole(roleList.get(0));
                    message.getChannel().sendMessage("Removed role from exp ratio modifier list").queue();
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
