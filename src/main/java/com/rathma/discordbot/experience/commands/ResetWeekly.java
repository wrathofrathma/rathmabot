package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import org.bson.BsonInt64;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResetWeekly extends Command {
    public ResetWeekly(CommandService commandService, Message message){
        this.database = commandService.database;
        this.jda = commandService.jda;
        this.commandService = commandService;
        identifier = "resetweekly";
        childIdentifier = null;
        defaultRequiredUserPermission=Permission.ADMINISTRATOR;
        requiredPermissions=null;
        aliases = new ArrayList<>();

        run(message);
    }
    public ResetWeekly() {
        identifier = "resetweekly";
        childIdentifier = null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        requiredPermissions=null;
        aliases = new ArrayList<>();

    }

    public void setLastReset(long guildID, long timestamp){
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(ExperienceService.settingsTable, filter);

        Document updateData = new Document().append("LAST WEEKLY RESET", new BsonInt64(timestamp));
        Document update = new Document().append("$set", updateData);
        database.updateOne(ExperienceService.settingsTable, filter, update);
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

        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        commandService.ongoingCommands.remove(this);
        if(!parsedCommand.isEmpty()){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: too many parameters").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        resetGuildExp(message.getGuild().getIdLong());
        if(writePermissions) {
            message.getChannel().sendMessage("Guild's weekly experience has been reset.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        commandService.ongoingCommands.remove(this);
    }
}
