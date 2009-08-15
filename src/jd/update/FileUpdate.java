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

import jd.event.JDBroadcaster;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class FileUpdate {

    private static final int DOWNLOAD_SOURCE = 1;
    private static final int ERROR = 2;
    private static final int SERVER_STATS = 3;
    private static final int SUCCESS = 4;
    private String localPath;
    private String url;
    private String hash;
    private ArrayList<Server> serverList;

    private Server currentServer;

    private String relURL;
    private File workingDir;
    private JDBroadcaster<MessageListener, MessageEvent> broadcaster;

    public FileUpdate(String serverString, String hash) {
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

    public FileUpdate(String serverString, String hash, File workingdir) {
        this(serverString, hash);
        this.workingDir = workingdir;
        relURL = serverString;
        initBroadcaster();
    }

    private void initBroadcaster() {
        this.broadcaster = new JDBroadcaster<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(MessageListener listener, MessageEvent event) {
                listener.onMessage(event);

            }

        };
    }


    public JDBroadcaster<MessageListener, MessageEvent> getBroadcaster() {
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
        if (!exists()) return false;
        String localHash = getLocalHash();
        if (localHash == null) return false;
        return localHash.equalsIgnoreCase(hash);
    }

    private String getLocalHash() {
        if (this.getLocalTmpFile().exists()) return JDHash.getMD5(getLocalTmpFile());
        return JDHash.getMD5(getLocalFile());

    }

    public File getLocalFile() {
        if (workingDir != null) {
            return new File(workingDir + getLocalPath());
        } else {
            return JDUtilities.getResourceFile(getLocalPath());
        }

    }

    public void reset(ArrayList<Server> availableServers) {
        this.serverList = new ArrayList<Server>();
        serverList.addAll(availableServers);
    }

    public boolean hasServer() {
        return serverList.size() > 0;
    }

    /**
     * verwendet alle server bis die datei gefunden wurde
     * 
     * @return
     * @throws IOException
     */
    public boolean update(ArrayList<Server> availableServers) {

        Browser br = new Browser();
        br.setReadTimeout(20 * 1000);
        br.setConnectTimeout(10 * 1000);
        long startTime, endTime;
        for (int retry = 0; retry < 3; retry++) {
            if (availableServers == null || availableServers.size() == 0) {
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
                //delete tmp file
                tmpFile.delete();

                if (this.getLocalTmpFile().exists() && JDHash.getMD5(this.getLocalTmpFile()).equals(hash)) {
                    //if update/... already exists and hash is ok.. 
            
//                    this.getLocalTmpFile().renameTo(tmpFile);
                } else {
                    //remove local tmp file, since it does either not exist or is invalid
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
                        //Open connection
                        con = br.openGetConnection(url);
                        endTime = System.currentTimeMillis();
                        response = con.getResponseCode();
                        currentServer.setRequestTime(endTime - startTime);

                    } catch (Exception e) {
                        //Failed connection.retry next server
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Connection error"));
                        currentServer.setRequestTime(100000l);
                        try {
                            con.disconnect();
                        } catch (Exception e1) {
                        }
                        continue;
                    }
                    //connection estabilished
                    if (response != 200) {
                    //responscode  has errors. Try next server
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Connection error " + response + ""));
                        currentServer.setRequestTime(500000l);
                        try {
                            con.disconnect();
                        } catch (Exception e) {
                        }
                        continue;

                    }
//connection is ok. download now to *.,tmp file
                    try {
                        Browser.download(tmpFile, con);
                    } catch (Exception e) {
                        //DOwnload failed. try next server
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Connection broken"));
                        currentServer.setRequestTime(100000l);
                        try {
                            con.disconnect();
                        } catch (Exception e1) {
                        }
                        continue;
                    }
                    //Download is ok. b
                    try {
                        con.disconnect();
                    } catch (Exception e) {
                    }
                    broadcaster.fireEvent(new MessageEvent(this, SERVER_STATS, currentServer + " requesttimeAVG=" + currentServer.getRequestTime() + ""));

                }
                
                
                String downloadedHash = JDHash.getMD5(tmpFile);
                if (downloadedHash!=null&&downloadedHash.equalsIgnoreCase(hash)) {
                    //hash of fresh downloaded file is ok
                    broadcaster.fireEvent(new MessageEvent(this, SUCCESS, "Hash OK"));
                    //move to update folder
                    this.getLocalTmpFile().delete();
                    //tinyupdate has to be updated directly
                    boolean ret;
                    if(tmpFile.getName().startsWith("tinyupdate")){
                         ret = tmpFile.renameTo(this.getLocalFile());
                    }else{
                         ret = tmpFile.renameTo(getLocalTmpFile()); 
                    }
                  
                    if (ret) {
                        //rename ok
                        return ret;
                    } else {
                        //rename failed. needs subfolder?
                        getLocalTmpFile().getParentFile().mkdirs();
                        ret = tmpFile.renameTo(getLocalTmpFile());
                        if (!ret) {
                            //rename failed finally
                            broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Rename failed"));
                        } else {
                            //rename succeeded
                            return ret;
                        }
                    }
                } else {
                    //Download failed. delete tmp file and exit
                    
                    broadcaster.fireEvent(new MessageEvent(this, ERROR, "Hash Failed"));
                    if (hasServer()) {
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Retry"));
                    } else {
                        broadcaster.fireEvent(new MessageEvent(this, ERROR, "Error. Updateserver down"));
                    }
                    tmpFile.delete();
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

    private File getLocalTmpFile() {
        if (workingDir != null) {
            return new File(new File(workingDir, "update") + getLocalPath());
        } else {
            return new File(JDUtilities.getResourceFile("update") + getLocalPath());
        }

    }

    private String mergeUrl(String server, String file) {

        String ret = "";
        if (server.endsWith("/") || file.startsWith("/")) {
            ret = server + file;
        } else {
            ret = server + "/" + file;
        }
        ret = ret.replaceAll("//", "/");
        return ret.replaceAll("http:/", "http://");
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
        if (url.toLowerCase().startsWith("http://")) { return url; }
        serv = Server.selectServer(serverList);
        this.currentServer = serv;
        serverList.remove(serv);
        return mergeUrl(serv.getPath(), url);
    }

    public boolean needsRestart() {
        String hash = JDHash.getMD5(getLocalTmpFile());
        if (hash == null) return false;
        if (hash.equalsIgnoreCase(hash)) return true;
        return false;
    }

}
