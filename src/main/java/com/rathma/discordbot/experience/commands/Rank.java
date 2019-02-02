package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Rank extends Command {
    public Rank(CommandService commandService, Message message){
        requiredPermissions= Permission.MESSAGE_WRITE;
        requiredUserPermissions=null;
        identifier="rank";
        childIdentifier=null;
        this.jda=commandService.jda;
        this.database=commandService.database;
        this.commandService=commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public Rank(){
        requiredUserPermissions=null;
        requiredPermissions=Permission.MESSAGE_WRITE;
        identifier="rank";
        aliases = new ArrayList<>();
        childIdentifier=null;
    }

    public long getExperience(long uuid, long guildID){
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
    public long getRank(long uuid, long guildID){
        List<Document> sortedMembers=null;
        List<Object> listeners = jda.getRegisteredListeners();
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                sortedMembers=((ExperienceService) o).getSortedGuildMembers(guildID);
            }
        }
        if(sortedMembers==null){
            return -1;
        }
        /* Find position of user */
        int rank=0;
        for(int i=0; i<sortedMembers.size(); i++){
            if(sortedMembers.get(i).containsValue(uuid)){
                rank=i+1;
            }
        }
        return rank;
    }


    private static int getLevelExp(int n){
        return (int)(5 * Math.pow(n,2) + 50*n + 100);
    }

    public static int getLevel(long exp){
        long remainingExp = exp;
        int level=0;
        while(remainingExp>=getLevelExp(level)){
            remainingExp -= getLevelExp(level);
            level+=1;
        }
        return level;
    }

    long getTotalExpLevel(int n){
        double total = 0;
        for(int i = 0; i<n; i++){
            total = total + (5 * Math.pow(i,2) + 50*i + 100);
        }
        return (int)total;
    }

    public long getUserLevel(long uuid, long guildID){
        Document filter = new Document().append("UUID", uuid);
        Document query = database.queryOne(ExperienceService.userTable, filter);
        if(query==null)
            return 0;
        if(!query.containsKey(Long.toString(guildID))){
            return 0;
        }
        if(!query.get(Long.toString(guildID), Document.class).containsKey("EXPERIENCE"))
            return 0;
        int exp = query.get(Long.toString(guildID), Document.class).getInteger("EXPERIENCE");
        return getLevel(exp);
    }
    public double getRoleMultiplier(Member member){
        double multiplier = 1.0;
        long guildID = member.getGuild().getIdLong();
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService) {
                experienceService = (ExperienceService) o;
            }
        }
        List<Role> userRoles = member.getRoles();
        for(Role r : userRoles){
            if(experienceService.expRatioRoles.get(guildID).containsKey(r.getIdLong())){
                multiplier *= experienceService.expRatioRoles.get(guildID).get(r.getIdLong());
            }
        }
        return multiplier;
    }
    public int getComboExp(long uuid, long guildID){
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService) {
                experienceService = (ExperienceService) o;
            }
        }
        String userTable = experienceService.getUserTable();

        Document filter = new Document().append("UUID", uuid);
        Document query = database.queryOne( userTable, filter);
        if(query==null)
            return 0;
        if(!query.containsKey(Long.toString(guildID)))
            return 0;
        if(!query.get(Long.toString(guildID), Document.class).containsKey("COMBO"))
            return 0;
        return query.get(Long.toString(guildID), Document.class).getInteger("COMBO")*experienceService.comboBonusPercent;
    }

    public MessageEmbed createRankMessage(long uuid, long guildID, Message message){
        User user = jda.getUserById(uuid);

        long memberTotalExp = getExperience(uuid, guildID);
        long level = getUserLevel(uuid, guildID);

        long currentLevelExpReq = getTotalExpLevel((int) level);
        long nextLevelExpReq = getTotalExpLevel((int) (level+1));


        long expToNextLevel =  memberTotalExp - currentLevelExpReq;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setDescription("**"+ user.getName()+"**");

        embedBuilder.addField("Level", Long.toString(getUserLevel(uuid,guildID)), true);
        embedBuilder.addField("Rank", Long.toString(getRank(uuid,guildID)) + "/" + jda.getGuildById(guildID).getMembers().size(), true);
        embedBuilder.addField("Experience", Long.toString(expToNextLevel) + "/" + Long.toString(nextLevelExpReq - currentLevelExpReq) + " - total (" + memberTotalExp + ")" , true);
        embedBuilder.addField("Combo Bonus", Integer.toString(getComboExp(uuid, guildID)) + "%", true);
        embedBuilder.addField("Total Role Multiplier", getRoleMultiplier(message.getMember())+"", true);
        embedBuilder.setThumbnail(user.getAvatarUrl());
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
        if(writePermissions==false){
            commandService.ongoingCommands.remove(this);
            return;
        }
        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        long uuid=0;

        if(parsedCommand.isEmpty()){
            /* Check user's exp */
            uuid = message.getMember().getUser().getIdLong();
        }
        else {
            /* Search for user with name or ID and look for their exp */
            List<User> mentions = message.getMentionedUsers();
            /* TODO check for uuid */
            if (mentions.size() < 1) {
                message.getChannel().sendMessage("Syntax error: No user provided or user not in server.").queue();
                commandService.ongoingCommands.remove(this);
                return;
            } else {
                uuid = mentions.get(0).getIdLong();
            }
        }
        message.getChannel().sendMessage(createRankMessage(uuid, message.getGuild().getIdLong(), message)).queue();
        commandService.ongoingCommands.remove(this);
    }
}
