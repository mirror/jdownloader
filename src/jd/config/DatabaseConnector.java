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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.SQLException;
import java.util.HashMap;

import jd.utils.JDUtilities;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.net.LimitedInputStream;
import org.jdownloader.logging.LogController;
import org.seamless.util.io.IO;
import org.tmatesoft.svn.core.internal.util.CountingInputStream;

public class DatabaseConnector {

    private HashMap<String, Long[]> objectIndices = new HashMap<String, Long[]>();

    private String                  configpath    = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";

    private static boolean          dbshutdown    = false;

    /**
     * return if Database is still open
     * 
     * @return
     */
    public static boolean isDatabaseShutdown() {
        return dbshutdown;
    }

    public static void main(String[] args) throws SQLException {
        new DatabaseConnector();
    }

    public DatabaseConnector() throws SQLException {
        LogController.CL().finer("Loading database");
        if (!new File(configpath + "database.script").exists()) throw new SQLException("Database does not exist!");
        CountingInputStream fis = null;
        String[] searchForEntries = new String[] { "INSERT INTO CONFIG VALUES('", "INSERT INTO LINKS VALUES('" };
        String[] searchForEntriesType = new String[] { "config_", "links_" };
        String searchForObject = ",'aced";
        for (int searchForEntriesIndex = 0; searchForEntriesIndex < searchForEntries.length; searchForEntriesIndex++) {
            String searchForEntry = searchForEntries[searchForEntriesIndex];
            try {
                fis = new CountingInputStream(new BufferedInputStream(new FileInputStream(configpath + "database.script"), 8192));
                int read = -1;
                int entryIndex = 0;
                insertLoop: while ((read = fis.read()) != -1) {
                    if (searchForEntry.charAt(entryIndex) == read) {
                        entryIndex++;
                    } else {
                        entryIndex = 0;
                        continue insertLoop;
                    }
                    if (entryIndex == searchForEntry.length() - 1) {
                        entryIndex = 0;
                        int objectIndex = 0;
                        long objectStartIndex = 0;
                        long objectStopIndex = -1;
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        nameLoop: while ((read = fis.read()) != -1) {
                            bos.write(read);
                            if (searchForObject.charAt(objectIndex) == read) {
                                objectIndex++;
                            } else {
                                objectIndex = 0;
                                continue nameLoop;
                            }
                            if (objectIndex == searchForObject.length() - 1) {
                                String name = new String(bos.toByteArray(), 1, bos.size() - (searchForObject.length() + 1));
                                objectStartIndex = fis.getBytesRead() - 3;
                                while ((read = fis.read()) != -1) {
                                    if (read == '\'') {
                                        objectStopIndex = fis.getBytesRead() - 1;
                                        break;
                                    }
                                }
                                if (objectStopIndex == -1) { throw new WTFException(); }
                                objectIndices.put(searchForEntriesType[searchForEntriesIndex] + name, new Long[] { objectStartIndex, objectStopIndex });
                                continue insertLoop;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new SQLException(e);
            } finally {
                try {
                    fis.close();
                } catch (final Throwable e) {
                }
            }
        }
    }

    public synchronized boolean hasData(final String name) {
        return objectIndices.containsKey("config_" + name);
    }

    public synchronized void removeData(final String name) {
        objectIndices.remove("config_" + name);
    }

    /**
     * Returns a CONFIGURATION
     */
    public Object getData(final String name) {
        Long[] indices = null;
        synchronized (this) {
            indices = objectIndices.get("config_" + name);
        }
        Object ret = null;
        if (indices != null) {
            ret = readObject(indices[0], indices[1]);
        }
        return ret;
    }

    private Object readObject(long startIndex, long stopIndex) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(configpath + "database.script");
            fis.skip(startIndex);
            LimitedInputStream is = new LimitedInputStream(fis, stopIndex - startIndex);
            byte[] hex = IO.readBytes(is);
            return new ObjectInputStream(new ByteArrayInputStream(HexFormatter.hexToByteArray(new String(hex, "ASCII")))).readObject();
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * Shutdowns the database
     */
    public void shutdownDatabase() {
        dbshutdown = true;
    }

    /**
     * Returns the saved linklist
     */
    public Object getLinks() {
        Long[] indices = null;
        synchronized (this) {
            indices = objectIndices.get("links_links");
        }
        Object ret = null;
        if (indices != null) {
            ret = readObject(indices[0], indices[1]);
        }
        return ret;
    }

    public void save() {
    }

}