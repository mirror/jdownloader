package jd.controlling.downloadcontroller;

import java.util.ArrayList;
import java.util.List;

import jd.plugins.DownloadLink;

import org.appwork.utils.net.throttledconnection.ThrottledConnection;
import org.appwork.utils.net.throttledconnection.ThrottledConnectionHandler;
import org.appwork.utils.speedmeter.SpeedMeterInterface;

public class ManagedThrottledConnectionHandler implements ThrottledConnectionHandler {

    private java.util.List<ThrottledConnection> connections = new ArrayList<ThrottledConnection>();
    private int                            limit       = 0;
    private volatile long                  traffic     = 0;
    final private DownloadLink             link;
    private DownloadSpeedManager           managedBy   = null;

    public ManagedThrottledConnectionHandler(DownloadLink link) {
        this.link = link;
    }

    public DownloadLink getLink() {
        return link;
    }

    public void addThrottledConnection(ThrottledConnection con) {
        if (this.connections.contains(con)) return;
        synchronized (this) {
            java.util.List<ThrottledConnection> newConnections = new ArrayList<ThrottledConnection>(connections);
            newConnections.add(con);
            connections = newConnections;
        }
        DownloadSpeedManager lmanagedBy = managedBy;
        if (lmanagedBy != null && lmanagedBy.getLimit() > 0 || getLimit() > 0) con.setLimit(10);
        con.setHandler(this);
    }

    public List<ThrottledConnection> getConnections() {
        return connections;
    }

    public int getLimit() {
        return limit;
    }

    public int getSpeed() {
        java.util.List<ThrottledConnection> lconnections = connections;
        int ret = 0;
        for (ThrottledConnection con : lconnections) {
            ret += ((SpeedMeterInterface) con).getSpeedMeter();
        }
        return ret;
    }

    public long getTraffic() {
        java.util.List<ThrottledConnection> lconnections = null;
        long ret = 0;
        synchronized (this) {
            ret = traffic;
            lconnections = connections;
        }
        for (ThrottledConnection con : lconnections) {
            ret += con.transfered();
        }
        return ret;
    }

    public void removeThrottledConnection(ThrottledConnection con) {
        if (!this.connections.contains(con)) return;
        synchronized (this) {
            java.util.List<ThrottledConnection> newConnections = new ArrayList<ThrottledConnection>(connections);
            newConnections.remove(con);
            connections = newConnections;
            traffic += con.transfered();
        }
        con.setHandler(null);
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int size() {
        return connections.size();
    }

    protected void setManagedBy(DownloadSpeedManager downloadSpeedManager) {
        this.managedBy = downloadSpeedManager;
    }

}
