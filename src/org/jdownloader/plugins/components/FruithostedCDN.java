package org.jdownloader.plugins.components;

//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

public class FruithostedCDN extends antiDDoSForHost {
    public FruithostedCDN(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://" + this.getHost() + "/register");
    }

    @Override
    public String getAGBLink() {
        return "http://fruithosted.net/";
    }

    /**
     * Returns whether resume is supported or not for current download mode based on account availability and account type. <br />
     * Override this function to set resume settings!
     */
    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    /**
     * Returns how many max. chunks per file are allowed for current download mode based on account availability and account type. <br />
     * Override this function to set chunks settings!
     *
     * @param downloadlink
     *            TODO
     */
    public int getDownloadModeMaxChunks(final Account account, DownloadLink downloadlink) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getDownloadLinkID(link);
        if (linkid != null) {
            return getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    protected String getDownloadLinkID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    public String getAPIBase() {
        return "https://api.fruithosted.net";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    sb.append(getDownloadLinkID(dl));
                    sb.append(",");
                }
                getPage(br, URLHelper.parseLocation(new URL(getAPIBase()), "/file/info?file=" + URLEncode.encodeURIComponent(sb.toString())));
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("result");
                LinkedHashMap<String, Object> finfo;
                for (final DownloadLink link : links) {
                    final String linkid = getDownloadLinkID(link);
                    /* They only host video files */
                    link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                    finfo = (LinkedHashMap<String, Object>) entries.get(linkid);
                    if (finfo == null) {
                        link.setAvailable(false);
                    } else {
                        final long status = JavaScriptEngineFactory.toLong(finfo.get("status"), 404);
                        if (status != 200) {
                            /* For offline files, nearly all other values will be booleans! */
                            /*
                             * E.g.
                             * {"status":200,"msg":"OK","result":{"anqbqnmdtksrpeds":{"id":"anqbqnmdtksrpeds","status":404,"name":false,
                             * "size":false,"sha1":false,"content_type":false,"cstatus":"error"}}}
                             */
                            link.setAvailable(false);
                        } else {
                            String filename = (String) finfo.get("name");
                            final String sha1 = (String) finfo.get("sha1");
                            final long filesize = JavaScriptEngineFactory.toLong(finfo.get("size"), 0);
                            if (!StringUtils.isEmpty(filename)) {
                                if (!filename.toLowerCase().endsWith(".mp4")) {
                                    /* 2019-08-13: In very rare cases there are filenames containing .mp4 but not ending with .mp4! */
                                    filename += ".mp4";
                                }
                                link.setFinalFileName(filename);
                            }
                            if (filesize > 0) {
                                link.setDownloadSize(filesize);
                            }
                            if (!StringUtils.isEmpty(sha1)) {
                                link.setSha1Hash(sha1);
                            }
                            link.setAvailable(true);
                        }
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null, "free_directlink");
    }

    public void doFree(final DownloadLink downloadLink, final Account acc, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            /*
             * Try website first as it does not require a captcha. Downloaded files are the same as via API, file-hashes fit also for the
             * "website downloads".
             */
            try {
                dllink = getDllinkWebsite(downloadLink, acc, directlinkproperty);
            } catch (final Throwable e) {
                logger.log(e);
            }
            if (StringUtils.isEmpty(dllink)) {
                dllink = getDllinkAPI(downloadLink, null);
            }
        }
        handleDownload(downloadLink, acc, dllink, directlinkproperty);
    }

    private String getDllinkAPI(final DownloadLink downloadLink, final Account acc) throws Exception, PluginException {
        // if (true) {
        // /* 2019-02-01: Download mode not yet done! */
        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // }
        String dllink = null;
        final Form ticketForm = new Form();
        /* TODO: Add more errorhandling */
        ticketForm.setAction(URLHelper.parseLocation(new URL(getAPIBase()), "/file/dlticket"));
        ticketForm.setMethod(MethodType.GET);
        ticketForm.put("file", getDownloadLinkID(downloadLink));
        if (acc != null) {
            ticketForm.put("login", acc.getUser());
            ticketForm.put("key", acc.getPass());
        }
        submitForm(ticketForm);
        handleErrorsAPI();
        final String ticket = PluginJSonUtils.getJson(br, "ticket");
        final String captcha_url = PluginJSonUtils.getJson(br, "captcha_url");
        final long timeBefore = System.currentTimeMillis();
        final Form dlForm = new Form();
        dlForm.setAction(getAPIBase() + "file/dl");
        dlForm.setMethod(MethodType.GET);
        dlForm.put("file", getDownloadLinkID(downloadLink));
        dlForm.put("ticket", ticket);
        if (captcha_url != null) {
            final String captcha_response = this.getCaptchaCode(captcha_url, downloadLink);
            dlForm.put("captcha_response", captcha_response);
        }
        waitTime(downloadLink, timeBefore);
        submitForm(dlForm);
        dllink = PluginJSonUtils.getJson(br, "url");
        return dllink;
    }

    private String getDllinkWebsite(final DownloadLink downloadLink, final Account acc, final String directlinkproperty) throws Exception, PluginException {
        br.getPage(downloadLink.getPluginPatternMatcher());
        String[] match = br.getRegex("type:\"video/mp4\",src:d\\('([^']+)',(\\d+)\\)").getRow(0);
        if (match == null || match.length != 2) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String dllink = websiteDecodeStreamlink(match[0], Integer.parseInt(match[1]));
        return dllink;
    }

