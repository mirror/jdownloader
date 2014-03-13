//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hulkshare.com" }, urls = { "http://(www\\.)?(hulksharedecrypted\\.com/playlistsong/\\d+|(((hulkshare\\.com|hu\\.lk)/dl/|hulksharedecrypted\\.com/)|old\\.hulkshare\\.com/dl/)[a-z0-9]{12})" }, flags = { 2 })
public class HulkShareCom extends PluginForHost {

    private String              BRBEFORE            = "";
    private static final String PASSWORDTEXT        = "(<br><b>Password:</b> <input|<br><b>Passwort:</b> <input)";
    private static final String COOKIE_HOST         = "http://hulkshare.com";
    public boolean              NOPREMIUM           = false;
    private static final String MAINTENANCE         = ">This server is in maintenance mode";
    private static final String MAINTENANCEUSERTEXT = "This server is under Maintenance";
    private static Object       LOCK                = new Object();

    private static final String TYPE_PLAYLISTSONG   = "http://(www\\.)?hulksharedecrypted\\.com/playlistsong/\\d+";

    public HulkShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/premium.html");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches("http://(www\\.)?(((hulkshare\\.com|hu\\.lk)/dl/|hulksharedecrypted\\.com/)|old\\.hulkshare\\.com/dl/)[a-z0-9]{12}")) {
            link.setUrlDownload("http://www.hulkshare.com/" + new Regex(link.getDownloadURL(), "/([a-z0-9]{12})$").getMatch(0));
        } else if (link.getDownloadURL().contains("hulksharedecrypted.com/")) {
            link.setUrlDownload(link.getDownloadURL().replace("hulksharedecrypted.com/", "hulkshare.com/"));
            link.setProperty("fileoffline", true);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        br.setCookie(COOKIE_HOST, "lang", "english");
        if (link.getBooleanProperty("fileoffline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Convert playlist-songs to real single links
        if (link.getDownloadURL().matches(TYPE_PLAYLISTSONG)) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://www.hulkshare.com/mp3PlayerScripts/flConfig_hsPlayerLight.php?id=" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "&playlist=1", "");
            if (br.containsHTML("<empty>1</empty>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final String realID = br.getRegex("hulkshare\\.com/dl/([a-z0-9]{12})").getMatch(0);
            if (realID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setUrlDownload("http://www.hulkshare.com/" + realID);
        }
        br.getPage(link.getDownloadURL());
        final String argh = br.getRedirectLocation();
        // Handling for direct links
        if (argh != null) {
            if (argh.contains("hulkshare.com/404.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(argh);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(getFileNameFromHeader(con));
                    link.setProperty("freelink", argh);
                    return AvailableStatus.TRUE;
                } else {
                    br.getPage(argh);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (br.containsHTML("You have reached the download\\-limit")) {
            logger.warning("Waittime detected, please reconnect to make the linkchecker work!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(No such file|No such user exist|File not found)")) {
            logger.warning("file is 99,99% offline, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(DMCA notice)")) {
            logger.warning("This file has been subject to a DMCA notice and has accordingly been disabled for public access, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("fileName = \"([^<>]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("You have requested.*?http://.*?[a-z0-9]{12}/(.*?)</font>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("Filename:</b></td><td >(.*?)</td>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("File ?name.*?nowrap.*?>(.*?)</td").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("class=\"jp\\-file\\-string\">(.*?)</div>").getMatch(0);
                            if (filename == null) {
                                filename = br.getRegex("<title>Listen to (.*?) on Hulkshare.*</title>").getMatch(0);
                                if (filename == null) {
                                    filename = br.getRegex("<h2>[\r\n\t ]+(.*?)[\r\n\t ]+<input").getMatch(0);
                                    if (filename == null) {
                                        filename = br.getRegex("<meta property=\"og:title\" content=\"([^\"]+)\"").getMatch(0);
                                        if (filename == null) { // old, duped from new rule
                                            filename = br.getRegex("<title>(.*?) - Hulk Share -  Music Distribution Platform</title>").getMatch(0);
                                            if (filename == null) { // old, duped from new rule
                                                filename = br.getRegex("<h2>(.*?)</h2>[\r\n\t ]+by").getMatch(0);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        String filesize = br.getRegex("<small>\\((.*?)\\)</small>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
        if (filesize == null) filesize = br.getRegex("</font>[ ]+\\((.*?)\\)(.*?)</font>").getMatch(0);
        if (filesize == null) filesize = br.getRegex("<b>Size:</b> (.*?)<br />").getMatch(0);
        if (filesize == null) filesize = br.getRegex("class=\"tsSize\">(.*?)<").getMatch(0);

        if (filename == null) {
            logger.warning("The filename equals null, throwing \"file not found\" now...");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = Encoding.htmlDecode(filename.trim().replaceAll("(</b>|<b>|\\.html)", ""));
        String ext = null;
        int index = filename.lastIndexOf(".");
        if (index >= 0) {
            ext = filename.substring(index);
        }
        if (br.containsHTML("hulkshare\\.com/socialplayer/hsfbPlayer") && (ext == null || ext.length() > 5) && !filename.endsWith(".mp3")) filename += ".mp3";
        link.setFinalFileName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private String checkDLLink(DownloadLink downloadLink) {
        URLConnectionAdapter con = null;
        try {
            Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            String url = "http://hulkshare.com/dl/" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0);
            con = br2.openGetConnection(url);
            if (con.getContentType().contains("html") || con.getLongContentLength() == -1) { return null; }
            return url;
        } catch (Exception e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    public void checkErrors(DownloadLink theLink, boolean checkAll, String passCode) throws NumberFormatException, PluginException {
        if (checkAll) {
            if (new Regex(BRBEFORE, PASSWORDTEXT).matches() || BRBEFORE.contains("Wrong password")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                theLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            if (BRBEFORE.contains("Wrong captcha")) {
                logger.warning("Wrong captcha or wrong password!");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (BRBEFORE.contains("\">Skipped countdown<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Fatal countdown error (countdown skipped)");
        }
        // Some waittimes...
        if (BRBEFORE.contains("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = new Regex(BRBEFORE, "You have to wait.*?\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = new Regex(BRBEFORE, "You have to wait.*?\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = new Regex(BRBEFORE, "You have to wait.*?\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            if (waittime != 0) {
                logger.info("Detected waittime #1, waiting " + waittime + " milliseconds");
                // Not enough waittime to reconnect->Wait and try again
                if (waittime < 120000) {
                    sleep(waittime, theLink);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            } else {
                logger.info("Waittime regexes seem to be broken");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        }
        if (BRBEFORE.contains("You have reached the download-limit")) {
            String tmphrs = new Regex(BRBEFORE, "\\s+(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(BRBEFORE, "\\s+(\\d+)\\s+minutes?").getMatch(0);
            String tmpsec = new Regex(BRBEFORE, "\\s+(\\d+)\\s+seconds?").getMatch(0);
            String tmpdays = new Regex(BRBEFORE, "\\s+(\\d+)\\s+days?").getMatch(0);
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
                // Not enough waittime to reconnect->Wait and try again
                if (waittime < 120000) {
                    sleep(waittime, theLink);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        }
        if (BRBEFORE.contains("You're using all download slots for IP")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l); }
        if (BRBEFORE.contains("Error happened when generating Download Link")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error!", 10 * 60 * 1000l);
        // Errorhandling for only-premium links
        if (new Regex(BRBEFORE, "( can download files up to |Upgrade your account to download bigger files|>Upgrade your account to download larger files|>The file You requested  reached max downloads limit for Free Users|Please Buy Premium To download this file<|This file reached max downloads limit)").matches()) {
            String filesizelimit = new Regex(BRBEFORE, "You can download files up to(.*?)only").getMatch(0);
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
                throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable via premium or registered");
            }
        }
        if (BRBEFORE.contains(MAINTENANCE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.xfilesharingprobasic.undermaintenance", MAINTENANCEUSERTEXT), 2 * 60 * 60 * 1000l);
    }

    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(BRBEFORE, Pattern.compile("(No file|No htmlCode read)", Pattern.CASE_INSENSITIVE)).matches()) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (new Regex(BRBEFORE, "(File Not Found|<h1>404 Not Found</h1>)").matches()) {
            logger.warning("Server says link offline, please recheck that!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final boolean checkFastWay) throws Exception, PluginException {
        String passCode = null;
        String md5hash = new Regex(BRBEFORE, "<b>MD5.*?</b>.*?nowrap>(.*?)<").getMatch(0);
        if (md5hash != null) {
            md5hash = md5hash.trim();
            logger.info("Found md5hash: " + md5hash);
            downloadLink.setMD5Hash(md5hash);
        }
        if (br.containsHTML("class=\"nhsDisabledPlayerText\"")) throw new PluginException(LinkStatus.ERROR_FATAL, "This file cannot be downloaded");
        String dllink = null;
        if (checkFastWay) {
            dllink = downloadLink.getStringProperty("freelink");
            if (dllink != null) {
                try {
                    Browser br2 = br.cloneBrowser();
                    URLConnectionAdapter con = br2.openGetConnection(dllink);
                    if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                        downloadLink.setProperty("freelink", Property.NULL);
                        dllink = null;
                    }
                    con.disconnect();
                } catch (Exception e) {
                    dllink = null;
                }
            }
        }
        if (dllink == null) dllink = apiDownload(downloadLink);
        if (dllink == null) dllink = checkDLLink(downloadLink);
        // Videolinks can already be found here, if a link is found here we can skip waittimes and captchas
        if (dllink == null) {
            checkErrors(downloadLink, false, passCode);
            if (BRBEFORE.contains("\"download1\"")) {
                br.postPage(downloadLink.getDownloadURL(), "op=download1&usr_login=&id=" + new Regex(downloadLink.getDownloadURL(), COOKIE_HOST.replace("http://", "") + "/" + "([a-z0-9]{12})").getMatch(0) + "&fname=" + Encoding.urlEncode(downloadLink.getName()) + "&referer=&method_free=Free+Download");
                doSomething();
                checkErrors(downloadLink, false, passCode);
            }
            dllink = getDllink(downloadLink);
        }
        if (dllink == null) {
            Form dlForm = br.getFormbyProperty("name", "F1");
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            long timeBefore = System.currentTimeMillis();
            boolean password = false;
            boolean skipWaittime = false;
            if (new Regex(BRBEFORE, PASSWORDTEXT).matches()) {
                password = true;
                logger.info("The downloadlink seems to be password protected.");
            }

            /* Captcha START */
            if (BRBEFORE.contains(";background:#ccc;text-align")) {
                logger.info("Detected captcha method \"plaintext captchas\" for this host");
                // Captcha method by ManiacMansion
                String[][] letters = new Regex(Encoding.htmlDecode(br.toString()), "<span style=\\'position:absolute;padding\\-left:(\\d+)px;padding\\-top:\\d+px;\\'>(\\d)</span>").getMatches();
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
                dlForm.put("code", code.toString());
                logger.info("Put captchacode " + code.toString() + " obtained by captcha metod \"plaintext captchas\" in the form.");
            } else if (BRBEFORE.contains("/captchas/")) {
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
                String code = getCaptchaCode("xfilesharingprobasic", captchaurl, downloadLink);
                dlForm.put("code", code);
                logger.info("Put captchacode " + code + " obtained by captcha metod \"Standard captcha\" in the form.");
            } else if (new Regex(BRBEFORE, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
                logger.info("Detected captcha method \"Re Captcha\" for this host");
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.setForm(dlForm);
                String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
                rc.setId(id);
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                Form rcform = rc.getForm();
                rcform.put("recaptcha_challenge_field", rc.getChallenge());
                rcform.put("recaptcha_response_field", Encoding.urlEncode(c));
                logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                dlForm = rc.getForm();
                // waittime is often skippable for reCaptcha handling
                // skipWaittime = true;
            }
            /* Captcha END */
            if (password) passCode = handlePassword(passCode, dlForm, downloadLink);
            if (!skipWaittime) waitTime(timeBefore, downloadLink);
            br.submitForm(dlForm);
            logger.info("Submitted DLForm");
            doSomething();
            checkErrors(downloadLink, true, passCode);
            dllink = getDllink(downloadLink);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = Encoding.htmlDecode(dllink);
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        // Workaround for missing extensions for audio files
        if (dl.getConnection().getContentType().equals("audio/mpeg") && !downloadLink.getFinalFileName().endsWith(".mp3")) downloadLink.setFinalFileName(downloadLink.getFinalFileName() + ".mp3");
        if (dl.getConnection().getContentType().contains("html")) {
            downloadLink.setProperty("freelink", null);
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            /** Downloadlink just redirects into nowhere */
            if (br.getURL().contains("hulkshare.com/dl/") || br.containsHTML("(Uploaded Now|<b>Size:</b> 0 b<br)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: bad downloadlink!", 30 * 60 * 1000l);
            if (br.containsHTML("<h2>This is a private file.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    // This is how their downloadmanager works
    private String apiDownload(final DownloadLink dl) throws IOException {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(false);
        br2.getHeaders().put("User-Agent", "Java/1.6.0_37");
        br2.getHeaders().put("Accept", "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2");
        br2.getHeaders().remove("Accept-Language");
        br2.getPage("http://www.hulkshare.com/api.php?q=%7B%22header%22%3A%7B%22clientRevision%22%3A1.3%2C%22system%22%3A%22windows+7%22%2C%22userId%22%3A%22%22%7D%2C%22function%22%3A%22file%22%2C%22parameters%22%3A%7B%22fileCode%22%3A%5B%22" + new Regex(dl.getDownloadURL(), "([a-z0-9]{12})$").getMatch(0) + "%22%5D%7D%7D");
        return br2.getRedirectLocation();
    }

    // Removed fake messages which can kill the plugin
    public void doSomething() throws NumberFormatException, PluginException {
        BRBEFORE = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            BRBEFORE = BRBEFORE.replace(fun, "");
        }
    }

    private String execJS(final String fun) throws Exception {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            result = engine.eval(fun);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (result == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return result.toString();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex(Pattern.compile("<td>Used space:</td>.*?<td.*?b>([0-9\\.]+) of [0-9\\.]+ (Mb|GB)</b>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim() + " Mb");
        String points = br.getRegex(Pattern.compile("<td>You have collected:</td.*?b>([^<>\"\\']+)premium points", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) {
            // Who needs half points ? If we have a dot in the points, just remove it
            if (points.contains(".")) {
                String dot = new Regex(points, ".*?(\\.(\\d+))").getMatch(0);
                points = points.replace(dot, "");
            }
            ai.setPremiumPoints(Long.parseLong(points.trim()));
        }
        account.setValid(true);
        String availabletraffic = new Regex(BRBEFORE, "Traffic available.*?:</TD><TD><b>([^<>\"\\']+)</b>").getMatch(0);
        if (availabletraffic != null && !availabletraffic.contains("nlimited") && !availabletraffic.equalsIgnoreCase(" Mb")) {
            availabletraffic.trim();
            // need to set 0 traffic left, as getSize returns positive result, even when negative value supplied.
            if (!availabletraffic.startsWith("-")) {
                ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
            } else {
                ai.setTrafficLeft(0);
            }
        } else {
            ai.setUnlimitedTraffic();
        }
        if (!NOPREMIUM) {
            String expire = new Regex(BRBEFORE, Pattern.compile("<td>Premium(\\-| )Account expires?:</td>.*?<td>(<b>)?(\\d{1,2} [A-Za-z]+ \\d{4})(</b>)?</td>", Pattern.CASE_INSENSITIVE)).getMatch(2);
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

    // XfileSharingProBasic Version 2.5.0.7
    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/tos.html";
    }

    public String getDllink(DownloadLink downloadLink) throws Exception {
        String dllink = br.getRegex("<div style=\"width: 300px; margin: 0 10px 15px; margin\\-top: 10px; float: left; text\\-align:justify;\">[\t\n\r ]+<div style=\"float: right; text\\-align:justify;\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("This direct link will be available for your IP.*?href=\"(http.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("Download: <a href=\"(.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://[0-9\\.]+/d/[a-z0-9]+/.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("\"(http://[0-9w]+\\.hulkshare\\.com/d/[a-z0-9]+/.*?)\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("\"(http://[a-z0-9]+\\.hulkshare\\.com/hulkdl/[a-z0-9]+/.*?)\"").getMatch(0);
                            if (dllink == null) {
                                /* Don't use the existing browser here! */
                                final Browser br2 = new Browser();
                                br2.setFollowRedirects(false);
                                br2.getPage("http://www.hulkshare.com/ap-" + new Regex(downloadLink.getDownloadURL(), "hulkshare\\.com/(.+)").getMatch(0) + ".mp3");
                                dllink = br2.getRedirectLocation();
                                if (dllink != null) {
                                    br2.getPage(dllink);
                                    dllink = br2.getRedirectLocation();
                                }
                                if (dllink == null) {
                                    String jsCrap = br.getRegex("style=\"width: 360px; height: 100px; margin: 0 10px; overflow: auto; float: left;\">[\t\n\r ]+<script language=\"JavaScript\">[\t\n\r ]+function [A-Za-z0-9]+\\(\\)[\t\n\r ]+\\{(.*?)window\\.").getMatch(0);
                                    if (jsCrap != null) dllink = execJS(jsCrap);
                                }
                            }
                        }
                    }
                }
            }
        }
        return dllink;

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
        doFree(downloadLink, false, 1, true);
    }

    public String handlePassword(String passCode, Form pwform, DownloadLink thelink) throws IOException, PluginException {
        passCode = thelink.getStringProperty("pass", null);
        if (passCode == null) passCode = Plugin.getUserInput("Password?", thelink);
        pwform.put("password", passCode);
        logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
        return Encoding.urlEncode(passCode);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        String passCode = null;
        requestFileInformation(link);
        login(account, false);
        String dllink = null;
        if (NOPREMIUM) {
            br.getPage(link.getDownloadURL());
            doSomething();
            doFree(link, true, 0, true);
        } else {
            dllink = link.getStringProperty("premlink");
            if (dllink != null) {
                try {
                    Browser br2 = br.cloneBrowser();
                    URLConnectionAdapter con = br2.openGetConnection(dllink);
                    if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                        link.setProperty("premlink", Property.NULL);
                        dllink = null;
                    }
                    con.disconnect();
                } catch (Exception e) {
                    dllink = null;
                }
            }
            if (dllink == null) {
                br.getPage(link.getDownloadURL());
                doSomething();
                dllink = getDllink(link);
                if (dllink == null) {
                    checkErrors(link, true, passCode);
                    Form DLForm = br.getFormbyProperty("name", "F1");
                    if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    if (new Regex(BRBEFORE, PASSWORDTEXT).matches()) passCode = handlePassword(passCode, DLForm, link);
                    br.submitForm(DLForm);
                    doSomething();
                    dllink = getDllink(link);
                    checkErrors(link, true, passCode);
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (passCode != null) link.setProperty("pass", passCode);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                doSomething();
                checkServerErrors();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("premlink", dllink);
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
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

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("login") && cookies.containsKey("xfss") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(COOKIE_HOST, key, value);
                    }
                    return;
                }
            }
            br.setCookie(COOKIE_HOST, "lang", "english");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://www.hulkshare.com/ajax/login.php", "simple=1&autologin=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.containsHTML("\"error\":\"1\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            if (br.getCookie(COOKIE_HOST, "hsrememberme") == null || br.getCookie(COOKIE_HOST, "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            doSomething();
            if (!new Regex(BRBEFORE, "(Premium\\-Account expire|>Renew premium<)").matches()) NOPREMIUM = true;
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(COOKIE_HOST);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void waitTime(long timeBefore, DownloadLink downloadLink) throws PluginException {
        int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
        // Ticket Time
        String ttt = new Regex(BRBEFORE, "countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt == null) ttt = new Regex(BRBEFORE, "id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span").getMatch(0);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            tt -= passedTime;
            logger.info("Waittime detected, waiting " + ttt + " - " + passedTime + " seconds from now on...");
            if (tt > 0) sleep(tt * 1001l, downloadLink);
        }
    }

}