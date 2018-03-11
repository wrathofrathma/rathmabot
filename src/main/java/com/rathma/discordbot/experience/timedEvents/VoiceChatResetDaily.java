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

public class VoiceChatResetDaily extends TimedEvent {
    public JDA jda;
    public Database database;
    public Map<Long, Long> guildLastReset;
    public long resetInterval = 60000*60*24; //60s * 60 * 24
    public VoiceChatResetDaily(JDA jda, Database database) {
        this.jda = jda;
        this.database = database;
        guildLastReset = new HashMap<>();

        setExecuteInterval(3600000);
        setLastRun(0);
    }
    public void loadLastReset() {
        List<Document> query = database.queryTable(ExperienceService.settingsTable);
        if(query==null)
            return;
        for(Document g : query){
            if(g.containsKey("LAST VC RESET")){
                long lastReset = g.getLong("LAST VC RESET");
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

        Document updateData = new Document().append("LAST VC RESET", new BsonInt64(timestamp));
        Document update = new Document().append("$set", updateData);
        database.updateOne(ExperienceService.settingsTable, filter, update);
        guildLastReset.put(guildID, timestamp);
    }
    public void resetGuildExp(long guildID){
        List<Document> query = database.queryTable(ExperienceService.userTable);
        if(query==null)
            return;
        for(Document d : query){
            if(d.containsKey(Long.toString(guildID))){
                Document guildData = d.get(Long.toString(guildID), Document.class);
                guildData.append("DAILY VC TIME", 0);
                Document updateData = new Document().append(Long.toString(guildID), guildData);
                Document filter = new Document().append("UUID", d.getLong("UUID"));
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
