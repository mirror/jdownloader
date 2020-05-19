package jd.plugins.hoster;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "proleech.link" }, urls = { "https?://proleech\\.link/download/[a-zA-Z0-9]+(?:/.*)?" })
public class ProLeechLink extends antiDDoSForHost {
    public ProLeechLink(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://proleech.link/signup");
        setConfigElements();
    }

    private static MultiHosterManagement        mhm                                    = new MultiHosterManagement("proleech.link");
    /** Contains all filenames of files we attempted to download or downloaded via this account. */
    private static CopyOnWriteArrayList<String> deleteDownloadHistoryFilenameWhitelist = new CopyOnWriteArrayList<String>();
    // private static List<String> deleteDownloadHistoryFilenameBlacklist = new ArrayList<String>();

    @Override
    public String getAGBLink() {
        return "https://proleech.link/page/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        final String fileName = new Regex(parameter.getPluginPatternMatcher(), "download/[a-zA-Z0-9]+/([^/\\?]+)").getMatch(0);
        if (fileName != null && !parameter.isNameSet()) {
            parameter.setName(fileName);
        }
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, ai, true);
        /* Contains all filehosts available for free account users regardless of their status */
        String[] filehosts_free = null;
        /* Contains all filehosts available for premium users and listed as online/working */
        String[] filehosts_premium_online = null;
        List<String> filehosts_premium_onlineArray = new ArrayList<String>();
        /* Contains all filehosts available for premium users and listed as online/working */
        List<String> filehosts_free_onlineArray = new ArrayList<String>();
        String traffic_max_dailyStr = null;
        {
            /* Grab free hosts */
            if (br.getURL() == null || !br.getURL().contains("/downloader")) {
                this.getPage("/downloader");
            }
            final String html_free_filehosts = br.getRegex("<section id=\"content\">.*?Free Filehosters</div>.*?</section>").getMatch(-1);
            filehosts_free = new Regex(html_free_filehosts, "domain=([^\"]+)").getColumn(0);
        }
        {
            /* Grab premium hosts */
            getPage("/page/hostlist");
            filehosts_premium_online = br.getRegex("<td>\\s*\\d+\\s*</td>\\s*<td>\\s*<img[^<]+/?>\\s*([^<]*?)\\s*</td>\\s*<td>\\s*<span\\s*class\\s*=\\s*\"label\\s*label-success\"\\s*>\\s*Online").getColumn(0);
            if (filehosts_premium_online == null || filehosts_premium_online.length == 0) {
                logger.warning("Failed to find list of supported hosts");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String filehost_premium_online : filehosts_premium_online) {
                if (filehost_premium_online.contains("/")) {
                    /* 2019-11-11: WTF They sometimes display multiple domains of one filehost in one entry, separated by ' / ' */
                    logger.info("Special case: Multiple domains of one filehost given: " + filehost_premium_online);
                    final String[] filehost_domains = filehost_premium_online.split("/");
                    for (String filehost_domain : filehost_domains) {
                        filehost_domain = filehost_domain.trim();
                        filehosts_premium_onlineArray.add(filehost_domain);
                    }
                } else {
                    filehosts_premium_onlineArray.add(filehost_premium_online);
                }
            }
            /* 2019-11-11: New: Max daily traffic value [80 GB at this moment] */
            traffic_max_dailyStr = br.getRegex("(\\d+(?:\\.\\d+)? GB) Daily Traff?ic").getMatch(0);
        }
        /* Set supported hosts depending on account type */
        if (account.getType() == AccountType.PREMIUM && !ai.isExpired()) {
            /* Premium account - bigger list of supported hosts */
            ai.setMultiHostSupport(this, filehosts_premium_onlineArray);
            if (traffic_max_dailyStr != null) {
                ai.setTrafficLeft(SizeFormatter.getSize(traffic_max_dailyStr));
            }
        } else {
            /* Free & Expired[=Free] accounts - they support much less hosts */
            if (filehosts_free != null && filehosts_free.length != 0) {
                /*
                 * We only see the online status of each host in the big list. Now we have to find out which of all free filehosts are
                 * currently online/supported.
                 */
                for (final String host : filehosts_free) {
                    if (filehosts_premium_onlineArray.contains(host)) {
                        /* Filehost should be supported/online --> Add to the list we will later set */
                        filehosts_free_onlineArray.add(host);
                    }
                }
                ai.setMultiHostSupport(this, filehosts_free_onlineArray);
            }
        }
        /* Clear download history on every accountcheck (if selected by user) */
        clearDownloadHistory(account, true);
        return ai;
    }

    private boolean isLoggedin(final Browser br) throws PluginException {
        final boolean cookie_ok_amember_nr = br.getCookie("amember_nr", Cookies.NOTDELETEDPATTERN) != null;
        final boolean cookie_ok_amember_ru = br.getCookie("amember_ru", Cookies.NOTDELETEDPATTERN) != null;
        final boolean cookie_ok_amember_rp = br.getCookie("amember_rp", Cookies.NOTDELETEDPATTERN) != null;
        final boolean cookies_ok = cookie_ok_amember_nr && cookie_ok_amember_ru && cookie_ok_amember_rp;
        final boolean html_ok = br.containsHTML("/logout");
        logger.info("cookies_ok = " + cookies_ok);
        logger.info("html_ok = " + html_ok);
        /* 2019-11-11: Allow validation via cookies OR html code! */
        if (cookies_ok || html_ok) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param validateCookies
     *            true = Check whether stored cookies are still valid, if not, perform full login <br/>
     *            false = Set stored cookies and trust them if they're not older than 300000l
     *
     */
    private boolean login(Account account, AccountInfo ai, final boolean validateCookies) throws Exception {
        synchronized (account) {
            try {
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= 300000l && !validateCookies) {
                        /* We trust these cookies as they're not that old --> Do not check them */
                        logger.info("Trust login-cookies without checking as they should still be fresh");
                        return false;
                    }
                    getPage("https://" + this.getHost() + "/member");
                    br.followRedirect();
                    loggedIN = this.isLoggedin(this.br);
                }
                if (!loggedIN) {
                    logger.info("Performing full login");
                    br.clearCookies(getHost());
                    getPage("https://" + this.getHost());
                    getPage("/login");
                    final Form loginform = br.getFormbyAction("/login");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("amember_login", URLEncoder.encode(account.getUser(), "UTF-8"));
                    loginform.put("amember_pass", URLEncoder.encode(account.getPass(), "UTF-8"));
                    final String loginAttemptID = br.getRegex("name=\"login_attempt_id\" value=\"(\\d+)\"").getMatch(0);
                    if (loginAttemptID != null) {
                        /* 2020-05-19 */
                        logger.info("Found loginAttemptID: " + loginAttemptID);
                        if (!loginform.hasInputFieldByName("login_attempt_id")) {
                            loginform.put("login_attempt_id", loginAttemptID);
                        }
                    } else {
                        logger.info("Failed to find loginAttemptID");
                    }
                    final boolean forceCaptcha = false;
                    if (loginform.containsHTML("recaptcha") || forceCaptcha) {
                        final DownloadLink dlinkbefore = this.getDownloadLink();
                        try {
                            final DownloadLink dl_dummy;
                            if (dlinkbefore != null) {
                                dl_dummy = dlinkbefore;
                            } else {
                                dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                                this.setDownloadLink(dl_dummy);
                            }
                            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                            loginform.put("g-recaptcha-response", URLEncoder.encode(recaptchaV2Response, "UTF-8"));
                        } finally {
                            this.setDownloadLink(dlinkbefore);
                        }
                    }
                    submitForm(loginform);
                    br.followRedirect();
                    if (!br.getURL().contains("/member")) {
                        getPage("/member");
                    }
                    if (!isLoggedin(this.br)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /*
                 * 2019-11-19: Important! This is NOT the 'daily traffic used' - this is the total traffic ever used with the current
                 * account! We can display it in the account status but we cannot use this to calculate the remaining traffic!
                 */
                final String total_traffic_ever_used_with_this_accountStr = br.getRegex("Bandwidth Used\\s*:\\s*<span[^<>]*><b>\\s*(\\d+(?:\\.\\d{1,2})[ ]*[A-Za-z]+)\\s*<").getMatch(0);
                final String activeSubscription = br.getRegex("am-list-subscriptions\">\\s*<li[^<]*>(.*?)</li>").getMatch(0);
                String accountStatus = null;
                if (activeSubscription != null) {
                    final String expireDate = new Regex(activeSubscription, "([a-zA-Z]+\\s*\\d+,\\s*\\d{4})").getMatch(0);
                    if (expireDate != null) {
                        final long validUntil = TimeFormatter.getMilliSeconds(expireDate, "MMM' 'dd', 'yyyy", Locale.ENGLISH);
                        if (ai != null) {
                            ai.setValidUntil(validUntil);
                        }
                        account.setType(AccountType.PREMIUM);
                        /* 2019-11-19: Set premium account status in fetchAccountInfo */
                        accountStatus = "Premium user";
                        account.setConcurrentUsePossible(true);
                        account.setMaxSimultanDownloads(-1);
                    }
                } else {
                    account.setType(AccountType.FREE);
                    accountStatus = "Free user";
                    account.setConcurrentUsePossible(true);
                    if (ai != null) {
                        /* Only get/set hostlist if we're not currently trying to download a file (quick login) */
                        getPage("/downloader");
                        int maxfiles_per_day_used = 0;
                        int maxfiles_per_day_maxvalue = 0;
                        final Regex maxfiles_per_day = br.getRegex("<li>Files per day:\\s*?<b>\\s*?(\\d+)?\\s*?/\\s*?(\\d+)\\s*?</li>");
                        final String maxfiles_per_day_usedStr = maxfiles_per_day.getMatch(0);
                        final String maxfiles_per_day_maxvalueStr = maxfiles_per_day.getMatch(1);
                        final String max_free_filesize = br.getRegex("<li>Max\\. Filesize: <b>(\\d+ [^<>\"]+)</li>").getMatch(0);
                        if (maxfiles_per_day_usedStr != null) {
                            maxfiles_per_day_used = Integer.parseInt(maxfiles_per_day_usedStr);
                        }
                        if (maxfiles_per_day_maxvalueStr != null) {
                            maxfiles_per_day_maxvalue = Integer.parseInt(maxfiles_per_day_maxvalueStr);
                        }
                        /* ok = "/2" or "1/2", not ok = "2/2" */
                        if (max_free_filesize != null && maxfiles_per_day_maxvalue > 0) {
                            final int files_this_day_remaining = maxfiles_per_day_maxvalue - maxfiles_per_day_used;
                            if (files_this_day_remaining > 0) {
                                ai.setTrafficLeft(SizeFormatter.getSize(max_free_filesize));
                                /* 2019-08-14: Max files per day = 2 so a limit of 1 should be good! */
                                account.setMaxSimultanDownloads(1);
                            } else {
                                /*
                                 * No links remaining --> Display account with ZERO traffic left as it cannot be used to download at the
                                 * moment!
                                 */
                                logger.info("Cannot use free account because the max number of dail downloads has already been reached");
                                ai.setTrafficLeft(0);
                                account.setMaxSimultanDownloads(0);
                            }
                            accountStatus += " [" + files_this_day_remaining + "/" + maxfiles_per_day_maxvalueStr + " daily downloads remaining]";
                        } else {
                            logger.info("Cannot use free account because we failed to find any information about the current account limits");
                            account.setMaxSimultanDownloads(0);
                            ai.setTrafficLeft(0);
                            accountStatus += " [No downloads possible]";
                        }
                    }
                }
                if (ai != null) {
                    if (total_traffic_ever_used_with_this_accountStr != null) {
                        accountStatus += String.format(" [Total traffic ever used: %s]", total_traffic_ever_used_with_this_accountStr);
                    }
                    ai.setStatus(accountStatus);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
                return true;
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        /* 2019-11-11: Login is not required to check previously generated directurls */
        // login(account, null, false);
        final String generatedDownloadURL = link.getStringProperty(getHost(), null);
        String dllink = null;
        /* We do not have to login to check previously generated downloadurls */
        boolean isLoggedIN = false;
        if (generatedDownloadURL != null) {
            /*
             * 2019-11-11: Seems like generated downloadurls are only valid for some seconds after genration but let's try to re-use them
             * anyways!
             */
            logger.info("Trying to re-use old generated downloadlink");
            try {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, generatedDownloadURL, true, 0);
                final boolean isOkay = isDownloadConnection(dl.getConnection());
                if (!isOkay) {
                    /* 2019-11-11: E.g. "Link expired! Please leech again." */
                    logger.info("Saved downloadurl did not work");
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                } else {
                    dllink = generatedDownloadURL;
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                logger.log(e);
            }
        }
        /*
         * TODO: Check if we need this to reset properties to force generation of new downloadurls to prevent infinite loops! Serverside
         * Cloud-download entries may as well link to non-working final downloadurls!
         */
        // final boolean is_forced_cloud_download = this.isForcedCloudDownload(link);
        // boolean found_downloadurl_in_cloud_downloads = false;
        if (dllink == null) {
            logger.info("Trying to generate/find final downloadurl");
            /* First, try to get downloadlinks for previously started cloud-downloads as this does not create new directurls */
            /* TODO: Try to grab previously generated direct-downloadurls for non-forced-cloud-downloads too */
            dllink = getDllinkCloud(link, account);
            if (dllink != null) {
                // found_downloadurl_in_cloud_downloads = true;
            } else {
                final long userDefinedWaitHours = this.getPluginConfig().getLongProperty("DOWNLOADLINK_GENERATION_LIMIT", 0);
                final long timestamp_next_downloadlink_generation_allowed = link.getLongProperty("PROLEECH_TIMESTAMP_LAST_SUCCESSFUL_DOWNLOADLINK_CREATION", 0) + (userDefinedWaitHours * 60 * 60 * 1000);
                if (userDefinedWaitHours > 0 && timestamp_next_downloadlink_generation_allowed > System.currentTimeMillis()) {
                    final long waittime_until_next_downloadlink_generation_is_allowed = timestamp_next_downloadlink_generation_allowed - System.currentTimeMillis();
                    final String waittime_until_next_downloadlink_generation_is_allowed_Str = TimeFormatter.formatSeconds(waittime_until_next_downloadlink_generation_is_allowed / 1000, 0);
                    logger.info("Next downloadlink generation is allowed in: " + waittime_until_next_downloadlink_generation_is_allowed_Str);
                    /*
                     * 2019-08-14: Set a small waittime here so links can be tried earlier again - so not set the long waittime
                     * waittime_until_next_downloadlink_generation_is_allowed!
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Next downloadlink generation is allowed in " + waittime_until_next_downloadlink_generation_is_allowed_Str, 5 * 60 * 1000l);
                }
                /* Login - first try without validating cookies! */
                final boolean validatedCookies = login(account, null, false);
                final PostRequest post = new PostRequest("https://" + this.getHost() + "/dl/debrid/deb_process.php");
                post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
                post.put("urllist", URLEncoder.encode(url, "UTF-8"));
                final String pass = link.getDownloadPassword();
                if (StringUtils.isEmpty(pass)) {
                    post.put("pass", "");
                } else {
                    post.put("pass", URLEncoder.encode(pass, "UTF-8"));
                }
                post.put("boxlinklist", "0");
                sendRequest(post);
                dllink = getDllink(link, account);
                if (StringUtils.isEmpty(dllink) && !validatedCookies && !this.isLoggedin(this.br)) {
                    /* Bad login - try again with fresh / validated cookies! */
                    login(account, null, true);
                    sendRequest(post);
                    dllink = getDllink(link, account);
                }
            }
            link.setProperty("PROLEECH_TIMESTAMP_LAST_SUCCESSFUL_DOWNLOADLINK_CREATION", System.currentTimeMillis());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            /* Now we are definitely loggedIN */
            isLoggedIN = true;
        }
        final String server_filename = getFileNameFromDispositionHeader(dl.getConnection());
        final boolean isOkay = isDownloadConnection(dl.getConnection());
        if (!isOkay) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "unknowndlerror", 50, 2 * 60 * 1000l);
        }
        link.setProperty(getHost(), dllink);
        if (dl.startDownload()) {
            final String internal_filename = getInternalFilename(link);
            if (internal_filename != null) {
                deleteDownloadHistoryFilenameWhitelist.add(internal_filename);
            } else if (!StringUtils.isEmpty(server_filename)) {
                /* Try this as fallback */
                deleteDownloadHistoryFilenameWhitelist.add(server_filename);
            }
            clearDownloadHistory(account, isLoggedIN);
        }
    }

    /** Deletes entries from serverside download history if: File is successfully downloaded, file is olde than X days */
    private void clearDownloadHistory(final Account account, final boolean isLoggedIN) {
        synchronized (account) {
            try {
                /* Only clear download history if user wants it AND if we're logged-in! */
                if (this.getPluginConfig().getBooleanProperty("CLEAR_DOWNLOAD_HISTORY_AFTER_EACH_SUCCESSFUL_DOWNLOAD_AND_ON_ACCOUNTCHECK", false) && isLoggedIN) {
                    logger.info("Trying to delete download history");
                    /*
                     * Do not use Cloudflare browser here - we do not want to get any captchas here! Rather fail than having to enter a
                     * captcha!
                     */
                    if (br.getURL() == null || !br.getURL().contains("/mydownloads")) {
                        /* Only access this URL if it has not been accessed before! */
                        getPage("https://" + this.getHost() + "/mydownloads");
                    }
                    final String[] cloudDownloadRows = getDownloadHistoryRows();
                    final ArrayList<String> filename_entries_to_delete = new ArrayList<String>();
                    /* First, let's check for old/dead entries and add them to our list of items we will remove later. */
                    for (final String filenameToCheck : deleteDownloadHistoryFilenameWhitelist) {
                        if (!br.toString().contains(filenameToCheck)) {
                            filename_entries_to_delete.add(filenameToCheck);
                        }
                    }
                    if (cloudDownloadRows != null && cloudDownloadRows.length > 0) {
                        logger.info("Found " + cloudDownloadRows.length + " possible download_ids in history to delete");
                        String postData = "delete=Delete+selected";
                        int numberofDownloadIdsToDelete = 0;
                        for (final String cloudDownloadRow : cloudDownloadRows) {
                            final String download_id = new Regex(cloudDownloadRow, "id=\"checkbox\\[\\]\" value=\"(\\d+)\"").getMatch(0);
                            if (download_id == null) {
                                /* Skip invalid entries */
                                continue;
                            }
                            final String date_when_entry_was_addedStr = new Regex(cloudDownloadRow, ">\\s*(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s*<").getMatch(0);
                            final long delete_after_x_days = 1;
                            boolean isOldEntry = false;
                            final boolean isStillDownloadingToCloud = new Regex(cloudDownloadRow, "Transfering \\d{1,3}\\.\\d{1,2} ").matches();
                            boolean deletionAllowedByFilename = false;
                            String filename_of_current_entry = null;
                            for (final String deletionAllowedFilename : deleteDownloadHistoryFilenameWhitelist) {
                                if (cloudDownloadRow.contains(deletionAllowedFilename)) {
                                    deletionAllowedByFilename = true;
                                    filename_of_current_entry = deletionAllowedFilename;
                                    filename_entries_to_delete.add(deletionAllowedFilename);
                                    break;
                                }
                            }
                            if (date_when_entry_was_addedStr != null) {
                                final long current_time = getCurrentServerTime(br, System.currentTimeMillis());
                                final long date_when_entry_was_added = TimeFormatter.getMilliSeconds(date_when_entry_was_addedStr, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                                final long time_passed = current_time - date_when_entry_was_added;
                                if (time_passed > delete_after_x_days * 24 * 60 * 60 * 1000l) {
                                    isOldEntry = true;
                                }
                            }
                            if (isStillDownloadingToCloud) {
                                logger.info("NOT deleting the following download_id because cloud download is still in progress: " + download_id);
                                continue;
                            } else if (!deletionAllowedByFilename && !isOldEntry) {
                                logger.info("NOT deleting the following download_id because its' filename is not allowed for deletion and it is not old enough: " + download_id);
                                continue;
                            }
                            if (isOldEntry) {
                                logger.info("Deleting the following entry as it is older than " + delete_after_x_days + " days: " + download_id);
                            } else {
                                logger.info("Deleting the following entry as deletion is allowed by filename: " + download_id + " | " + filename_of_current_entry);
                            }
                            postData += "&checkbox%5B%5D=" + download_id;
                            numberofDownloadIdsToDelete++;
                        }
                        if (numberofDownloadIdsToDelete == 0) {
                            /* This is unlikely but possible! */
                            logger.info("Found no download_ids to delete --> Probably all existing download_ids are download_ids with serverside cloud download in progress or they were added via website or via another JD instance and are not yet old enough to get deleted");
                        } else {
                            logger.info("Deleting " + numberofDownloadIdsToDelete + " of " + cloudDownloadRows.length + " download_ids");
                            br.postPage(br.getURL(), postData);
                            logger.info("Successfully cleared download history");
                        }
                        /*
                         * Cleanup deleteDownloadHistoryFilenameWhitelist - remove supposedly deleted elements from that list. And also
                         * elements which do not exist at all in the website list e.g. automatically deleted or deleted by other user in
                         * case people share accounts and download the same files.
                         */
                        for (final String deleted_filename : filename_entries_to_delete) {
                            deleteDownloadHistoryFilenameWhitelist.remove(deleted_filename);
                        }
                    }
                } else {
                    logger.info("Not deleting download history - used has disabled this setting");
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                logger.info("Error occured in delete-download-history handling");
            }
        }
    }

    private long getCurrentServerTime(final Browser br, final long fallback) {
        return getCurrentServerTime(br, "EEE, dd MMM yyyy HH:mm:ss z", fallback);
    }

    private long getCurrentServerTime(final Browser br, final String formatter, final long fallback_time) {
        long serverTime = -1;
        if (br != null && br.getHttpConnection() != null) {
            // lets use server time to determine time out value; we then need to adjust timeformatter reference +- time against server time
            final String dateString = br.getHttpConnection().getHeaderField("Date");
            if (dateString != null) {
                if (StringUtils.isNotEmpty(formatter) && dateString.matches("[A-Za-z]+, \\d{1,2} [A-Za-z]+ \\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} [A-Z]+")) {
                    serverTime = TimeFormatter.getMilliSeconds(dateString, formatter, Locale.ENGLISH);
                } else {
                    final Date date = TimeFormatter.parseDateString(dateString);
                    if (date != null) {
                        serverTime = date.getTime();
                    }
                }
            }
        }
        if (serverTime == -1) {
            /* Fallback */
            serverTime = fallback_time;
        }
        return serverTime;
    }

    private String[] getDownloadHistoryRows() {
        return br.getRegex("<tr>\\s*<td><input[^>]*name=\"checkbox\\[\\]\"[^>]+>.*?</tr>").getColumn(-1);
    }

    private String getDllink(final DownloadLink link, final Account account) throws Exception {
        String dllink = br.getRegex("class=\"[^\"]*success\".*<a href\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
        /*
         * We can only identify our cloud-download-item by filename so let's get the filename they display as it may differ from the
         * filename we know/expect which means this is the only identifier we can use!
         */
        final String normal_download_internal_filename = br.getRegex("<a href=\"[^\"]+\"[^>]*?><b>([^<>\"]+)</a>").getMatch(0);
        String cloud_download_internal_filename = br.getRegex(">Transloading in progress.*?once completed\\!?</a>([^<>\"]+)</div>").getMatch(0);
        if (cloud_download_internal_filename == null) {
            /* 2019-12-01: 2nd version */
            cloud_download_internal_filename = br.getRegex(">Transloading in progress.*?once completed\\!?</a>\\s+<b>([^<>\"]*?)\\s*\\(<font color").getMatch(0);
        }
        if (dllink == null && cloud_download_internal_filename != null) {
            logger.info("Enforced cloud download: Needs to finish serverside download before we can download it --> Checking one time whether it might already be downloadable");
            link.setProperty("proleech_internal_filename", cloud_download_internal_filename);
            /* Mark as cloud download so that getDllinkCloud can be used! */
            link.setProperty("is_cloud_download", true);
            try {
                dllink = getDllinkCloud(link, account);
            } catch (final PluginException e) {
            }
            if (dllink == null) {
                /**
                 * TODO: If a cloud download gets stuck serverside, we might need a handling to force-delete old entries and fully re-add
                 * them to the cloud downloader after a certain time ...
                 */
                logger.info("Unable to find downloadurl right away --> Waiting to retry later");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Added URL to proleech.link Cloud-downloader: Cloud download pending", 30 * 1000);
            }
        } else if (dllink != null && normal_download_internal_filename != null) {
            link.setProperty("proleech_internal_filename", normal_download_internal_filename);
        } else if (dllink != null && normal_download_internal_filename == null) {
            logger.info("Failed to find internal_filename --> We will probably be unable to delete this file");
        }
        if (StringUtils.isEmpty(dllink)) {
            final String website_errormessage = br.getRegex("class=\"[^\"]*danger\".*?<b>\\s*([^<>]+)").getMatch(0);
            if (website_errormessage != null) {
                /* 2019-11-11: E.g. "Too many requests! Please try again in a few seconds." */
                logger.info("Found errormessage on website:");
                logger.info(website_errormessage);
            }
            if (br.containsHTML(">\\s*?No link entered\\.?\\s*<")) {
                mhm.handleErrorGeneric(account, link, "no_link_entered", 50, 2 * 60 * 1000l);
            } else if (br.containsHTML(">\\s*Error getting the link from this account")) {
                mhm.putError(account, link, 2 * 60 * 1000l, "Error getting the link from this account");
            } else if (br.containsHTML(">\\s*Our account has reached traffic limit")) {
                mhm.putError(account, link, 2 * 60 * 1000l, "Error getting the link from this account");
            } else if (br.containsHTML(">\\s*This filehost is only enabled in")) {
                mhm.putError(account, link, 10 * 60 * 1000l, "This filehost is only available in premium mode");
            } else if (br.containsHTML(">\\s*You can only generate this link during Happy Hours")) {
                /* 2019-08-15: Can happen in free account mode - no idea when this "Happy Hour" is. Tested with uptobox.com URLs. */
                throw new AccountUnavailableException("You can only generate this link during Happy Hours", 5 * 60 * 1000l);
            } else if (website_errormessage != null) {
                /* Unknown failure but let's display errormessage from website to user. */
                // mhm.handleErrorGeneric(account, link, website_errormessage, 50, 2 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, website_errormessage, 5 * 60 * 1000l);
            } else {
                /* Unknown failure and we failed to find an errormessage */
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 2 * 60 * 1000l);
            }
        }
        return dllink;
    }

    /**
     * Tries to find final downloadurl for URLs which either had to be first downloaded to the proleech cloud or which were attempted to
     * download before and are still in the cloud.
     *
     * @throws Exception
     */
    private String getDllinkCloud(final DownloadLink link, final Account account) throws Exception {
        if (link == null) {
            return null;
        }
        final boolean is_forced_cloud_download = this.isForcedCloudDownload(link);
        final String internal_filename = this.getInternalFilename(link);
        if (internal_filename != null || is_forced_cloud_download) {
            if (is_forced_cloud_download) {
                logger.info("Checking for directurl of forced cloud download URL");
            } else {
                logger.info("Checking for directurl of NON-forced cloud download URL");
            }
            getPage("https://" + this.getHost() + "/mydownloads");
            if (!this.isLoggedin(this.br)) {
                /* Ensure that we're logged-in */
                login(account, null, true);
                getPage("/mydownloads");
            }
            final String errortext_basic = "Proleech.link Cloud-downloader:";
            boolean foundDownloadHistoryRow = false;
            String cloud_download_progress = null;
            String dllink = null;
            final String[] downloadHistoryRows = getDownloadHistoryRows();
            if (downloadHistoryRows == null || downloadHistoryRows.length == 0) {
                logger.warning("Failed to find any download history objects --> Possible plugin failure");
                if (is_forced_cloud_download) {
                    logger.info("Failed to find any information about pending cloud download");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext_basic + " Failed to find any information about pending cloud download", 30 * 1000l);
                }
                /* No forced cloud download --> Continue so maybe upper handling will re-add downloadurl to list */
                return null;
            }
            for (final String downloadHistoryRow : downloadHistoryRows) {
                if (downloadHistoryRow.contains(internal_filename)) {
                    logger.info("Looks like we found the row containing our cloud-download");
                    foundDownloadHistoryRow = true;
                    /* RegEx for cloud-forced-downloads ("Save To Cloud" column) */
                    dllink = new Regex(downloadHistoryRow, "<a href=\"(https?://[^/]+/download/[^<>\"]+)\"").getMatch(0);
                    /* TODO: Add proper errorhandling for broken final downloadurls, then enable this. */
                    // if (dllink == null) {
                    // /* RegEx for non-cloud-forced-downloads ("Direct link" column) */
                    // dllink = new Regex(downloadHistoryRow, "<a href=\"(https?://[^/]+/d\\d+/[^<>\"]+)\"><u>Download</u>").getMatch(0);
                    // }
                    if (dllink != null) {
                        logger.info("Successfully found final downloadurl");
                        return dllink;
                    } else {
                        logger.info("Failed to find final downloadurl");
                        cloud_download_progress = new Regex(downloadHistoryRow, "Transfering (\\d{1,3}\\.\\d{1,2})\\s*?\\%").getMatch(0);
                        /* Continue! It can happen that there are multiple entries for the same filename! */
                    }
                }
            }
            if (dllink == null && is_forced_cloud_download) {
                /* This could also be a plugin failure but let's hope that a final downloadurl will be available later! */
                if (!foundDownloadHistoryRow) {
                    logger.info("Failed to find download history row");
                    /* TODO: Maybe remove this property to perform a full retry next time */
                    // link.removeProperty("proleech_internal_filename");
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errortext_basic + " Failed to find cloud download information", 30 * 1000l);
                } else {
                    logger.info("Failed to find downloadurl for forced cloud download --> Serverside download is probably still running");
                    String error_text = errortext_basic + " Cloud download pending(?)";
                    if (!StringUtils.isEmpty(cloud_download_progress)) {
                        error_text = "[" + cloud_download_progress + "%] " + error_text;
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error_text, 15 * 1000l);
                }
            }
        }
        return null;
    }

    private boolean isForcedCloudDownload(final DownloadLink link) {
        return link.getBooleanProperty("is_cloud_download", false);
    }

    private String getInternalFilename(final DownloadLink link) {
        return link.getStringProperty("proleech_internal_filename", null);
    }

    private boolean isDownloadConnection(URLConnectionAdapter con) throws IOException {
        final boolean ret = con.isOK() && (con.isContentDisposition() || StringUtils.containsIgnoreCase(con.getContentType(), "application/force-download"));
        return ret;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, null, true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), true, 0);
        final boolean isOkay = isDownloadConnection(dl.getConnection());
        if (!isOkay) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (StringUtils.endsWithCaseInsensitive(br.getURL(), "/downloader")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        dl.startDownload();
    }

    private void setConfigElements() {
        /* Crawler settings */
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "DOWNLOADLINK_GENERATION_LIMIT", "Allow new downloadlink generation every X hours (default = 0 = unlimited/disabled)\r\nThis can save traffic but this can also slow down the download process", 0, 72, 1).setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "CLEAR_DOWNLOAD_HISTORY_AFTER_EACH_SUCCESSFUL_DOWNLOAD_AND_ON_ACCOUNTCHECK", "Delete download history after every successful download and on every account check (all successfully downloaded entries & all older than 24 hours)?").setDefaultValue(false));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
