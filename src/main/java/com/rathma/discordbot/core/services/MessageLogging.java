package com.rathma.discordbot.core.services;

import com.rathma.discordbot.database.Database;
import com.rathma.discordbot.entities.Service;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.bson.Document;

public class MessageLogging extends Service {

    public JDA jda;
    public Database database;


    public MessageLogging(JDA jda, Database database){
        this.jda=jda;
        this.database=database;
    }

    public void logMessage(Message message){
        Document document = new Document();

        document.append("USER ID", message.getAuthor().getIdLong());
        document.append("GUILD ID", message.getGuild().getIdLong());
        document.append("CHANNEL ID", message.getChannel().getIdLong());
        document.append("CONTENTS", message.getContentRaw());

        database.insertOne("CORE_MESSAGES", document);
    }

 //   @Override
    public void onEvent(Event event) {
        if(event instanceof GuildMessageReceivedEvent){
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
            logMessage(e.getMessage());
        }
    }
}
