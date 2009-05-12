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

import java.util.Date;

import jd.config.Property;
import jd.controlling.AccountController;
import jd.parser.Regex;

public class AccountInfo extends Property {

    private static final long serialVersionUID = 1825140346023286206L;
    public static final String PARAM_INSTANCE = "accountinfo";
    private transient PluginForHost plugin;
    private Account account;
    private boolean account_valid = true;
    private long account_validUntil = -1;
    private long account_trafficLeft = -1;
    private long account_trafficMax = -1;
    private long account_filesNum = -1;
    private long account_premiumPoints = -1;
    private long account_newPremiumPoints = -1;
    private long account_accountBalance = -1;
    private long account_usedSpace = -1;
    private long account_trafficShareLeft = -1;
    private boolean account_expired = false;
    private String account_status;
    private long account_createTime;

    public AccountInfo(PluginForHost plugin, Account account) {
        this.plugin = plugin;
        this.account = account;
        this.account_createTime = System.currentTimeMillis();
    }

    public long getCreateTime() {
        return account_createTime;
    }

    public void setCreateTime(long createTime) {
        this.account_createTime = createTime;
    }

    public Account getAccount() {
        return account;
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

    public long getNewPremiumPoints() {
        return account_newPremiumPoints;
    }

    public PluginForHost getPlugin() {
        return plugin;
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
        return account_expired || (this.getValidUntil() != -1 && this.getValidUntil() < new Date().getTime());
    }

    /**
     * Gibt zurück ob es sich um einen Gültigen Account handelt, logins richtige
     * etc.
     * 
     * @return
     */
    public boolean isValid() {
        return account_valid;
    }

    public void setAccount(Account account) {
        this.account = account;

    }

    public void setAccountBalance(long parseInt) {
        if (account_accountBalance == parseInt) return;
        this.account_accountBalance = parseInt;
    }

    public void setAccountBalance(String string) {
        this.setAccountBalance((long) (Double.parseDouble(string) * 100));
    }

    public void setExpired(boolean b) {
        if (account_expired == b) return;
        this.account_expired = b;
        if (b) {
            this.setTrafficLeft(-1);
        }
        AccountController.getInstance().throwUpdateEvent(plugin, account);
    }

    public void setFilesNum(long parseInt) {
        if (account_filesNum == parseInt) return;
        this.account_filesNum = parseInt;
    }

    public void setNewPremiumPoints(long newPremiumPoints) {
        if (account_newPremiumPoints == newPremiumPoints) return;
        this.account_newPremiumPoints = newPremiumPoints;
    }

    public void setPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public void setPremiumPoints(long parseInt) {
        if (account_premiumPoints == parseInt) return;
        this.account_premiumPoints = parseInt;
    }

    public void setPremiumPoints(String string) {
        this.setPremiumPoints(Integer.parseInt(string.trim()));
    }

    public void setStatus(String string) {
        if (account_status == string) return;
        if (account_status != null && account_status.equals(string)) return;
        this.account_status = string;
    }

    public void setTrafficLeft(long size) {
        if (account_trafficLeft == size) return;
        this.account_trafficLeft = size;
    }

    public void setTrafficLeft(String freeTraffic) {
        this.setTrafficLeft(Regex.getSize(freeTraffic));
    }

    public void setTrafficMax(long trafficMax) {
        if (account_trafficMax == trafficMax) return;
        this.account_trafficMax = trafficMax;
    }

    public void setTrafficShareLeft(long size) {
        if (account_trafficShareLeft == size) return;
        this.account_trafficShareLeft = size;
    }

    public void setUsedSpace(long usedSpace) {
        if (account_usedSpace == usedSpace) return;
        this.account_usedSpace = usedSpace;
    }

    public void setUsedSpace(String string) {
        this.setUsedSpace(Regex.getSize(string));

    }

    public void setValid(boolean b) {
        if (account_valid == b) return;
        this.account_valid = b;
        AccountController.getInstance().throwUpdateEvent(plugin, account);
    }

    /**
     * -1 für Niemals ablaufen
     * 
     * @param validUntil
     */
    public void setValidUntil(long validUntil) {
        if (account_validUntil == validUntil) return;
        this.account_validUntil = validUntil;
        if (validUntil != -1 && validUntil < new Date().getTime()) {
            this.setExpired(true);
        }
    }
}
