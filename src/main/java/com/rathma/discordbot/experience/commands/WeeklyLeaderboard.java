package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import org.bson.Document;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WeeklyLeaderboard extends Command {
    int pageSize = 10;
    public WeeklyLeaderboard(CommandService commandService, Message message){
        this.database = commandService.database;
        this.jda = commandService.jda;
        this.commandService = commandService;
        identifier = "weekly";
        childIdentifier = null;
        requiredPermissions=null;
        defaultRequiredUserPermission=null;
        aliases = new ArrayList<>();
        aliases.add("wklb");
        aliases.add("weeklylb");
        aliases.add("lbw");
        run(message);
    }
    public WeeklyLeaderboard() {
        identifier = "weekly";
        childIdentifier = null;
        requiredPermissions=null;
        defaultRequiredUserPermission=null;
        aliases = new ArrayList<>();
        aliases.add("wklb");
        aliases.add("weeklylb");
        aliases.add("lbw");
    }

    public MessageEmbed createLeaderboardMessage(long guildID, int page){
        List<Document> leaderboard = getLeaderboardPage(guildID, page);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.GREEN);
        Guild guild = jda.getGuildById(guildID);
        embedBuilder.setDescription("**"+guild.getName()+"'s Weekly Leaderboard**");
        embedBuilder.setFooter("Page " + page, null);
        if(leaderboard==null)
            return embedBuilder.build();
        int rank = (page-1)*pageSize+1;
        for(Document m : leaderboard){
            long uuid = m.getLong("UUID");
            Member member = guild.getMemberById(uuid);
            if(m.get(Long.toString(guildID), Document.class).containsKey("WEEKLY EXPERIENCE")) {
                int weeklyExperience = m.get(Long.toString(guildID), Document.class).getInteger("WEEKLY EXPERIENCE");
                //int totalExperience = m.get(Long.toString(guildID), Document.class).getInteger("EXPERIENCE");
                //int level = Rank.getLevel(totalExperience);
                String nickname;
                if (member != null) {
                    nickname = member.getEffectiveName();
                } else {
                    //System.out.println(jda.getUserById(uuid).getId());
                    User user = jda.getUserCache().getElementById(uuid);
                    if (user == null) {
                        nickname = Long.toString(uuid);
                    } else {

                        nickname = user.getName();
                    }
                }
                embedBuilder.addField("Rank " + rank + " - " + nickname, "Weekly Experience - " + weeklyExperience, false);
                rank = rank + 1;
            }
        }
        return embedBuilder.build();
    }
    public List<Document> getLeaderboardPage(long guildID, int page){
        List<Object> listeners = jda.getRegisteredListeners();
        List<Document> sortedMembers=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                sortedMembers = ((ExperienceService) o).getWeeklySortedGuildMembers(guildID);
            }
        }
        if(sortedMembers==null)
            return null;

        if((page-1)*pageSize >= sortedMembers.size()){
            return null;
        }
        List<Document> leaderboardPage = new ArrayList<>();

        int start = (page-1)*pageSize;
        for(int i=start; i<=start+pageSize-1; i++){
            if(i>=sortedMembers.size())
                break;
            leaderboardPage.add(sortedMembers.get(i));
        }
        return leaderboardPage;
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
        if(!writePermissions){
            commandService.ongoingCommands.remove(this);
            return;
        }
        long guildID = message.getGuild().getIdLong();
        String parsedCommand = getStrippedParameters(guildID, message.getContentRaw());

        String[] messageSplit = parsedCommand.split(" ");


        if(messageSplit.length>1){
            /* Too many arguments */
            message.getChannel().sendMessage("Syntax Error: too many arguments").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else if(parsedCommand.isEmpty()){
            /* Page 1 default */
            MessageEmbed messageEmbed = createLeaderboardMessage(message.getGuild().getIdLong(), 1);
            if(message!=null)
                message.getChannel().sendMessage(messageEmbed).queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        /* Has a single argument, hopefully it's a number */
        try{
            int page = Integer.parseInt(parsedCommand);
            MessageEmbed messageEmbed = createLeaderboardMessage(message.getGuild().getIdLong(), page);
            if(messageEmbed!=null){
                message.getChannel().sendMessage(messageEmbed).queue();
            }
        }
        catch (NumberFormatException e){
            message.getChannel().sendMessage("Syntax Error: No number specified.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        catch (Exception e){
            e.printStackTrace();
            commandService.ongoingCommands.remove(this);
            return;
        }

        commandService.ongoingCommands.remove(this);
    }
}
