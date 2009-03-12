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

import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class FileUpdate {

    private String localPath;
    private String url;
    private String hash;
    private ArrayList<Server> serverList;
    private StringBuilder result;
    private Server currentServer;

    private String relURL;
    private File workingDir;

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
    }

    public FileUpdate(String serverString, String hash, File workingdir) {
        this(serverString, hash);
        this.workingDir = workingdir;
        relURL = serverString;
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

        if (workingDir != null) {
            return getLocalFile().exists();
        } else {
            return JDUtilities.getResourceFile(getLocalPath()).exists();
        }

    }

    public boolean equals() {
        if (!exists()) return false;
        String localHash = getLocalHash();

        return localHash.equalsIgnoreCase(hash);
    }

    private String getLocalHash() {

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

    public String toString() {
        if (result == null) return this.getLocalFile().getAbsolutePath();
        return result.toString();
    }

    /**
     * verwendet alle server bis die datei gefunden wurde
     * 
     * @return
     * @throws IOException
     */
    public boolean update() throws IOException {
        this.result = new StringBuilder();
        Browser br = new Browser();
        long startTime, endTime;
        while (hasServer()) {
            String url = getURL();
            // String localHash = getLocalHash();
            File tmpFile;
            if (workingDir != null) {
                tmpFile = new File(workingDir + getLocalPath() + ".tmp");
            } else {
                tmpFile = JDUtilities.getResourceFile(getLocalPath() + ".tmp");
            }
            if (url.contains("?")) {
                url += "&r=" + System.currentTimeMillis();
            } else {
                url += "?r=" + System.currentTimeMillis();
            }
            result.append("Downloadsource: " + url + "\r\n");
            startTime = System.currentTimeMillis();
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(url);
                endTime = System.currentTimeMillis();
                currentServer.setRequestTime(endTime - startTime);
            } catch (Exception e) {
                currentServer.setRequestTime(10000l);

            }

            if (con == null) {
                result.append("Error. Connection error\r\n");
                return false;
            }
            Browser.download(tmpFile, con);
            String downloadedHash = JDHash.getMD5(tmpFile);
            if (downloadedHash.equalsIgnoreCase(hash)) {
                this.getLocalFile().delete();
                boolean ret = tmpFile.renameTo(getLocalFile());

                if (ret) {
                    return ret;
                } else {
                    result.append("Error. Rename failed\r\n");
                }
            } else {

                if (hasServer()) {
                    result.append("Error. Retry\r\n");
                } else {
                    result.append("Error. Updateserver down\r\n");
                }
                tmpFile.delete();
                continue;
            }
        }
        return false;

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

}
