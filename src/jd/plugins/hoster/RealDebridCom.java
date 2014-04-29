//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.utils.JDUtilities;

import org.appwork.utils.Hash;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.downloadcore.v15.HashInfo;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "real-debrid.com" }, urls = { "https?://\\w+\\.real\\-debrid\\.com/dl/\\w+/.+" }, flags = { 2 })
public class RealDebridCom extends PluginForHost {

    // DEV NOTES
    // supports last09 based on pre-generated links and jd2 (but disabled with interfaceVersion 3)

    private final String                                   mName                 = "real-debrid.com";
    private final String                                   mProt                 = "https://";
    private int                                            maxChunks             = 0;
    private boolean                                        resumes               = true;
    private boolean                                        swapped               = false;
    private static Object                                  LOCK                  = new Object();
    private static AtomicInteger                           RUNNING_DOWNLOADS     = new AtomicInteger(0);
    private static AtomicInteger                           MAX_DOWNLOADS         = new AtomicInteger(Integer.MAX_VALUE);
    private static final long                              UNKNOWN_ERROR_RETRY_1 = 50;
    private static final long                              UNKNOWN_ERROR_RETRY_2 = 50;
    private static final long                              UNKNOWN_ERROR_RETRY_3 = 20;
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap    = new HashMap<Account, HashMap<String, Long>>();

    public RealDebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public String getAGBLink() {
        return mProt + mName + "/terms";
    }

    private Browser prepBrowser(Browser prepBr) {
        // define custom browser headers and language settings.
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        prepBr.setCookie(mProt + mName, "lang", "en");
        prepBr.getHeaders().put("User-Agent", "JDownloader");
        prepBr.setCustomCharset("utf-8");
        prepBr.setConnectTimeout(2 * 60 * 1000);
        prepBr.setReadTimeout(2 * 60 * 1000);
        prepBr.setFollowRedirects(true);
        prepBr.setAllowedResponseCodes(new int[] { 504 });
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
                dl.setProperty("directRD", true);
                if (dl.getFinalFileName() == null) dl.setFinalFileName(getFileNameFromHeader(con));
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                dl.setProperty("directRD", false);
                dl.setAvailable(false);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (downloadLink.getDownloadURL().matches(this.getLazyP().getPatternSource())) {
            // generated links do not require an account to download
            return true;
        } else if (account == null) {
            // no non account handleMultiHost support.
            return false;
        } else {
            synchronized (hostUnavailableMap) {
                HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
                if (unavailableMap != null) {
                    Long lastUnavailable = unavailableMap.get(downloadLink.getHost());
                    if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                        return false;
                    } else if (lastUnavailable != null) {
                        unavailableMap.remove(downloadLink.getHost());
                        if (unavailableMap.size() == 0) hostUnavailableMap.remove(account);
                    }
                }
            }
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDL(null, downloadLink, downloadLink.getDownloadURL());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return MAX_DOWNLOADS.get();
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, final Account account) {
        return MAX_DOWNLOADS.get();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        showMessage(link, "Task 2: Download begins!");
        handleDL(account, link, link.getDownloadURL());
    }

