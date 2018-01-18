package com.rathma.discordbot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.io.FileReader;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Created by rathma on 1/16/18.
 */
public class Database {
    public Configuration config;
    public Connection connection;

    Database(Configuration configuration) {
        this.config = configuration;

        /* Connecting to database and validating data structure */
        connection = connect();
        if (!checkDBExists(config.getDbName())) {
            createDatabase();
        }
        selectDatabase(config.getDbName().toUpperCase());
        validateTables();
    }

    public Connection connect() {
        System.out.println("Connecting to database...");
        try {
            Connection connection = null;

            Properties connectionProps = new Properties();
            connectionProps.put("user", config.getDbUser());
            connectionProps.put("password", config.getDbPass());

            connection = DriverManager.getConnection("jdbc:mysql://" +
                    config.getDbServer() + ":" + config.getDbPort() + "/", connectionProps);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    //SQL Queries/Updates
    public boolean execute(String sql) {
        Statement statement = null;
        boolean result = true;
        try {
            statement = connection.createStatement();
            result = statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // Catch and fix SQL stuff
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public int executeUpdate(String sql) {
        Statement statement = null;
        int rowsAffected = 0;
        try {
            statement = connection.createStatement();
            rowsAffected = statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // Catch and fix SQL stuff
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rowsAffected;
    }

    public ResultSet executeQuery(String sql) {
        ResultSet resultSet = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // Catch and fix SQL stuff
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return resultSet;
    }

    //TABLE operations
    public int dropTable(String table) {
        System.out.println("Dropping table " + table.toUpperCase());
        String sql = "DROP TABLE " + table.toUpperCase();
        return executeUpdate(sql);
    }

    public boolean checkTableExists(String table) {
        try {
            ResultSet resultSet = connection.getMetaData().getTables(config.getDbName(), null, table, null);
            if (resultSet.next()) {
                //If the result set has any value at all, then it found a table with that name.
                System.out.println("Found table: " + table);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Table not found: " + table);
        return false;
    }

    public int createTable(String tableName, String[] values) {
        System.out.println("Creating database table: " + tableName.toUpperCase());
        String sql = "CREATE TABLE " + tableName.toUpperCase() + " (";
        for (int i = 0; i < values.length; i++) {
            if (i != values.length - 1)
                sql = sql.concat(values[i] + ",");
            else
                sql = sql.concat(values[i] + ");");
        }
        System.out.println(sql);
        return executeUpdate(sql);
    }

    //DB manipulation
    public boolean selectDatabase(String database) {
        return execute("USE " + database.toUpperCase());
    }

    public boolean checkDBExists(String dbName) {

        try {
            ResultSet resultSet = connection.getMetaData().getCatalogs();
            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);
                if (databaseName.equals(dbName)) {
                    System.out.println("Found database...");
                    return true;
                }
            }
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Could not find database...");
        return false;
    }

    public int createDatabase() {
        String sql = "CREATE DATABASE " + config.getDbName();
        return executeUpdate(sql);
    }
    //Add message to DB

    // Data validation
    public int validateTables() {

        if (checkTableExists("MESSAGES")) {
            //validate columns
        } else {
            createTable("MESSAGES", new String[]{
                    "SERVERID BIGINT",
                    "CHANNELID BIGINT",
                    "AUTHORID BIGINT",
                    "TIMESTAMP TIMESTAMP",
                    "CONTENTS MEDIUMTEXT"
            });
        }

        if (checkTableExists("USERS")) {
            //Validate columns
        } else {
            createTable("USERS", new String[]{
                    "USERID BIGINT",
                    "ROLES MEDIUMTEXT", //Per server by ID. That's why it has to be a json.
                    "EXPERIENCE MEDIUMTEXT", //Per server by ID. That's why it has to be a json.
                    "FIRSTMESSAGE MEDIUMTEXT", //Per server by ID. That's why it has to be a json.
                    "LASTMESSAGE MEDIUMTEXT" //Per server by ID. That's why it has to be a json.
            });
        }
        if (checkTableExists("CONFIGURATION")) {
            //validate columns
        } else {
            //Later we definitely want to add bump credentials, but we need encryption for that.
            createTable("CONFIGURATION", new String[]{
                    "SERVER BIGINT",
                    "OWNERIDS MEDIUMTEXT",
                    "ROLES MEDIUMTEXT",
                    "COMMANDS MEDIUMTEXT",
                    "CHANNELS MEDIUMTEXT",
                    "EXPEREINCE MEDIUMTEXT",
                    "DISCORDME MEDIUMTEXT"
            });
        }
        return 0;
    }
    public JsonElement getJsonElement(String key, String jsonText){
        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject) parser.parse(jsonText);
        return obj.get(key);
    }
    public String setJsonElement(String keyvalue, String jsonString){
        JsonParser parser = new JsonParser();
        JsonObject jsonText = (JsonObject) parser.parse(jsonString);
        JsonObject keyValues = (JsonObject) parser.parse(keyvalue);

        Set<String> keySet = keyValues.keySet(); //We need to change to a set to access specifically the keys.
        Iterator<String> iterator = keySet.iterator(); //Apparently Set<> types don't like array style iteration. Although iterators seem cool and safe.
        String key=""; //Our individual key used during looping
        while(iterator.hasNext()){
            key=iterator.next();
            System.out.println("Adding: K - " + key + " V - " + keyValues.get(key).getAsString());

            jsonText.add(key, keyValues.get(key));
        }
        System.out.println(jsonText.getAsString());
        return jsonText.getAsString();
    }

    public int insertUser(Member member) {
        PreparedStatement statement = null;
        int rowsAffected = 0;
        try {
            statement = connection.prepareStatement("INSERT INTO USERS (USERID ");

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return rowsAffected;
    }
    /* Prepared statements allow us to not worry much about input sanitisation. */
    public int logMessage(Message message) {
        PreparedStatement statement = null;
        int rowsAffected=0;
        try {
          //  Timestamp creationTime = timeConversion(message.getCreationTime());

            statement = connection.prepareStatement("INSERT INTO MESSAGES VALUES (?, ?, ?, ?, ?)");
            statement.setLong(1, message.getGuild().getIdLong());
            statement.setLong(2, message.getChannel().getIdLong());
            statement.setLong(3, message.getAuthor().getIdLong());
            statement.setTimestamp(4, Timestamp.valueOf(message.getCreationTime().toLocalDateTime()));
            statement.setString(5, message.getContentRaw());
            rowsAffected = statement.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
        }
        finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // Catch and fix SQL stuff
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rowsAffected;
    }
}
