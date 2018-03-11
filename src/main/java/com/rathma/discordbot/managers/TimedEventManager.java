package com.rathma.discordbot.managers;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.TimedEvent;
import net.dv8tion.jda.core.JDA;

import java.util.ArrayList;
import java.util.List;

public class TimedEventManager extends Thread {
    private JDA jda;
    private Database database;
    public List<TimedEvent> loadedEvents;

    public TimedEventManager(JDA jda, Database database) {
        this.jda = jda;
        this.database = database;
        loadedEvents = new ArrayList<>();
    }
    /* Loading & Unloading */
    public void loadTimedEvent(TimedEvent event){
        loadedEvents.add(event);
    }
    public void unloadTimedEvent(TimedEvent event){
        if(loadedEvents.contains(event)){
            loadedEvents.remove(event);
        }
    }

    public void run() {
        synchronized (loadedEvents) {
            while (true) {
                long currentTime = System.currentTimeMillis();
                for (TimedEvent event : loadedEvents) {
                    if ((currentTime - event.getLastRun()) > event.getExecuteInterval() || event.getLastRun() == 0) {
                        /* Execute Event */
                        event.run();
                        event.setLastRun(currentTime);
                    }
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
