package com.rathma.discordbot.audio.commands;

import com.rathma.discordbot.audio.AudioPlayerSendHandler;
import com.rathma.discordbot.audio.services.AudioService;
import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.List;

public class Play extends Command {
    public Play(CommandService commandService, Message message){
        requiredUserPermissions=null;
        requiredPermissions=null;
        identifier = "play";
        childIdentifier = null;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();

        run(message);
    }

    public Play(){
        requiredUserPermissions=null;
        requiredPermissions=null;
        identifier="play";
        childIdentifier=null;
        aliases = new ArrayList<>();
    }

    public void run(Message message){
        if(!checkUserPermissions(message)){
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
        if(parsedCommand.isEmpty()){
            if(writePermissions)
                message.getChannel().sendMessage("Invalid Syntax\nExample: <prefix>play <link>").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        if(messageSplit.length!=1 && writePermissions){
            message.getChannel().sendMessage("Invalid Syntax\nExample: <prefix>play <link>").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        else if(messageSplit.length!=1){
            commandService.ongoingCommands.remove(this);
            return;
        }

        /* Message length is 1, which should just be the link */
        /* First check if user is in VC.
         * Check if already in channel or if we can connect to the channel.
         * Check channel perms if connecting
         * Connect if not connected.
         */

        Guild guild = jda.getGuildById(guildID);
        List<VoiceChannel> voiceChannelList = guild.getVoiceChannels();
        //Finding if user is in the voice channel.
        VoiceChannel voiceChannel=message.getMember().getVoiceState().getChannel();
        if(voiceChannel==null){
            if(writePermissions)
                message.getChannel().sendMessage("Error: User not connected to voice channel.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        //Getting the audio manager & connecting.
        AudioManager audioManager = guild.getAudioManager();
        List<Permission> channelPermissions = guild.getSelfMember().getPermissions(voiceChannel);
        if(!channelPermissions.contains(Permission.VOICE_CONNECT)){
            if(writePermissions)
                message.getChannel().sendMessage("Error: Insufficient permissions to connect to voice channel.").queue();
            commandService.ongoingCommands.remove(this);
            return;
        }
        audioManager.openAudioConnection(voiceChannel);
        /* Successfully connected. Let's get some audio going */
        //Finding the guild player.
        AudioService audioService = AudioService.getActiveAudioService(jda);
        AudioPlayerSendHandler audioPlayerSendHandler = audioService.getGuildHandler(guildID);
        audioManager.setSendingHandler(audioPlayerSendHandler);
        //Found & set handler. Time to start queuing shit up


        commandService.ongoingCommands.remove(this);
    }
}
