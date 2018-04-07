package com.rathma.discordbot.audio.services;

import com.rathma.discordbot.audio.AudioPlayerSendHandler;
import com.rathma.discordbot.audio.TrackScheduler;
import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.Service;
import com.rathma.discordbot.experience.services.ExperienceService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.events.Event;

import java.util.List;
import java.util.Map;

/* This class technically shouldn't exist and is just a very spaghetti way of doing some things without a full refractor. */
public class AudioService extends Service{
    public JDA jda;
    public Database database;

    private AudioPlayerManager playerManager;
    private Map<Long, AudioPlayerSendHandler> playerMap;

    public AudioService(JDA jda, Database database) {
        this.jda=jda;
        this.database=database;
        serviceID="audio";
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

    }





    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }
    public AudioPlayerSendHandler getGuildHandler(long guildID){
        if(playerMap.containsKey(guildID)){
            return playerMap.get(guildID);
        }
        else{
            AudioPlayer player = playerManager.createPlayer();
            playerMap.put(guildID, new AudioPlayerSendHandler(player));
            return playerMap.get(guildID);
        }
    }

    public static AudioService getActiveAudioService(JDA jda1){
        List<Object> listeners = jda1.getRegisteredListeners();
        for(Object o : listeners){
            if(o instanceof AudioService)
                return (AudioService)o;
        }
        return null;
    }
    @Override
    public void onEvent(Event event) {
        //Do nothing
    }
}
