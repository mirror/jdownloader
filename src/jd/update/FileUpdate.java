package jd.update;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.http.Browser;
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

    public FileUpdate(String serverString, String hash) {
        this.hash = hash;
        serverString = serverString.replace("http://78.143.20.68/update/jd/", "");
        String[] dat = new Regex(serverString, "(.*)\\?(.*)").getRow(0);
        if (dat == null) {
            this.localPath = serverString;
        } else {
            localPath = dat[0];
            this.url = dat[1];
        }
    }

    public String getLocalPath() {
        // TODO Auto-generated method stub
        return localPath;
    }

    public String getRawUrl() {
        // TODO Auto-generated method stub
        return url;
    }

    public String getRemoteHash() {
        // TODO Auto-generated method stub
        return hash;
    }

    public boolean exists() {
        // TODO Auto-generated method stub
        return JDUtilities.getResourceFile(getLocalPath()).exists();
    }

    public boolean equals() {
        if (!exists()) return false;
        String localHash = getLocalHash();

        return localHash.equalsIgnoreCase(hash);
    }

    private String getLocalHash() {

        return JDHash.getMD5(getLocalFile());
    }

    private File getLocalFile() {
        return JDUtilities.getResourceFile(getLocalPath());
    }

    public void reset(ArrayList<Server> availableServers) {
        this.serverList = new ArrayList<Server>();
        serverList.addAll(availableServers);
    }

    public boolean hasServer() {
        // TODO Auto-generated method stub
        return serverList.size() > 0;
    }

    public String toString() {
        if (result == null) return super.toString();
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
            File tmpFile = JDUtilities.getResourceFile(getLocalPath() + ".tmp");
            result.append("Downloadsource: " + url + "\r\n");
            startTime = System.currentTimeMillis();
            try {
                br.openGetConnection(url);
                endTime = System.currentTimeMillis();
                currentServer.setRequestTime(endTime - startTime);
            } catch (Exception e) {
                currentServer.setRequestTime(10000l);
            }

            Browser.download(tmpFile, url);
            String downloadedHash = JDHash.getMD5(tmpFile);
            if (downloadedHash.equalsIgnoreCase(hash)) {
                this.getLocalFile().delete();
                boolean ret=tmpFile.renameTo(getLocalFile());
                
                if(ret){
                    return ret;
                }else{
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
        return ret.replaceAll("(^:)//", "$1/");
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
            return mergeUrl(serv.getPath(), this.getLocalPath());
        }
        if (url.toLowerCase().startsWith("http://")) { return url; }
        serv = Server.selectServer(serverList);
        this.currentServer = serv;
        serverList.remove(serv);
        return mergeUrl(serv.getPath(), url);
    }

}
