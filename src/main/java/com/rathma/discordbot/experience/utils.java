package com.rathma.discordbot.experience;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class utils {

    public static List<Document> getSortedGuildMembers(Database db, long guildID, String sortKey){
        List<Document> table = db.queryTable(ExperienceService.userTable);
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
                if(u1.containsKey(sortKey) && u2.containsKey(sortKey)) {
                    if (u1.getInteger(sortKey) == u2.getInteger(sortKey)) {
                        /* Equal exp */
                        return 0;
                    }
                    else if (u1.getInteger(sortKey) > u2.getInteger(sortKey)) {
                        return -1;
                    }
                    else
                        return 1;
                }
                else if(u1.containsKey(sortKey)){
                    /* We know u2 doesn't contain the key, so let's just return here */
                    return 1;
                }
                else if(u2.containsKey(sortKey)){
                    /* u2 exists and u1 doesn't u2 wins */
                    return -1;
                }
                /* Neither exist, so basically equal */
                return 0;
            }
        });
        return usersInGuild;
    }

    public static int getMemberRank(long uuid, List<Document> leaderboard){
        int rank = 0;
        for(int i=0; i<leaderboard.size(); i++){
            rank++;
            if(leaderboard.get(i).containsValue(uuid)){
                break;
            }
        }
        return rank;
    }

}
