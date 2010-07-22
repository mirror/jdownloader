package jd.plugins.optional.jdpremserver.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jd.plugins.optional.jdpremserver.controlling.UserController;

public class PremServUser {
    private PremServUser() {
    }

    private String username;
    private String password;
    private boolean enabled;
    private HashMap<String, PremServHoster> hosters;
    private ArrayList<TrafficLog> trafficLog = new ArrayList<TrafficLog>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

    public HashMap<String, PremServHoster> getHosters() {
        if (hosters == null) hosters = new HashMap<String, PremServHoster>();
        return hosters;
    }

    public void setHosters(HashMap<String, PremServHoster> hosters) {
        this.hosters = hosters;
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.toLowerCase();
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

    public PremServUser(String username, String password) {
        this.username = username.toLowerCase();
        this.password = password;
    }

    public void addTrafficLog(String domain, long traffic) {
        trafficLog.add(new TrafficLog(domain.toLowerCase(), traffic));
        cleanTrafficLog();
    }

    public long calculateTrafficLeft(String domain) {
        domain = domain.toLowerCase();
        PremServHoster hosterImpl = getHosters().get(domain);
        if (hosterImpl == null) return Long.MAX_VALUE;
        long trafficUsed = calculateTrafficUsed(domain);
        return hosterImpl.getTraffic() - trafficUsed;
    }

    public long calculateTrafficUsed(String domain) {
        domain = domain.toLowerCase();
        long ret = 0;
        for (TrafficLog l : trafficLog) {
            if (l.getDomain().equals(domain)) ret += l.getTraffic();
        }
        return ret;
    }

    public void setTrafficLog(ArrayList<TrafficLog> trafficLog) {

        this.trafficLog = trafficLog;
        cleanTrafficLog();

    }

    /**
     * Removes traffic log entries that are older than 30 days
     */
    private void cleanTrafficLog() {
        TrafficLog next;
        for (Iterator<TrafficLog> it = trafficLog.iterator(); it.hasNext();) {
            next = it.next();
            if ((System.currentTimeMillis() - next.getTimestamp()) > 30 * 24 * 60 * 60 * 1000l) {
                it.remove();
            }
        }

    }

    public ArrayList<TrafficLog> getTrafficLog() {
        return trafficLog;
    }

}
