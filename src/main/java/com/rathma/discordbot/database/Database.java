package com.rathma.discordbot.database;


import com.rathma.discordbot.Configuration;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

/**
 * An interface our bot will use to access whatever database we decide to go with. Realistically we only need to abstract the get/insert requests, but some methods are for convenience.
 */
public interface Database {
    /* Bot Config, should have database connection information */
    public Configuration config=null;

    /** Fetch Requests we might want **/

    /* Returns size of table/collection */
    public long getTableSize(String tableName);

    /* Returns org.bson.Document with the result or null if none */
    public Document queryOne(String tableName, Bson filter);

    /* Returns a list of org.bson.Documents or null if none */
    public List<Document> queryMany(String tableName, Bson filter);

    public List<Document> queryTable(String tableName);

    public Document queryField(String tableName, Bson filter, Bson projection);

    /* Returns a list of tables in the database */
    public List<String> getTables();

    /**  Insertions **/

    /* Returns number of rows effected maybe */
    public void insertOne(String tableName, Document document);
    public void insertMany(String tableName, List<Document> documents);
    public void updateOne(String tableName, Bson filter, Bson update);
    public void updateMany(String tableName, Bson filter, Bson update);
    public void replaceOne(String tableName, Bson filter, Bson replacement);

    /** Drops **/
    public void dropTable(String tableName);

    /** Deletes **/
    public void deleteOne(String tableName, Bson filter);
    public void deleteMany(String tableName, Bson filter);
}