package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class ExpReset extends Command {

    public ExpReset(CommandService commandService, Message message){
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        identifier = "exp";
        childIdentifier = "reset";
        aliases = new ArrayList<>();

        run(message);
    }

    public ExpReset() {
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        identifier = "exp";
        childIdentifier = "reset";
        aliases = new ArrayList<>();


    }

    public void resetUserExp(long uuid, long guildID){
        Document update = new Document();
        Document filter = new Document().append("UUID", uuid);
        Document query = database.queryOne(ExperienceService.userTable, filter);
        if(query==null){
            return;
        }
        if(!query.containsKey(Long.toString(guildID))){
            return;
        }
        Document guildInfo = query.get(Long.toString(guildID), Document.class);
        guildInfo.append("EXPERIENCE", 0);
        update.append("$set", new Document().append(Long.toString(guildID), guildInfo));
        database.updateOne(ExperienceService.userTable, filter, update);
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

        String[] messageSplit = parsedCommand.split(" ");

        if(messageSplit.length<1){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No user specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        List<User> mentionedUsers = message.getMentionedUsers();
        if(mentionedUsers.size()<1 || mentionedUsers==null){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No user specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        resetUserExp(mentionedUsers.get(0).getIdLong(), message.getGuild().getIdLong());
        if(writePermissions){
            message.getChannel().sendMessage(mentionedUsers.get(0).getName() + "'s experience has been reset.").queue();
        }
        commandService.ongoingCommands.remove(this);
    }
}
