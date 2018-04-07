package com.rathma.discordbot.marriage.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.marriage.services.MarriageService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SetTicketRole extends Command{
    public SetTicketRole(CommandService commandService, Message message){
        requiredPermissions= Permission.MESSAGE_WRITE;
        requiredUserPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        identifier="set";
        childIdentifier="ticket_role";
        this.jda=commandService.jda;
        this.database=commandService.database;
        this.commandService=commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public SetTicketRole(){
        requiredUserPermissions=null;
        requiredPermissions=null;
        defaultRequiredUserPermission= Permission.ADMINISTRATOR;
        identifier="set";
        aliases = new ArrayList<>();
        childIdentifier="ticket_role";
    }

    public Role getMentionedRole(long guildID, String message){
        Guild guild = jda.getGuildById(guildID);
        for(Role role : guild.getRolesByName(message, true)){
            return role;
        }
        return null;
    }
    public void setTicketRole(Role role){
        Document filter = new Document().append("GUILD ID", role.getGuild().getIdLong());

        Document query = database.queryOne(MarriageService.MARRIAGE_SETTTINGS, filter);
        if(query==null){
            Document document = new Document().append("GUILD ID", role.getGuild().getIdLong());
            document.append("TICKET ROLE", role.getIdLong());
            database.insertOne(MarriageService.MARRIAGE_SETTTINGS, document);
        }
        else{
            //update
            if(query.containsKey("_id"))
                query.remove("_id");
            if(query.containsKey("TICKET ROLE")){
                query.remove("TICKET ROLE");
                query.append("TICKET ROLE", role.getIdLong());
            }
            else{
                query.append("TICKET ROLE", role.getIdLong());
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
                message.getChannel().sendMessage("Syntax Error: No role specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else {
            /* Role might exist, let's find it */
            List<Role> roleList = message.getMentionedRoles();
            if(roleList.isEmpty() || roleList == null){
                Role role = getMentionedRole(guildID, parsedCommand);
                if(role!=null){
                    /* Set Ticket Role */
                    setTicketRole(role);
                    if(writePermissions)
                        message.getChannel().sendMessage("Setting marriage ticket role to " + role.getName()).queue();
                }
                else{
                    if(writePermissions)
                        message.getChannel().sendMessage("Role not found.").queue();
                    commandService.ongoingCommands.remove(this);
                    return;
                }
            }
            else{
                /* Set Ticket Role */
                setTicketRole(roleList.get(0));
                message.getChannel().sendMessage("Setting marriage ticket role to " + roleList.get(0).getName()).queue();

            }
        }
        commandService.ongoingCommands.remove(this);
    }
}
