package com.rathma.discordbot.database;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.UpdateOptions;
import com.rathma.discordbot.Configuration;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MongoDB implements Database {

    /* Necessary components to make MongoDB work */
    private MongoDatabase database;
    private MongoClient mongoClient;
    private Configuration config;

    public MongoDB (Configuration configuration){
        this.config=configuration;

        /* Connecting to the database */
        /* TODO - Add authentication in the future */
        try {

            mongoClient = new MongoClient(config.getDbServer(), config.getDbPort());

            database = mongoClient.getDatabase(config.getDbName());

            System.out.println("Connected to database.");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public long getTableSize(String tableName) {
        MongoCollection table = database.getCollection(tableName);
        return table.count();
    }

    @Override
    public List<Document> queryTable(String tableName) {
        MongoCollection collection = database.getCollection(tableName);
        List<Document> resultList=new ArrayList<>();

        FindIterable<Document> iterDoc = collection.find();
        Iterator it = iterDoc.iterator();

        while(it.hasNext()){
            resultList.add((Document) it.next());
        }
        if(resultList.size()==0)
            return null;
        return resultList;    }

    @Override
    public Document queryField(String tableName, Bson filter, Bson projection) {

        MongoCollection collection = database.getCollection(tableName);
        FindIterable<Document> iterDoc = collection.find(filter).projection(projection);
        Iterator it = iterDoc.iterator();
        if(it.hasNext()){
            return (Document) it.next();
        }
        return null;    }

    /* Will return the first query result */
    @Override
    public Document queryOne(String tableName, Bson filter) {
        MongoCollection collection = database.getCollection(tableName);
        FindIterable<Document> iterDoc = collection.find(filter);
        Iterator it = iterDoc.iterator();
        if(it.hasNext()){
            return (Document) it.next();
        }
        return null;
    }

    @Override
    public List<Document> queryMany(String tableName, Bson filter) {
        MongoCollection collection = database.getCollection(tableName);
        List<Document> resultList=new ArrayList<>();

        FindIterable<Document> iterDoc = collection.find(filter);
        Iterator it = iterDoc.iterator();

        while(it.hasNext()){
            resultList.add((Document) it.next());
        }
        if(resultList.size()==0)
            return null;
        return resultList;
    }

    @Override
    public List<String> getTables() {
        List<String> tables = new ArrayList<>();
        MongoIterable mongoIter = database.listCollectionNames();
        Iterator it = mongoIter.iterator();
        while(it.hasNext()){
            tables.add(it.next().toString());
        }
        if(tables.size()==0)
            return null;
        return tables;
    }

    @Override
    public void insertOne(String tableName, Document document) {
        MongoCollection collection = database.getCollection(tableName);
        collection.insertOne(document);
    }

    @Override
    public void insertMany(String tableName, List<Document> documents) {
        MongoCollection collection = database.getCollection(tableName);
        collection.insertMany(documents);
    }

    @Override
    public void updateOne(String tableName, Bson filter, Bson update) {
        MongoCollection collection = database.getCollection(tableName);
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        collection.updateOne(filter, update, options);
    }

    @Override
    public void replaceOne(String tableName, Bson filter, Bson replacement) {
        MongoCollection collection = database.getCollection(tableName);
        UpdateOptions options = new UpdateOptions();
        options.upsert(true);
        collection.replaceOne(filter, replacement, options);
    }

    @Override
    public void updateMany(String tableName, Bson filter, Bson update) {
        MongoCollection collection = database.getCollection(tableName);
        collection.updateMany(filter, update);
    }

    @Override
    public void dropTable(String tableName) {
        database.getCollection(tableName).drop();
    }

    @Override
    public void deleteOne(String tableName, Bson filter) {
        MongoCollection collection = database.getCollection(tableName);
        collection.deleteOne(filter);
    }

    @Override
    public void deleteMany(String tableName, Bson filter) {
        MongoCollection collection = database.getCollection(tableName);
        collection.deleteMany(filter);
    }
}

/*

Core Module
------------------------
Messages Table(CORE_MESSAGES)
    USER ID
    SERVER ID : BIGINT,
    CHANNEL ID : BIGINT,
    TIMESTAMP :  TIMESTAMP,
    AUTHOR ID : BIGINT,
    CONTENTS : MEDIUMTEXT

Owner ID Table(CORE_OWNERS)
    Server ID
        Owner IDs

Command Module
--------------------------------------
Command Table(CORE_COMMAND)
    Server ID
        Command[]
            Cooldown Timer
            Command Enabled
            Role Required

Experience Module
-------------------------------
Experience Table(EXP_EXP)
    AuthorID
        Server ID
            Experience
                 Timestamp of last message

Discord.me Module
-------------------------
Discordme Table(DISCORDME_SERVERS)
    Server ID
        username
        password
        channel

Roles Module
-----------------------------------
Roles Table(ROLES_ROLES)
    Author ID
        Server ID
             [ "role1", "role2", "role3"]

Stats Module
----------------------------------
Stats Table(STATS_STATS)
    Author ID
        Timestamp of first message


Currency Module
------------------------------------
Currency Table(CURRENCY_CURRENCY)
    User ID
        Currency Amount

Config Table(CURRENCY_CONFIG)
    Currency Name
    Currency Name Plural



 */