    private void handleDownload(final DownloadLink downloadLink, final Account account, final String dllink, final String directlinkproperty) throws Exception {
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean resume = isResumeable(downloadLink, account);
        final int maxchunks = getDownloadModeMaxChunks(account, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private void handleErrorsAPI() throws PluginException {
        int status = 200;
        final String statusStr = PluginJSonUtils.getJson(br, "status");
        if (statusStr != null) {
            status = Integer.parseInt(statusStr);
        }
        switch (status) {
        case 200:
            /* Everything is fine */
            break;
        case 400:
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 400");
        case 403:
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403");
        case 404:
            /* File or Folder not found */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 451:
            /* Not available due to legal reasons */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 509:
            /*
             * {"status":509,
             * "msg":"bandwidth usage too high (peak hours). out of capacity for non-browser downloads. please use browser download"
             * ,"result":null}
             */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Bandwidth usage too high (peak hours)");
        default:
            /* Unknown error */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    /**
     * Handles pre download (pre-captcha) waittime.
     */
    private void waitTime(final DownloadLink downloadLink, final long timeBefore) throws PluginException {
        int wait = 10;
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        /* Ticket Time */
        final String waitStr = PluginJSonUtils.getJson(br, "wait_time");
        if (waitStr != null && waitStr.matches("\\d+")) {
            wait = Integer.parseInt(waitStr);
        }
        wait -= passedTime;
        if (wait > 0) {
            logger.info("Waiting waittime: " + wait);
            sleep(wait * 1000l, downloadLink);
        } else {
            logger.info("No waittime left after captcha");
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
                if (con.isOK() && con.isContentDisposition()) {
                    return dllink;
                } else if (!con.isOK() || con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    return null;
                } else {
                    return dllink;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
            } finally {
                if (con != null) {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return null;
    }

    private void login(final Account acc, final boolean force) throws Exception {
        synchronized (acc) {
            try {
                /** TODO: Check if cookie-login even works */
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = acc.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                /**
                 * 2019-01-01: API Login requires special name/password which can be found here: http://fruithosts.net/account#usersettings
                 * --> FTP/API Information
                 */
                getPage(URLHelper.parseLocation(new URL(getAPIBase()), "/account/info?login=" + acc.getUser() + "&key=" + Encoding.urlEncode(acc.getPass())));
                try {
                    handleErrorsAPI();
                } catch (final PluginException e) {
                    logger.log(e);
                    /* Typically error 403 */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Wrong login!\r\nMake sure that you are using your API/FTP login credentials which can be found here:\r\nhttps://" + getHost() + "/account#usersettings", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                acc.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    acc.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("result");
        final String date_account_created = (String) entries.get("signup_at");
        // final long storage_left = JavaScriptEngineFactory.toLong(entries.get("storage_left"), 0);
        // final long balance = JavaScriptEngineFactory.toLong(entries.get("balance"), 0);
        // final long traffic_used_24h = JavaScriptEngineFactory.toLong(entries.get("used_24h"), 0);
        entries = (LinkedHashMap<String, Object>) entries.get("traffic");
        final long traffic_left = JavaScriptEngineFactory.toLong(entries.get("left"), 0);
        if (!StringUtils.isEmpty(date_account_created)) {
            ai.setCreateTime(TimeFormatter.getMilliSeconds(date_account_created, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        }
        if (traffic_left == -1) {
            ai.setUnlimitedTraffic();
        } else {
            ai.setTrafficLeft(traffic_left);
        }
        if (traffic_left == -1) {
            account.setType(AccountType.FREE);
            /* free accounts can still have captcha */
            account.setMaxSimultanDownloads(getMaxSimultaneousFreeAccountDownloads());
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) user");
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(getMaxSimultanPremiumDownloadNum());
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            doFree(link, account, "account_free_directlink");
        } else {
            /* Force API usage */
            String dllink = checkDirectLink(link, "premium_directlink");
            if (StringUtils.isEmpty(dllink)) {
                dllink = getDllinkAPI(link, account);
            }
            handleDownload(link, account, dllink, "premium_directlink");
        }
    }

    private String websiteDecodeStreamlink(final String url, final int mask) {
        final String key = "=/+9876543210zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA";
        StringBuffer result = new StringBuffer();
        final String u = url.replaceAll("[^A-Za-z0-9\\+\\/\\=]", "");
        int idx = 0;
        while (idx < u.length()) {
            int a = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int b = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int c = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int d = key.indexOf(u.substring(idx, idx + 1));
            idx++;
            int s1 = ((a << 0x2) | (b >> 0x4)) ^ mask;
            result.append(Character.valueOf((char) s1));
            int s2 = ((b & 0xf) << 0x4) | (c >> 0x2);
            if (c != 0x40) {
                result.append(Character.valueOf((char) s2));
            }
            int s3 = ((c & 0x3) << 0x6) | d;
            if (d != 0x40) {
                result.append(Character.valueOf((char) s3));
            }
        }
        return result.toString();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* No account, yes we can expect captcha */
            return true;
        }
        if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        }
        /* Premium accounts should not have captchas */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKED;
    }
}