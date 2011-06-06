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
import jd.controlling.AccountController;

public class Account extends Property {

    private static final long serialVersionUID      = -7578649066389032068L;

    private String            user;
    private String            pass;

    private boolean           enabled               = true;
    private boolean           valid                 = true;

    private transient long    tmpDisabledIntervalv3 = 10 * 60 * 1000;
    private transient boolean tempDisabled          = false;
    private transient long    tmpDisabledTime       = 0;

    private String            hoster                = null;
    private AccountInfo       accinfo               = null;
    /**
     * if an account becomes invalid, or outdated, we can set active to false.
     * enabled should be used for user en/disable active should be used to
     * en/disable the account programmatically
     */
    private boolean           active;

    public boolean isActive() {

        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private long                        updatetime   = 0;

    private int                         maxDownloads = 0;

    private transient AccountController ac           = null;

    /**
     * 
     * @param string
     * @return
     */
    private static final String trim(final String string) {
        return (string == null) ? null : string.trim();
    }

    public void setAccountController(AccountController ac) {
        this.ac = ac;
    }

    /**
     * 
     * @param user
     * @param pass
     */
    public Account(final String user, final String pass) {
        this.user = trim(user);
        this.pass = trim(pass);
        // this.hoster = null;
        this.setTmpDisabledIntervalv3(10 * 60 * 1000l);
    }

    public int getMaxSimultanDownloads() {
        return maxDownloads;
    }

    /*-1 = unlimited, 0 = use deprecated getMaxSimultanPremiumDownloadNum/getMaxSimultanFreeDownloadNum,>1 = use this*/
    public void setMaxSimultanDownloads(int max) {
        if (max < 0) {
            maxDownloads = -1;
        } else {
            maxDownloads = max;
        }
    }

    public String getPass() {
        // if (pass != null) return pass.trim();
        // return null;
        return pass;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(final boolean b) {
        valid = b;
    }

    public long lastUpdateTime() {
        return updatetime;
    }

    public void setUpdateTime(final long l) {
        updatetime = l;
    }

    public String getHoster() {
        return hoster;
    }

    public void setHoster(final String h) {
        hoster = h;
    }

    public AccountInfo getAccountInfo() {
        return accinfo;
    }

    public void setAccountInfo(final AccountInfo info) {
        accinfo = info;
    }

    public String getUser() {
        // if (user != null) return user.trim();
        // return null;
        return user;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void readObject(final java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        /* nach dem deserialisieren sollen die transienten neu geholt werden */
        stream.defaultReadObject();

        tmpDisabledIntervalv3 = 10 * 60 * 1000l;
        tempDisabled = false;
        tmpDisabledTime = 0;
    }

    public boolean isTempDisabled() {
        if (tempDisabled && (System.currentTimeMillis() - tmpDisabledTime) > this.getTmpDisabledIntervalv3()) {
            tempDisabled = false;
        }
        return tempDisabled;
    }

    public void setEnabled(final boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            final AccountInfo ai = accinfo;
            if (enabled && (!isValid() || ai != null && ai.isExpired())) {
                setUpdateTime(0);
            }
            if (ac != null) ac.throwUpdateEvent(null, this);
        }
    }

    public void setPass(final String pass) {
        // if (this.pass == pass) return;
        // if (pass != null) pass = pass.trim();
        // if (this.pass != null && this.pass.equals(pass)) return;
        // this.pass = pass;
        final String newPass = trim(pass);
        if (this.pass != newPass && (this.pass == null || !this.pass.equals(newPass))) {
            this.pass = newPass;
            accinfo = null;
            setUpdateTime(0);
            if (ac != null) ac.throwUpdateEvent(null, this);
        }
    }

    public void setTempDisabled(final boolean tempDisabled) {
        if (this.tempDisabled != tempDisabled) {
            this.tmpDisabledTime = System.currentTimeMillis();
            this.tempDisabled = tempDisabled;
            if (ac != null) ac.throwUpdateEvent(null, this);
        }
    }

    public void setUser(final String user) {
        final String newUser = trim(user);
        if (this.user != newUser && (this.user == null || !this.user.equals(newUser))) {
            accinfo = null;
            setUpdateTime(0);
            this.user = newUser;
            if (ac != null) ac.throwUpdateEvent(null, this);
        }
    }

    // @Override
    public String toString() {
        return user + ":" + pass + " " + enabled + " " + super.toString();
    }

    /**
     * returns how lon a premiumaccount will stay disabled if he got temporary
     * disabled
     * 
     * @return
     */
    public long getTmpDisabledIntervalv3() {
        return tmpDisabledIntervalv3;
    }

    public void setTmpDisabledIntervalv3(final long tmpDisabledInterval) {
        this.tmpDisabledIntervalv3 = tmpDisabledInterval;
    }

    public boolean equals(final Account account2) {
        if (account2 == null) return false;
        if (account2 == this) return true;
        if (this.user == null) {
            if (account2.user != null) { return false; }
        } else {
            if (account2.user == null || !this.user.equalsIgnoreCase(account2.user)) { return false; }
        }

        if (this.pass == null) {
            if (account2.pass != null) { return false; }
        } else {
            if (account2.pass == null || !this.pass.equalsIgnoreCase(account2.pass)) { return false; }
        }
        return true;
    }
}
