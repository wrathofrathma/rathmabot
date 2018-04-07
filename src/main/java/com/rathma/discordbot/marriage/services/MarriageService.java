package com.rathma.discordbot.marriage.services;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.Service;
import com.rathma.discordbot.marriage.utils.Proposal;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.requests.restaction.RoleAction;
import org.bson.Document;

import java.awt.*;
import java.util.HashMap;
import java.util.List;


public class MarriageService extends Service {
    public JDA jda;
    public Database database;
    public static String MARRIAGE_SETTTINGS = "MARRIAGE_SETTINGS";
    public static String MARRIAGE_TABLE = "MARRIAGE";
    private HashMap<Long, List<Proposal>> activeProposals;



    public MarriageService(JDA jda, Database database){
        this.jda=jda;
        this.database=database;
        serviceID="marriage";
        activeProposals = new HashMap<>();
    }

    public HashMap<Long, List<Proposal>> getActiveProposals() {
        return activeProposals;
    }


    public void createMarriage(long guildID, long channelID, long proposerID, long proposeeID){
        Guild guild = jda.getGuildById(guildID);
        GuildController guildController = guild.getController(); //Needed to add roles or manage roles on people.
        TextChannel channel = guild.getTextChannelById(channelID);
        Member proposer = guild.getMemberById(proposerID);
        Member proposee = guild.getMemberById(proposeeID);


        boolean writePermissions = guild.getSelfMember().getPermissions(channel).contains(Permission.MESSAGE_WRITE);

        /* Check if role exists */
        List<Role> roleList = guild.getRolesByName(proposer.getEffectiveName() + " ❤ " + proposee.getEffectiveName(), false);
        if(roleList.isEmpty()) {
            /* Create the role */
            if (guild.getRoles().size() < 250)
                guildController.createRole()
                        .setColor(Color.PINK)
                        .setMentionable(false)
                        .setName(proposer.getEffectiveName() + " ❤ " + proposee.getEffectiveName())
                        .setPermissions(0L)
                        .queue();
            else {
                channel.sendMessage("Error: Too many guild roles, cannot create anymore roles.").queue();
                return;
            }
        }
        /* Go find the role since creating doesn't return a role ID */
        Role marriageRole=null;

        //TODO please fix this shit. I think the guild controller is its own thread and creating a role takes time and sometimes the role isn't found after creation because execution is too fast.
        //TODO We need to find a way to wait until the role is created instead of looping like an autist.
        roleList = guild.getRolesByName(proposer.getEffectiveName() + " ❤ " + proposee.getEffectiveName(), false);
        int attempt = 0;
        while(roleList.isEmpty() && attempt <=5){
            roleList = guild.getRolesByName(proposer.getEffectiveName() + " ❤ " + proposee.getEffectiveName(), false);
            try{
                Thread.sleep(1000);
            }
            catch(Exception e){
                e.printStackTrace();
                return;
            }
            attempt++;
        }
        if(!roleList.isEmpty()){
            marriageRole = roleList.get(0);
        }
        else{
            channel.sendMessage("There was an issue finding the marriage role after creation.").queue();
            return;
        }
        /* Add role to peeps */
        guildController.addSingleRoleToMember(proposer, marriageRole).queue();
        guildController.addSingleRoleToMember(proposee, marriageRole).queue();

        /* Add role to DB */
        Document filter = new Document().append("GUILD ID", guildID).append("ROLE ID", marriageRole.getIdLong());
        Document query = database.queryOne(MARRIAGE_TABLE, filter);
        if(query==null) {
            Document marriage = new Document().append("ROLE ID", marriageRole.getIdLong());
            marriage.append("GUILD ID", guildID);
            marriage.append("MEMBER1", proposerID);
            marriage.append("MEMBER2", proposeeID);
            database.insertOne(MARRIAGE_TABLE, marriage);
        }
        else{
            /* Marriage exists in the table, probably just had their role removed. Just leave it. */
        }
        /* Marriage successful */
        if(writePermissions)
            channel.sendMessage("<@" + proposerID + "> and <@" + proposeeID + "> are now banging. Congratulations!").queue();
        /* Remove marriage ticket from proposer */
        Document filter2 = new Document().append("GUILD ID", guildID);
        Document query2 = database.queryOne(MARRIAGE_SETTTINGS, filter2);
        if(query2==null || !query2.containsKey("TICKET ROLE")){
            return;
        }
        Role ticketRole = guild.getRoleById(query2.getLong("TICKET ROLE"));
        if(ticketRole==null)
            return;
        if(proposer.getRoles().contains(ticketRole)){
            guildController.removeSingleRoleFromMember(proposer, ticketRole).queue();
        }
    }
    public void removeProposal(Proposal proposal){
        synchronized (activeProposals) {
            activeProposals.get(proposal.guildID).remove(proposal);
        }
    }

    @Override
    public void onEvent(Event event) {
        if(event instanceof GuildMessageReceivedEvent){
            Message message = ((GuildMessageReceivedEvent)event).getMessage();
            long guild = message.getGuild().getIdLong();
            long channelID = message.getChannel().getIdLong();
            long uuid = message.getMember().getUser().getIdLong();
            boolean writePermissions = message.getGuild().getSelfMember().getPermissions(message.getTextChannel()).contains(Permission.MESSAGE_WRITE);

            if(activeProposals.containsKey(guild)){
                if(!activeProposals.get(guild).isEmpty()){
                    /* Check proposals */
                    for(Proposal p : activeProposals.get(guild)){
                        if(p.proposee == uuid){
                            /* Proposal exists, check text channel then text */
                            if(channelID==p.channel) {
                                String messageText = message.getContentRaw().toString().toLowerCase();
                                if (messageText.equals("yes") || messageText.equals("i do")) {
                                    /* Create Marriage */
                                    createMarriage(guild, channelID, p.proposer, p.proposee);
                                    removeProposal(p);
                                    return;
                                }
                                else if(messageText.equals("no") || messageText.equals("fuck no")){
                                    if(writePermissions)
                                        message.getChannel().sendMessage("Rip my dude. <@" + p.proposee + "> has rejected <@" + p.proposer + ">.").queue();
                                    removeProposal(p);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
