//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "quickupload.net" }, urls = { "http://[\\w\\.]*?quickupload\\.net/([a-z0-9]{12}/uploads/.*?\\.html|[a-z0-9]{12})" }, flags = { 2 })
public class QuickUploadDotNet extends PluginForHost {

    private static final String COOKIE_HOST = "http://www.quickupload.net";

    public boolean              nopremium   = false;

    private static final String FILEOFFLINE = "<b>(No such file with this filename|No such user exist|File not found)</b>";

    public QuickUploadDotNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    public void checkErrors(DownloadLink theLink) throws NumberFormatException, PluginException {
        // Some waittimes...
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("You have to wait.*?\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            logger.info("Detected waittime #1, waiting " + waittime + "milliseconds");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        if (br.containsHTML("You have reached the download-limit")) {
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = br.getRegex("\\s+(\\d+)\\s+days?").getMatch(0);
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 60 * 60 * 1000l);
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                if (tmpdays != null) days = Integer.parseInt(tmpdays);
                int waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                logger.info("Detected waittime #2, waiting " + waittime + "milliseconds");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("class=\"err\">Error happened when generating Download Link\\.")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        // Errorhandling for only-premium links
        if (br.containsHTML("(You can download files up to.*?only|Upgrade your account to download bigger files|This file reached max downloads)")) {
            String filesizelimit = br.getRegex("You can download files up to(.*?)only").getMatch(0);
            if (filesizelimit != null) {
                filesizelimit = filesizelimit.trim();
                logger.warning("As free user you can download files up to " + filesizelimit + " only");
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Free users can only download files up to " + filesizelimit);
            } else {
                logger.warning("Only downloadable via premium");
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium");
            }
        }
    }

