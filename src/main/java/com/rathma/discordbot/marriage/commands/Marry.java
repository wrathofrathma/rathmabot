package com.rathma.discordbot.marriage.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.marriage.services.MarriageService;
import com.rathma.discordbot.marriage.utils.Proposal;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.GuildController;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Marry extends Command {


    public Marry(CommandService commandService, Message message){
        requiredPermissions= Permission.MESSAGE_WRITE;
        requiredUserPermissions=null;
        identifier="marry";
        childIdentifier=null;
        this.jda=commandService.jda;
        this.database=commandService.database;
        this.commandService=commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public Marry(){
        requiredUserPermissions=null;
        requiredPermissions=Permission.MESSAGE_WRITE;
        identifier="marry";
        aliases = new ArrayList<>();
        childIdentifier=null;
    }

    public boolean checkTicket(long guildID, long uuid){
        Guild guild = jda.getGuildById(guildID);
        GuildController guildController = guild.getController();
        Member member = guild.getMemberById(uuid);

        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(MarriageService.MARRIAGE_SETTTINGS, filter);
        if(query==null){
            System.out.println("No marriage settings yet!");
            return false;
        }
        if(!query.containsKey("TICKET ROLE")){
            System.out.println("No ticket role specified yet!");
            return false;
        }

        Role marriageTicket = guild.getRoleById(query.getLong("TICKET ROLE"));
        if(marriageTicket==null){
            System.out.println("Ticket role doesn't exist in guild.");
            return false;
        }

        if(member.getRoles().contains(marriageTicket))
            return true;
        return false;
    }

    public boolean checkMarried(long guildID, long uuid){
        Document filter = new Document().append("GUILD ID", guildID);
        List<Document> query = database.queryMany(MarriageService.MARRIAGE_TABLE, filter);
        if(query==null || query.isEmpty()){
            return false;
        }
        for(Document m : query){
            if(m.containsValue(uuid))
                return true;
        }
        return false;
    }

    /* Returns true if it hits the guild limit */
    public boolean checkGuildLimit(long guildID){
        Document filter = new Document().append("GUILD ID", guildID);
        Document query = database.queryOne(MarriageService.MARRIAGE_SETTTINGS, filter);
        if(query==null || !query.containsKey("GUILD LIMIT")){
            System.out.println("No guild settings for marriage.");
            return true;
        }
        int guildLimit = query.getInteger("GUILD LIMIT");

        List<Document> query2 = database.queryMany(MarriageService.MARRIAGE_TABLE, filter);
        if(query2==null){
            return false;
        }
        if(query2.size() < guildLimit)
            return false;
        return true;
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

        long proposerID = message.getMember().getUser().getIdLong();
        long proposee = 0;

        if(!checkTicket(guildID, proposerID)){
            if(writePermissions)
                message.getChannel().sendMessage("You don't have the marriage ticket! Check your local shop for the limited availability ticket!").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        if(parsedCommand.isEmpty()){
            if(writePermissions)
                message.getChannel().sendMessage("You fucking autist, who do you want to marry?").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else {
            List<User> mentions = message.getMentionedUsers();
            if (mentions.size() < 1) {
                message.getChannel().sendMessage("Syntax error: No user provided or user not in server.").queue();
                commandService.ongoingCommands.remove(this);
                return;
            } else {
                proposee = mentions.get(0).getIdLong();
            }
        }

        if(proposee == proposerID){
            if(writePermissions)
                message.getChannel().sendMessage("You can't marry yourself, that'd be weird.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        if(checkGuildLimit(guildID)){
            if(writePermissions){
                message.getChannel().sendMessage("Unfortunately the guild marriage capacity has been hit. Hold onto your ticket and wait until someone fucks up or divorces!").queue();
            }
            commandService.ongoingCommands.remove(this);
            return;
        }

        if(checkMarried(guildID, proposerID)){
            /* proposer is married */
            if(writePermissions)
                message.getChannel().sendMessage("You slut, how dare you try to marry more than one person. Shame on you!").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else if(checkMarried(guildID, proposee)){
            /* Proposee is married */
            if(writePermissions)
                message.getChannel().sendMessage("You homewrecker....just leave the happy couple in peace....or at least wait for them to get divorced you depraved sadistic fuck.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else {
            MarriageService marriageService=null;
            List<Object> listeners = jda.getRegisteredListeners();
            for(Object o : listeners){
                if(o instanceof MarriageService){
                    marriageService = (MarriageService)o;
                }
            }
            if(marriageService==null){
                System.out.println("Marriage service not found.");
                commandService.ongoingCommands.remove(this);
                return;
            }
            HashMap<Long, List<Proposal>> activeProposals = marriageService.getActiveProposals();
            if(activeProposals.containsKey(guildID)){
                for(Proposal p : activeProposals.get(guildID)){
                    if(p.proposer==proposerID){
                        /* Found an existing proposal, break it down boys */
                        if(writePermissions)
                            message.getChannel().sendMessage("Slooowwww down there junior. One proposal at a time my dude.").queue();
                        commandService.ongoingCommands.remove(this);
                        return;
                    }
                    else if(p.proposee==proposee){
                        /* Proposal towards the target exists. For simplification we'll only allow one. */
                        if(writePermissions)
                            message.getChannel().sendMessage("Someone has already confessed their love for your waifu. Wait for them to respond before confessing your love.").queue();
                        commandService.ongoingCommands.remove(this);
                        return;
                    }
                }
                /* Guild exists, but no proposal from this nigga yet */
                marriageService.getActiveProposals().get(guildID).add(new Proposal(guildID, message.getChannel().getIdLong(), proposerID, proposee, message.getCreationTime().toEpochSecond()));
                if(writePermissions)
                    message.getChannel().sendMessage("<@" + proposee + "> do you take <@" + proposerID + "> to be your lovely waifu?").queue();
            }
            else{
                marriageService.getActiveProposals().put(guildID, new ArrayList<>());
                marriageService.getActiveProposals().get(guildID).add(new Proposal(guildID, message.getChannel().getIdLong(), proposerID, proposee, message.getCreationTime().toEpochSecond()));
                /* Proposal */
                if(writePermissions)
                    message.getChannel().sendMessage("<@" + proposee + "> do you take <@" + proposerID + "> to be your lovely waifu?").queue();
            }
        }
        commandService.ongoingCommands.remove(this);
    }
}
