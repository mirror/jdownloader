//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SecretfileNet extends XFileSharingProBasic {
    public SecretfileNet(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-09-02: Premium limits == Free limits (well, at least [no] chunks, [no] resume!) <br />
     * captchatype-info: 2020-09-02: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "secretfile.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return false;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        AccountInfo ai = null;
        loginWebsite(account, true);
        final String account_info_url_relative = getRelativeAccountInfoURL();
        /*
         * Only access URL if we haven't accessed it before already. Some sites will redirect to their Account-Info page right after
         * logging-in or our login-function when it is verifying cookies and not performing a full login.
         */
        if (br.getURL() == null || !br.getURL().contains(account_info_url_relative)) {
            getPage(this.getMainPage() + account_info_url_relative);
        }
        boolean api_success = false;
        {
            /*
             * 2019-07-11: apikey handling - prefer account info via API instead of website if allowed.
             */
            String apikey = null;
            try {
                /*
                 * 2019-08-13: Do not hand over corrected_br as source as correctBR() might remove important parts of the html and because
                 * XFS owners will usually not add html traps into the html of accounts (especially ) we can use the original unmodified
                 * html here.
                 */
                apikey = this.findAPIKey(br.toString());
            } catch (InterruptedException e) {
                throw e;
            } catch (final Throwable e) {
                /*
                 * 2019-08-16: All kinds of errors may happen when trying to access the API. It is preferable if it works but we cannot rely
                 * on it working so we need that website fallback!
                 */
                logger.info("Failed to find apikey (with Exception) --> Continuing via website");
                logger.log(e);
            }
            if (apikey != null) {
                /*
                 * 2019-07-11: Use API even if 'supports_api()' is disabled because if it works it is a much quicker and more reliable way
                 * to get account information.
                 */
                logger.info("Found apikey --> Trying to get accountinfo via API");
                /* Save apikey for possible future usage */
                synchronized (account) {
                    account.setProperty(PROPERTY_ACCOUNT_apikey, apikey);
                    try {
                        ai = this.fetchAccountInfoAPI(this.br.cloneBrowser(), account);
                        api_success = ai != null;
                    } catch (final InterruptedException e) {
                        throw e;
                    } catch (final Throwable e) {
                        e.printStackTrace();
                        logger.warning("Failed to find accountinfo via API even though apikey is given; probably serverside API failure --> Fallback to website handling");
                    }
                }
            }
        }
        if (ai == null) {
            /*
             * apikey can also be used e.g. for mass-linkchecking - make sure that we keep only a valid apikey --> Remove apikey if
             * accountcheck via API fails!
             */
            account.removeProperty(PROPERTY_ACCOUNT_apikey);
            /*
             * Do not remove the saved API domain because if a user e.g. adds an apikey without adding an account later on, it might still
             * be useful!
             */
            // this.getPluginConfig().removeProperty(PROPERTY_PLUGIN_api_domain_with_protocol);
            /* Use new AccountInfo object to use with account data from website. */
            ai = new AccountInfo();
        }
        if (api_success && !ai.isUnlimitedTraffic()) {
            /* trafficleft given via API. TODO: Allow fetchAccountInfoAPI to set unlimited traffic and trust it here. */
            logger.info("Successfully found complete AccountInfo with trafficleft via API");
            return ai;
        }
        /*
         * trafficleft is usually not given via API so we'll have to check for it via website. Also we do not trsut 'unlimited traffic' via
         * API yet.
         */
        String trafficLeftStr = regExTrafficLeft();
        /* Example non english: brupload.net */
        final boolean userHasUnlimitedTraffic = trafficLeftStr != null && trafficLeftStr.matches(".*?(nlimited|Ilimitado).*?");
        if (trafficLeftStr != null && !userHasUnlimitedTraffic && !trafficLeftStr.equalsIgnoreCase("Mb")) {
            trafficLeftStr = Encoding.htmlDecode(trafficLeftStr);
            trafficLeftStr.trim();
            /* Need to set 0 traffic left, as getSize returns positive result, even when negative value supplied. */
            long trafficLeft = 0;
            if (trafficLeftStr.startsWith("-")) {
                /* Negative traffic value = User downloaded more than he is allowed to (rare case) --> No traffic left */
                trafficLeft = 0;
            } else {
                trafficLeft = SizeFormatter.getSize(trafficLeftStr);
            }
            /* 2019-02-19: Users can buy additional traffic packages: Example(s): subyshare.com */
            final String usableBandwidth = br.getRegex("Usable Bandwidth\\s*<span[^>]*>\\s*([0-9\\.]+\\s*[TGMKB]+)\\s*/\\s*[0-9\\.]+\\s*[TGMKB]+\\s*<").getMatch(0);
            if (usableBandwidth != null) {
                trafficLeft = Math.max(trafficLeft, SizeFormatter.getSize(usableBandwidth));
            }
            ai.setTrafficLeft(trafficLeft);
        } else {
            ai.setUnlimitedTraffic();
        }
        if (api_success) {
            logger.info("Successfully found AccountInfo without trafficleft via API (fetched trafficleft via website)");
            return ai;
        }
        /* 2019-07-11: It is not uncommon for XFS websites to display expire-dates even though the account is not premium anymore! */
        String expireStr = new Regex(correctedBR, "(\\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \\d{4})").getMatch(0);
        final String expireStrSpecial = new Regex(correctedBR, "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
        long expire_milliseconds = 0;
        long expire_milliseconds_from_expiredate = 0;
        long expire_milliseconds_precise_to_the_second = 0;
        if (expireStr != null) {
            /*
             * 2019-12-17: XFS premium accounts usually don't expire just before the next day. They will end to the same time of the day
             * when they were bought but website only displays it to the day which is why we set it to just before the next day to prevent
             * them from expiring too early in JD. XFS websites with API may provide more precise information on the expiredate (down to the
             * second).
             */
            expireStr += " 23:59:59";
            expire_milliseconds_from_expiredate = TimeFormatter.getMilliSeconds(expireStr, "dd MMMM yyyy HH:mm:ss", Locale.ENGLISH);
        } else if (expireStrSpecial != null) {
            expire_milliseconds_precise_to_the_second = TimeFormatter.getMilliSeconds(expireStrSpecial, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        final String[] supports_precise_expire_date = this.supports_precise_expire_date();
        if (supports_precise_expire_date != null && supports_precise_expire_date.length > 0 && expire_milliseconds_precise_to_the_second <= 0) {
            /*
             * A more accurate expire time, down to the second. Usually shown on 'extend premium account' page. Case[0] e.g. 'flashbit.cc',
             * Case [1] e.g. takefile.link, example website which has no precise expiredate at all: anzfile.net
             */
            final List<String> paymentURLs;
            final String last_working_payment_url = this.getPluginConfig().getStringProperty("property_last_working_payment_url", null);
            if (last_working_payment_url != null) {
                paymentURLs = new ArrayList<String>();
                logger.info("Found stored last_working_payment_url --> Trying this first in an attempt to save http requests: " + last_working_payment_url);
                paymentURLs.add(last_working_payment_url);
                /* Add all remaining URLs, start with the last working one */
                for (final String paymentURL : supports_precise_expire_date) {
                    if (!paymentURLs.contains(paymentURL)) {
                        paymentURLs.add(paymentURL);
                    }
                }
            } else {
                /* Add all possible payment URLs. */
                logger.info("last_working_payment_url is not available --> Going through all possible paymentURLs");
                paymentURLs = Arrays.asList(supports_precise_expire_date);
            }
            /* Go through possible paymentURLs in an attempt to find an exact expiredate if the account is premium. */
            for (final String paymentURL : paymentURLs) {
                if (StringUtils.isEmpty(paymentURL)) {
                    continue;
                } else {
                    try {
                        getPage(paymentURL);
                    } catch (final Throwable e) {
                        logger.log(e);
                        /* Skip failures due to timeout or bad http error-responses */
                        continue;
                    }
                }
                /* Find html snippet which should contain our expiredate. */
                final String preciseExpireHTML = new Regex(correctedBR, "<div[^>]*class=\"accexpire\"[^>]*>.*?</div>").getMatch(-1);
                String expireSecond = new Regex(preciseExpireHTML, "Premium(-| )Account expires?\\s*:\\s*(?:</span>)?\\s*(?:<span>)?\\s*([a-zA-Z0-9, ]+)\\s*</").getMatch(-1);
                if (StringUtils.isEmpty(expireSecond)) {
                    /*
                     * Last attempt - wider RegEx but we expect the 'second(s)' value to always be present!! Example: file-up.org:
                     * "<p style="direction: ltr; display: inline-block;">1 year, 352 days, 22 hours, 36 minutes, 45 seconds</p>"
                     */
                    expireSecond = new Regex(preciseExpireHTML, Pattern.compile(">\\s*(\\d+ years?, )?(\\d+ days?, )?(\\d+ hours?, )?(\\d+ minutes?, )?\\d+ seconds\\s*<", Pattern.CASE_INSENSITIVE)).getMatch(-1);
                }
                if (StringUtils.isEmpty(expireSecond) && !StringUtils.isEmpty(preciseExpireHTML)) {
                    /*
                     * 2019-09-07: This html-class may also be given for non-premium accounts e.g. fileup.cc
                     */
                    logger.info("html contains 'accexpire' class but we failed to find a precise expiredate --> Either we have a free account or failed to find precise expiredate although it is given");
                }
                if (!StringUtils.isEmpty(expireSecond)) {
                    String tmpYears = new Regex(expireSecond, "(\\d+)\\s+years?").getMatch(0);
                    String tmpdays = new Regex(expireSecond, "(\\d+)\\s+days?").getMatch(0);
                    String tmphrs = new Regex(expireSecond, "(\\d+)\\s+hours?").getMatch(0);
                    String tmpmin = new Regex(expireSecond, "(\\d+)\\s+minutes?").getMatch(0);
                    String tmpsec = new Regex(expireSecond, "(\\d+)\\s+seconds?").getMatch(0);
                    long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
                    if (!StringUtils.isEmpty(tmpYears)) {
                        years = Integer.parseInt(tmpYears);
                    }
                    if (!StringUtils.isEmpty(tmpdays)) {
                        days = Integer.parseInt(tmpdays);
                    }
                    if (!StringUtils.isEmpty(tmphrs)) {
                        hours = Integer.parseInt(tmphrs);
                    }
                    if (!StringUtils.isEmpty(tmpmin)) {
                        minutes = Integer.parseInt(tmpmin);
                    }
                    if (!StringUtils.isEmpty(tmpsec)) {
                        seconds = Integer.parseInt(tmpsec);
                    }
                    expire_milliseconds_precise_to_the_second = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                }
                if (expire_milliseconds_precise_to_the_second > 0) {
                    /* Later we will decide whether we are going to use this value or not. */
                    logger.info("Successfully found precise expire-date via paymentURL: \"" + paymentURL + "\" : " + expireSecond);
                    this.getPluginConfig().setProperty("property_last_working_payment_url", paymentURL);
                    break;
                } else {
                    logger.info("Failed to find precise expire-date via paymentURL: \"" + paymentURL + "\"");
                }
            }
        }
        final long currentTime = br.getCurrentServerTime(System.currentTimeMillis());
        if (expire_milliseconds_precise_to_the_second > 0) {
            /* Add current time to parsed value */
            expire_milliseconds_precise_to_the_second += currentTime;
        }
        if (expire_milliseconds_precise_to_the_second > 0) {
            logger.info("Using precise expire-date");
            expire_milliseconds = expire_milliseconds_precise_to_the_second;
        } else if (expire_milliseconds_from_expiredate > 0) {
            logger.info("Using expire-date which is up to 24 hours precise");
            expire_milliseconds = expire_milliseconds_from_expiredate;
        } else {
            logger.info("Failed to find any useful expire-date at all");
        }
        if ((expire_milliseconds - currentTime) <= 0) {
            /* If the premium account is expired or we cannot find an expire-date we'll simply accept it as a free account. */
            if (expire_milliseconds > 0) {
                logger.info("Premium expired --> Free account");
            } else {
                logger.info("Account is a FREE account as no expiredate has been found");
            }
            setAccountLimitsByType(account, AccountType.FREE);
        } else {
            /* Expire date is in the future --> It is a premium account */
            logger.info("Premium account");
            ai.setValidUntil(expire_milliseconds);
            setAccountLimitsByType(account, AccountType.PREMIUM);
        }
        return ai;
    }
}