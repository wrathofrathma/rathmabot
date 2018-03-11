package com.rathma.discordbot.utils;

import com.google.gson.*;
import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.io.FileNotFoundException;
import java.io.FileReader;

/* Just a utility class to import mee6 leaderboards by json, 1 at a time
* Might be a little messed up currently, mee6 was in the process of switching around their json exporting at the same time I was writing this. So I have to go back sometime.
*
* */
public class Mee6Import
{
    public static void importLB(JDA jda, Database database, String filename){

        try {
            JsonParser parser = new JsonParser();
            JsonObject obj = (JsonObject) parser.parse(new FileReader(filename));

            JsonArray players = obj.getAsJsonArray("players");
            long guild = obj.getAsJsonObject("guild").get("id").getAsLong();
            System.out.println("Guild ID: " + guild);
            int x = 0;
            for(JsonElement player : players){
                importPlayer(jda, database, (player.getAsJsonObject()), guild);
                x++;
            }
            System.out.println("Imported: " + x);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Couldn't locate " + filename);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void importPlayer(JDA jda, Database database, JsonObject player, long guildID){
        //System.out.println("Importing player: " + player.get("username") + " | xp:  " + player.get("xp"));
        long userID = player.get("id").getAsLong();

        Guild guild = jda.getGuildById(guildID);
        User jdaUser = jda.getUserById(userID);
        if(jdaUser==null)
            return;
        if(!guild.isMember(jdaUser))
            return;
        try {

            Document user = new Document();
            user.append("UUID", userID);

            Document guildExp = new Document();
            guildExp.append("EXPERIENCE", player.get("xp").getAsInt());
            guildExp.append("WEEKLY EXPERIENCE", 0);
            user.append(Long.toString(guildID), guildExp);
            database.insertOne(ExperienceService.userTable, user);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
