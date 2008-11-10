//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.config;

import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

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

    /**
     * Constructor
     */
    @SuppressWarnings("unused")
    public DatabaseConnector() {
        try {
            logger.info("Loading database");
            // new Exception().printStackTrace();
            con = DriverManager.getConnection("jdbc:hsqldb:file:" + configpath + "database;shutdown=true", "sa", "");
            con.setAutoCommit(true);
            con.createStatement().executeUpdate("SET LOGSIZE 1");
            boolean checktables = true;
            try {
                ResultSet rs = con.createStatement().executeQuery("SELECT COUNT(name) FROM config WHERE name = 'dummy'");
            } catch (Exception e) {
                logger.warning("Database tables error");
                e.printStackTrace();
                checktables = false;
            }
            if (!new File(configpath + "database.script").exists() | !checktables) {
                logger.info("No configuration database found. Creating new one.");

                con.createStatement().executeUpdate("CREATE TABLE config (name VARCHAR(256), obj OTHER)");
                con.createStatement().executeUpdate("CREATE TABLE links (name VARCHAR(256), obj OTHER)");

                PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
                logger.info("Starting database wrapper");

                File f = null;
                for (String tmppath : new File(configpath).list()) {
                    try {
                        if (tmppath.endsWith(".cfg")) {
                            logger.finest("Wrapping " + tmppath);

                            Object props = JDUtilities.loadObject(null, f = JDUtilities.getResourceFile("config/" + tmppath), false);

                            if (props != null) {
                                pst.setString(1, tmppath.split(".cfg")[0]);
                                pst.setObject(2, props);
                                pst.execute();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM config");
            String str;
            Object obj;
            while (rs.next()) {
                try {
                    str = rs.getString(1);
                    obj = rs.getObject(2);

                    dbdata.put(str, obj);
                    System.out.println(str);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a configuration
     */
    public Object getData(String name) {
        return dbdata.get(name);
    }

    /**
     * Saves a configuration into the database
     */
    public void saveConfiguration(String name, Object data) {
        dbdata.put(name, data);
        try {
            ResultSet rs = con.createStatement().executeQuery("SELECT COUNT(name) FROM config WHERE name = '" + name + "'");
            rs.next();
            if (rs.getInt(1) > 0) {
                PreparedStatement pst = con.prepareStatement("UPDATE config SET obj = ? WHERE name = '" + name + "'");
                pst.setObject(1, data);
                pst.execute();
            } else {
                PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
                pst.setString(1, name);
                pst.setObject(2, data);
                pst.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shutdowns the database
     */
    public void shutdownDatabase() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the saved linklist
     */
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

    /**
     * Saves the linklist into the database
     */
    public void saveLinks(Object obj) {
        try {
            if (getLinks() == null) {
                PreparedStatement pst = con.prepareStatement("INSERT INTO links VALUES (?,?)");
                pst.setString(1, "links");
                pst.setObject(2, obj);
                pst.execute();
            } else {
                PreparedStatement pst = con.prepareStatement("UPDATE links SET obj=? WHERE name='links'");
                pst.setObject(1, obj);
                pst.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the connection to the database
     */
    public Connection getDatabaseConnection() {
        return con;
    }
}