    public void doFree(DownloadLink downloadLink, boolean doWait) throws Exception, PluginException {
        boolean resumable = true;
        int maxchunks = 0;
        // If the filesize regex above doesn't match you can copy this part into
        // the available status (and delete it here)
        Form freeform = br.getFormBySubmitvalue("Kostenloser+Download");
        if (freeform == null) {
            freeform = br.getFormBySubmitvalue("Free+Download");
            if (freeform == null) {
                freeform = br.getFormbyKey("download1");
            }
        }
        if (freeform != null) {
            freeform.remove("method_premium");
            br.submitForm(freeform);
        }
        checkErrors(downloadLink);
        String md5hash = br.getRegex("<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        if (md5hash != null) {
            md5hash = md5hash.trim();
            logger.info("Found md5hash: " + md5hash);
            downloadLink.setMD5Hash(md5hash);
        }
        br.setFollowRedirects(false);
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (doWait) {
            // Ticket Time (only needed when downloading through an account
            String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
            if (ttt == null) ttt = br.getRegex("id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span").getMatch(0);
            if (ttt != null) {
                logger.info("Waittime detected, waiting " + ttt + " seconds from now on...");
                int tt = Integer.parseInt(ttt);
                sleep(tt * 1001, downloadLink);
            }
        }
        String passCode = null;
        boolean password = false;
        boolean recaptcha = false;
        if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input)")) {
            password = true;
            logger.info("The downloadlink seems to be password protected.");
        }

        /* Captcha START */
        if (br.containsHTML("background:#ccc;text-align") && !br.containsHTML("display:none;\">background:#ccc;text-align")) {
            logger.info("Detected captcha method \"plaintext captchas\" for this host");
            // Captcha method by ManiacMansion
            String[][] letters = new Regex(Encoding.htmlDecode(br.toString()), "<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
            if (letters == null || letters.length == 0) {
                logger.warning("plaintext captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
            for (String[] letter : letters) {
                capMap.put(Integer.parseInt(letter[0]), letter[1]);
            }
            StringBuilder code = new StringBuilder();
            for (String value : capMap.values()) {
                code.append(value);
            }
            DLForm.put("code", code.toString());
            logger.info("Put captchacode " + code.toString() + " obtained by captcha metod \"plaintext captchas\" in the form.");
        } else if (br.containsHTML("/captchas/")) {
            logger.info("Detected captcha method \"Standard captcha\" for this host");
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            String captchaurl = null;
            if (sitelinks == null || sitelinks.length == 0) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String link : sitelinks) {
                if (link.contains("/captchas/")) {
                    captchaurl = link;
                    break;
                }
            }
            if (captchaurl == null) {
                logger.warning("Standard captcha captchahandling broken!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String code = getCaptchaCode(captchaurl, downloadLink);
            DLForm.put("code", code);
            logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
        } else if (br.containsHTML("api.recaptcha.net")) {
            // Manual Re Captcha handling
            logger.info("Detected captcha method \"Re Captcha\" for this host");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            // rc.parse();
            String id = br.getRegex("challenge\\?k=(.*?)\"").getMatch(0);
            if (id == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            rc.setId(id);
            rc.setForm(br.getFormbyProperty("name", "F1"));
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            if (password == true) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                rc.getForm().put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                password = false;
            }
            recaptcha = true;
            rc.setCode(c);
            logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
        }
        /* Captcha END */

        // If the hoster uses Re Captcha the form has already been sent before
        // here so here it's checked. Most hosters don't use Re Captcha so
        // usually recaptcha is false
        if (recaptcha == false) {
            if (password == true) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
            }
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLForm, resumable, maxchunks);
            logger.info("Submitted DLForm");
        }
        boolean error = false;
        try {
            if (dl.getConnection().getContentType().contains("html")) {
                error = true;
            }
        } catch (Exception e) {
            error = true;
        }
        if (br.getRedirectLocation() != null || error == true) {
            br.followConnection();
            logger.info("followed connection...");
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                checkErrors(downloadLink);
                if (br.containsHTML("You're using all download slots for IP")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
                if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input|Wrong password)")) {
                    logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                    downloadLink.setProperty("pass", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (br.containsHTML("Wrong captcha")) {
                    logger.warning("Wrong captcha or wrong password!");
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (dllink == null) {
                    dllink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("This direct link will be available for your IP.*?href=\"(http.*?)\"").getMatch(0);
                        if (dllink == null) {
                            // This was for fileop.com, maybe also works for
                            // others!
                            dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                        }
                    }
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        boolean error2 = false;
        try {
            if (dl.getConnection().getContentType().contains("html")) {
                error2 = true;
            }
        } catch (Exception e) {
            error2 = true;
        }
        if (error2 == true) {
            logger.warning("The final dllink seems not to be a file!");
            // Experimental errorhandling
            boolean exception = false;
            try {
                br.followConnection();
            } catch (Exception e) {
                logger.info("The final dllink seems to be a file, trying to download...");
                exception = true;
            }
            if (!exception) {
                if (br.containsHTML("File Not Found")) {
                    logger.warning("Server says link offline, please recheck that!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?b>(.*?)of.*?Mb</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " Mb");
        String points = br.getRegex(Pattern.compile("<td>You have collected:</td.*?b>(.*?)premium points", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) {
            // Who needs half points ? If we have a dot in the points, just
            // remove it
            if (points.contains(".")) {
                String dot = new Regex(points, ".*?(\\.(\\d+))").getMatch(0);
                points = points.replace(dot, "");
            }
            ai.setPremiumPoints(Long.parseLong(points.trim()));
        }
        account.setValid(true);
        String availabletraffic = br.getRegex("Traffic available.*?:</TD><TD><b>(.*?)</b>").getMatch(0);
        if (availabletraffic != null && !availabletraffic.equals(" Mb")) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
        } else {
            ai.setUnlimitedTraffic();
        }
        if (!nopremium) {
            String expire = br.getRegex("<td>Premium-Account expire:</td>.*?<td>(.*?)</td>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                expire = expire.replaceAll("(<b>|</b>)", "");
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", null));
            }
            ai.setStatus("Premium User");
        } else {
            ai.setStatus("Registered (free) User");
        }
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://quickupload.net/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, false);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account);
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (nopremium) {
            doFree(link, true);
        } else {
            String dllink = br.getRedirectLocation();
            if (dllink == null) {
                Form DLForm = br.getFormbyProperty("name", "F1");
                if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input)")) {
                    if (link.getStringProperty("pass", null) == null) {
                        passCode = Plugin.getUserInput("Password?", link);
                    } else {
                        /* gespeicherten PassCode holen */
                        passCode = link.getStringProperty("pass", null);
                    }
                    DLForm.put("password", passCode);
                    logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                }
                br.submitForm(DLForm);
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    if (br.containsHTML("(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input|Wrong password)")) {
                        logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                        link.setProperty("pass", null);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                    if (dllink == null) {
                        dllink = br.getRegex("dotted #bbb;padding.*?<a href=\"(.*?)\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("This direct link will be available for your IP.*?href=\"(http.*?)\"").getMatch(0);
                            if (dllink == null) {
                                // This was for fileop.com, maybe also works for
                                // others!
                                dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                            }
                        }
                    }
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
            boolean error = false;
            try {
                if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                    error = true;
                }
            } catch (Exception e) {
                error = true;
            }
            if (error == true) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                if (br.containsHTML("File Not Found")) {
                    logger.warning("Server says link offline, please recheck that!");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.setDebug(true);
        br.getPage(COOKIE_HOST + "/login.html");
        Form loginform = br.getForm(0);
        if (loginform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginform.put("login", Encoding.urlEncode(account.getUser()));
        loginform.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        br.getPage(COOKIE_HOST + "/?op=my_account");
        if (br.getCookie(COOKIE_HOST, "login") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("Premium-Account expire") && !br.containsHTML("Upgrade to premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (!br.containsHTML("Premium-Account expire")) nopremium = true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie(COOKIE_HOST, "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<input type=\"hidden\" name=\"fname\" value=\"(.*?)\">").getMatch(0));
        String filesize = br.getRegex("You have requested <font color=\"red\">http://quickupload.net/.*?/.*?</font> \\((.*?)\\)</font>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setFinalFileName(filename.replace("-quickupload.net", ""));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}