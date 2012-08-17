//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.IO;
import org.jdownloader.logging.LogController;

public class DatabaseConnector implements Serializable {

    private static final long             serialVersionUID = 8074213660382482620L;

    private String                        configpath       = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";

    private final HashMap<String, Object> dbdata           = new HashMap<String, Object>();

    public static final Object            LOCK             = new Object();

    private static boolean                dbshutdown       = false;

    private static Connection             con              = null;

    static {

        try {
            Class.forName("org.hsqldb.jdbcDriver").newInstance();
        } catch (final IllegalAccessException e) {
            e.printStackTrace();
        } catch (final InstantiationException e) {
            e.printStackTrace();
        } catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * return if Database is still open
     * 
     * @return
     */
    public static boolean isDatabaseShutdown() {
        return dbshutdown;
    }

    public DatabaseConnector(String config) throws SQLException {
        if (config != null) this.configpath = config;
        if (con != null) return;
        LogController.CL().finer("Loading database");
        if (new File(configpath + "database.script").exists()) {
            if (!checkDatabaseHeader()) { throw new SQLException("Database broken!");

            }
        }

        con = DriverManager.getConnection("jdbc:hsqldb:file:" + configpath + "database;shutdown=true", "sa", "");

        con.setAutoCommit(true);
        con.createStatement().executeUpdate("SET LOGSIZE 1");

        if (!new File(configpath + "database.script").exists()) {
            LogController.CL().finer("No CONFIGURATION database found. Creating new one.");

            con.createStatement().executeUpdate("CREATE TABLE config (name VARCHAR(256), obj OTHER)");
            con.createStatement().executeUpdate("CREATE TABLE links (name VARCHAR(256), obj OTHER)");

            final PreparedStatement pst = con.prepareStatement("INSERT INTO config VALUES (?,?)");
            LogController.CL().finer("Starting database wrapper");
            // cfg file conversion
            for (final String tmppath : new File(configpath).list()) {
                try {
                    if (tmppath.endsWith(".cfg")) {
                        LogController.CL().finest("Wrapping " + tmppath);

                        final Object props = JDIO.loadObject(JDUtilities.getResourceFile("config/" + tmppath), false);

                        if (props != null) {
                            pst.setString(1, tmppath.split(".cfg")[0]);
                            pst.setObject(2, props);
                            pst.execute();
                        }
                    }
                } catch (final Exception e) {
                    LogController.CL().log(e);
                }
            }
        }
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                shutdownDatabase();
            }

            @Override
            public int getHookPriority() {
                return 0;
            }

            @Override
            public String toString() {
                return "save database...";
            }
        });
    }

    /**
     * Constructor
     * 
     * @throws Exception
     */
    public DatabaseConnector() throws SQLException {
        this(null);
    }

    /**
     * Checks the database of inconsistency
     * 
     * @throws IOException
     */
    private boolean checkDatabaseHeader() {
        LogController.CL().finer("Checking database");
        final File f = new File(configpath + "database.script");
        if (!f.exists()) return true;
        boolean databaseok = true;

        BufferedInputStream in = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            in = new BufferedInputStream(fis);
            String line = "";
            int counter = 0;
            byte[] buffer = new byte[100];
            main: while (counter < 7) {
                line = IO.readLine(in, buffer);
                if (line == null) {
                    databaseok = false;
                    break main;
                }
                switch (counter) {
                case 0:
                    if (!line.equals("CREATE SCHEMA PUBLIC AUTHORIZATION DBA")) {
                        databaseok = false;
                    }
                    break;
                case 1:
                    if (!line.equals("CREATE MEMORY TABLE CONFIG(NAME VARCHAR(256),OBJ OBJECT)")) {
                        databaseok = false;
                    }
                    break;
                case 2:
                    if (!line.equals("CREATE MEMORY TABLE LINKS(NAME VARCHAR(256),OBJ OBJECT)")) {
                        databaseok = false;
                    }
                    break;
                case 3:
                    if (!line.equals("CREATE USER SA PASSWORD \"\"")) {
                        databaseok = false;
                    }
                    break;
                case 4:
                    if (!line.equals("GRANT DBA TO SA")) {
                        databaseok = false;
                    }
                    break;
                case 5:
                    if (!line.equals("SET WRITE_DELAY 10")) {
                        databaseok = false;
                    }
                    break;
                case 6:
                    if (!line.equals("SET SCHEMA PUBLIC")) {
                        databaseok = false;
                    }
                    break;
                }
                counter++;
            }

            while (((line = IO.readLine(in, buffer)) != null)) {
                if (!line.startsWith("INSERT INTO")) {
                    databaseok = false;
                    break;
                }
            }

        } catch (final FileNotFoundException e) {
            databaseok = false;
        } catch (final IOException e) {
            databaseok = false;
        } finally {
            try {
                in.close();
            } catch (final Throwable e1) {
            }
            try {
                fis.close();
            } catch (final Throwable e1) {
            }
        }
        return databaseok;
    }

    public synchronized boolean hasData(final String name) {
        synchronized (LOCK) {
            if (!isDatabaseShutdown()) {
                Statement statement = null;
                ResultSet rs = null;
                try {
                    // try to init the table
                    statement = con.createStatement();
                    rs = statement.executeQuery("SELECT * FROM config WHERE name = '" + name + "'");
                    if (rs.next()) { return true; }
                } catch (final Exception e) {
                } finally {
                    try {
                        rs.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        statement.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            return false;
        }
    }

    public synchronized void removeData(final String name) {
        synchronized (LOCK) {
            if (!isDatabaseShutdown()) {
                dbdata.remove(name);
                Statement statement = null;
                ResultSet rs = null;
                try {
                    statement = con.createStatement();
                    rs = statement.executeQuery("DELETE FROM config WHERE name = '" + name + "'");
                } catch (final Exception e) {
                } finally {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (final Throwable e) {
                        }
                    }
                    if (statement != null) {
                        try {
                            statement.close();
                        } catch (final Throwable e) {
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a CONFIGURATION
     */
    public synchronized Object getData(final String name) {
        synchronized (LOCK) {
            Object ret = null;
            if (!isDatabaseShutdown()) {
                ret = dbdata.get(name);
                Statement statement = null;
                ResultSet rs = null;
                try {
                    if (ret == null) {
                        // try to init the table
                        statement = con.createStatement();
                        rs = statement.executeQuery("SELECT * FROM config WHERE name = '" + name + "'");
                        if (rs.next()) {
                            ret = rs.getObject(2);
                            dbdata.put(rs.getString(1), ret);
                        }
                    }
                } catch (final Exception e) {
                    LogController.CL().warning("Database not available. " + name);
                } finally {
                    try {
                        rs.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        statement.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            return ret;
        }
    }

    /**
     * Returns all Subconfigurations
     * 
     * @return
     */
    public java.util.List<SubConfiguration> getSubConfigurationKeys() {
        synchronized (LOCK) {
            if (isDatabaseShutdown()) return null;
            final java.util.List<SubConfiguration> ret = new ArrayList<SubConfiguration>();
            Statement statement = null;
            ResultSet rs = null;

            try {
                statement = con.createStatement();
                rs = statement.executeQuery("SELECT * FROM config");
                while (rs.next()) {
                    final SubConfiguration conf = SubConfiguration.getConfig(rs.getString(1));
                    HashMap<String, Object> props = conf.getProperties();
                    if (props != null && props.size() > 0) {
                        ret.add(conf);
                    }
                }
            } catch (final Throwable e) {
                LogController.CL().log(e);
            } finally {
                try {
                    rs.close();
                } catch (final Throwable e) {
                }
                try {
                    statement.close();
                } catch (final Throwable e) {
                }
            }
            return ret;
        }
    }

    /**
     * Saves a CONFIGURATION into the database
     */
    public void saveConfiguration(final String name, final Object data) {
    }

    /**
     * Shutdowns the database
     */
    public void shutdownDatabase() {
        synchronized (LOCK) {
            if (!isDatabaseShutdown()) {
                try {
                    dbshutdown = true;
                    con.close();
                } catch (final Throwable e) {
                }
            }
        }
    }

    /**
     * Returns the saved linklist
     */
    public Object getLinks() {
        synchronized (LOCK) {
            if (!isDatabaseShutdown()) {

                Statement statement = null;
                ResultSet rs = null;

                try {
                    statement = con.createStatement();
                    rs = statement.executeQuery("SELECT * FROM links");
                    rs.next();
                    return rs.getObject(2);
                } catch (final Throwable e) {
                    LogController.CL().warning("Database not available.");
                } finally {
                    try {
                        rs.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        statement.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            return null;
        }
    }

    public void save() {
    }

    /**
     * Returns the connection to the database
     */
    public Connection getDatabaseConnection() {
        return con;
    }

}