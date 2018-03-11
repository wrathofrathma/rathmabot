package com.rathma.discordbot.entities;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.database.Database;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.bson.Document;

import java.util.List;
import java.util.Map;

public abstract class Command {
    public Database database;
    public JDA jda;
    public CommandService commandService;

    /* Primary identifier followed by child. Example would be as such
     * .prefix set !
     * . would be the prefix, prefix would be the primary identifier, set would be the child identifier to specify which command to run.
     */
    public String identifier;
    public String childIdentifier;
    public List<String> aliases;
    public Map<Long, List<String>> requiredRoles;
    public Map<Long, Permission> requiredUserPermissions;
    public Permission defaultRequiredUserPermission;
    public Permission requiredPermissions;
    /** TODO Set/Get permissions required. If null, then anyone can use.
     * Load Settings: Required Roles, Required Permissions, Cooldown, Command enabled.
     * Required Permissions: Default permissions, load altered from database if exist, set new altered permission required. If null, then anyone can use.
     * Required Roles: Required roles, similar to permissions. If null then anyone can use.
     * Cooldown: Cooldown per server
     * Command Enabled: Enabled per server?
     */

    /* These are the methods that will run when called */
    public void run(Event event){};
    public void run(GuildMessageReceivedEvent event){};
    public void run(Message message){};

    public boolean checkUserPermissions(Message message){
        if(defaultRequiredUserPermission==null)
            return true;
        Member member = message.getMember();
        TextChannel channel = message.getTextChannel();
        if(member.getPermissions().contains(defaultRequiredUserPermission)){
            return true;
        }
        else{
            if(checkChannelWritePermission(message.getTextChannel()))
                channel.sendMessage("<@"+message.getAuthor().getId() + ">, you don't have the required permissions to use this command").queue();
            return false;
        }
    }
    /* Strips the alias & prefix */
    public String getStrippedParameters(long guildID, String command){
        String prefix = commandService.prefixes.get(guildID);
        String commandNoPrefix = command.substring(prefix.length());
        String[] commandArray = commandNoPrefix.split(" " );

        /* If the array is only one thing long, then it has no parameters */
        if(commandArray.length==1){
            return "";
        }

        if(aliases.size()==0 || commandArray[0].equals(identifier)){
            /* Parse normally */
            if(childIdentifier==null){
                /* Primary identifier with an argument */
                return commandNoPrefix.substring(identifier.length()+1);
            }
            else{
                /* Primary identifier with a child */
                if(commandArray.length>2)
                    return commandNoPrefix.substring(identifier.length()+childIdentifier.length()+2);
                else
                    return commandNoPrefix.substring(identifier.length()+childIdentifier.length()+1);
            }
        }

        else{
            /* Well, now we just need to check for an alias */
            for(String alias : aliases){
                if(commandArray[0].equals(alias)){
                    if(childIdentifier==null){
                        /* Matched alias has an argument */
                        return commandNoPrefix.substring(alias.length() + 1);
                    }
                    else{
                        /* Matched alias with a child */
                        if(commandArray.length>2)
                            return commandNoPrefix.substring(identifier.length()+childIdentifier.length()+2);
                        else
                            return commandNoPrefix.substring(identifier.length()+childIdentifier.length()+1);
                    }

                }
            }
        }
        return "";
    }

    public boolean checkBotPermissions(Message message){
        if(requiredPermissions==null)
            return true;
        if(message.getGuild().getSelfMember().getPermissions().contains(requiredPermissions))
            return true;
        if(checkChannelWritePermission(message.getTextChannel()))
            message.getChannel().sendMessage("I do not have the required (" + requiredPermissions.getName() + ") permission to execute this command.").queue();
        return false;
    }

    public static boolean checkChannelWritePermission(TextChannel channel){
        if(channel.getGuild().getSelfMember().getPermissions(channel).contains(Permission.MESSAGE_WRITE))
            return true;
        System.out.println("Bot cannot write to channel");
        return false;
    }
}
