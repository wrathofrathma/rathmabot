package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ExpChannelBlock extends Command {
    public ExpChannelBlock(CommandService commandService, Message message){
        identifier = "exp";
        childIdentifier = "channelblock";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public ExpChannelBlock() {
        identifier = "exp";
        childIdentifier = "channelblock";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        aliases = new ArrayList<>();

    }

    public List<String> getBlockedChannels(long guildID){
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService)
                experienceService = (ExperienceService)o;
        }

        List<String> channelList = new ArrayList<>();
        if(!experienceService.blockedChannels.containsKey(guildID)) {
            channelList.add("None");
            return channelList;
        }
        if(experienceService.blockedChannels.get(guildID).isEmpty()) {
            channelList.add("None");
            return channelList;
        }

        for(Long channelID : experienceService.blockedChannels.get(guildID)){
            TextChannel channel = jda.getTextChannelById(channelID);
            if(channel!=null){
                channelList.add(channel.getName());
            }
        }
        return channelList;
    }
    public MessageEmbed buildChannelList(long guildID){
        List<String> channelList = getBlockedChannels(guildID);
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLUE);
        String channels = "";
        for(String s : channelList){
            channels = channels.concat(s + "\n");
        }
        embedBuilder.setDescription(jda.getGuildById(guildID).getName() + "'s experience ignored channels.");
        embedBuilder.addField("Channel List", channels, true);
        return embedBuilder.build();
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
                message.getChannel().sendMessage("Syntax Error: Too few arguments.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        if(!(messageSplit[0].equals("add") || messageSplit[0].equals("remove") || messageSplit[0].equals("list"))){
            if(writePermissions)
                message.getChannel().sendMessage("Syntax Error: second argument should be add/remove/list.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }

        List<TextChannel> channelList = message.getMentionedChannels();
        if(channelList.size()==0){
            if(messageSplit[0].equals("list")){
                if(writePermissions)
                    message.getChannel().sendMessage(buildChannelList(guildID)).queue();
                    commandService.ongoingCommands.remove(this);
                    return;
            }
            else {
                if (writePermissions)
                    message.getChannel().sendMessage("No channels listed.").queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
        }

        List<Object> listeners =  jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService = (ExperienceService)o;
            }
        }
        if(experienceService==null){
            commandService.ongoingCommands.remove(this);
            return;
        }

        int count = 0;
        if(messageSplit[0].equals("add")) {
            for (TextChannel c : channelList) {
                experienceService.setChannelBlock(c);
                count++;
            }
            if (writePermissions)
                message.getChannel().sendMessage("Successfully added " + count + " channels to the experience ignore list.").queue();
        }
        else if(messageSplit[0].equals("remove")){
            for (TextChannel c : channelList) {
                experienceService.removeChannelBlock(c);
                count++;
            }
            if (writePermissions)
                message.getChannel().sendMessage("Successfully removed " + count + " channels from the experience ignore list.").queue();
        }

        commandService.ongoingCommands.remove(this);
    }
}
