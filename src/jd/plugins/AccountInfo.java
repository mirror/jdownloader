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

import jd.config.Property;
import jd.parser.Regex;

public class AccountInfo extends Property {

    private static final long serialVersionUID = 1825140346023286206L;
    private long account_validUntil = -1;
    private long account_trafficLeft = -1;
    private long account_trafficMax = -1;
    private long account_filesNum = -1;
    private long account_premiumPoints = -1;
    private long account_accountBalance = -1;
    private long account_usedSpace = -1;
    private long account_trafficShareLeft = -1;
    private boolean unlimitedTraffic = true;
    private boolean account_expired = false;
    private String account_status;
    private long account_createTime = 0;
    /**
     * indicator that host, account has special traffic handling, do not temp
     * disable if traffic =0
     */
    private boolean specialTraffic = false;

    public long getCreateTime() {
        return account_createTime;
    }

    public void setSpecialTraffic(boolean b) {
        specialTraffic = b;
    }

    public boolean isSpecialTraffic() {
        return specialTraffic;
    }

    public void setCreateTime(long createTime) {
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
        return account_trafficLeft;
    }

    public long getTrafficMax() {
        return Math.max(account_trafficLeft, account_trafficMax);
    }

    /**
     * Gibt zurück wieviel Trafficshareonch übrig ist (in bytes). Trafficshare
     * ist Traffic, den man über einen PremiumAccount den Freeusern zur
     * Verfügung stellen kann. -1: Feature ist nicht unterstützt
     * 
     * @return
     */
    public long getTrafficShareLeft() {
        return account_trafficShareLeft;
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
     * Gibt einen Timestamp zurück zu dem der Account auslaufen wird bzw.
     * ausgelaufen ist.(-1 für Nie)
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
        validUntilCheck();
        return account_expired;
    }

    public void setAccountBalance(long parseInt) {
        this.account_accountBalance = parseInt;
    }

    public void setAccountBalance(String string) {
        this.setAccountBalance((long) (Double.parseDouble(string) * 100));
    }

    public void setExpired(boolean b) {
        this.account_expired = b;
        if (b) {
            this.setTrafficLeft(-1);
        }
    }

    public void setFilesNum(long parseInt) {
        this.account_filesNum = parseInt;
    }

    public void setPremiumPoints(long parseInt) {
        this.account_premiumPoints = parseInt;
    }

    public void setPremiumPoints(String string) {
        this.setPremiumPoints(Integer.parseInt(string.trim()));
    }

    public void setStatus(String string) {
        this.account_status = string;
    }

    /*
     * -1 = unlimited Traffic
     */
    public void setTrafficLeft(long size) {
        this.account_trafficLeft = size;
        if (account_trafficLeft != -1) {
            unlimitedTraffic = false;
        }
    }

    public boolean isUnlimitedTraffic() {
        return unlimitedTraffic;
    }

    public void setTrafficLeft(String freeTraffic) {
        this.setTrafficLeft(Regex.getSize(freeTraffic));
    }

    public void setTrafficMax(long trafficMax) {
        this.account_trafficMax = trafficMax;
    }

    public void setTrafficShareLeft(long size) {
        this.account_trafficShareLeft = size;
    }

    public void setUsedSpace(long usedSpace) {
        this.account_usedSpace = usedSpace;
    }

    public void setUsedSpace(String string) {
        this.setUsedSpace(Regex.getSize(string));

    }

    /**
     * -1 für Niemals ablaufen
     * 
     * @param validUntil
     */
    public void setValidUntil(long validUntil) {
        if (account_validUntil == validUntil) return;
        this.account_validUntil = validUntil;
        validUntilCheck();
    }

    public void validUntilCheck() {
        if (account_validUntil != -1) {
            long cur = System.currentTimeMillis();
            if (account_validUntil < cur) this.setExpired(true);
        }
    }

}
