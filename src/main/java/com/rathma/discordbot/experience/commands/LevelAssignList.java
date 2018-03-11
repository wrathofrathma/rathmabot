package com.rathma.discordbot.experience.commands;

import com.rathma.discordbot.core.services.CommandService;
import com.rathma.discordbot.entities.Command;
import com.rathma.discordbot.experience.services.ExperienceService;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LevelAssignList extends Command {
    public LevelAssignList(CommandService commandService, Message message) {
        identifier = "levelassign";
        childIdentifier = "list";
        requiredPermissions = null;
        defaultRequiredUserPermission = null;
        this.jda = commandService.jda;
        this.database = commandService.database;
        this.commandService = commandService;
        aliases = new ArrayList<>();

        run(message);
    }

    public LevelAssignList() {
        identifier = "levelassign";
        childIdentifier = "list";
        requiredPermissions = null;
        defaultRequiredUserPermission=null;
        aliases = new ArrayList<>();
    }

    public MessageEmbed createLevelAssignList(long guildID, Map<Long, Integer> levelRoles){
        Guild guild = jda.getGuildById(guildID);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLUE);
        embedBuilder.setDescription("**" + guild.getName() + "'s roles auto-assigned by level.**");
        String roles = "";
        String levels = "";
        for(Long key : levelRoles.keySet()){
            Role r = guild.getRoleById(key);
            if(r!=null){
                roles = roles.concat(r.getName() + "\n");
                levels = levels.concat(levelRoles.get(key).toString()+"\n");
            }
        }
        embedBuilder.addField("Roles", roles, true);
        embedBuilder.addField("Level Requirement", levels, true);
        return embedBuilder.build();
    }

    public void run(Message message){
        if(!checkUserPermissions(message)) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        if(!checkBotPermissions(message)) {
            commandService.ongoingCommands.remove(this);
            return;
        }
        boolean writePermissions = checkChannelWritePermission(message.getTextChannel());
        long guildID = message.getGuild().getIdLong();

        if(!writePermissions){
            commandService.ongoingCommands.remove(this);
            return;
        }
        List<Object> listeners = jda.getRegisteredListeners();
        ExperienceService experienceService=null;
        for(Object o : listeners){
            if(o instanceof ExperienceService){
                experienceService = (ExperienceService)o;
            }
        }
        if(experienceService==null){
            commandService.ongoingCommands.remove(this);
            return;
        }
        if(!experienceService.levelRoles.containsKey(guildID)){
            commandService.ongoingCommands.remove(this);
            return;
        }

        MessageEmbed messageEmbed = createLevelAssignList(guildID, experienceService.levelRoles.get(guildID));
        if(messageEmbed!=null)
            message.getChannel().sendMessage(messageEmbed).queue();
        commandService.ongoingCommands.remove(this);
    }

}
