package com.rathma.discordbot.core.services;

import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.Service;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.bson.Document;

import java.util.*;


/* The goal/purpose of this service is to manage multi-server prefixes, enforce permissions, and prevent command naming conflicts */
public class CommandService extends Service
{
    private String databaseTable="CORE_COMMAND";


    public Map<Long, String> prefixes;
    public List<Command> ongoingCommands;

    public List<Command> loadedCommands;

    public JDA jda;
    public Database database;

    public CommandService(JDA jda, Database database){
        this.jda = jda;
        this.database = database;
        prefixes = new HashMap<>();
        ongoingCommands = new ArrayList<>();
        loadedCommands = new ArrayList<>();
        serviceID = "command";
        loadPrefix();

    }

    public void loadPrefix() {
        List<Document> configs = database.queryTable(databaseTable);
        if(configs==null)
            return;
        Iterator it = configs.iterator();

        while(it.hasNext()){
            Document next = (Document)it.next();
            if(next.containsKey("PREFIX")){
                prefixes.put(next.getLong("GUILD ID"), next.getString("PREFIX"));
            }
        }
    }


    public String generatePrefix(long guildID) {
        String defaultPrefix = ".";
        Document filter = new Document();
        filter.append("GUILD ID", guildID);
        Document update = new Document();
        update.append("$set", new Document().append("PREFIX", defaultPrefix));
        database.updateOne(databaseTable,filter, update);
        prefixes.put(guildID, defaultPrefix);
        return defaultPrefix;
    }
    public void updatePrefix(long guildID, String prefix){
        Document filter = new Document();
        filter.append("GUILD ID", guildID);
        Document update = new Document();
        update.append("$set", new Document().append("PREFIX", prefix));
        database.updateOne(databaseTable,filter, update);
        prefixes.put(guildID, prefix);
    }

    /* Load commands into our list to check for later. */
    public void loadCommand(Command command){
        loadedCommands.add(command);
    }

    public void onEvent(Event event) {
        if(event instanceof GuildMessageReceivedEvent) {

            GuildMessageReceivedEvent guildMessageReceivedEvent = (GuildMessageReceivedEvent)event;
            /* Check if the guild has a prefix already, or generate the default */
            if(prefixes.containsKey(guildMessageReceivedEvent.getGuild().getIdLong())){
                String pfx = prefixes.get(guildMessageReceivedEvent.getGuild().getIdLong());
                if(guildMessageReceivedEvent.getMessage().getContentRaw().startsWith(pfx)){
                    commandParse(guildMessageReceivedEvent.getMessage(), pfx);
                }
            }
            else {
                String pfx = generatePrefix(guildMessageReceivedEvent.getGuild().getIdLong());
                if(guildMessageReceivedEvent.getMessage().getContentRaw().startsWith(pfx)){
                    commandParse(guildMessageReceivedEvent.getMessage(), pfx);
                }
            }

        }
    }

    public void commandParse(Message message, String prefix){
        try {
            int matches = 0;
            String messageNoPrefix = message.getContentRaw().substring(prefix.length());
            String[] messageSplit = messageNoPrefix.split(" ");
            /* Let's grab our identifiers if they exist */
            String primaryIdentifier = messageSplit[0];
            String childIdentifier = null; //This might not be a real identifier, but rather a parameter
            if(messageSplit.length>1)
                childIdentifier = messageSplit[1];

            /* First loop will look for the number of matches and if there are any children, if we find a child then we launch, otherwise we have to reloop to the base.*/
            for (Command c : loadedCommands) {
                if(c.aliases.size()==0) {
                    if (primaryIdentifier.equals(c.identifier)) {
                        /* The primary identifier has matched. */
                        matches = matches + 1;
                        if (childIdentifier == null && c.childIdentifier == null) {
                            /* Found the base command =D */
                            ongoingCommands.add(c.getClass()
                                    .getConstructor(CommandService.class, Message.class)
                                    .newInstance(this, message));
                            return;
                        } else if (childIdentifier != null && c.childIdentifier != null && childIdentifier.equals(c.childIdentifier)) {
                            /* Found the child class */
                            ongoingCommands.add(c.getClass()
                                    .getConstructor(CommandService.class, Message.class)
                                    .newInstance(this, message));
                            return;
                        }
                    }
                }
                else{
                    for(String alias : c.aliases){
                        if (primaryIdentifier.equals(c.identifier) || primaryIdentifier.equals(alias)) {
                            /* The primary identifier has matched. */
                            matches = matches + 1;
                            if (childIdentifier == null && c.childIdentifier == null) {
                                /* Found the base command =D */
                                ongoingCommands.add(c.getClass()
                                        .getConstructor(CommandService.class, Message.class)
                                        .newInstance(this, message));
                                return;
                            } else if (childIdentifier != null && c.childIdentifier != null && childIdentifier.equals(c.childIdentifier)) {
                                /* Found the child class */
                                ongoingCommands.add(c.getClass()
                                        .getConstructor(CommandService.class, Message.class)
                                        .newInstance(this, message));
                                return;
                            }
                        }
                    }
                }
            }

            /* If we get this far then we know the child identifier is just a paramter and to go find the base class */
            for (Command c : loadedCommands){
                if(c.aliases.size()==0) {
                    if (primaryIdentifier.equals(c.identifier) && c.childIdentifier == null) {
                        ongoingCommands.add(c.getClass()
                                .getConstructor(CommandService.class, Message.class)
                                .newInstance(this, message));
                        return;
                    }
                }
                else{
                    for(String alias : c.aliases){
                        if ((primaryIdentifier.equals(c.identifier) && c.childIdentifier == null) || (primaryIdentifier.equals(alias) && c.childIdentifier == null)) {
                            ongoingCommands.add(c.getClass()
                                    .getConstructor(CommandService.class, Message.class)
                                    .newInstance(this, message));
                            return;
                        }
                    }
                }
            }
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }

}
