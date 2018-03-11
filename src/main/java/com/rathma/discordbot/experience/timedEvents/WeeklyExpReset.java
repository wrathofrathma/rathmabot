package com.rathma.discordbot.experience.timedEvents;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.TimedEvent;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import org.bson.BsonInt64;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeeklyExpReset extends TimedEvent {
    public Database database;
    public JDA jda;
    long resetInterval = 60000 * 60 * 24 * 7;
    public Map<Long, Long> guildLastReset;

    public WeeklyExpReset(JDA jda, Database database) {
        this.database = database;
        this.jda = jda;
        guildLastReset = new HashMap<>();

        setExecuteInterval(3600000);
        setLastRun(0);
    }
    public void loadLastReset() {
        List<Document> query = database.queryTable(ExperienceService.settingsTable);
        if(query==null)
            return;
        for(Document g : query){
            if(g.containsKey("LAST WEEKLY RESET")){
                long lastReset = g.getLong("LAST WEEKLY RESET");
                long guildID = g.getLong("GUILD ID");
                guildLastReset.put(guildID, lastReset);
            }
            else{
                setLastReset(g.getLong("GUILD ID"), System.currentTimeMillis() - resetInterval);
            }
        }
    }
    public void setLastReset(long guildID, long timestamp){
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(ExperienceService.settingsTable, filter);

        Document updateData = new Document().append("LAST WEEKLY RESET", new BsonInt64(timestamp));
        Document update = new Document().append("$set", updateData);
        database.updateOne(ExperienceService.settingsTable, filter, update);
        guildLastReset.put(guildID, timestamp);
    }

    public void resetGuildExp(long guildID){
        System.out.println("Resetting guild's weekly exp");
        List<Document> query = database.queryTable(ExperienceService.userTable);
        for(Document d : query){
            if(d.containsKey(Long.toString(guildID))){
                Document filter = new Document().append("UUID", d.getLong("UUID"));

                Document guildData = d.get(Long.toString(guildID), Document.class);
                guildData.append("WEEKLY EXPERIENCE",0);
                Document updateData = new Document().append(Long.toString(guildID), guildData);
                Document update = new Document().append("$set", updateData);
                database.updateOne(ExperienceService.userTable, filter, update);
            }
        }
        setLastReset(guildID, System.currentTimeMillis());
    }
    public void run(){
        loadLastReset();
        for(Long key : guildLastReset.keySet()){
            if((System.currentTimeMillis() - guildLastReset.get(key)) > resetInterval){
                /* Reset the guild */
                resetGuildExp(key);
            }
        }
    }
}
