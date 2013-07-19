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
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.util.HashMap;

import jd.utils.JDUtilities;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.LimitedInputStream;
import org.jdownloader.logging.LogController;
import org.seamless.util.io.IO;
import org.tmatesoft.svn.core.internal.util.CountingInputStream;

public class DatabaseConnector {

    private HashMap<String, Long[]> objectIndices = new HashMap<String, Long[]>();

    private String                  configpath    = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/config/";
    private final LogSource         logger;

    public DatabaseConnector() {
        logger = LogController.CL(false);
        File dataBase = null;
        if (!(dataBase = new File(configpath + "database.script")).exists()) {
            logger.info("Found no old database to import!");
            return;
        }
        logger.info("Open old database: " + dataBase);
        CountingInputStream fis = null;
        String[] searchForEntries = new String[] { "INSERT INTO CONFIG VALUES('", "INSERT INTO LINKS VALUES('" };
        String[] searchForEntriesType = new String[] { "config_", "links_" };
        String searchForObject = ",'aced";
        for (int searchForEntriesIndex = 0; searchForEntriesIndex < searchForEntries.length; searchForEntriesIndex++) {
            String searchForEntry = searchForEntries[searchForEntriesIndex];
            try {
                fis = new CountingInputStream(new BufferedInputStream(new FileInputStream(dataBase), 32767));
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
                                logger.info("Found Entry: " + name + "Start: " + objectStartIndex + " End: " + objectStopIndex + " Size: " + (objectStopIndex - objectStartIndex));
                                objectIndices.put(searchForEntriesType[searchForEntriesIndex] + name, new Long[] { objectStartIndex, objectStopIndex });
                                continue insertLoop;
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                logger.log(e);
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
            return new ObjectInputStream(new ByteArrayInputStream(HexFormatter.hexToByteArray(new String(hex, "ASCII")))) {
                /*
                 * IDEA from http://sanjitmohanty.wordpress.com/2011/11/23/making-jvm-to-ignore-serialversionuids-mismatch/
                 * 
                 * changed it to modify the suid in resultClassDescriptor to match local suid -> else deserializer broke for me
                 * 
                 * @see java.io.ObjectInputStream#readClassDescriptor()
                 */
                @Override
                protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
                    ObjectStreamClass resultClassDescriptor = super.readClassDescriptor(); // initially streams descriptor

                    Class<?> localClass; // the class in the local JVM that this descriptor represents.
                    try {
                        localClass = Class.forName(resultClassDescriptor.getName());
                    } catch (ClassNotFoundException e) {
                        return resultClassDescriptor;
                    }
                    ObjectStreamClass localClassDescriptor = ObjectStreamClass.lookup(localClass);
                    if (localClassDescriptor != null) {
                        final long localSUID = localClassDescriptor.getSerialVersionUID();
                        final long streamSUID = resultClassDescriptor.getSerialVersionUID();
                        if (streamSUID != localSUID) {
                            final StringBuffer s = new StringBuffer("Potentially Fatal Deserialization Operation. Overriding serialized class version mismatch: ");
                            s.append(" class = ").append(localClassDescriptor.getName());
                            s.append(" local serialVersionUID = ").append(localSUID);
                            s.append(" stream serialVersionUID = ").append(streamSUID);
                            Exception e = new InvalidClassException(s.toString());
                            logger.log(e);
                            try {
                                final Field field = resultClassDescriptor.getClass().getDeclaredField("suid");
                                field.setAccessible(true);
                                field.set(resultClassDescriptor, Long.valueOf(localSUID));
                            } catch (final Throwable e2) {
                                logger.log(e2);
                            }
                        }
                    }
                    return resultClassDescriptor;
                }
            }.readObject();
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