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

package jd.plugins;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

import jd.config.Property;

import org.appwork.utils.formatter.SizeFormatter;

public class AccountInfo extends Property {

    private static final long serialVersionUID       = 1825140346023286206L;

    private long              account_validUntil     = -1;

    private long              account_trafficLeft    = -1;
    private long              account_trafficMax     = -1;

    private long              account_filesNum       = -1;
    private long              account_premiumPoints  = -1;
    private long              account_accountBalance = -1;
    private long              account_usedSpace      = -1;

    private String            account_status;
    private long              account_createTime     = 0;
    /**
     * indicator that host, account has special traffic handling, do not temp disable if traffic =0
     */
    private boolean           specialTraffic         = false;

    public long getCreateTime() {
        return account_createTime;
    }

    public void setSpecialTraffic(final boolean b) {
        specialTraffic = b;
    }

    public boolean isSpecialTraffic() {
        return specialTraffic;
    }

    public void setCreateTime(final long createTime) {
        this.account_createTime = createTime;
    }

    /**
     * Gibt zurück wieviel (in Cent) Geld gerade auf diesem Account ist
     * 
     * @return
     */
    public long getAccountBalance() {
        return account_accountBalance;
    }

    /**
     * Gibt zurück wieviele Files auf dem Account hochgeladen sind
     * 
     * @return
     */
    public long getFilesNum() {
        return account_filesNum;
    }

    /**
     * Gibt an wieviele PremiumPunkte der Account hat
     * 
     * @return
     */
    public long getPremiumPoints() {
        return account_premiumPoints;
    }

    public String getStatus() {
        return account_status;
    }

    /**
     * Gibt an wieviel Traffic noch frei ist (in bytes)
     * 
     * @return
     */
    public long getTrafficLeft() {
        return Math.max(0, account_trafficLeft);
    }

    public long getTrafficMax() {
        return Math.max(getTrafficLeft(), account_trafficMax);
    }

    /**
     * Gibt zurück wieviel Platz (bytes) die Oploads auf diesem Account belegen
     * 
     * @return
     */
    public long getUsedSpace() {
        return account_usedSpace;
    }

    /**
     * Gibt einen Timestamp zurück zu dem der Account auslaufen wird bzw. ausgelaufen ist.(-1 für Nie)
     * 
     * @return
     */
    public long getValidUntil() {
        return account_validUntil;
    }

    /**
     * Gibt zurück ob der Account abgelaufen ist
     * 
     * @return
     */
    public boolean isExpired() {
        long validUntil = getValidUntil();
        if (validUntil < 0) {
            return false;
        }
        if (validUntil == 0) {
            return true;
        }
        return validUntil < System.currentTimeMillis();
    }

    public void setAccountBalance(final long parseInt) {
        this.account_accountBalance = Math.max(0, parseInt);
    }

    public void setAccountBalance(final String string) {
        this.setAccountBalance((long) (Double.parseDouble(string) * 100));
    }

    public void setExpired(final boolean b) {
        if (b) {
            setValidUntil(0);
        } else {
            setValidUntil(-1);
        }
    }

    public void setFilesNum(final long parseInt) {
        this.account_filesNum = Math.max(0, parseInt);
    }

    public void setPremiumPoints(final long parseInt) {
        this.account_premiumPoints = Math.max(0, parseInt);
    }

    public void setPremiumPoints(final String string) {
        this.setPremiumPoints(Integer.parseInt(string.trim()));
    }

    public void setStatus(final String string) {
        this.account_status = string;
    }

    public void setTrafficLeft(long size) {
        this.account_trafficLeft = Math.max(0, size);
    }

    public void setUnlimitedTraffic() {
        account_trafficLeft = -1;
    }

    public boolean isUnlimitedTraffic() {
        return account_trafficLeft == -1;
    }

    public void setTrafficLeft(final String freeTraffic) {
        this.setTrafficLeft(SizeFormatter.getSize(freeTraffic, true, true));
    }

    public void setTrafficMax(final long trafficMax) {
        this.account_trafficMax = Math.max(0, trafficMax);
    }

    public void setUsedSpace(final long size) {
        this.account_usedSpace = Math.max(0, size);
    }

    public void setUsedSpace(final String string) {
        this.setUsedSpace(SizeFormatter.getSize(string, true, true));
    }

    /**
     * -1 für Niemals ablaufen
     * 
     * @param validUntil
     */
    public void setValidUntil(final long validUntil) {
        this.account_validUntil = validUntil;
    }

    private long validPremiumUntil = 0l;

    public long getValidPremiumUntil() {
        return validPremiumUntil;
    }

    /**
     * @param validPremiumUntil
     * @since JD2
     */
    public void setValidPremiumUntil(long validPremiumUntil) {
        this.validPremiumUntil = validPremiumUntil;
    }

    /**
     * Removes forbidden hosts, adds host corrections, and then sets AccountInfo property 'multiHostSupport'
     * 
     * @author raztoki
     * @since JD2
     * */
    public void setMultiHostSupport(final ArrayList<String> multiHostSupport) {
        ArrayList<String> supportedHosts = null;
        if (multiHostSupport != null && !multiHostSupport.isEmpty()) {
            final HashSet<String> supportedHostsSet = new HashSet<String>();
            for (final String host : multiHostSupport) {
                if (host != null) {
                    supportedHostsSet.add(host.toLowerCase(Locale.ENGLISH));
                }
            }

            // remove forbidden hosts! needed to remove from tooltip

            // change when can construct from plugin cache from canNotMultihost(), doing this manually is pain in the ass and requires core
            // update each time.
            supportedHostsSet.remove("youtube.com");
            supportedHostsSet.remove("youtu.be");
            supportedHostsSet.remove("vimeo.com");

            // central place to fix up issues when JD 'names' doesn't match multihoster supported host array or vise versa

            // work around for freakshare.com
            if (supportedHostsSet.contains("freakshare.net") || supportedHostsSet.contains("freakshare.com")) {
                supportedHostsSet.add("freakshare.net");
                supportedHostsSet.add("freakshare.com");
            }
            // workaround for uploaded.to
            if (supportedHostsSet.contains("uploaded.net") || supportedHostsSet.contains("ul.to") || supportedHostsSet.contains("uploaded.to")) {
                supportedHostsSet.add("uploaded.net");
                supportedHostsSet.add("ul.to");
                supportedHostsSet.add("uploaded.to");
            }
            // workaround for keep2share.cc, as they keep changing hosts..
            if (supportedHostsSet.contains("keep2share.cc") || supportedHostsSet.contains("k2s.cc") || supportedHostsSet.contains("keep2s.cc") || supportedHostsSet.contains("keep2.cc")) {
                supportedHostsSet.add("keep2share.cc");
                supportedHostsSet.add("k2s.cc");
                supportedHostsSet.add("keep2s.cc");
                supportedHostsSet.add("keep2.cc");
            }
            supportedHosts = new ArrayList<String>(supportedHostsSet);
        }
        // set array!
        if (supportedHosts == null || supportedHosts.size() == 0) {
            this.setProperty("multiHostSupport", Property.NULL);
        } else {
            this.setProperty("multiHostSupport", supportedHosts);
        }
    }
}
