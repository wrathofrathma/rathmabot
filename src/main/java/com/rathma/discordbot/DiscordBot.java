package com.rathma.discordbot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.*;


/**
 * Created by rathma on 1/15/18.
 */
public class DiscordBot {

    private JDA jda;
    public Configuration config;
    public Database db;
    public DiscordBot() { }

    public int run() {
        config = new Configuration();
        db = new Database(config);

        System.out.println("Attempting to login...");
        try
        {
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(config.getToken())
                    .addEventListener(new MessageListener(this))
                    .buildBlocking();
        }
        catch (LoginException e)
        {
            e.printStackTrace();
        }
        catch (RateLimitedException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Logged in...");
        return 0;
    }

}
