package com.rathma.discordbot;

import com.rathma.discordbot.core.commands.Prefix;
import com.rathma.discordbot.core.commands.PrefixSet;
import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.database.MongoDB;
import com.rathma.discordbot.entities.Service;
import com.rathma.discordbot.experience.commands.*;
import com.rathma.discordbot.experience.services.ExperienceService;
import com.rathma.discordbot.experience.timedEvents.*;
import com.rathma.discordbot.managers.TimedEventManager;
import com.rathma.discordbot.utils.Mee6Import;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.*;
import java.util.List;


/**
 * Created by rathma on 1/15/18.
 */
public class DiscordBot {

    private JDA jda;
    public Configuration config;
    public MongoDB db;

    public DiscordBot() { }
    public TimedEventManager timedEventManager;
    public int run() {

        config = new Configuration();
        db = new MongoDB(config);

        System.out.println("Attempting to login...");
        try
        {
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(config.getToken())
                    .buildBlocking();
        }
        catch (LoginException e)
        {
            System.out.println("Login Exception reached");
            e.printStackTrace();
        }
        catch (RateLimitedException e)
        {
            System.out.println("Rate Limited Exception");
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Logged in...");
        //Mee6Import.importLB(jda, db, "import.json");

        loadServices();
        loadCommands();
        timedEventManager = new TimedEventManager(jda, db);
        new Thread(timedEventManager).start();
        loadTimedEvents();


        return 0;
    }

    public void loadServices(){
        //jda.addEventListener(new MessageLogging(jda, db));
        jda.addEventListener(new CommandService(jda, db));
        jda.addEventListener(new ExperienceService(jda, db));
    }

    public void loadTimedEvents(){
        synchronized (timedEventManager.loadedEvents) {
            timedEventManager.loadTimedEvent(new GuildRankUpdate(jda, db));
            timedEventManager.loadTimedEvent(new VoiceChatExperience(jda, db));
            timedEventManager.loadTimedEvent(new VoiceChatResetDaily(jda, db));
            timedEventManager.loadTimedEvent(new WeeklyExpReset(jda, db));
            timedEventManager.loadTimedEvent(new GuildWeeklyRankUpdate(jda, db));
            //timedEventManager.loadTimedEvent(new GuildLevelRolesCheck(jda, db));
        }
    }
    /* Temp command to house loading commands */
    public void loadCommands(){
        List<Object> listenerObjects = jda.getRegisteredListeners();
        CommandService commandService=null;
        for(Object o : listenerObjects){
            if(((Service)o).serviceID=="commandservice"){
                commandService = (CommandService)o;
            }
        }
        if(commandService==null){
            commandService=new CommandService(jda, db);
            jda.addEventListener(commandService);
        }

        /* Load commands */
        commandService.loadCommand(new Prefix());
        commandService.loadCommand(new PrefixSet());

       //Experience Commands
        commandService.loadCommand(new Rank());
        commandService.loadCommand(new SetExpRatio());
        commandService.loadCommand(new ExpRatio());
        commandService.loadCommand(new Leaderboard());
        commandService.loadCommand(new ExpReset());
        commandService.loadCommand(new RankAssignAdd());
        commandService.loadCommand(new RankAssignRemove());
        commandService.loadCommand(new ExpFullreset());
        commandService.loadCommand(new ExpChannelBlock());
        commandService.loadCommand(new RankAssignList());
        commandService.loadCommand(new LevelAssignAdd());
        commandService.loadCommand(new LevelAssignRemove());
        commandService.loadCommand(new LevelAssignList());
        commandService.loadCommand(new ExpVCBlock());
        commandService.loadCommand(new ExpRoleIgnore());
        commandService.loadCommand(new ExpSet());
        commandService.loadCommand(new WeeklyLeaderboard());
        commandService.loadCommand(new MVPAssign());

    }

}