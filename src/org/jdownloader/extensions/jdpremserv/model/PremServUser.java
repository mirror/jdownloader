package org.jdownloader.extensions.jdpremserv.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.jdownloader.extensions.jdpremserv.controlling.UserController;


public class PremServUser {

    private String username;
    private long allowedTrafficPerMonth = -1;

    public long getAllowedTrafficPerMonth() {
        return allowedTrafficPerMonth;
    }

    public void setAllowedTrafficPerMonth(long traffic) {
        this.allowedTrafficPerMonth = traffic;
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

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

    @SuppressWarnings("unused")
    private PremServUser() {
    }

    public PremServUser(String username, String password) {
        this.username = username.toLowerCase();
        this.password = password;
    }

    /**
     * Add a trafficlog fpr domain
     * 
     * @param domain
     * @param traffic
     */
    public synchronized void addTrafficLog(String domain, long traffic) {
        synchronized (trafficLog) {
            trafficLog.add(new TrafficLog(domain.toLowerCase(), traffic));
        }
        cleanTrafficLog();
        if (UserController.getInstance() != null) UserController.getInstance().fireUserUpdate(this);
    }

    /**
     * Calculates how much traffic is left for the given domain returns
     * Long.MAX_VALUE or total trafficlimit if the is no limitation for the
     * given domain If the total limit is lower than the domainlimit, the total
     * limit is returned.
     * 
     * Short: This method always return the actuall traffic left for the given
     * domain.
     * 
     * 
     * @param domain
     * @return
     */
    public synchronized long calculateTrafficLeft(String domain) {
        domain = domain.toLowerCase();
        PremServHoster hosterImpl = getHosters().get(domain);
        if (hosterImpl == null) return Math.min(Long.MAX_VALUE, calculateTotalTrafficLeft());
        long trafficUsed = calculateTrafficUsed(domain);
        return Math.min(hosterImpl.getTraffic() - trafficUsed, calculateTotalTrafficLeft());
    }

    /**
     * Calculates how much traffic has been used over the last 30 mdays for the
     * given domain
     * 
     * @param domain
     * @return
     */
    public synchronized long calculateTrafficUsed(String domain) {
        domain = domain.toLowerCase();
        long ret = 0;
        synchronized (trafficLog) {
            for (TrafficLog l : trafficLog) {
                if (l.getDomain().equals(domain)) ret += l.getTraffic();
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    private synchronized void setTrafficLog(ArrayList<TrafficLog> trafficLog) {

        this.trafficLog = trafficLog;
        cleanTrafficLog();

    }

    /**
     * Removes traffic log entries that are older than 30 days
     */
    private synchronized void cleanTrafficLog() {
        TrafficLog next;
        synchronized (trafficLog) {
            for (Iterator<TrafficLog> it = trafficLog.iterator(); it.hasNext();) {
                next = it.next();
                if ((System.currentTimeMillis() - next.getTimestamp()) > 30 * 24 * 60 * 60 * 1000l) {
                    it.remove();
                }
            }
        }

    }

    /**
     * Returns an array with all stored trafficlogs.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public ArrayList<TrafficLog> getTrafficLog() {
        synchronized (trafficLog) {
            return (ArrayList<TrafficLog>) trafficLog.clone();
        }
    }

    /**
     * Returns a hashmap with domain=trafficused pairs. TRaffic is calculated
     * over the last 30 days
     * 
     * @return
     */
    public synchronized HashMap<String, Long> createTrafficStats() {
        HashMap<String, Long> ret = new HashMap<String, Long>();
        TrafficLog next;
        synchronized (trafficLog) {
            for (Iterator<TrafficLog> it = trafficLog.iterator(); it.hasNext();) {
                next = it.next();
                if ((System.currentTimeMillis() - next.getTimestamp()) > 30 * 24 * 60 * 60 * 1000l) {
                    it.remove();
                    continue;
                }
                Long value = ret.get(next.getDomain());
                if (value == null) value = 0l;
                value += next.getTraffic();
                ret.put(next.getDomain(), value);
            }
        }
        return ret;
    }

    /**
     * Returns how much traffic has been used by the user during the last 30
     * days
     * 
     * @return
     */
    public long calculateTotalTraffic() {
        TrafficLog next;
        long ret = 0;
        synchronized (trafficLog) {
            for (Iterator<TrafficLog> it = trafficLog.iterator(); it.hasNext();) {
                next = it.next();
                if ((System.currentTimeMillis() - next.getTimestamp()) > 30 * 24 * 60 * 60 * 1000l) {
                    it.remove();
                    continue;
                }
                ret += next.getTraffic();
            }
        }
        return ret;
    }

    /**
     * Returns how much traffic is left fromt he users global limit or
     * Long.MAX_VALUE if there is no global limit
     * 
     * @return
     */
    public long calculateTotalTrafficLeft() {
        if (this.getAllowedTrafficPerMonth() < 0) return Long.MAX_VALUE;

        return this.getAllowedTrafficPerMonth() - this.calculateTotalTraffic();
    }

}
