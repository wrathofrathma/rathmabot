package com.rathma.discordbot.experience.timedEvents;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.TimedEvent;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VoiceChatExperience extends TimedEvent {
    public Database database;
    public JDA jda;

    public VoiceChatExperience(JDA jda, Database database) {
        this.database = database;
        this.jda = jda;
        setExecuteInterval(60000);
        setLastRun(0);
    }

    public List<VoiceChannel> getUnblockedChannels(long guildID){
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService = null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService = (ExperienceService)o;
            }
        }
        Map<Long, List<Long>> blockedVCs = experienceService.blockedVoiceChannels;
        if(!blockedVCs.containsKey(guildID)){
            experienceService.blockedVoiceChannels.put(guildID, new ArrayList<>());
        }

        List<VoiceChannel> voiceChannelList = new ArrayList<>();
        for(VoiceChannel voiceChannel : jda.getGuildById(guildID).getVoiceChannels()){
            /* For each voice channel, check if they're blocked. If not, add to list */
             if(!blockedVCs.get(guildID).contains(voiceChannel.getIdLong())){
                 /* Not blocked */
                 voiceChannelList.add(voiceChannel);
             }
        }
        return voiceChannelList;
    }
    public int generateRandomExp(long guildID, double timeInVC){
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService = null;
        for(Object o : listeners){
            if(o instanceof ExperienceService)
                experienceService = (ExperienceService)o;
        }
        int baseExp = experienceService.generateRandomExp(guildID,0);

        /* Create VC penalty */
        double vcMod = timeInVC / 30;
        int newExp = 0;
        if(vcMod <= 2){
            newExp = baseExp;
        }
        else if(vcMod <= 10 ){
            double vcPenalty = Math.pow(0.8, vcMod-2);
            newExp = (int)Math.round(baseExp * vcPenalty);
        }
        else{
            newExp = (int)Math.round(baseExp * 0.15);
        }
        return newExp;
    }
    public void updateMemberExp(Member member){
        /* Well, we could do a bunch of separate methods, but I think it's probably better to minimalise our db calls */
        long uuid = member.getUser().getIdLong();
        long guildID = member.getGuild().getIdLong();
        Document filter = new Document().append("UUID", uuid);
        Document query = database.queryOne(ExperienceService.userTable, filter);

        Document guildData;
        Document userData;
        Document update = new Document();
        if(query==null ||!query.containsKey(Long.toString(guildID))){
            /* User doesn't exist or has never spoken in the guild before */
            guildData = new Document().append("EXPERIENCE", generateRandomExp(member.getGuild().getIdLong(),0));
            guildData.append("WEEKLY EXPERIENCE", 0);
            guildData.append("DAILY VC TIME", 1);
            userData = new Document().append(member.getGuild().getId(), guildData);
        }
        else{
            /* User exists, we don't know if they've spoken in VC before */
            guildData = query.get(Long.toString(guildID), Document.class);
            if(!guildData.containsKey("EXPERIENCE")){
                /* User exists, guild exists, but never gotten exp? */
                guildData.append("EXPERIENCE", 0);
                guildData.append("WEEKLY EXPERIENCE", 0);
            }
            if(!guildData.containsKey("DAILY VC TIME")){
                /* User exists, Experience exists, never talked in VC */
                guildData.append("DAILY VC TIME", 0);
            }
            /* User exists, vc time exists, exp exists */

            /* Let's generate our new exp and stuff */
            int currentExp = guildData.getInteger("EXPERIENCE");
            int timeInVC = guildData.getInteger("DAILY VC TIME");
            int generatedExp = generateRandomExp(guildID, timeInVC);
            int newExp = currentExp + generatedExp;
            int newWeeklyExp = guildData.getInteger("WEEKLY EXPERIENCE") + generatedExp;
            /* Push new values to database */
            guildData.append("EXPERIENCE", newExp);
            guildData.append("DAILY VC TIME", timeInVC + 1);
            guildData.append("WEEKLY EXPERIENCE", newWeeklyExp);
            userData = new Document().append(Long.toString(guildID),guildData);
        }

        update.append("$set", userData);
        database.updateOne(ExperienceService.userTable, filter, update);
    }

    public boolean checkRoleBlocked(Member member){
        List<Object> listeners =  jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService = (ExperienceService)o;
            }
        }
        long guildID = member.getGuild().getIdLong();
        if(!experienceService.blockedRoles.containsKey(guildID)){
            return false;
        }
        for(Role role : member.getRoles()){
            for(Long roleID : experienceService.blockedRoles.get(guildID))
                if(roleID == role.getIdLong())
                    return true;
        }
        return false;
    }

    public boolean isFarming(Member member){
        if(member.getVoiceState().isMuted() || member.getVoiceState().isGuildDeafened() || member.getVoiceState().isGuildMuted() || member.getVoiceState().isDeafened())
            return true;
        return false;
    }

    public void run(){
        List<Guild> guilds = jda.getGuilds();
        for(Guild g : guilds){
            long guildID = g.getIdLong();
            for(VoiceChannel vc : getUnblockedChannels(guildID)){
                for(Member member : vc.getMembers()){
                    if(!checkRoleBlocked(member))
                        if(!isFarming(member))
                            updateMemberExp(member);
                }
            }
        }
    }
}
