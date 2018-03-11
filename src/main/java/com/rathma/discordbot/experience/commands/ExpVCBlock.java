package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.VoiceChannel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ExpVCBlock extends Command {
    public ExpVCBlock(CommandService commandService, Message message){
        identifier = "exp";
        childIdentifier = "vcblock";
        requiredPermissions = null;
        defaultRequiredUserPermission = Permission.ADMINISTRATOR;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();

        run(message);
    }
    public ExpVCBlock() {
        identifier = "exp";
        childIdentifier = "vcblock";
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
        if(!experienceService.blockedVoiceChannels.containsKey(guildID)) {
            channelList.add("None");
            return channelList;
        }
        if(experienceService.blockedVoiceChannels.get(guildID).isEmpty()) {
            channelList.add("None");
            return channelList;
        }

        for(Long channelID : experienceService.blockedVoiceChannels.get(guildID)){
            VoiceChannel channel = jda.getVoiceChannelById(channelID);
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
        embedBuilder.setDescription(jda.getGuildById(guildID).getName() + "'s experience ignored voice channels.");
        embedBuilder.addField("Voice Channel List", channels, true);
        return embedBuilder.build();
    }

    public List<VoiceChannel> getMentionedVoiceChannels(long guildID, String message){
        String[] messageSplit = message.split(" " );
        Guild guild = jda.getGuildById(guildID);
        List<VoiceChannel> mentionedChannels;
        if(messageSplit[0].equals("list")){
            return new ArrayList<>();
        }
        else if(messageSplit[0].equals("add") || messageSplit[0].equals("remove")){
            String channelString = message.substring(messageSplit[0].length()+1);
            System.out.println("Channel String:" + channelString);
            mentionedChannels = guild.getVoiceChannelsByName(channelString, true);
            if(mentionedChannels!=null){
                return mentionedChannels;
            }
            return new ArrayList<>();
        }
        else{
            return new ArrayList<>();
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

        List<VoiceChannel> channelList = getMentionedVoiceChannels(guildID, parsedCommand);

        if(channelList.size()==0){
            if(messageSplit[0].equals("list")){
                if(writePermissions)
                    message.getChannel().sendMessage(buildChannelList(guildID)).queue();
                commandService.ongoingCommands.remove(this);
                return;
            }
            else {
                if (writePermissions)
                    message.getChannel().sendMessage("No voice channels listed.").queue();
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
            for (VoiceChannel c : channelList) {
                experienceService.setVoiceChannelBlock(c);
                count++;
            }
            if (writePermissions)
                message.getChannel().sendMessage("Successfully added " + count + " voice channels to the experience ignore list.").queue();
        }
        else if(messageSplit[0].equals("remove")){
            for (VoiceChannel c : channelList) {
                experienceService.removeVoiceChannelBlock(c);
                count++;
            }
            if (writePermissions)
                message.getChannel().sendMessage("Successfully removed " + count + " channels from the experience ignore list.").queue();
        }

        commandService.ongoingCommands.remove(this);
    }
}
