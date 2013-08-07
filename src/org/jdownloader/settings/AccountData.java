package org.jdownloader.settings;

import java.util.HashMap;
import java.util.Map;

import jd.plugins.Account;
import jd.plugins.AccountInfo;

import org.appwork.storage.Storable;

public class AccountData implements Storable {
    private Map<String, Object> properties;

    private String              hoster;

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getHoster() {
        return hoster;
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    public int getMaxSimultanDownloads() {
        return maxSimultanDownloads;
    }

    public void setMaxSimultanDownloads(int maxSimultanDownloads) {
        this.maxSimultanDownloads = maxSimultanDownloads;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private int                 maxSimultanDownloads;
    private String              password;
    private Map<String, Object> infoProperties;
    private long                createTime;
    private long                trafficLeft;
    private long                trafficMax;
    private long                validUntil;
    private boolean             active;
    private boolean             enabled;
    private boolean             tempDisabled;
    private boolean             valid;

    private boolean             trafficUnlimited;
    private boolean             specialtraffic;
    private String              user;

    private boolean             concurrentUsePossible = true;

    public boolean isConcurrentUsePossible() {
        return concurrentUsePossible;
    }

    public void setConcurrentUsePossible(boolean concurrentUsePossible) {
        this.concurrentUsePossible = concurrentUsePossible;
    }

    public AccountData() {
        // reuqired by Storable
    }

    public static AccountData create(Account a) {
        AccountData ret = new AccountData();
        // WARNING: only storable or primitives should be used here
        ret.properties = a.getProperties();
        if (a.getAccountInfo() != null) {
            ret.infoProperties = a.getAccountInfo().getProperties();
            if (ret.infoProperties == null) {
                /*
                 * we need at least an empty hashmap, so account restore also restores account info
                 */
                ret.infoProperties = new HashMap<String, Object>();
            }
            ret.createTime = a.getAccountInfo().getCreateTime();
            ret.trafficLeft = a.getAccountInfo().getTrafficLeft();
            ret.trafficMax = a.getAccountInfo().getTrafficMax();
            ret.validUntil = a.getAccountInfo().getValidUntil();
            ret.trafficUnlimited = a.getAccountInfo().isUnlimitedTraffic();
            // whatever??
            ret.specialtraffic = a.getAccountInfo().isSpecialTraffic();
        }
        ret.concurrentUsePossible = a.isConcurrentUsePossible();
        ret.enabled = a.isEnabled();
        ret.tempDisabled = a.isTempDisabled();
        ret.valid = a.isValid();
        ret.hoster = a.getHoster();
        ret.maxSimultanDownloads = a.getMaxSimultanDownloads();
        ret.password = a.getPass();
        ret.user = a.getUser();

        return ret;
    }

    public Map<String, Object> getInfoProperties() {
        return infoProperties;
    }

    public void setInfoProperties(Map<String, Object> infoProperties) {
        this.infoProperties = infoProperties;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getTrafficLeft() {
        return trafficLeft;
    }

    public void setTrafficLeft(long trafficLeft) {
        this.trafficLeft = trafficLeft;
    }

    public long getTrafficMax() {
        return trafficMax;
    }

    public void setTrafficMax(long trafficMax) {
        this.trafficMax = trafficMax;
    }

    public long getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTempDisabled() {
        return tempDisabled;
    }

    public void setTempDisabled(boolean tempDisabled) {
        this.tempDisabled = tempDisabled;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isTrafficUnlimited() {
        return trafficUnlimited;
    }

    public void setTrafficUnlimited(boolean trafficUnlimited) {
        this.trafficUnlimited = trafficUnlimited;
    }

    public boolean isSpecialtraffic() {
        return specialtraffic;
    }

    public void setSpecialtraffic(boolean specialtraffic) {
        this.specialtraffic = specialtraffic;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Account toAccount() {
        Account ret = new Account(user, password);
        if (infoProperties != null) {
            AccountInfo ai = new AccountInfo();
            ret.setAccountInfo(ai);
            ai.setProperties(infoProperties);
            ai.setCreateTime(createTime);
            ai.setTrafficLeft(trafficLeft);
            ai.setTrafficMax(trafficMax);
            ai.setValidUntil(validUntil);
            if (trafficUnlimited) ai.setUnlimitedTraffic();
            ai.setSpecialTraffic(specialtraffic);
        }
        ret.setConcurrentUsePossible(concurrentUsePossible);
        ret.setEnabled(enabled);
        ret.setHoster(hoster);
        ret.setMaxSimultanDownloads(maxSimultanDownloads);
        ret.setPass(password);
        ret.setProperties(properties);
        ret.setTempDisabled(tempDisabled);
        ret.setUser(user);
        ret.setValid(valid);

        return ret;
    }
}
