package com.rathma.discordbot;

import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

/**
 * Created by rathma on 1/15/18.
 */
public class MessageListener implements net.dv8tion.jda.core.hooks.EventListener {

    private DiscordBot bot;
    MessageListener(DiscordBot discordBot)
    {
        bot = discordBot;
    }

    public void onEvent(Event event)
    {
        if(event instanceof GuildMessageReceivedEvent)
        {
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
            bot.db.logMessage(e.getMessage());
        }
        else if(event instanceof PrivateMessageReceivedEvent)
        {

        }
    }

}
