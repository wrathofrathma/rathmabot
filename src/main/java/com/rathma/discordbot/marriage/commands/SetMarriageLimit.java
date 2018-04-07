package com.rathma.discordbot.marriage.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.marriage.services.MarriageService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import org.bson.Document;

import java.util.ArrayList;

public class SetMarriageLimit extends Command{

    public SetMarriageLimit(CommandService commandService, Message message){
        requiredPermissions= Permission.MESSAGE_WRITE;
        requiredUserPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        identifier="set";
        childIdentifier="marriage_limit";
        this.jda=commandService.jda;
        this.database=commandService.database;
        this.commandService=commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public SetMarriageLimit(){
        requiredUserPermissions=null;
        requiredPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        identifier="set";
        aliases = new ArrayList<>();
        childIdentifier="marriage_limit";
    }

    public void setGuildLimit(long guildID, int limit){
        Document filter = new Document().append("GUILD ID", guildID);

        Document query = database.queryOne(MarriageService.MARRIAGE_SETTTINGS, filter);
        if(query==null){
            Document document = new Document().append("GUILD ID", guildID);
            document.append("GUILD LIMIT", limit);
            database.insertOne(MarriageService.MARRIAGE_SETTTINGS, document);
        }
        else{
            //update
            if(query.containsKey("_id"))
                query.remove("_id");
            if(query.containsKey("GUILD LIMIT")){
                query.remove("GUILD LIMIT");
                query.append("GUILD LIMIT", limit);
            }
            else{
                query.append("GUILD LIMIT", limit);
            }
            Document update = new Document().append("$set", query);
            database.updateOne(MarriageService.MARRIAGE_SETTTINGS, filter, update);
        }
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
        if(writePermissions==false){
            commandService.ongoingCommands.remove(this);
            return;
        }
        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        if(parsedCommand.isEmpty()){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: No limit specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else {
            try {
                int limit = Integer.parseInt(parsedCommand.split(" ")[0]);
                if(0 > limit || limit > 250){
                    if(writePermissions)
                        message.getChannel().sendMessage("Input Error: Limit not within bounds 0-250.").queue();
                    commandService.ongoingCommands.remove(this);
                    return;
                }
                else{
                    setGuildLimit(guildID, limit);
                    if(writePermissions)
                        message.getChannel().sendMessage("Setting guild marriage role limit to: " + limit).queue();
                }
            }
            catch(NumberFormatException e){
                if(writePermissions)
                    message.getChannel().sendMessage("Syntax Error: Input was not an integer.").queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
        }


        commandService.ongoingCommands.remove(this);
    }
}
