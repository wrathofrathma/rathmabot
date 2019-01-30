package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class AwardRole extends Command {
    public AwardRole(CommandService commandService, Message message){
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.MANAGE_ROLES;
        identifier = "award";
        childIdentifier = "role";
        aliases = new ArrayList<>();

        run(message);
    }

    public AwardRole() {
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.MANAGE_ROLES;
        identifier = "award";
        childIdentifier = "role";
        aliases = new ArrayList<>();
    }
    public Role getMentionedRole(long guildID, String roleName){
        Guild guild = jda.getGuildById(guildID);
        for(Role role : guild.getRolesByName(roleName, true)){
            return role;
        }
        return null;
    }

    public void setUserExp(long uuid, long guildID, int exp){
        Document update = new Document();
        Document filter = new Document().append("UUID", uuid);
        Document query = database.queryOne(ExperienceService.userTable, filter);
        if(query==null){
            return;
        }
        if(!query.containsKey(Long.toString(guildID))){
            return;
        }
        Document guildInfo = query.get(Long.toString(guildID), Document.class);
        guildInfo.append("EXPERIENCE", exp);
        update.append("$set", new Document().append(Long.toString(guildID), guildInfo));
        database.updateOne(ExperienceService.userTable, filter, update);
    }

    public int getMessageExp(String message) throws NumberFormatException {
        return Integer.parseInt(message);
    }

    public int getCurrentExp(long uuid, long guildID){
        Document filter = new Document().append("UUID", uuid);
        Document query = database.queryOne(ExperienceService.userTable,filter);
        /* If neither the query or the guild exists, then they have 0 exp */
        if(query==null || !query.containsKey(Long.toString(guildID))){
            return 0;
        }
        /* User & Guild Exists */
        else{
            Document guildInfo = query.get(Long.toString(guildID), Document.class);
            if(!guildInfo.containsKey("EXPERIENCE")){
                return 0;
            }
            else{
                int experience = guildInfo.getInteger("EXPERIENCE");
                return experience;
            }
        }
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
                message.getChannel().sendMessage("Syntax Error: No award exp specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        if(messageSplit.length<2){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No role specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        Role role = null;
        List<Role> roleList = message.getMentionedRoles();
        if (roleList == null || roleList.isEmpty()) {
            role = getMentionedRole(guildID, parsedCommand.substring(messageSplit[0].length() + 1));

            if (role == null) {
                if (writePermissions)
                    message.getChannel().sendMessage("Error: No role found").queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
        }
        else
            role = roleList.get(0);

        try {
            int exp = getMessageExp(messageSplit[0]);
            if(writePermissions){
                message.getChannel().sendMessage(awardRoleExp(guildID, role, exp) + " users have been awarded " + exp + " experience.").queue();
            }

        }
        catch (NumberFormatException e) {
            if (writePermissions) {
                message.getChannel().sendMessage("Syntax Error: No number specified").queue();
            }
            commandService.ongoingCommands.remove(this);
            return;
        }
        commandService.ongoingCommands.remove(this);
    }
    public int awardRoleExp(long guildID, Role role, int experience){
        System.out.println("Awarding role: " + role.getName() + " " + experience + " experience");
        Guild guild = jda.getGuildById(guildID);

        //For each user in the guild in the role, let's just update exp
        List<Member> membersInRole = guild.getMembersWithRoles(role);
        if(membersInRole!=null) {
            for (Member member : membersInRole) {
                int currentExp = getCurrentExp(member.getUser().getIdLong(), guildID);
                setUserExp(member.getUser().getIdLong(), guildID, experience + currentExp);
            }
            return membersInRole.size();
        }
        return 0;
    }
}
