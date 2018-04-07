package com.rathma.discordbot.marriage.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.marriage.services.MarriageService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.GuildController;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Divorce extends Command {
    public Divorce(CommandService commandService, Message message){
        requiredPermissions= Permission.MESSAGE_WRITE;
        requiredUserPermissions=null;
        identifier="divorce";
        childIdentifier=null;
        this.jda=commandService.jda;
        this.database=commandService.database;
        this.commandService=commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public Divorce(){
        requiredUserPermissions=null;
        requiredPermissions=Permission.MESSAGE_WRITE;
        identifier="divorce";
        aliases = new ArrayList<>();
        childIdentifier=null;
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

        long uuid = message.getMember().getUser().getIdLong();

        Document filter = new Document().append("GUILD ID", guildID);
        List<Document> query = database.queryMany(MarriageService.MARRIAGE_TABLE, filter);
        if(query==null){
            if(writePermissions)
                message.getChannel().sendMessage("Are you hallucinating? You're not married to anyone....").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        Document mDoc = null;
        for(Document m : query){
            if(m.containsValue(uuid)){
                mDoc = m;
            }
        }
        if(mDoc==null){
            if(writePermissions)
                message.getChannel().sendMessage("Are you hallucinating? You're not married to anyone....").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        /* First remove from DB */
        filter.append("MEMBER1", mDoc.getLong("MEMBER1"));
        filter.append("MEMBER2", mDoc.getLong("MEMBER2"));
        database.deleteOne(MarriageService.MARRIAGE_TABLE, filter);

        /* Next remove their role */
        GuildController guildController = jda.getGuildById(guildID).getController();
        Role marriageRole = jda.getGuildById(guildID).getRoleById(mDoc.getLong("ROLE ID"));
        if(marriageRole==null){
            //no role found, we good.
        }
        else{
            marriageRole.delete().queue();
        }
        long partnerID = 0;
        if(uuid == mDoc.getLong("MEMBER1")){
            partnerID = mDoc.getLong("MEMBER2");
        }
        else
            partnerID = mDoc.getLong("MEMBER1");

        if(writePermissions)
            message.getChannel().sendMessage("<@" + uuid + "> has successfully divorced <@" + partnerID + ">.").queue();
        commandService.ongoingCommands.remove(this);
    }
}
