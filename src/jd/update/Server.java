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
    private long requestTime = 0;
    private int requestCount = 0;

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

    /**
     * selects depending on the percents a ranodm server
     * 
     * @param list
     * @return
     */
    public static Server selectServer(ArrayList<Server> list) {

        int rand = (int) (Math.random() * 100);
        int total = 0;
        Server ret = null;
        for (Server s : list) {
            if (s.getPercent() <= 0) { return selectServerByRequestTime(list); }
            ret = s;
            total += s.getPercent();
            if (rand<=total && rand>total-s.getPercent()) break;
        }

        return ret;

    }
/**
 * Gibt den server mit der besten requestzeit zurück
 * @param list
 * @return
 */
    private static Server selectServerByRequestTime(ArrayList<Server> list) {
      
        Server ret = null;
        for (Server s : list) {
            if (ret == null || s.getRequestTime() < ret.getRequestTime()) ret = s;

        }

        return ret;
    }

    /**
     * summiert die requestzeiten zu einem durchschnitt
     * 
     * @param l
     */
    public void setRequestTime(long l) {
        requestTime = (requestTime * requestCount + l) / (requestCount + 1);
        this.requestCount++;
        System.out.println(this+" requesttime="+requestTime);
    }

    public long getRequestTime() {
        return requestTime;
    }
}