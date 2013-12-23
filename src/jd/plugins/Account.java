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

import java.util.concurrent.atomic.AtomicBoolean;

import jd.config.Property;
import jd.controlling.AccountController;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;

public class Account extends Property {

    private static final String LATEST_VALID_TIMESTAMP         = "LATEST_VALID_TIMESTAMP";

    public static final String  IS_MULTI_HOSTER_ACCOUNT        = "IS_MULTI_HOSTER_ACCOUNT";

    private static final long   serialVersionUID               = -7578649066389032068L;

    private String              user;
    private String              pass;

    private boolean             enabled                        = true;
    private boolean             concurrentUsePossible          = true;

    public static final String  PROPERTY_TEMP_DISABLED_TIMEOUT = "PROPERTY_TEMP_DISABLED_TIMEOUT";
    public static final String  PROPERTY_REFRESH_TIMEOUT       = "PROPERTY_REFRESH_TIMEOUT";

    public boolean isConcurrentUsePossible() {
        return concurrentUsePossible;
    }

    /**
     * @since JD2
     * */
    public void setConcurrentUsePossible(boolean concurrentUsePossible) {
        this.concurrentUsePossible = concurrentUsePossible;
    }

    private transient volatile long tmpDisabledTimeout = -1;

    public long getTmpDisabledTimeout() {
        return Math.max(-1, tmpDisabledTimeout);
    }

    private transient UniqueAlltimeID       id           = new UniqueAlltimeID();

    /* keep for comp. reasons */
    private String                          hoster       = null;
    private AccountInfo                     accinfo      = null;

    private long                            updatetime   = 0;
    private int                             maxDownloads = 0;

    private transient AccountController     ac           = null;
    private transient PluginForHost         plugin       = null;
    private transient boolean               isMulti      = false;

    private transient volatile AccountError error;

    private transient volatile String       errorString;

    public PluginForHost getPlugin() {
        return plugin;
    }

    public void setPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

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

    public AccountController getAccountController() {
        return ac;
    }

    /**
     * 
     * @param user
     * @param pass
     */
    public Account(final String user, final String pass) {
        this.user = trim(user);
        this.pass = trim(pass);
    }

    public int getMaxSimultanDownloads() {
        return maxDownloads;
    }

    /**
     * -1 = unlimited, 0 = use deprecated getMaxSimultanPremiumDownloadNum/getMaxSimultanFreeDownloadNum,>1 = use this
     * 
     * @since JD2
     * */
    public void setMaxSimultanDownloads(int max) {
        if (max < 0) {
            maxDownloads = -1;
        } else {
            maxDownloads = max;
        }
    }

    public String getPass() {
        return pass;
    }

    public boolean isValid() {
        AccountError lerror = getError();
        return lerror == null || AccountError.TEMP_DISABLED.equals(lerror);
    }

    public long getLastValidTimestamp() {
        return getLongProperty(LATEST_VALID_TIMESTAMP, -1);
    }

