package com.rathma.discordbot.entities;

import net.dv8tion.jda.core.entities.Message;

public abstract class TimedEvent {

    private long executeInterval;
    private long lastRun;

    public long getExecuteInterval() {
        return executeInterval;
    }

    public void setExecuteInterval(long executeInterval) {
        this.executeInterval = executeInterval;
    }

    public long getLastRun() {
        return lastRun;
    }

    public void setLastRun(long lastRun) {
        this.lastRun = lastRun;
    }

    public void run() {
    }

    public void run(Message message) {
    }

    ;
}