    private void handleDL(final Account acc, final DownloadLink link, final String dllink) throws Exception {
        // real debrid connections are flakey at times! Do this instead of repeating download steps.
        int repeat = 3;
        for (int i = 0; i <= repeat; i++) {
            DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link);
            if (swapped) {
                downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
                    @Override
                    public HashInfo getHashInfo() {
                        return null;
                    }
                };
            }
            final Browser br2 = br.cloneBrowser();
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLinkDownloadable, br2.createGetRequest(dllink), resumes, maxChunks);
                if (dl.getConnection().isContentDisposition()) {
                    /* content disposition, lets download it */
                    RUNNING_DOWNLOADS.incrementAndGet();
                    boolean ret = dl.startDownload();
                    if (ret && link.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        // download is 100%
                        break;
                    }
                    if (link.getLinkStatus().getErrorMessage().contains("Unexpected rangeheader format:")) {
                        logger.warning("Bad Range Header! Resuming isn't possible without resetting");
                        throw new PluginException(LinkStatus.ERROR_FATAL);

                        // logger.warning("BAD HEADER RANGES!, auto resetting");
                        // link.reset();
                    }
                } else if (dl.getConnection().getResponseCode() == 404) {
                    br2.followConnection();
                    // $.msgbox("You can not download this file because you have exceeded your traffic on this hoster !", {type: "error"});
                    final String msg = br2.getRegex("msgbox\\(\"([^\"]+)").getMatch(0);
                    if (msg != null) {
                        link.getLinkStatus().setErrorMessage(msg);
                        logger.info(msg);
                        if (msg.contains("You can not download this file because you already have download(s) currently downloading and this hoster is limited")) {
                            // You can not download this file because you already have download(s) currently downloading and this hoster is limited.
                        	tempUnavailableHoster(acc, link, 5 * 60 * 60 * 1000l);
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else if (msg.contains("You can not download this file because you have too many download(s) currently downloading")) {
                            // You can not download this file because you have too many download(s) currently downloading                            
                            // set upper max sim dl ?
                            /* You have too many simultaneous downloads */
                            errTooManySimCon(acc, link);
                        } else if (msg.contains("You can not download this file because you have exceeded your traffic on this hoster")) {
                        	errNoHosterTrafficLeft(acc, link);
                        } else if (msg.contains("A Premium account for the hoster is missing or inactive")) {
                            // short retry?
                            tempUnavailableHoster(acc, link, 5 * 60 * 1000l);
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else if (msg.contains("The traffic of the Premium account for the hoster is exceeded")) {
                        	tempUnavailableHoster(acc, link, 1 * 60 * 60 * 1000l);
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        }  else if (msg.contains("An error occured while generating a premium link")) {
                        	// An error occured while generating a premium link, please contact an Administrator with these following informations :<br/><br/>Link: http://rapidgator.net/file/e1e8957be6a02dbfbe28b4b8f8ec465e<br/>Server: 31<br/>Code: 64i4u284w2v293333033", {type: "error"});
                        	// An error occured while generating a premium link, please contact an Administrator with these following informations :<br/><br/>Link: http://rapidgator.net/file/170bb859202bf1f5927f6b5a22f8a6ac<br/>Server: 31<br/>Code: 64i4u284w23383634237", {type: "error"});
                            tempUnavailableHoster(acc, link, 15 * 60 * 1000l);
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else if (msg.contains("An error occured while attempting to download the file")) {
                            // An error occured while attempting to download the file. Too many attempts, please contact an Administrator with these following informations :
                            // An error occured while attempting to download the file. Multiple "Location:" headers, please contact an Administrator with these following informations :
                            // issue with THIS downloadlink, throw instantly to next download routine. ** Using Jiaz new handling..
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                        } else if (msg.contains("An error occured while read your file on the remote host")) {
                            // An error occured while read your file on the remote host ! Timeout of 90s exceeded !<br/>The download server is down or ban from our server, please contact an Administrator with these following informations :<br/><br/>Link:*****)
                            // An error occured while read your file on the remote host ! Timeout of 15s exceeded (Passive FTP Mode) !
                            // An error occured while read your file on the remote host ! Timeout of 15s exceeded (FTP Mode) !
                            tempUnavailableHoster(acc, link, 15 * 60 * 60 * 1000l);
                            throw new PluginException(LinkStatus.ERROR_RETRY);
                        } else if (msg.contains("You are not allowed to download this file !<br/>Your current IP adress is")) {
                            // You are not allowed to download this file !<br/>Your current IP adress is:
                			String l = "Dedicated server detected!";
                			if (acc != null) { 
                			    l += " Account Disabled!";
                			    logger.info(l);
                			    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                			} else { 
                				// free dl, without an account (generated by the user)
                				throw new PluginException(LinkStatus.ERROR_FATAL, l);
                			}
                		} else if (msg.contains("You are not allowed to download this file !<br/>Your account is not Premium or your account is suspended.")) {
                            // You are not allowed to download this file !<br/>Your account is not Premium or your account is suspended.
                		    // so free account or suspended account... 
                		    if (acc != null) {
                                logger.info("Suspended Account!");
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                		    } else {
                		        // handleFree :: no account in jd account manager.. could be copy paste someone else' generated link, or manually import links, without an account
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium required, or Account has been disabled");
                		    }
                		} else if (msg.contains("You can not change your server manually on this hoster")) {
                		    // as title says user changed the premium link server... would only happen if a user manually imports final links!
                            if (new Regex(link.getDownloadURL(), this.getLazyP().getPattern()).matches()) {
                                // manually imported
                                throw new PluginException(LinkStatus.ERROR_FATAL, "Premium required, or Account has been disabled");
                            } else {
                                // should never happen!
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                		} else { 
                		    // unhandled error msg type!
                		    logger.warning("Please report this issue to JDownloader Development Team!");
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                		}
                    } else {
                        // unhandled error!
                        logger.warning("Please report this issue to JDownloader Development Team!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    /* download is not content disposition. */
                    br2.followConnection();
                    logger.info(this.getHost() + "Unknown Error3");
                    int timesFailed = link.getIntegerProperty("timesfailedrealdebridcom_unknown3", 0);
                    link.getLinkStatus().setRetryCount(0);
                    if (timesFailed <= UNKNOWN_ERROR_RETRY_3) {
                        timesFailed++;
                        link.setProperty("timesfailedrealdebridcom_unknown3", timesFailed);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error1");
                    } else {
                        link.setProperty("timesfailedrealdebridcom_unknown3", Property.NULL);
                        logger.info(this.getHost() + ": Unknown error3 - disabling current host!");
                        tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                    }
                }
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
                if (e instanceof InterruptedException) throw (InterruptedException) e;
                logger.info("Download failed " + i + " of " + repeat);
                sleep(3000, link);
                LogSource.exception(logger, e);
                continue;
            } finally {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                if (RUNNING_DOWNLOADS.decrementAndGet() == 0) {
                    MAX_DOWNLOADS.set(Integer.MAX_VALUE);
                }
            }
        }
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait 30 mins to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        // work around
        if (link.getBooleanProperty("hasFailed", false)) {
            final int hasFailedInt = link.getIntegerProperty("hasFailedWait", 60);
            // nullify old storeables
            link.setProperty("hasFailed", Property.NULL);
            link.setProperty("hasFailedWait", Property.NULL);
            sleep(hasFailedInt * 1001, link);
        }

        prepBrowser(br);
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        /* request Download */
        String dllink = link.getDownloadURL();
        for (int retry = 0; retry < 3; retry++) {
            try {
                if (retry != 0) sleep(3000l, link);
                br.getPage(mProt + mName + "/ajax/unrestrict.php?link=" + Encoding.urlEncode(dllink) + ((link.getStringProperty("pass", null) != null ? "&password=" + Encoding.urlEncode(link.getStringProperty("pass", null)) : "")));
                if (br.containsHTML("\"error\":4,")) {
                    if (retry != 2) {
                        if (dllink.contains("https://")) {
                            dllink = dllink.replace("https://", "http://");
                        } else {
                            // not likely but lets try anyway.
                            dllink = dllink.replace("http://", "https://");
                        }
                    } else {
                        logger.warning("Problemo in the old corral");
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Can not download from " + mName);
                    }
                } else if (br.getHttpConnection().getResponseCode() == 504 || "23764902a26fbd6345d3cc3533d1d5eb".equalsIgnoreCase(Hash.getMD5(br.toString()))) {
                    if (retry == 2) {
                        logger.warning(mName + " has problems! Repeated bad gateway!");
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Can not download from " + mName);
                    }
                    continue;
                }
                break;
            } catch (SocketException e) {
                if (retry == 2) throw e;
            }
        }
        // we only ever generate one link at a time, we don't need String[]
        String genLnk = getJson("main_link");
        final String chunks = getJson("max_chunks");
        if (chunks != null) {
            if ("-1".equals(chunks))
                maxChunks = 0;
            else if ("1".equals(chunks))
                resumes = false;
            else
                maxChunks = -Integer.parseInt(chunks);
        }
        // switcheroonie
        final String switcheroonie = getJson("swap");
        if (switcheroonie != null) {
            final boolean swap = Boolean.parseBoolean(switcheroonie);
            if (swap) swapped = swap;
        }
        if (genLnk == null) {
            if (br.containsHTML("\"error\":1,")) {
                // from rd
                // 1: Happy hours activated BUT the concerned hoster is not included => Upgrade to Premium to use it
                logger.info("This Hoster isn't supported in Happy Hour!");
                tempUnavailableHoster(acc, link, 30 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.containsHTML("\"error\":2,")) {
                // from rd
                // 2: Free account, come back at happy hours
                logger.info("It's not happy hour, free account, you need premium!.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("\"error\":3,")) {
                // {"error":3,"message":"Ein dedicated Server wurde erkannt, es ist dir nicht erlaubt Links zu generieren"}
                // dedicated server is detected, it does not allow you to generate links
                logger.info("Dedicated server detected, account disabled");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (br.containsHTML("\"error\":4,")) {
                // {"error":4,"message":"This hoster is not included in our free offer"}
                // {"error":4,"message":"Unsupported link format or unsupported hoster"}
                logger.info("Unsupported link format or unsupported hoster");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else if (br.containsHTML("\"error\":5,")) {
                // {"error":5,"message":"Non sei utente premium, puoi utilizzare il servizio premium gratuito solo durante la fascia oraria \"Happy Hours\". Aggiorna il tuo Account acquistando il servizio premium."}
                /* no happy hour */
                logger.info("It's not happy hour, free account, you need premium!.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else if (br.containsHTML("error\":6,")) {
                // {"error":6,"message":"Ihr Tages-Limit wurde erreicht."}
                // {"error":6,"message":"Daily limit exceeded."}
                errNoHosterTrafficLeft(acc, link);
            } else if (br.containsHTML("error\":7,")) {
                // {"error":7,"message":"F\u00fcr den Hoster ist kein Server vorhanden."}
                // Für den Hoster ist kein Server vorhanden.
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.containsHTML("error\":10,")) {
                logger.info("File's hoster is in maintenance. Try again later");
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.containsHTML("error\":11,")) {
                logger.info("Host seems buggy, remove it from list");
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (br.containsHTML("error\":12,")) {
                /* You have too many simultaneous downloads */
                errTooManySimCon(acc, link);
            } else if (br.containsHTML("error\":(13|9),")) {
                String num = "";
                String err = "";
                if (br.containsHTML("error\":13,")) {
                    err = "Unknown error";
                    num = "13";
                }
                // doesn't warrant not retrying! it just means no available host at this point in time! ??
                if (br.containsHTML("error\":9,")) {
                    err = "Host is currently not possible because no server is available!";
                    num = "9";
                }

                /*
                 * after x retries we disable this host and retry with normal plugin
                 */
                int retry = link.getIntegerProperty("retry_913", 0);
                if (retry == 3) {
                    /* reset retry counter */
                    link.setProperty("retry_913", Property.NULL);
                    link.setProperty("hasFailedWait", Property.NULL);
                    // remove host from download method, but don't remove host from array!
                    logger.warning("Exausted retry count! :: " + err);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                } else {
                    String msg = (retry + 1) + " / 3";
                    logger.warning("Error " + num + " : Retry " + msg);
                    link.setProperty("hasFailed", true);
                    retry++;
                    link.setProperty("retry_913", retry);
                    link.setProperty("hasFailedWait", 120);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Error " + num + " : Retry " + msg);
                }
            } else {
                // unknown error2
                logger.info(this.getHost() + "Unknown Error1");
                int timesFailed = link.getIntegerProperty("timesfailedrealdebridcom_unknown1", 0);
                link.getLinkStatus().setRetryCount(0);
                if (timesFailed <= UNKNOWN_ERROR_RETRY_1) {
                    timesFailed++;
                    link.setProperty("timesfailedrealdebridcom_unknown1", timesFailed);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error1");
                } else {
                    link.setProperty("timesfailedrealdebridcom_unknown1", Property.NULL);
                    logger.info(this.getHost() + ": Unknown error1 - disabling current host!");
                    tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
                }
            }
        }
        if (!genLnk.startsWith("http")) throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported protocol");
        // no longer have issues, with above error handling. Next time download starts, error count starts from 0.
        link.setProperty("retry_913", Property.NULL);
        showMessage(link, "Task 2: Download begins!");
        try {
            handleDL(acc, link, genLnk);
            return;
        } catch (PluginException e1) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            if (br.containsHTML("An error occured while generating a premium link, please contact an Administrator")) {
                logger.info("Error while generating premium link, removing host from supported list");
                tempUnavailableHoster(acc, link, 60 * 60 * 1000l);
            }
            if (br.containsHTML("An error occured while attempting to download the file.")) { throw new PluginException(LinkStatus.ERROR_RETRY); }

            throw e1;
        }
    }

    private void errNoHosterTrafficLeft(Account acc, DownloadLink link) throws Exception {
        logger.info("You have run out of download quota for this hoster");
        tempUnavailableHoster(acc, link, 3 * 60 * 60 * 1000l);
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }
    
    private void errTooManySimCon(Account acc, DownloadLink link) throws PluginException {
        MAX_DOWNLOADS.set(RUNNING_DOWNLOADS.get());
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        for (int retry = 0; retry < 3; retry++) {
            try {
                br.getPage(mProt + mName + "/api/account.php");
                if (br.getHttpConnection().getResponseCode() == 504 || "23764902a26fbd6345d3cc3533d1d5eb".equalsIgnoreCase(Hash.getMD5(br.toString()))) {
                    if (retry != 2) {
                        Thread.sleep(3000l);
                        continue;
                    } else {
                        logger.warning(mName + " has problems! Repeated bad gateway!");
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Can not download from " + mName);
                    }
                }
                break;
            } catch (SocketException e) {
                if (retry == 2) throw e;
                Thread.sleep(1000);
            }
        }
        String expire = br.getRegex("<premium-left>-?(\\d+)</premium-left>").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expire) * 1000));
        }
        String acctype = br.getRegex("<type>(\\w+)</type>").getMatch(0).toLowerCase();
        if (acctype.equals("premium")) {
            ai.setStatus("Premium User");
            account.setProperty("free", false);
        } else {
            // non supported account type here.
            logger.warning("Sorry we do not support this account type at this stage.");
            account.setValid(false);
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        if ("01/01/1970 01:00:00".equals(expire)) {
            ai.setValidUntil(-1);
            ai.setStatus("Free User");
            account.setProperty("free", true);
        }
        try {
            String hostsSup = null;
            for (int retry = 0; retry < 3; retry++) {
                try {
                    hostsSup = br.cloneBrowser().getPage(mProt + mName + "/api/hosters.php");
                    break;
                } catch (SocketException e) {
                    if (retry == 2) throw e;
                    Thread.sleep(1000);
                }
            }
            String[] hosts = new Regex(hostsSup, "\"([^\"]+)\"").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            if (supportedHosts.contains("freakshare.net")) {
                supportedHosts.add("freakshare.com");
            }
            // workaround for uploaded.to
            if (supportedHosts.contains("uploaded.net") || supportedHosts.contains("ul.to") || supportedHosts.contains("uploaded.to")) {
                if (!supportedHosts.contains("uploaded.net")) {
                    supportedHosts.add("uploaded.net");
                }
                if (!supportedHosts.contains("ul.to")) {
                    supportedHosts.add("ul.to");
                }
                if (!supportedHosts.contains("uploaded.to")) {
                    supportedHosts.add("uploaded.to");
                }
            }
            // workaround for keep2share.cc, as they keep changing hosts..
            if (supportedHosts.contains("keep2share.cc") || supportedHosts.contains("k2s.cc") || supportedHosts.contains("keep2s.cc") || supportedHosts.contains("keep2.cc")) {
                if (!supportedHosts.contains("keep2share.cc")) {
                    supportedHosts.add("keep2share.cc");
                }
                if (!supportedHosts.contains("k2s.cc")) {
                    supportedHosts.add("k2s.cc");
                }
                if (!supportedHosts.contains("keep2s.cc")) {
                    supportedHosts.add("keep2s.cc");
                }
                if (!supportedHosts.contains("keep2.cc")) {
                    supportedHosts.add("keep2.cc");
                }
            }
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Could not fetch ServerList: " + e.toString());
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(mProt + mName, key, value);
                        }
                        return;
                    }
                }
                for (int retry = 0; retry < 3; retry++) {
                    try {
                        br.getPage(mProt + mName + "/ajax/login.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Hash.getMD5(account.getPass()) + "&captcha_challenge=&captcha_answer=&time=" + System.currentTimeMillis() + "&pin_challenge=&pin_answer=");
                        if (br.containsHTML("\"captcha\":1")) {
                            DownloadLink dummyLink = new DownloadLink(this, "Account", mProt + mName, mProt + mName, true);
                            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                            final String challenge = getJson("captcha_challenge");
                            final String image = getJson("captcha_url");
                            if (challenge == null || image == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            rc.setChallenge(challenge);
                            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                            final String c = getCaptchaCode(cf, dummyLink);
                            br.getPage(mProt + mName + "/ajax/login.php?user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Hash.getMD5(account.getPass()) + "&captcha_challenge=" + rc.getChallenge() + "&captcha_answer=" + Encoding.urlEncode(c) + "&time=" + System.currentTimeMillis() + "&pin_challenge=&pin_answer=");
                            if (br.containsHTML("\"captcha\":1")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nError either captcha is incorrect or your user:password is incorrect", PluginException.VALUE_ID_PREMIUM_DISABLE); }
                        }
                        if (br.containsHTML("\"message\":\"PIN Code required\"")) {
                            try {
                                SwingUtilities.invokeAndWait(new Runnable() {

                                    @Override
                                    public void run() {
                                        try {
                                            String lng = System.getProperty("user.language");
                                            String message = null;
                                            String title = null;
                                            boolean xSystem = CrossSystem.isOpenBrowserSupported();
                                            if ("de".equalsIgnoreCase(lng)) {
                                                title = mName + " Zwei-Faktor-Authentifizierung wird benoetigt!";
                                                message = " Zwei-Faktor-Authentifizierung wird benoetigt!\r\n";
                                                message = "Oeffne bitte Deinen Webbrowser:\r\n";
                                                message += " - Melde den Nutzer " + mName + " ab.\r\n";
                                                message += " - Melde Dich neu an. \r\n";
                                                message += " - Vervollstaendige die Zwei-Faktor-Authentifizierung.\r\n";
                                                message += "Nach dem erfolgreichen Login im Browser kannst du deinen Account wieder im JD hinzufuegen.\r\n\r\n";
                                                if (xSystem)
                                                    message += "Klicke -OK- (Oeffnet " + mName + " in deinem Webbrowser)\r\n";
                                                else
                                                    message += new URL(mProt + mName);
                                            } else {
                                                title = mName + " Two Factor Authentication Required";
                                                message = "Please goto your Browser:\r\n";
                                                message += " - Logout of " + mName + ".\r\n";
                                                message += " - Re-Login. \r\n";
                                                message += " - Complete Two Factor Authentication.\r\n";
                                                message += "Once completed, you will be able to relogin within JDownloader.\r\n\r\n";
                                                if (xSystem)
                                                    message += "Click -OK- (Opens " + mName + " in your Browser)\r\n";
                                                else
                                                    message += new URL(mProt + mName);
                                            }
                                            int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                                            if (xSystem && JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL(mProt + mName));
                                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                                        } catch (Throwable e) {
                                        }
                                    }
                                });
                            } catch (Throwable e) {
                            }

                        }
                        break;
                    } catch (SocketException e) {
                        if (retry == 2) throw e;
                        Thread.sleep(1000);
                    }
                }

                if (br.getCookie(mProt + mName, "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(mProt + mName);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.setProperty("retry_913", Property.NULL);
        link.setProperty("hasFailed", Property.NULL);
        link.setProperty("hasFailedWait", Property.NULL);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     * 
     * @author raztoki
     * */
    private String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        if (result != null) result = result.replaceAll("\\\\/", "/");
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    /**
     * Tries to return value of key from JSon response, from provided Browser.
     * 
     * @author raztoki
     * */
    private String getJson(final Browser ibr, final String key) {
        return getJson(ibr.toString(), key);
    }

}