    /**
     * @Deprecated Use #setError
     * @param b
     */
    @Deprecated
    public void setValid(final boolean b) {
        if (b) {
            if (getError() == AccountError.INVALID) {
                setError(null, null);
            }
        } else {
            setError(AccountError.INVALID, null);
        }
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

    private AtomicBoolean checking = new AtomicBoolean(false);

    public void setChecking(boolean b) {
        checking.set(b);
    }

    public boolean isChecking() {
        return checking.get();
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
        tmpDisabledTimeout = -1;
        isMulti = false;
        id = new UniqueAlltimeID();
    }

    public UniqueAlltimeID getId() {
        return id;
    }

    public void setId(long id) {
        if (id > 0) {
            this.id = new UniqueAlltimeID(id);
        }
    }

    public boolean isTempDisabled() {
        if (AccountError.TEMP_DISABLED.equals(getError())) {
            synchronized (this) {
                if (getTmpDisabledTimeout() < 0 || System.currentTimeMillis() >= getTmpDisabledTimeout()) {
                    tmpDisabledTimeout = -1;
                    setTempDisabled(false);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public static enum AccountError {
        TEMP_DISABLED,
        EXPIRED,
        INVALID,
        PLUGIN_ERROR;
    }

    public void setError(final AccountError error, String errorString) {
        if (error == null) errorString = null;
        if (this.error != error || !StringUtils.equals(this.errorString, errorString)) {
            if (AccountError.TEMP_DISABLED.equals(error)) {
                long defaultTmpDisabledTimeOut = 60 * 60 * 1000l;
                Long timeout = this.getLongProperty(PROPERTY_TEMP_DISABLED_TIMEOUT, defaultTmpDisabledTimeOut);
                if (timeout == null || timeout <= 0) timeout = defaultTmpDisabledTimeOut;
                this.tmpDisabledTimeout = System.currentTimeMillis() + timeout;
            } else {
                this.tmpDisabledTimeout = -1;
            }
            this.error = error;
            this.errorString = errorString;
            notifyUpdate(AccountProperty.Property.ERROR, error);
        }
    }

    public AccountError getError() {
        return error;
    }

    public String getErrorString() {
        return errorString;
    }

    public void setEnabled(final boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            final AccountInfo ai = accinfo;
            if (enabled && (!isValid() || ai != null && ai.isExpired())) {
                setUpdateTime(0);
            }
            notifyUpdate(AccountProperty.Property.ENABLED, enabled);
        }
    }

    public long getRefreshTimeout() {
        /* default refresh timeout is 30 mins */
        long defaultRefreshTimeOut = 30 * 60 * 1000l;
        Long timeout = this.getLongProperty(PROPERTY_REFRESH_TIMEOUT, defaultRefreshTimeOut);
        if (timeout == null || timeout <= 0) timeout = defaultRefreshTimeOut;
        return timeout;
    }

    public boolean refreshTimeoutReached() {
        if (updatetime <= 0) return true;
        return System.currentTimeMillis() - updatetime >= getRefreshTimeout();
    }

    public static interface AccountPropertyChangeHandler {
        boolean fireAccountPropertyChange(AccountProperty property);
    }

    private transient volatile AccountPropertyChangeHandler notifyHandler = null;

    public void setNotifyHandler(AccountPropertyChangeHandler notifyHandler) {
        this.notifyHandler = notifyHandler;
    }

    private void notifyUpdate(AccountProperty.Property property, Object value) {
        AccountPropertyChangeHandler notify = notifyHandler;
        boolean notifyController = true;
        AccountProperty event = null;
        if (notify != null) {
            event = new AccountProperty(this, property, value);
            notifyController = notify.fireAccountPropertyChange(event);
        }
        notify = getAccountController();
        if (notify != null && notifyController) {
            if (event == null) event = new AccountProperty(this, property, value);
            notifyController = notify.fireAccountPropertyChange(event);
        }
    }

    public void setPass(String newPass) {
        newPass = trim(newPass);
        if (!StringUtils.equals(this.pass, newPass)) {
            this.pass = newPass;
            accinfo = null;
            setUpdateTime(0);
            notifyUpdate(AccountProperty.Property.PASSWORD, newPass);
        }
    }

    public void setTempDisabled(final boolean tempDisabled) {
        if (tempDisabled) {
            setError(AccountError.TEMP_DISABLED, null);
        } else {
            if (AccountError.TEMP_DISABLED.equals(getError())) {
                setError(null, null);
            }
        }
    }

    public void setUser(String newUser) {
        newUser = trim(newUser);
        if (!StringUtils.equals(this.user, newUser)) {
            this.user = newUser;
            accinfo = null;
            setUpdateTime(0);
            notifyUpdate(AccountProperty.Property.USERNAME, newUser);
        }
    }

    public String toString() {
        AccountInfo ai = this.accinfo;
        if (ai != null) {
            return user + ":" + pass + " " + enabled + " " + super.toString() + " AccInfo: " + ai.toString();
        } else {
            return user + ":" + pass + " " + enabled + " " + super.toString();
        }
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

    public boolean isMulti() {
        return isMulti;
    }

    @Override
    public boolean setProperty(String key, Object value) {
        if (IS_MULTI_HOSTER_ACCOUNT.equalsIgnoreCase(key)) {
            isMulti = value != null && Boolean.TRUE.equals(value);
            return false;
        } else {
            return super.setProperty(key, value);
        }

    }

    public void setLastValidTimestamp(long currentTimeMillis) {
        setProperty(LATEST_VALID_TIMESTAMP, currentTimeMillis);
    }

}
