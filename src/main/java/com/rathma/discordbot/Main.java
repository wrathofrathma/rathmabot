package com.rathma.discordbot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by rathma on 1/15/18.
 */
public class Main {

    public static void main(String[] args) {
        //Logger rootLog= (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        //rootLog.setLevel(Level.ERROR);
        DiscordBot discord = new DiscordBot();
        discord.run();
    }
}
