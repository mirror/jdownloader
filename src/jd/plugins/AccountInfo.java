package jd.plugins;

import java.util.Date;

import jd.config.Property;

public class AccountInfo extends Property {

    /**
     * 
     */
    private static final long serialVersionUID = 1825140346023286206L;
    private PluginForHost plugin;
    private Account account;
    private boolean valid = true;
    private long validUntil = -1;
    private long trafficLeft = -1;
    private int filesNum = -1;
    private int premiumPoints = -1;
    private int accountBalance = -1;
    private long usedSpace = -1;
    private long trafficShareLeft = -1;
    private boolean expired = false;

    public AccountInfo(PluginForHost plugin, Account account) {
        this.plugin = plugin;
        this.account = account;
    }

    public void setValid(boolean b) {
        this.valid = b;

    }

    public void setValidUntil(long time) {
        validUntil = time;

    }

    public void setTrafficLeft(long size) {
        this.trafficLeft = size;

    }

    public void setFilesNum(int parseInt) {
        this.filesNum = parseInt;

    }

    public void setPremiumPoints(int parseInt) {
        this.premiumPoints = parseInt;

    }

    public void setAccountBalance(int parseInt) {
        this.accountBalance = parseInt;

    }

    public void setUsedSpace(long size) {
        usedSpace = size;

    }

    public void setTrafficShareLeft(long size) {
        this.trafficShareLeft = size;

    }

    public void setExpired(boolean b) {
        this.expired = b;

    }

    public PluginForHost getPlugin() {
        return plugin;
    }

    public void setPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    /**
     * GIbt zurück ob es sich um einen Gültigen Account handelt, logins richtige
     * etc.
     * 
     * @return
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * gibt einen Timestamp zurück zu dem der Account auslaufen wird.bzw
     * ausgelaufen ist.
     * 
     * @return
     */
    public long getValidUntil() {
        return validUntil;
    }

    /**
     * Gibt an wieviel Traffic noch frei ist (in bytes)
     * 
     * @return
     */
    public long getTrafficLeft() {
        return trafficLeft;
    }

    /**
     * Gibt zurück wieviele File sauf dem Account hochgeladen sind
     * 
     * @return
     */
    public int getFilesNum() {
        return filesNum;
    }

    /**
     * GIbt an wieviel premiumpunkte der Account hat
     * 
     * @return
     */
    public int getPremiumPoints() {
        return premiumPoints;
    }

    /**
     * Gibt zurück wieviel (in Cent) geld gerade auf diesem Account ist
     * 
     * @return
     */
    public int getAccountBalance() {
        return accountBalance;
    }

    /**
     * Gibt zurück wieviel PLatz (bytes) die uploads auf diesem account belegen
     * 
     * @return
     */
    public long getUsedSpace() {
        return usedSpace;
    }

    /**
     * gibt zurück wieviel TRafficshareonch übrig ist (in bytes). TRafficshare
     * ist Traffic, den man über einen premiumaccount Freeusern zur verfügung
     * stellen kann. -1: Feature ist nicht unterstützt
     * 
     * @return
     */
    public long getTrafficShareLeft() {
        return trafficShareLeft;
    }

    /**
     * Gibt zurück ob der accoun abgelaufen ist
     * 
     * @return
     */
    public boolean isExpired() {
        return expired||this.getValidUntil()<new Date().getTime();
    }

}
