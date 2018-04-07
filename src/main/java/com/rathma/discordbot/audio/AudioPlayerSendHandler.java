package com.rathma.discordbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.core.audio.AudioSendHandler;


public class AudioPlayerSendHandler implements AudioSendHandler {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;
    private TrackScheduler trackScheduler;
    public AudioPlayerSendHandler(AudioPlayer audioPlayer){
     this.audioPlayer = audioPlayer;
     trackScheduler = new TrackScheduler(audioPlayer);
     audioPlayer.addListener(trackScheduler);

    }
    public TrackScheduler getTrackScheduler(){
        return trackScheduler;
    }
    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}
