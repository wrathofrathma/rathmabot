package com.rathma.discordbot.entities;

/* Services will represent services that run in the background that aren't necessarily commands
 * Examples: Experience counter, Message logging, etc.
 */

import net.dv8tion.jda.core.hooks.EventListener;

public abstract class Service implements EventListener{
    public String serviceID;
}
