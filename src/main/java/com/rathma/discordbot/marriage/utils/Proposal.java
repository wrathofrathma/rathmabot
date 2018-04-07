package com.rathma.discordbot.marriage.utils;

public class Proposal {
    public long channel;
    public long proposer;
    public long proposee;
    public long timestamp;
    public long guildID;

    public Proposal(long guildID, long channel, long proposer, long proposee, long timestamp){
        this.channel = channel;
        this.proposer = proposer;
        this.proposee = proposee;
        this.timestamp = timestamp;
        this.guildID = guildID;
    }

    public long getChannel() {
        return channel;
    }

    public void setChannel(long channel) {
        this.channel = channel;
    }

    public long getProposer() {
        return proposer;
    }

    public void setProposer(long proposer) {
        this.proposer = proposer;
    }

    public long getProposee() {
        return proposee;
    }

    public void setProposee(long proposee) {
        this.proposee = proposee;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
