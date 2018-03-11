package com.rathma.discordbot.experience.timedEvents;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.TimedEvent;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.rathma.discordbot.experience.utils;



public class GuildWeeklyRankUpdate extends TimedEvent {
    JDA jda;
    Database database;
    public GuildWeeklyRankUpdate(JDA jda, Database database){
        this.jda = jda;
        this.database = database;
        setExecuteInterval(60000);
        setLastRun(0);
    }

    public void run(){
        List<Document> query = database.queryTable(ExperienceService.settingsTable);
        if(query==null)
            return;
        /* Going to get a list of each guild ID and just cycle through them */
        List<Long> guildIDs = new ArrayList<>();
        for(Document d : query){
            if(d.containsKey("GUILD ID")){
                long guildID = d.getLong("GUILD ID");
                if(!guildIDs.contains(guildID))
                    guildIDs.add(guildID);
            }
        }
        for(Long guild : guildIDs){
            checkGuildRankRoles(guild);
        }
    }

    public void checkGuildRankRoles(long guildID){
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService)
                experienceService = (ExperienceService)o;
        }

        /* Get a list of the auto assign roles */
        Guild guild = jda.getGuildById(guildID);
        if(guild==null)
            return;
        if(!experienceService.weeklyRoles.containsKey(guildID))
            return;
        if(experienceService.weeklyRoles.get(guildID).isEmpty())
            return;

        Map<Long, Integer> roleMap =  experienceService.weeklyRoles.get(guildID);
        List<Role> roleList = new ArrayList<>();
        List<Long> removalList = new ArrayList<>();

        for(Map.Entry<Long, Integer> entry : roleMap.entrySet()){
            Role role = guild.getRoleById(entry.getKey());
            if(role!=null){
                roleList.add(role);
            }
            else{
                removalList.add(entry.getKey());
            }
        }
        /* Basically, if it isn't found in the guild then it's been removed. Let's just do some maintenance and remove it */
        if(removalList.size()>0)
            experienceService.removeWeeklyRole(guildID, removalList);

        /* Now that we have our role list and leaderboard
         * let's loop through the various roles, get all members in the roles, then loop through the leaderboard and see if they should keep the role
         */

        List<Document> leaderboard = utils.getSortedGuildMembers(database, guildID, "WEEKLY EXPERIENCE");
        for(Document lbMembers : leaderboard){
            /* Find their position on the LB */
            int rank = utils.getMemberRank(lbMembers.getLong("UUID"), leaderboard);

            for(Role r : roleList){
                int rankReq = roleMap.get(r.getIdLong());
                Member member = jda.getGuildById(guildID).getMemberById(lbMembers.getLong("UUID"));
                if(member!=null){
                    List<Role> memberRoles = member.getRoles();
                    if((rank==0 || rank>rankReq) && memberRoles.contains(r)){
                        /* Remove role since they no longer meet the requirements */
                        System.out.println("Removing role \"" + r.getName() + "\" from weekly rank " + rank + " - " + member.getEffectiveName());
                        guild.getController().removeRolesFromMember(member, r).queue();
                    }
                    else if(rank<=rankReq && !memberRoles.contains(r)){
                        System.out.println("Adding role \"" + r.getName() + "\" to weekly rank " + rank + " - " + member.getEffectiveName());
                        guild.getController().addSingleRoleToMember(member,r).queue();
                    }
                }
            }
        }
    }
}
