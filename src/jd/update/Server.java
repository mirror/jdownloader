package jd.update;

import java.io.Serializable;
import java.util.ArrayList;

public class Server implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 701200615385983L;
    private int percent;
    private String path;

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String toString() {
        return this.path + " (" + percent + "%)";
    }

    public Server(int percent, String host) {
        this.percent = percent;
        this.path = host;
    }

    public static Server selectServer() {
        ArrayList<Server> list = (ArrayList<Server>) WebUpdater.getConfig("WEBUPDATE").getProperty("SERVERLIST");
        int rand = (int) (Math.random() * 100);
        int total = 0;
        Server ret = null;
        for (Server s : list) {
            ret = s;
            total += s.getPercent();
            if (total >= rand) break;
        }
        
        return ret;

    }
}