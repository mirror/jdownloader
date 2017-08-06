package org.jdownloader.captcha.v2.solver.solver9kw;

import java.util.HashMap;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface Captcha9kwSettings extends ChallengeSolverConfig {

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Your (User) ApiKey from 9kw.eu")
    String getApiKey();

    void setApiKey(String jser);

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
    @DefaultBooleanValue(true)
    boolean isEnabledGlobally();

    void setEnabledGlobally(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the Puzzle Captchas")
    boolean ispuzzle();

    void setpuzzle(boolean b);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Hosteroptions for 9kw.eu")
    String gethosteroptions();

    void sethosteroptions(String jser);

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
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Captcha whitelist for hoster with prio")
    String getwhitelistprio();

    void setwhitelistprio(String jser);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Captcha blacklist for hoster with prio")
    String getblacklistprio();

    void setblacklistprio(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the blacklist with prio")
    boolean getblacklistpriocheck();

    void setblacklistpriocheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
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
    @DefaultIntValue(0)
    @SpinnerValidator(min = 0, max = 9999)
    @DescriptionForConfigEntry("Max. Captchas per minute")
    int getminute();

    void setminute(int seconds);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Only https requests to 9kw.eu")
    boolean ishttps();

    void sethttps(boolean b);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Captcha whitelist for hoster")
    String getwhitelist();

    void setwhitelist(String jser);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Captcha blacklist for hoster")
    String getblacklist();

    void setblacklist(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the blacklist")
    boolean getblacklistcheck();

    void setblacklistcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
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
    @DescriptionForConfigEntry("Activate the option selfsolve (sandbox)")
    boolean isSelfsolve();

    void setSelfsolve(boolean b);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is required after changes")
    @DefaultIntValue(1)
    @SpinnerValidator(min = 0, max = 20)
    @DescriptionForConfigEntry("Max. Captchas Parallel")
    int getThreadpoolSize();

    void setThreadpoolSize(int size);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Captcha whitelist for hoster with timeout")
    String getwhitelisttimeout();

    void setwhitelisttimeout(String jser);

    @AboutConfig
    @DefaultStringValue("")
    @DescriptionForConfigEntry("Captcha blacklist for hoster with timeout")
    String getblacklisttimeout();

    void setblacklisttimeout(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the blacklist with timeout")
    boolean getblacklisttimeoutcheck();

    void setblacklisttimeoutcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the whitelist with timeout")
    boolean getwhitelisttimeoutcheck();

    void setwhitelisttimeoutcheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Activate the lowcredits dialog")
    boolean getlowcredits();

    void setlowcredits(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the high queue dialog")
    boolean gethighqueue();

    void sethighqueue(boolean b);

    @AboutConfig
    @DefaultIntValue(600000)
    @SpinnerValidator(min = 75000, max = 3999000)
    @DescriptionForConfigEntry("Default max. Timeout in ms")
    int getDefaultMaxTimeout();

    void setDefaultMaxTimeout(int ms);

    @AboutConfig
    @DefaultJsonObject("{\"jdownloader.org\":60000}")
    @DescriptionForConfigEntry("Host bound Waittime before using CES. Use CaptchaExchangeChanceToSkipBubbleTimeout for a global timeout")
    HashMap<String, Integer> getBubbleTimeoutByHostMap();

    void setBubbleTimeoutByHostMap(HashMap<String, Integer> map);

    @DefaultBooleanValue(false)
    boolean isEnabled();

}
