package com.rathma.discordbot.marriage.timedEvents;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.TimedEvent;
import com.rathma.discordbot.marriage.services.MarriageService;
import com.rathma.discordbot.marriage.utils.Proposal;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.bson.Document;

import java.util.*;

/* Will check for manually deleted roles/marriages, as well as handle ongoing proposal cleanup */


public class MarriageCleanup extends TimedEvent {

    private JDA jda;
    private Database database;
    public MarriageCleanup(JDA jda, Database database) {
        this.database = database;
        this.jda = jda;
        setExecuteInterval(30000);
        setLastRun(0);
    }

    public void run(){
        List<Object> listeners = jda.getRegisteredListeners();
        MarriageService marriageService = null;
        for(Object o : listeners){
            if(o instanceof MarriageService){
                marriageService = (MarriageService)o;
                break;
            }
        }
        long currentTime = System.currentTimeMillis();
        if(marriageService!=null){
            /* Proposal cleanup */
            List<Proposal> proposals = new ArrayList<Proposal>();
            Set<Long> keySet = marriageService.getActiveProposals().keySet();
            for(Long guildKey : keySet){
                for(Proposal p : marriageService.getActiveProposals().get(guildKey)){
                    /* Check if proposal is active within the guild */
                    if(currentTime - p.timestamp*1000 >= 60000){
                        /* It's been longer than a minute since proposal, time to queue for cleanup */
                        proposals.add(p);
                    }
                }
            }
            synchronized (marriageService.getActiveProposals()){
                for(Proposal p : proposals){
                    marriageService.removeProposal(p);
                    TextChannel channel = jda.getTextChannelById(p.getChannel());
                    channel.sendMessage("<@" + p.proposer + ">, " + "<@" + p.proposee + "> didn't respond to your proposal. Get rekt.").queue();
                }
            }
        }
        /* Database cleanup */
        List<Document> query = database.queryTable(MarriageService.MARRIAGE_TABLE);
        if(query==null){
            return;
        }
        Map<Long, Guild> guildMap = new HashMap<>();
        for(Document m : query){
            long guildID = m.getLong("GUILD ID");
            if(!guildMap.containsKey(guildID)){
                guildMap.put(guildID, jda.getGuildById(guildID));
            }

            /* Check if role exists */
            Role role = guildMap.get(guildID).getRoleById(m.getLong("ROLE ID"));
            if(role==null){
                /* Role doesn't exist / was manually removed. Remove marriage. */
                Document filter = new Document().append("ROLE ID", m.getLong("ROLE ID"));
                database.deleteOne(MarriageService.MARRIAGE_TABLE, filter);
            }
        }
    }
}
