package com.rathma.discordbot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by rathma on 1/16/18.
 */
public class Configuration {

    private String token="";
    private String dbUser="";
    private String dbPass="";
    private String dbServer="";
    private String dbName="";
    private int dbPort;

    Configuration() {
        loadConfig();
    }

    public int loadConfig() {
        System.out.println("Loading configuration...");
        try
        {
            JsonParser parser = new JsonParser();
            JsonObject obj = (JsonObject) parser.parse(new FileReader("config.json"));

            token  = obj.get("Token").getAsString();
            dbName = obj.get("DBName").getAsString().toUpperCase();
            dbServer = obj.get("DBServer").getAsString();
            dbUser = obj.get("DBUser").getAsString();
            dbPass = obj.get("DBPass").getAsString();
            dbPort = obj.get("DBPort").getAsInt();
        }
        catch (FileNotFoundException e)
        {
            System.out.println("Couldn't locate config.json");
            return 1;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("Configuration loaded successfully...");
        return 0;
    }

    public String getToken() {
        return token;
    }
    public String getDbName() {
        return dbName;
    }
    public String getDbPass() {
        return dbPass;
    }
    public String getDbUser() {
        return dbUser;
    }
    public String getDbServer() { return dbServer; }
    public int getDbPort() { return dbPort; }
}
