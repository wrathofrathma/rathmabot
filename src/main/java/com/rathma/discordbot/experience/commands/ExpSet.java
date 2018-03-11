package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class ExpSet extends Command {

    public ExpSet(CommandService commandService, Message message){
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        identifier = "exp";
        childIdentifier = "set";
        aliases = new ArrayList<>();

        run(message);
    }

    public ExpSet() {
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        identifier = "exp";
        childIdentifier = "set";
        aliases = new ArrayList<>();
    }

    public void setUserExp(long uuid, long guildID, int exp){
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
        guildInfo.append("EXPERIENCE", exp);
        update.append("$set", new Document().append(Long.toString(guildID), guildInfo));
        database.updateOne(ExperienceService.userTable, filter, update);
    }
    public int getExp(String message) throws NumberFormatException {
        return Integer.parseInt(message);
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
        if(messageSplit.length<2){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No exp specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        try {
            int exp = getExp(messageSplit[1]);
            setUserExp(mentionedUsers.get(0).getIdLong(), message.getGuild().getIdLong(), exp);
            if (writePermissions) {
                message.getChannel().sendMessage(mentionedUsers.get(0).getName() + "'s experience has been set.").queue();
            }

        }
        catch (NumberFormatException e){
            if (writePermissions) {
                message.getChannel().sendMessage("Syntax Error: No number specified").queue();
            }
            commandService.ongoingCommands.remove(this);
            return;
        }
        commandService.ongoingCommands.remove(this);
    }
}
