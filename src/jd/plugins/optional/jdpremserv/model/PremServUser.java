package jd.plugins.optional.jdpremserv.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jd.plugins.optional.jdpremserv.controlling.UserController;

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

    public synchronized void addTrafficLog(String domain, long traffic) {
        trafficLog.add(new TrafficLog(domain.toLowerCase(), traffic));
        cleanTrafficLog();
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

    public synchronized long calculateTrafficLeft(String domain) {
        domain = domain.toLowerCase();
        PremServHoster hosterImpl = getHosters().get(domain);
        if (hosterImpl == null) return Long.MAX_VALUE;
        long trafficUsed = calculateTrafficUsed(domain);
        return hosterImpl.getTraffic() - trafficUsed;
    }

    public synchronized long calculateTrafficUsed(String domain) {
        domain = domain.toLowerCase();
        long ret = 0;
        for (TrafficLog l : trafficLog) {
            if (l.getDomain().equals(domain)) ret += l.getTraffic();
        }
        return ret;
    }

    public synchronized void setTrafficLog(ArrayList<TrafficLog> trafficLog) {

        this.trafficLog = trafficLog;
        cleanTrafficLog();

    }

    /**
     * Removes traffic log entries that are older than 30 days
     */
    private synchronized void cleanTrafficLog() {
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

    public synchronized HashMap<String, Long> createTrafficStats() {
        HashMap<String, Long> ret = new HashMap<String, Long>();
        TrafficLog next;
        for (Iterator<TrafficLog> it = trafficLog.iterator(); it.hasNext();) {
            next = it.next();
            Long value = ret.get(next.getDomain());
            if (value == null) value = 0l;
            value += next.getTraffic();
            ret.put(next.getDomain(), value);
        }
        return ret;
    }

}
