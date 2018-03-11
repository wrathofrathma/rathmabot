package com.rathma.discordbot.experience.timedEvents;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.TimedEvent;
import com.rathma.discordbot.experience.commands.Rank;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.GuildController;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class GuildLevelRolesCheck extends TimedEvent {
    JDA jda;
    Database database;


    public GuildLevelRolesCheck(JDA jda, Database database) {
        this.jda = jda;
        this.database = database;
        setExecuteInterval(120000);
        setLastRun(0);
    }


    public void run(){
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService=(ExperienceService)o;
            }
        }
        for(long guildID : experienceService.levelRoles.keySet()){

            for(long roleID : experienceService.levelRoles.get(guildID).keySet()){

                Guild guild = jda.getGuildById(guildID);
                Role role = guild.getRoleById(roleID);
                if(role!=null) {
                    List<Member> membersInRole = guild.getMembersWithRoles(role);
                    for(Member member : membersInRole){
                        List<Role> removeRoles = new ArrayList<>();
                        for(Role r : member.getRoles()){
                            if(r.getIdLong()==roleID){
                                Document filter = new Document().append("UUID", member.getUser().getIdLong());
                                Document query = database.queryOne(ExperienceService.userTable, filter);
                                if(query!=null && query.containsKey(Long.toString(guildID))){
                                    Document guildData = query.get(Long.toString(guildID), Document.class);
                                    if(guildData.containsKey("EXPERIENCE")) {
                                        //check
                                        int experience = guildData.getInteger("EXPERIENCE");
                                        if(Rank.getLevel(experience)<experienceService.levelRoles.get(guildID).get(roleID)){
                                            removeRoles.add(r);
                                        }
                                    }
                                    else { /* No exp in db */
                                        removeRoles.add(r);
                                    }
                                }
                                else { /* No guild data in db */
                                    removeRoles.add(r);
                                }
                            }
                        }
                        if(!removeRoles.isEmpty()){
                            GuildController controller = guild.getController();
                            System.out.println("Removing roles from member: " + member.getNickname());
                            controller.removeRolesFromMember(member, removeRoles).queue();
                            /*
                            for(Role r1 : removeRoles) {
                                System.out.println("Removing role: " + r1.getName() + " from " + member.getNickname());
                                controller.removeSingleRoleFromMember(member, r1);
                            }*/
                        }
                    }
                }
            }
        }
    }
}
