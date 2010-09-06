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

package jd.update;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import org.appwork.utils.event.Eventsender;

public class FileUpdate {

    public static final int DOWNLOAD_SOURCE = 1;
    public static final int ERROR = 2;
    public static final int SERVER_STATS = 3;
    public static final int SUCCESS = 4;
    public static long WAITTIME_ON_ERROR = 15000;

    private final String localPath;
    private String url;
    private final String hash;
    private final ArrayList<Server> serverList = new ArrayList<Server>();

    private Server currentServer;

    private String relURL;
    private File workingDir;
    private Eventsender<MessageListener, MessageEvent> broadcaster;

    public FileUpdate(String serverString, final String hash) {
        this.hash = hash;
        serverString = serverString.replace("http://78.143.20.68/update/jd/", "");
        String[] dat = new Regex(serverString, "(.*)\\?(.*)").getRow(0);
        this.relURL = serverString;
        if (dat == null) {
            localPath = serverString;
        } else {
            localPath = dat[0];
            this.url = dat[1];
        }
        initBroadcaster();
    }

    public FileUpdate(final String serverString, final String hash, final File workingdir) {
        this(serverString, hash);
        this.workingDir = workingdir;
        relURL = serverString;
        initBroadcaster();
    }

    private void initBroadcaster() {
        this.broadcaster = new Eventsender<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(final MessageListener listener, final MessageEvent event) {
                listener.onMessage(event);
            }

        };
    }

    public Eventsender<MessageListener, MessageEvent> getBroadcaster() {
        return broadcaster;
    }

    public String toString() {
        return localPath;
    }

    public String getRelURL() {
        return relURL;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRawUrl() {
        return url;
    }

    public String getRemoteHash() {
        return hash;
    }

    public boolean exists() {
        // if (workingDir != null) {
        return getLocalFile().exists() || this.getLocalTmpFile().exists();
        // } else {
        // return JDUtilities.getResourceFile(getLocalPath()).exists();
        // }
    }

    public boolean equals() {
        if (exists()) {
            final String localHash = getLocalHash();
            if (localHash != null) { return localHash.equalsIgnoreCase(hash); }
        }
        return false;
    }

    private String getLocalHash() {
        return JDHash.getMD5(getLocalTmpFile().exists() ? getLocalTmpFile() : getLocalFile());
    }

    public File getLocalFile() {
        return (workingDir != null) ? new File(workingDir + getLocalPath()) : JDUtilities.getResourceFile(getLocalPath());
    }

    public void reset(final ArrayList<Server> availableServers) {
        this.serverList.clear();
        serverList.addAll(availableServers);
    }

    public boolean hasServer() {
        return !serverList.isEmpty();
    }

    /**
     * verwendet alle server bis die datei gefunden wurde
     * 
     * @return
     * @throws IOException
     */
    public boolean update(final ArrayList<Server> availableServers) {
        final Browser browser = new Browser();
        browser.setReadTimeout(20 * 1000);
        browser.setConnectTimeout(10 * 1000);
        long startTime, endTime;
        for (int retry = 0; retry < 3; retry++) {
            if (availableServers == null || availableServers.isEmpty()) {
                System.err.println("no downloadsource available!");
                return false;
            }
            reset(availableServers);
            while (hasServer()) {
                String url = getURL();
                // String localHash = getLocalHash();
                File tmpFile;
                if (workingDir != null) {
                    tmpFile = new File(workingDir + getLocalPath() + ".tmp");
                } else {
                    tmpFile = JDUtilities.getResourceFile(getLocalPath() + ".tmp");
                }
                // delete tmp file
                tmpFile.delete();
                File updatetmp = this.getLocalTmpFile();
                if (updatetmp.exists() && JDHash.getMD5(updatetmp).equals(hash)) {
                    return true;
                } else {
                    // remove local tmp file, since it does either not exist or
                    // is invalid
                    this.getLocalTmpFile().delete();

                    if (url.contains("?")) {
                        url += "&r=" + System.currentTimeMillis();
                    } else {
                        url += "?r=" + System.currentTimeMillis();
                    }

                    broadcaster.fireEvent(new MessageEvent(this, DOWNLOAD_SOURCE, "Downloadsource: " + url));

                    startTime = System.currentTimeMillis();
                    URLConnectionAdapter con = null;
                    int response = -1;
                    try {
                        // Open connection
                        con = browser.openGetConnection(url);
                        endTime = System.currentTimeMillis();
                        response = con.getResponseCode();
                        currentServer.setRequestTime(endTime - startTime);
                    } catch (Exception e) {
                        // Failed connection.retry next server
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Connection error"));
                        currentServer.setRequestTime(100000l);
                        try {
                            con.disconnect();
                        } catch (Exception e1) {
                        }
                        errorWait();
                        continue;
                    }
                    // connection estabilished
                    if (response != 200) {
                        // responscode has errors. Try next server
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Connection error " + response + ""));
                        currentServer.setRequestTime(500000l);
                        try {
                            con.disconnect();
                        } catch (Exception e) {
                        }
                        errorWait();
                        continue;

                    }
                    // connection is ok. download now to *.,tmp file
                    try {
                        Browser.download(tmpFile, con);
                    } catch (Exception e) {
                        // DOwnload failed. try next server
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Connection broken"));
                        currentServer.setRequestTime(100000l);
                        try {
                            con.disconnect();
                        } catch (Exception e1) {
                        }
                        errorWait();
                        continue;
                    }
                    // Download is ok. b
                    try {
                        con.disconnect();
                    } catch (Exception e) {
                    }
                    broadcaster.fireEvent(new MessageEvent(this, SERVER_STATS, currentServer + " requesttimeAVG=" + currentServer.getRequestTime()));
                }

                final String downloadedHash = JDHash.getMD5(tmpFile);
                if (downloadedHash != null && downloadedHash.equalsIgnoreCase(hash)) {
                    // hash of fresh downloaded file is ok
                    broadcaster.fireEvent(new MessageEvent(this, SUCCESS, "Hash OK"));
                    // move to update folder
                    this.getLocalTmpFile().delete();
                    // tinyupdate has to be updated directly
                    boolean ret;

                    if (tmpFile.getName().startsWith("tinyupdate")) {
                        this.getLocalFile().delete();
                        ret = tmpFile.renameTo(this.getLocalFile());
                    } else {
                        ret = tmpFile.renameTo(getLocalTmpFile());
                    }

                    if (ret) {
                        // rename ok
                        return ret;
                    } else {
                        // rename failed. needs subfolder?
                        getLocalTmpFile().getParentFile().mkdirs();
                        ret = tmpFile.renameTo(getLocalTmpFile());
                        if (!ret) {
                            // rename failed finally
                            broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Rename failed"));
                            errorWait();
                        } else {
                            // rename succeeded
                            return ret;
                        }
                    }
                } else {
                    // Download failed. delete tmp file and exit
                    broadcaster.fireEvent(new MessageEvent(this, ERROR, "Hash Failed"));
                    currentServer.setRequestTime(100000l);
                    if (hasServer()) {
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Retry"));
                    } else {
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Updateserver down"));
                    }
                    tmpFile.delete();
                    errorWait();
                    continue;
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                continue;
            }
        }
        return false;

    }

    private void errorWait() {
        try {
            broadcaster.fireEvent(new MessageEvent(this, ERROR, "Server Busy. Wait " + (WAITTIME_ON_ERROR / 1000) + " Seconds"));
            Thread.sleep(WAITTIME_ON_ERROR);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Returns the local tmp file
     * 
     * @return
     */
    public File getLocalTmpFile() {
        // if (workingDir != null) {
        // return new File(new File(workingDir, "update") + getLocalPath());
        // } else {
        // return new File(JDUtilities.getResourceFile("update") +
        // getLocalPath());
        // }
        return new File((workingDir != null ? new File(workingDir, "update") : JDUtilities.getResourceFile("update")) + getLocalPath());
    }

    private String mergeUrl(final String server, final String file) {
        final String ret = (server.endsWith("/") || file.charAt(0) == '/') ? server + file : server + "/" + file;
        return ret.replaceAll("//", "/").replaceAll("http:/", "http://");
    }

    /**
     * as long as there are valid servers, this method returns a valid url.
     * 
     * @return
     */
    private String getURL() {
        Server serv;
        if (url == null || url.trim().length() == 0) {
            serv = Server.selectServer(serverList);
            this.currentServer = serv;
            serverList.remove(serv);
            return mergeUrl(serv.getPath(), this.relURL);
        }
        if (url.toLowerCase(Locale.getDefault()).startsWith("http://")) { return url; }
        serv = Server.selectServer(serverList);
        this.currentServer = serv;
        serverList.remove(serv);
        return mergeUrl(serv.getPath(), url);
    }

    public boolean needsRestart() {
        final String hash = JDHash.getMD5(getLocalTmpFile());
        return (hash == null) ? false : hash.equalsIgnoreCase(hash);
    }

}
