package jd.config;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Logger;
import java.sql.Connection; 
import java.sql.DriverManager; 
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.SQLException;

import jd.utils.JDUtilities;

public class DatabaseConnector implements Serializable {

    private static final long serialVersionUID = 8074213660382482620L;
    
    private static Logger logger = JDUtilities.getLogger();
    
    private static String configpath = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";
    
    private HashMap<String, Object> dbdata = new HashMap<String, Object>();
    
    private Connection con = null;
    
    static { 
        try {
            Class.forName("org.hsqldb.jdbcDriver"); 
        } catch (ClassNotFoundException e) { 
            throw new Error(e); 
        } 
    }
    
    public DatabaseConnector() {
        try {
            logger.info("Loading database");
            
            con = DriverManager.getConnection("jdbc:hsqldb:file:" + configpath + "database;shutdown=true", "sa", "");
            con.setAutoCommit(true);
            
            if(!new File(configpath + "database.script").exists()) {
                logger.info("No configuration database found. Creating new one.");
                
                con.createStatement().executeUpdate("CREATE TABLE config (name VARCHAR(256), obj OTHER)"); 
                con.createStatement().executeUpdate("CREATE TABLE links (name VARCHAR(256), obj OTHER)");
                
                PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
                logger.info("Starting database wrapper");
                File f = null;
                for(String tmppath : new File(configpath).list()) {
                    try {
                        if(tmppath.endsWith(".cfg")) {
                            logger.finest("Wrapping " + tmppath);
                            
                            Object props = JDUtilities.loadObject(null, f = JDUtilities.getResourceFile("config/" + tmppath), false);
                            
                            if (props != null) {
                                pst.setString(1, tmppath.split(".cfg")[0]);
                                pst.setObject(2, props);
                                pst.execute();
                            }
                            //f.delete();
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM config");
            while(rs.next()) {
                System.out.println("reading " + rs.getString(1) + " " + rs.getObject(2).getClass().getName());
                //dbdata.put(rs.getString(1), rs.getObject(2));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Object getData(String name) {
        return dbdata.get(name);
    }
    
    public void saveConfiguration(String name, Object data) {
        System.out.println("save " + name);
        try {
            if(dbdata.containsKey(name)) {
                System.out.println("update " + name);
                System.out.println(dbdata.get(name).getClass().getName());
                PreparedStatement pst = con.prepareStatement("UPDATE config SET obj = ? WHERE name = '" + name + "'");
                pst.setObject(1, data);
                pst.execute();
            }
            else {
                System.out.println("insert " + name);
                PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
                pst.setString(1, name);
                pst.setObject(2, data);
                pst.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        dbdata.put(name, data);
    }
    
    public void shutdownDatabase() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public Object getLinks() {
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM links");
            rs.next();
            return rs.getObject(2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void saveLinks(Object obj) {
        try {
            if(getLinks() == null) {
                PreparedStatement pst = con.prepareStatement("INSERT INTO links VALUES (?,?)");
                pst.setString(1, "links");
                pst.setObject(2, obj);
                pst.execute();
            }
            else {
                PreparedStatement pst = con.prepareStatement("UPDATE links SET obj=? WHERE name='links'");
                pst.setObject(1, obj);
                pst.execute();
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}