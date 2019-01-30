package com.rathma.discordbot.experience.services;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.Service;
import com.rathma.discordbot.experience.commands.Rank;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.bson.*;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ExperienceService extends Service{
    public static String userTable = "EXP_USERS";
    public static String settingsTable = "EXP_SETTINGS";
    public static int comboBonusPercent = 4;
    public JDA jda;
    public Database database;

    public Map<Long, List<Long>> blockedChannels;
    public Map<Long, List<Long>> blockedRoles;
    public Map<Long, Map<Long, Integer>> levelRoles;
    public Map<Long, Map<Long, Integer>> rankRoles;
    public Map<Long, Double> guildCooldownTimers;
    public Map<Long, List<Long>> blockedVoiceChannels;
    public Map<Long, Double> guildExpRatios;
    public Map<Long, Map<Long, Integer>> weeklyRoles;

    public ExperienceService(JDA jda, Database database){
        this.jda=jda;
        this.database=database;
        serviceID="experience";
        blockedChannels = new HashMap<>();
        blockedRoles = new HashMap<>();
        blockedVoiceChannels = new HashMap<>();
        guildCooldownTimers = new HashMap<>();
        guildExpRatios = new HashMap<>();
        levelRoles = new HashMap<>();
        rankRoles = new HashMap<>();
        weeklyRoles = new HashMap<>();
        loadSettings();
    }

    public static String getSettingsTable() {
        return settingsTable;
    }

    public static String getUserTable() {
        return userTable;
    }


    /* Config methods */
    public void loadSettings(){
        List<Document> settingsList = database.queryTable(settingsTable);
        if(settingsList==null)
            return;
        for(Document i : settingsList){
            guildExpRatios.put(i.getLong("GUILD ID"),i.getDouble("RATIO"));
            guildCooldownTimers.put(i.getLong("GUILD ID"), i.getDouble("COOLDOWN"));
            rankRoles.put(i.getLong("GUILD ID"), new HashMap<>());
            levelRoles.put(i.getLong("GUILD ID"), new HashMap<>());
            if(i.containsKey("BLOCKED CHANNELS")){
                blockedChannels.put(i.getLong("GUILD ID"), new ArrayList<>());
                List queryChannels = i.get("BLOCKED CHANNELS", ArrayList.class);
                for(Object o : queryChannels){
                    if(o instanceof Long){
                        blockedChannels.get(i.getLong("GUILD ID")).add((long)o);
                    }
                }
            }
            if(i.containsKey("BLOCKED VOICE CHANNELS")){
                blockedVoiceChannels.put(i.getLong("GUILD ID"), new ArrayList<>());
                List queryVChannels = i.get("BLOCKED VOICE CHANNELS", ArrayList.class);
                for(Object o : queryVChannels){
                    if(o instanceof Long){
                        blockedVoiceChannels.get(i.getLong("GUILD ID")).add((long)o);
                    }
                }            }
            if(i.containsKey("BLOCKED ROLES")){
                blockedRoles.put(i.getLong("GUILD ID"), new ArrayList<>());
                List queryBRoles = i.get("BLOCKED ROLES", ArrayList.class);
                for(Object o : queryBRoles){
                    if(o instanceof Long){
                        blockedRoles.get(i.getLong("GUILD ID")).add((long)o);
                    }
                }

            }
            if(i.containsKey("RANK ROLES")){
                Document roles = i.get("RANK ROLES", Document.class);
                for(String key : roles.keySet()){
                    rankRoles.get(i.getLong("GUILD ID")).put(Long.decode(key), roles.getInteger(key));
                }
            }
            if(i.containsKey("LEVEL ROLES")){
                Document roles = i.get("LEVEL ROLES", Document.class);
                for(String key : roles.keySet()){
                    levelRoles.get(i.getLong("GUILD ID")).put(Long.decode(key), roles.getInteger(key));
                }
            }
            if(i.containsKey("WEEKLY ROLES")){
                weeklyRoles.put(i.getLong("GUILD ID"), new HashMap<>());
                Document roles = i.get("WEEKLY ROLES", Document.class);
                for(String key : roles.keySet()){
                    long guildID = i.getLong("GUILD ID");
                    long roleID = Long.decode(key);
                    int rankReq = roles.getInteger(key);
                    weeklyRoles.get(guildID).put(roleID, rankReq);
                }
            }
        }
    }
    public void generateGuildConfig(long guildID){
        /* Pretty much just generate a default config */
        Document document = new Document();
        document.append("GUILD ID", guildID);
        document.append("COOLDOWN", 60D);
        document.append("RATIO", 1D);
        document.append("LAST VC RESET", 0L);
        document.append("LAST WEEKLY RESET", 0L);
        database.insertOne(settingsTable, document);

        /* Adding it to our loaded settings */
        guildCooldownTimers.put(guildID, 60.0D);
        guildExpRatios.put(guildID, 1.0D);
    }

    /* User cooldown methods */
    public boolean checkUserCooldown(Message message){
        Document filter = new Document();
        filter.append("UUID", message.getMember().getUser().getIdLong());
        Document query = database.queryOne(userTable, filter);
        /* If the query is null then the user has never talked and can gain exp */
        if(query==null){
            database.insertOne(userTable,filter);
            return false;
        }
        /* If the guild doesn't exist for the user, they've never talked in this guild */
        if(!query.containsKey(message.getGuild().getId())){
            /* Generating a users' guild information since the user exists but not the guild. */
            Document guildInfo = new Document();
            guildInfo.append("EXPERIENCE", 0);
            guildInfo.append("WEEKLY EXPERIENCE", 0);
            guildInfo.append("COMBO", 0);
            Document userInfo = new Document().append(message.getGuild().getId(), guildInfo);
            Document update = new Document().append("$set", userInfo);
            database.updateOne(userTable, filter, update);
            return false;
        }
        /* User exists, they have guild information, now let's check guild info */
        if(!guildCooldownTimers.containsKey(message.getGuild().getIdLong())){
            /* No server config exists, first message in guild */
            generateGuildConfig(message.getGuild().getIdLong());
            return false;
        }
        /* User exists, guild exists/has timer, time to get the cooldown. */
        double coolownTimer = guildCooldownTimers.get(message.getGuild().getIdLong());

        Document guildSpecificInfo = query.get(message.getGuild().getId(), Document.class);
        /* If somehow this doesn't exist, they've managed to get guild settings without a message */
        if(!guildSpecificInfo.containsKey("LAST MESSAGE")){
            return false;
        }

        /* We finally fucking check the timer */
        long lastMessageTime = guildSpecificInfo.getLong("LAST MESSAGE");
        long messageTime = message.getCreationTime().toEpochSecond();
        long secondsBetweenMessages =  (messageTime-lastMessageTime);
        if(secondsBetweenMessages>=coolownTimer) {
            Document update = new Document();
            Document updateData = new Document();
            int currentCombo = 0;
            if(guildSpecificInfo.containsKey("COMBO")){
                currentCombo = guildSpecificInfo.getInteger("COMBO");
            }
            if(secondsBetweenMessages>=coolownTimer*2){
                /* Update their combo exp to break it */
                guildSpecificInfo.append("COMBO", 0);
            }
            else{
                /* Update combo exp to add */
                if(currentCombo<5)
                    guildSpecificInfo.append("COMBO", currentCombo+1);
            }
            updateData.append(message.getGuild().getId(), guildSpecificInfo);
            update.append("$set", updateData);
            database.updateOne(userTable, filter, update);
            return false;
        }
        else {
            return true;
        }
    }
    public void updateUserCooldown(Message message){
        Document filter = new Document();
        filter.append("UUID", message.getMember().getUser().getIdLong());

        //Document guildInfo = new Document();
        Document userInfo = database.queryOne(userTable, filter);
        Document guildInfo;

        if(userInfo!=null) {
            if(userInfo.containsKey(message.getGuild().getId())) {
                guildInfo = userInfo.get(message.getGuild().getId(), Document.class);
                guildInfo.append("LAST MESSAGE", message.getCreationTime().toEpochSecond());
            }
            else {
                guildInfo = new Document().append("LAST MESSAGE", message.getCreationTime().toEpochSecond());
            }
        }
        else{
            guildInfo = new Document();
            guildInfo.append("LAST MESSAGE", message.getCreationTime().toEpochSecond());
        }
        Document updateInfo = new Document().append(message.getGuild().getId(), guildInfo);
        Document update = new Document().append("$set", updateInfo);
        database.updateOne(userTable, filter, update);
    }

    /* Level Role stuff */
    public void setLevelRole(long guildID, int levelReq, long roleID){
        if(!levelRoles.containsKey(guildID)){
            levelRoles.put(guildID, new HashMap<>());
        }
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document updateData = new Document();
        Document levelData;
        Document update = new Document();
        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }
        if(query.containsKey("LEVEL ROLES")){
            levelData = query.get("LEVEL ROLES", Document.class);
            levelData.append(Long.toString(roleID), levelReq);
            updateData.append("LEVEL ROLES", levelData);
        }
        else{
            levelData = new Document().append(Long.toString(roleID), levelReq);
            updateData.append("LEVEL ROLES", levelData);
        }
        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        levelRoles.get(guildID).put(roleID, levelReq);
    }
    public void removeLevelRoles(long guildID, List<Long> roles){
        if(roles.size()<1)
            return;
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document updateData=null;
        Document update = new Document();

        boolean dbupdate = true;
        if(query==null || !query.containsKey("LEVEL ROLES"))
            dbupdate=false;
        if(dbupdate)
            updateData = query.get("LEVEL ROLES", Document.class);

        for(Long r : roles){
            if(levelRoles.containsKey(guildID))
                levelRoles.get(guildID).remove(r);
            if(dbupdate){
                updateData.remove(Long.toString(r));
            }
        }
        if(dbupdate) {
            update.append("$set", new Document().append("LEVEL ROLES",updateData));
            database.updateOne(settingsTable, filter, update);
        }
    }
    public void checkMemberLevelRoles(Member member, int level){
        long guildID = member.getGuild().getIdLong();

        if(!levelRoles.containsKey(guildID))
            return;
        Map<Long, Integer> roleList = levelRoles.get(guildID);
        if(roleList==null)
            return;
        /* Let's get the member's current role list */
        List<Role> currentRoles = member.getRoles();

        /* Iterates over each entry of the map */
        for(Map.Entry<Long, Integer> entry : roleList.entrySet()){
            if(level>=entry.getValue()) {
                boolean found=false;
                for(Role r : currentRoles){
                    if(r.getIdLong()==entry.getKey()){
                        found=true;
                    }
                }
                if(found==false){
                    GuildController guildController = member.getGuild().getController();
                    Role role = member.getGuild().getRoleById(entry.getKey());
                    if(role!=null){
                        System.out.println("Assigning " + role.getName() + " to level " + level + " " + member.getEffectiveName());
                        guildController.addSingleRoleToMember(member, role).queue();
                    }
                }
            }
            else{
                /* Remove role if they have it */
                boolean found = false;
                for(Role r : currentRoles){
                    if(r.getIdLong()==entry.getKey()){
                        found=true;
                    }
                }
                if(found==true){
                    GuildController guildController = member.getGuild().getController();
                    Role role = member.getGuild().getRoleById(entry.getKey());
                    if(role!=null){
                        guildController.removeRolesFromMember(member, role).queue();
                    }
                }
            }
        }
    }

    /* Rank Role stuff */
    public void removeRankRoles(long guildID, List<Long> roles){
        if(roles.size()<1)
            return;
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document updateData=null;
        Document update = new Document();

        boolean dbupdate = true;
        if(query==null || !query.containsKey("RANK ROLES"))
            dbupdate=false;
        if(dbupdate)
            updateData = query.get("RANK ROLES", Document.class);

        for(Long r : roles){
            if(rankRoles.containsKey(guildID))
                rankRoles.get(guildID).remove(r);
            if(dbupdate){
                updateData.remove(Long.toString(r));
            }
        }
        if(dbupdate) {
            update.append("$set", new Document().append("RANK ROLES",updateData));
            database.updateOne(settingsTable, filter, update);
        }
    }
    public void setRankRole(long guildID, int rank, long roleID){
        if(!rankRoles.containsKey(guildID)){
            rankRoles.put(guildID, new HashMap<>());
        }
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document updateData = new Document();
        Document rankData;
        Document update = new Document();
        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }
        if(query.containsKey("RANK ROLES")){
            rankData = query.get("RANK ROLES", Document.class);
            rankData.append(Long.toString(roleID), rank);
            updateData.append("RANK ROLES", rankData);
        }
        else{
            rankData = new Document().append(Long.toString(roleID), rank);
            updateData.append("RANK ROLES", rankData);
        }
        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        rankRoles.get(guildID).put(roleID, rank);
    }
    public void setWeeklyRole(long guildID, int rank, long roleID){
        if(!weeklyRoles.containsKey(guildID)){
            weeklyRoles.put(guildID, new HashMap<>());
        }
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document updateData = new Document();
        Document rankData;
        Document update = new Document();
        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }
        if(query.containsKey("WEEKLY ROLES")){
            rankData = query.get("WEEKLY ROLES", Document.class);
            rankData.append(Long.toString(roleID), rank);
            updateData.append("WEEKLY ROLES", rankData);
        }
        else{
            rankData = new Document().append(Long.toString(roleID), rank);
            updateData.append("WEEKLY ROLES", rankData);
        }
        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        weeklyRoles.get(guildID).put(roleID, rank);
    }
    public void removeWeeklyRole(long guildID, List<Long> roles){
        if(roles.size()<1)
            return;
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document updateData=null;
        Document update = new Document();

        boolean dbupdate = true;
        if(query==null || !query.containsKey("WEEKLY ROLES"))
            dbupdate=false;
        if(dbupdate)
            updateData = query.get("WEEKLY ROLES", Document.class);

        for(Long r : roles){
            if(weeklyRoles.containsKey(guildID))
                weeklyRoles.get(guildID).remove(r);
            if(dbupdate){
                updateData.remove(Long.toString(r));
            }
        }
        if(dbupdate) {
            update.append("$set", new Document().append("WEEKLY ROLES",updateData));
            database.updateOne(settingsTable, filter, update);
        }
    }
    public Map<Long, Integer> getWeeklyRoles(long guild){
        if(!weeklyRoles.containsKey(guild))
            weeklyRoles.put(guild, new HashMap<>());
        return weeklyRoles.get(guild);
    }
    /* Experience block methods  */
    public void removeChannelBlock(TextChannel channel){
        long guildID = channel.getGuild().getIdLong();
        boolean existsKey = true;
        boolean existsDB = true;
        if(!blockedChannels.containsKey(guildID)){
            blockedChannels.put(guildID, new ArrayList<>());
            existsKey = false;
        }

        /* Get the database query */
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        if(query==null) {
            generateGuildConfig(guildID);
            existsDB = false;
        }

        /* Remove from keyset */
        if(existsKey){

            blockedChannels.get(guildID).remove(channel.getIdLong());
        }
        if(existsDB){
            Document update = new Document();
            Document updateData = new Document();
            if(query.containsKey("BLOCKED CHANNELS")){
                List  queryChannels = query.get("BLOCKED CHANNELS", ArrayList.class);
                BsonArray bChannels = new BsonArray();
                for(Object o : queryChannels){
                    if(o instanceof Long){
                        if((long)o!=channel.getIdLong())
                            bChannels.add(new BsonInt64((long)o));
                    }
                }
                updateData.append("BLOCKED CHANNELS", bChannels);
                update.append("$set", updateData);
                database.updateOne(settingsTable, filter, update);
            }
        }
    }
    public void setChannelBlock(TextChannel channel){
        long guildID = channel.getGuild().getIdLong();
        if(!blockedChannels.containsKey(guildID)){
            blockedChannels.put(guildID, new ArrayList<>());
        }

        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document update = new Document();
        Document updateData = new Document();

        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }


        if(query.containsKey("BLOCKED CHANNELS")){
            List queryChannels = query.get("BLOCKED CHANNELS", ArrayList.class);
            BsonArray bChannels = new BsonArray();
            for(Object o : queryChannels){
                if(o instanceof Long){
                    if((long)o!=channel.getIdLong())
                        bChannels.add(new BsonInt64((long)o));
                }
            }
            bChannels.add(new BsonInt64(channel.getIdLong()));
            updateData.append("BLOCKED CHANNELS", bChannels);
        }
        else{
            BsonArray bChannels;
            bChannels = new BsonArray();
            bChannels.add(new BsonInt64(channel.getIdLong()));

            updateData.append("BLOCKED CHANNELS", bChannels);
        }

        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        blockedChannels.get(guildID).add(channel.getIdLong());
    }
    public boolean checkChannelBlocked(TextChannel channel){
        long guildID = channel.getGuild().getIdLong();
        if(!blockedChannels.containsKey(guildID)){
            blockedChannels.put(guildID, new ArrayList<>());
            return false;
        }
        if(blockedChannels.get(guildID).contains(channel.getIdLong()))
            return true;
        return false;
    }
    public boolean checkRoleBlocked(Member member){
        long guildID = member.getGuild().getIdLong();
        if(!blockedRoles.containsKey(guildID)){
            return false;
        }
        for(Role role : member.getRoles()){
            for(Long roleID : blockedRoles.get(guildID))
                if(roleID == role.getIdLong())
                    return true;
        }
        return false;
    }
    public void removeVoiceChannelBlock(VoiceChannel channel){
        long guildID = channel.getGuild().getIdLong();
        boolean existsKey = true;
        boolean existsDB = true;
        if(!blockedVoiceChannels.containsKey(guildID)){
            blockedVoiceChannels.put(guildID, new ArrayList<>());
            existsKey = false;
        }

        /* Get the database query */
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        if(query==null) {
            generateGuildConfig(guildID);
            existsDB = false;
        }

        /* Remove from keyset */
        if(existsKey){
            blockedVoiceChannels.get(guildID).remove(channel.getIdLong());
        }
        if(existsDB){
            Document update = new Document();
            Document updateData = new Document();
            if(query.containsKey("BLOCKED VOICE CHANNELS")){
                List  queryChannels = query.get("BLOCKED VOICE CHANNELS", ArrayList.class);
                BsonArray bChannels = new BsonArray();
                for(Object o : queryChannels){
                    if(o instanceof Long){
                        if((long)o!=channel.getIdLong())
                            bChannels.add(new BsonInt64((long)o));
                    }
                }
                updateData.append("BLOCKED VOICE CHANNELS", bChannels);
                update.append("$set", updateData);
                database.updateOne(settingsTable, filter, update);
            }
        }
    }
    public void setVoiceChannelBlock(VoiceChannel channel){
        long guildID = channel.getGuild().getIdLong();
        if(!blockedVoiceChannels.containsKey(guildID)){
            blockedVoiceChannels.put(guildID, new ArrayList<>());
        }

        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }

        Document update = new Document();
        Document updateData = new Document();
        if(query.containsKey("BLOCKED VOICE CHANNELS")){
            List queryChannels = query.get("BLOCKED VOICE CHANNELS", ArrayList.class);
            BsonArray bChannels = new BsonArray();
            for(Object o : queryChannels){
                if(o instanceof Long){
                    if((long)o!=channel.getIdLong())
                        bChannels.add(new BsonInt64((long)o));
                }
            }
            bChannels.add(new BsonInt64(channel.getIdLong()));
            updateData.append("BLOCKED VOICE CHANNELS", bChannels);
        }
        else{
            BsonArray bChannels;
            bChannels = new BsonArray();
            bChannels.add(new BsonInt64(channel.getIdLong()));

            updateData.append("BLOCKED VOICE CHANNELS", bChannels);
        }

        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        blockedVoiceChannels.get(guildID).add(channel.getIdLong());
    }
    public void removeIgnoreRoles(Role role){
        long guildID = role.getGuild().getIdLong();
        boolean existsKey = true;
        boolean existsDB = true;
        if(!blockedRoles.containsKey(guildID)){
            blockedRoles.put(guildID, new ArrayList<>());
            existsKey = false;
        }

        /* Get the database query */
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        if(query==null) {
            generateGuildConfig(guildID);
            existsDB = false;
        }

        /* Remove from keyset */
        if(existsKey){
            blockedRoles.get(guildID).remove(role.getIdLong());
        }
        if(existsDB){
            Document update = new Document();
            Document updateData = new Document();
            if(query.containsKey("BLOCKED ROLES")){
                List  queryRoles = query.get("BLOCKED ROLES", ArrayList.class);
                BsonArray bRoles = new BsonArray();
                for(Object o : queryRoles){
                    if(o instanceof Long){
                        if((long)o!=role.getIdLong())
                            bRoles.add(new BsonInt64((long)o));
                    }
                }
                updateData.append("BLOCKED ROLES", bRoles);
                update.append("$set", updateData);
                database.updateOne(settingsTable, filter, update);
            }
        }
    }
    public void removeIgnoreRoles(List<Role> roles){
        long guildID = roles.get(0).getGuild().getIdLong();
        boolean existsKey = true;
        boolean existsDB = true;
        if(!blockedRoles.containsKey(guildID)){
            blockedRoles.put(guildID, new ArrayList<>());
            existsKey = false;
        }

        /* Get the database query */
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document update = new Document();
        Document updateData = new Document();
        BsonArray bRoles = new BsonArray();
        List queryRoles=null;

        if(query.containsKey("BLOCKED ROLES"))
            queryRoles = query.get("BLOCKED ROLES", ArrayList.class);

        if(query==null) {
            generateGuildConfig(guildID);
            existsDB = false;
        }
        for(Role role : roles) {
            /* Remove from keyset */
            if (existsKey) {
                blockedRoles.get(guildID).remove(role.getIdLong());
            }
            if (existsDB) {
                if (query.containsKey("BLOCKED ROLES")) {
                    for (Object o : queryRoles) {
                        if (o instanceof Long) {
                            if ((long) o != role.getIdLong())
                                bRoles.add(new BsonInt64((long) o));
                        }
                    }
                }
            }
        }
        updateData.append("BLOCKED ROLES", bRoles);
        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
    }
    public void setIgnoreRole(Role role){
        long guildID = role.getGuild().getIdLong();
        if(!blockedRoles.containsKey(guildID)){
            blockedRoles.put(guildID, new ArrayList<>());
        }

        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document update = new Document();
        Document updateData = new Document();

        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }


        if(query.containsKey("BLOCKED ROLES")){
            List queryRoles = query.get("BLOCKED ROLES", ArrayList.class);
            BsonArray bRoles = new BsonArray();
            for(Object o : queryRoles){
                if(o instanceof Long){
                    if((long)o!=role.getIdLong())
                        bRoles.add(new BsonInt64((long)o));
                }
            }
            bRoles.add(new BsonInt64(role.getIdLong()));
            updateData.append("BLOCKED ROLES", bRoles);
        }
        else{
            BsonArray bRoles;
            bRoles = new BsonArray();
            bRoles.add(new BsonInt64(role.getIdLong()));
            updateData.append("BLOCKED ROLES", bRoles);
        }

        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        blockedRoles.get(guildID).add(role.getIdLong());
    }
    public void setIgnoreRole(List<Role> roles){
        long guildID = roles.get(0).getGuild().getIdLong();
        if(!blockedRoles.containsKey(guildID)){
            blockedRoles.put(guildID, new ArrayList<>());
        }

        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(settingsTable, filter);
        Document update = new Document();
        Document updateData = new Document();

        if(query==null) {
            generateGuildConfig(guildID);
            query = database.queryOne(settingsTable, filter);
        }
        List queryRoles = null;
        BsonArray bRoles = new BsonArray();
        if(query.containsKey("BLOCKED ROLES")) {
            queryRoles = query.get("BLOCKED ROLES", ArrayList.class);
        }
        for(Role role : roles) {
            if (query.containsKey("BLOCKED ROLES")) {
                for (Object o : queryRoles) {
                    if (o instanceof Long) {
                        if ((long) o != role.getIdLong())
                            bRoles.add(new BsonInt64((long) o));
                    }
                }
                bRoles.add(new BsonInt64(role.getIdLong()));
            } else {
                bRoles.add(new BsonInt64(role.getIdLong()));
            }
            blockedRoles.get(guildID).add(role.getIdLong());
        }

        if(query.containsKey("BLOCKED ROLES"))
            updateData.append("BLOCKED ROLES", bRoles);
        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
    }
    /* Sorted Leadeboards */
    public List<Document> getWeeklySortedGuildMembers(long guildID){
        List<Document> table = database.queryTable(ExperienceService.userTable);
        List<Document> usersInGuild= new ArrayList<>();

        /* Sort the people in the guild into the table */
        for(Document u : table){
            if(u.containsKey(Long.toString(guildID))){
                usersInGuild.add(u);
            }
        }
        /* So java makes sorting really easy, we just created an inline comparator class
         * Basically the comparator just decides how the sorting algorithm chooses whether something is greater or less than one another.
         * if u1 > u2 then you'd return 1, but that puts it higher in the array. For our purposes it makes more sense to swap it so they'll be closer
         * to the bottom of the array and we can just +1 the index and get their rank.
         * */
        usersInGuild.sort(new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                Document u1 = o1.get(Long.toString(guildID), Document.class);
                Document u2 = o2.get(Long.toString(guildID), Document.class);
                if(u1.containsKey("WEEKLY EXPERIENCE") && u2.containsKey("WEEKLY EXPERIENCE")) {
                    if (u1.getInteger("WEEKLY EXPERIENCE") == u2.getInteger("WEEKLY EXPERIENCE")) {
                        /* Equal exp */
                        return 0;
                    }
                    else if (u1.getInteger("WEEKLY EXPERIENCE") > u2.getInteger("WEEKLY EXPERIENCE")) {
                        return -1;
                    }
                    else
                        return 1;
                }
                else if(u1.containsKey("WEEKLY EXPERIENCE")){
                    /* We know u2 doesn't contain the key, so let's just return here */
                    return 1;
                }
                else if(u2.containsKey("WEEKLY EXPERIENCE")){
                    /* u2 exists and u1 doesn't u2 wins */
                    return -1;
                }
                /* Neither exist, so basically equal */
                return 0;
            }
        });
        return usersInGuild;
    }
    public List<Document> getSortedGuildMembers(long guildID){
        List<Document> table = database.queryTable(ExperienceService.userTable);
        List<Document> usersInGuild= new ArrayList<>();

        /* Sort the people in the guild into the table */
        for(Document u : table){
            if(u.containsKey(Long.toString(guildID))){
                usersInGuild.add(u);
            }
        }
        /* So java makes sorting really easy, we just created an inline comparator class
         * Basically the comparator just decides how the sorting algorithm chooses whether something is greater or less than one another.
         * if u1 > u2 then you'd return 1, but that puts it higher in the array. For our purposes it makes more sense to swap it so they'll be closer
         * to the bottom of the array and we can just +1 the index and get their rank.
         * */
        usersInGuild.sort(new Comparator<Document>() {
            @Override
            public int compare(Document o1, Document o2) {
                Document u1 = o1.get(Long.toString(guildID), Document.class);
                Document u2 = o2.get(Long.toString(guildID), Document.class);
                if(u1.containsKey("EXPERIENCE") && u2.containsKey("EXPERIENCE")) {
                    if (u1.getInteger("EXPERIENCE") == u2.getInteger("EXPERIENCE")) {
                        /* Equal exp */
                        return 0;
                    }
                    else if (u1.getInteger("EXPERIENCE") > u2.getInteger("EXPERIENCE")) {
                        return -1;
                    }
                    else
                        return 1;
                }
                else if(u1.containsKey("EXPERIENCE")){
                    /* We know u2 doesn't contain the key, so let's just return here */
                    return 1;
                }
                else if(u2.containsKey("EXPERIENCE")){
                    /* u2 exists and u1 doesn't u2 wins */
                    return -1;
                }
                /* Neither exist, so basically equal */
                return 0;
            }
        });
        return usersInGuild;
    }


    public void setGuildExpRatio(long guildID, float ratio){
        Document update = new Document();
        Document updateData = new Document();
        Document filter = new Document().append("GUILD ID", guildID);
        updateData.append("RATIO",(double)ratio);
        update.append("$set", updateData);
        database.updateOne(settingsTable, filter, update);
        guildExpRatios.put(guildID, (double)ratio);
    }
    public float getGuildExpRatio(long guildID){
        if(!guildExpRatios.containsKey(guildID))
            generateGuildConfig(guildID);
        return guildExpRatios.get(guildID).floatValue();

    }
    public void setGuildCooldown(long guildID, float cooldown){

    }
    public float getGuildCooldown(long guildID){
        return guildCooldownTimers.get(guildID).floatValue();
    }
    public int generateRandomExp(long guildID, int comboBonus){
        int exp = Math.round(ThreadLocalRandom.current().nextInt(15,26) * getGuildExpRatio(guildID) * (float)(1+(comboBonus*comboBonusPercent/100.0)));
        return exp;
    }


    public void addExperience(Member member, Message message){
        Document filter = new Document().append("UUID", member.getUser().getIdLong());
        Document query = database.queryOne(userTable, filter);

        Document update = new Document();
        Document guildInfo;
        Document userInfo;
        if(query==null ||!query.containsKey(member.getGuild().getId())){
            /* User doesn't exist or has never spoken in the guild before. Either way we do the same thing */
            int exp = generateRandomExp(member.getGuild().getIdLong(), 0);
            guildInfo = new Document().append("EXPERIENCE", exp);
            guildInfo.append("WEEKLY EXPERIENCE", exp);
            userInfo = new Document().append(member.getGuild().getId(), guildInfo);
        }
        else if(!query.get(member.getGuild().getId(), Document.class).containsKey("EXPERIENCE")){
            /* The experience field doesn't exist */
            int exp = generateRandomExp(member.getGuild().getIdLong(), 0);
            guildInfo = new Document().append("EXPERIENCE", exp);
            guildInfo.append("WEEKLY EXPERIENCE", exp);
            userInfo = new Document().append(member.getGuild().getId(), guildInfo);
        }
        else{
            guildInfo = query.get(message.getGuild().getId(), Document.class);
            int combo=0;
            if(guildInfo.containsKey("COMBO"))
                combo = guildInfo.getInteger("COMBO");
            int generatedExp = generateRandomExp(message.getGuild().getIdLong(), combo);
            int newTotalExp = guildInfo.getInteger("EXPERIENCE") + generatedExp;
            checkMemberLevelRoles(member, Rank.getLevel(newTotalExp));
            guildInfo.append("EXPERIENCE", newTotalExp);
            if(guildInfo.containsKey("WEEKLY EXPERIENCE")) {
                int newWeeklyExp = guildInfo.getInteger("WEEKLY EXPERIENCE") + generatedExp;
                guildInfo.append("WEEKLY EXPERIENCE", newWeeklyExp);
            }
            else{
                int newWeeklyExp = generatedExp;
                guildInfo.append("WEEKLY EXPERIENCE", newWeeklyExp);
            }
            userInfo = new Document().append(member.getGuild().getId(),guildInfo);
        }
        userInfo.append("LAST KNOWN USERNAME", message.getMember().getUser().getName());
        update.append("$set", userInfo);
        database.updateOne(userTable, filter, update);
        updateUserCooldown(message);
        checkMemberRankRoles(member);

    }
    public void checkMemberRankRoles(Member member){
        long guildID = member.getGuild().getIdLong();
        long uuid = member.getUser().getIdLong();
        List<Document> sortedMembers = getSortedGuildMembers(guildID);
        if(sortedMembers==null){
            return;
        }
        /* Find position of user */
        int rank=0;
        for(int i=0; i<sortedMembers.size(); i++){
            if(sortedMembers.get(i).containsValue(uuid)){
                rank=i+1;
            }
        }
        if(!rankRoles.containsKey(guildID))
            return;
        Map<Long, Integer> roleList = rankRoles.get(guildID);
        if(roleList==null)
            return;
        /* Let's get the member's current role list */
        List<Role> currentRoles = member.getRoles();

        /* Iterates over each entry of the map */
        for(Map.Entry<Long, Integer> entry : roleList.entrySet()){
            if(rank<=entry.getValue()) {
                boolean found=false;
                for(Role r : currentRoles){
                    if(r.getIdLong()==entry.getKey()){
                        found=true;
                    }
                }
                if(found==false){
                    GuildController guildController = member.getGuild().getController();
                    Role role = member.getGuild().getRoleById(entry.getKey());
                    if(role!=null){
                        guildController.addSingleRoleToMember(member, role).queue();
                    }
                }
            }
        }
    }
    @Override
    public void onEvent(Event event) {
        if(event instanceof GuildMessageReceivedEvent){
            Message message = ((GuildMessageReceivedEvent)event).getMessage();
            if(checkUserCooldown(message)) {
                return;
            }
            if(checkChannelBlocked(message.getTextChannel())) {
                return;
            }
            if(checkRoleBlocked(message.getMember()))
                return;
            addExperience(message.getMember(), message);
        }
    }
}
