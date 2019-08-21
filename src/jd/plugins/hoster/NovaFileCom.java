package jd.plugins.hoster;

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
//along with this program.  If not, see <http://www.gnu.org/licenses/>.;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.XFileSharingProBasicSpecialFilejoker;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class NovaFileCom extends XFileSharingProBasicSpecialFilejoker {
    public NovaFileCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
        this.setConfigElements();
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-08-21: All checked and set <br />
     * captchatype-info: 2019-08-21: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "novafile.com" });
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
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2019-08-21: Special */
        fileInfo[0] = new Regex(correctedBR, "class=\"name\">([^<>\"]+)<").getMatch(0);
        fileInfo[1] = new Regex(correctedBR, "class=\"size\">([^<>\"]+)<").getMatch(0);
        if (StringUtils.isEmpty(fileInfo[0]) || StringUtils.isEmpty(fileInfo[1])) {
            /* Fallback to template method */
            super.scanInfo(fileInfo);
        }
        return fileInfo;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-08-21: Special */
        return false;
    }

    @Override
    protected String regexWaittime() {
        /* 2019-08-21: Special */
        String wait = new Regex(correctedBR, "class=\"alert\\-success[^\"]*\">(\\d+)</span>").getMatch(0);
        if (StringUtils.isEmpty(wait)) {
            wait = super.regexWaittime();
        }
        return wait;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-08-21: Special */
        super.checkErrors(link, account, checkAll);
        if (new Regex(correctedBR, "You have reached the daily download limit").matches()) {
            /* It does not tell us how long we have to wait */
            if (account != null) {
                throw new AccountUnavailableException("You have reached the daily download limit", FREE_RECONNECTWAIT_DEFAULT);
            } else {
                this.setDownloadStarted(link, FREE_RECONNECTWAIT_DEFAULT);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached the daily download limit", FREE_RECONNECTWAIT_DEFAULT);
            }
        }
    }

    @Override
    protected String getRelativeAccountInfoURL() {
        /* 2019-08-20: Special */
        return "/profile";
    }

    @Override
    protected boolean allows_multiple_login_attempts_in_one_go() {
        /* 2019-08-21: Special */
        return true;
    }

    @Override
    public boolean isPremiumOnlyHTML() {
        /* 2019-08-21 */
        return new Regex(correctedBR, ">\\s*This file can only be downloaded by Premium").matches();
    }

    @Override
    protected String getDllink(final DownloadLink downloadLink, final Account account, final Browser br, final String src) {
        /* 2019-08-21: Special */
        final String dllink = super.getDllink(downloadLink, account, br, src);
        if (dllink != null) {
            /*
             * 2019-08-21: This is kind of a workaround: If we found a downloadlink we will soon start the download --> Save timestamp so
             * our special reconnect handling knows that the limit is reached now and user will not have to solve extra captchas!
             */
            setDownloadStarted(downloadLink, 0);
        }
        return dllink;
    }

    /* *************************** SPECIAL RECONNECT STUFF STARTS HERE *************************** */
    @Override
    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        final String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink != null) {
            /*
             * Found directurl? Start download! This is there to prevent our experimental reconnect handling from triggering when the user
             * starts downloads which have been started before and thus have a working saved directurl stored!
             */
            super.handleDownload(link, account, dllink, null);
        } else {
            /* No directurl? Check for saved reconnect-limit and if there is none, continue via template-handling! */
            currentIP.set(this.getIP());
            synchronized (CTRLLOCK) {
                /* Load list of saved IPs + timestamp of last download */
                final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LASTDOWNLOAD);
                if (lastdownloadmap != null && lastdownloadmap instanceof HashMap && blockedIPsMap.isEmpty()) {
                    blockedIPsMap = (HashMap<String, Long>) lastdownloadmap;
                }
            }
            /**
             * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
             */
            if (this.getPluginConfig().getBooleanProperty(EXPERIMENTALHANDLING, default_eh)) {
                /*
                 * If the user starts a download in free (unregistered) mode the waittime is on his IP. This also affects free accounts if
                 * he tries to start more downloads via free accounts afterwards BUT nontheless the limit is only on his IP so he CAN
                 * download using the same free accounts after performing a reconnect!
                 */
                long lastdownload = getPluginSavedLastDownloadTimestamp();
                long passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
                if (passedTimeSinceLastDl < FREE_RECONNECTWAIT_DEFAULT) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT_DEFAULT - passedTimeSinceLastDl);
                }
            }
            super.doFree(link, account);
        }
    }

    private static final long              FREE_RECONNECTWAIT_DEFAULT = 45 * 60 * 1000L;
    private static String[]                IPCHECK                    = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };
    private final String                   EXPERIMENTALHANDLING       = "EXPERIMENTALHANDLING";
    private Pattern                        IPREGEX                    = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicReference<String> lastIP                     = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                  = new AtomicReference<String>();
    private static HashMap<String, Long>   blockedIPsMap              = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                   = new Object();
    private String                         PROPERTY_LASTIP            = "NOVAFILE_PROPERTY_LASTIP";
    private static final String            PROPERTY_LASTDOWNLOAD      = "NOVAFILE_lastdownload_timestamp";

    @SuppressWarnings("deprecation")
    private void setDownloadStarted(final DownloadLink dl, final long remaining_reconnect_wait) {
        synchronized (CTRLLOCK) {
            try {
                final long timestamp_download_started;
                if (remaining_reconnect_wait > 0) {
                    /*
                     * FREE_RECONNECTWAIT minus remaining wait = We know when the user started his download - we want to get the timestamp.
                     * Add 1 minute to make sure that we wait long enough!
                     */
                    long timePassed = FREE_RECONNECTWAIT_DEFAULT - remaining_reconnect_wait;
                    /* Errorhandling for invalid values */
                    if (timePassed < 0) {
                        timePassed = 0;
                    }
                    timestamp_download_started = System.currentTimeMillis() - timePassed;
                } else {
                    /*
                     * Nothing given unknown starttime, wrong inputvalue 'remaining_reconnect_wait' or user has started the download just
                     * now.
                     */
                    timestamp_download_started = System.currentTimeMillis();
                }
                blockedIPsMap.put(currentIP.get(), timestamp_download_started);
                setIP(dl, null);
                getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, blockedIPsMap);
            } catch (final Throwable e) {
                logger.warning("Error happened while trying to save download_started_timestamp");
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean setIP(final DownloadLink link, final Account account) throws Exception {
        synchronized (IPCHECK) {
            if (currentIP.get() != null && !new Regex(currentIP.get(), IPREGEX).matches()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (ipChanged(link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                String lastIP = currentIP.get();
                link.setProperty(PROPERTY_LASTIP, lastIP);
                NovaFileCom.lastIP.set(lastIP);
                getPluginConfig().setProperty(PROPERTY_LASTIP, lastIP);
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private long getPluginSavedLastDownloadTimestamp() {
        long lastdownload = 0;
        synchronized (blockedIPsMap) {
            final Iterator<Entry<String, Long>> it = blockedIPsMap.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Long> ipentry = it.next();
                final String ip = ipentry.getKey();
                final long timestamp = ipentry.getValue();
                if (System.currentTimeMillis() - timestamp >= FREE_RECONNECTWAIT_DEFAULT) {
                    /* Remove old entries */
                    it.remove();
                }
                if (ip.equals(currentIP.get())) {
                    lastdownload = timestamp;
                }
            }
        }
        return lastdownload;
    }

    private boolean ipChanged(final DownloadLink link) throws Exception {
        String currIP = null;
        if (currentIP.get() != null && new Regex(currentIP.get(), IPREGEX).matches()) {
            currIP = currentIP.get();
        } else {
            currIP = getIP();
        }
        if (currIP == null) {
            return false;
        }
        String lastIP = link.getStringProperty(PROPERTY_LASTIP, null);
        if (lastIP == null) {
            lastIP = NovaFileCom.lastIP.get();
        }
        if (lastIP == null) {
            lastIP = this.getPluginConfig().getStringProperty(PROPERTY_LASTIP, null);
        }
        return !currIP.equals(lastIP);
    }

    private String getIP() throws Exception {
        Browser ip = new Browser();
        String currentIP = null;
        ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(IPCHECK));
        Collections.shuffle(checkIP);
        Exception exception = null;
        for (String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(IPREGEX).getMatch(0);
                    if (currentIP != null) {
                        break;
                    }
                } catch (Exception e) {
                    if (exception == null) {
                        exception = e;
                    }
                }
            }
        }
        if (currentIP == null) {
            if (exception != null) {
                throw exception;
            }
            logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    private final boolean default_eh = false;

    public void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), EXPERIMENTALHANDLING, "Activate reconnect workaround for freeusers: Prevents having to enter additional captchas in between downloads.").setDefaultValue(default_eh));
    }

    /* *************************** SPECIAL API STUFF STARTS HERE *************************** */
    @Override
    protected boolean useAPIZeusCloudManager() {
        return true;
    }

    @Override
    protected String getRelativeAPIBaseAPIZeusCloudManager() {
        return "/napi";
    }

    @Override
    protected String getRelativeAPILoginParamsFormatAPIZeusCloudManager() {
        return "?op=login&login=%s&pass=%s";
    }
}