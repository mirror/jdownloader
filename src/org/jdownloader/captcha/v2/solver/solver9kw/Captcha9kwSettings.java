package org.jdownloader.captcha.v2.solver.solver9kw;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface Captcha9kwSettings extends ConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Your (User) ApiKey from 9kw.eu")
    String getApiKey();

    void setApiKey(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the 9kw.eu service")
    boolean isEnabled();

    void setEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the debugmode for 9kw.eu service")
    boolean isDebug();

    void setDebug(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the Mouse Captchas")
    boolean ismouse();

    void setmouse(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Confirm option for captchas (Cost +6)")
    boolean isconfirm();

    void setconfirm(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Confirm option for mouse captchas (Cost +6)")
    boolean ismouseconfirm();

    void setmouseconfirm(boolean b);

    @AboutConfig
    @DefaultIntValue(0)
    @SpinnerValidator(min = 0, max = 20)
    @DescriptionForConfigEntry("More priority for captchas (Cost +1-20)")
    int getprio();

    void setprio(int seconds);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha whitelist for hoster with prio")
    String getwhitelistprio();

    void setwhitelistprio(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha blacklist for hoster with prio")
    String getblacklistprio();

    void setblacklistprio(String jser);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the blacklist with prio")
    boolean getblacklistpriocheck();

    void setblacklistpriocheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the whitelist with prio")
    boolean getwhitelistpriocheck();

    void setwhitelistpriocheck(boolean b);

    @AboutConfig
    @DefaultIntValue(0)
    @SpinnerValidator(min = 0, max = 9999)
    @DescriptionForConfigEntry("Max. Captchas per hour")
    int gethour();

    void sethour(int seconds);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Only https requests to 9kw.eu")
    boolean ishttps();

    void sethttps(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha whitelist for hoster")
    String getwhitelist();

    void setwhitelist(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha blacklist for hoster")
    String getblacklist();

    void setblacklist(String jser);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the blacklist")
    boolean getblacklistcheck();

    void setblacklistcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the whitelist")
    boolean getwhitelistcheck();

    void setwhitelistcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the Captcha Feedback")
    boolean isfeedback();

    void setfeedback(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the option selfsolve")
    boolean isSelfsolve();

    void setSelfsolve(boolean b);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is required after changes")
    @DefaultIntValue(3)
    @SpinnerValidator(min = 0, max = 10)
    @DescriptionForConfigEntry("Max. Captchas Parallel")
    int getThreadpoolSize();

    void setThreadpoolSize(int size);

    @AboutConfig
    @DefaultIntValue(600000)
    @SpinnerValidator(min = 60000, max = Integer.MAX_VALUE)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Other max. timeout only for 9kw Service")
    int getCaptchaOther9kwTimeout();

    void setCaptchaOther9kwTimeout(int ms);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha whitelist for hoster with timeout")
    String getwhitelisttimeout();

    void setwhitelisttimeout(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha blacklist for hoster with timeout")
    String getblacklisttimeout();

    void setblacklisttimeout(String jser);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the blacklist with timeout")
    boolean getblacklisttimeoutcheck();

    void setblacklisttimeoutcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the whitelist with timeout")
    boolean getwhitelisttimeoutcheck();

    void setwhitelisttimeoutcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the lowcredits dialog")
    boolean getlowcredits();

    void setlowcredits(boolean b);
}
