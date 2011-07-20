//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filepost.com" }, urls = { "http://(www\\.)?filepost\\.com/files/[a-z0-9]+" }, flags = { 2 })
public class FilePostCom extends PluginForHost {

    public FilePostCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filepost.com/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://filepost.com/terms/";
    }

    private static final String FILEIDREGEX = "filepost\\.com/files/(.+)";
    private static final String MAINPAGE    = "https://filepost.com/";
    private static final Object LOCK        = new Object();

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) break;
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://filepost.com/files/checker/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", sb.toString());
                String correctedBR = br.toString().replace("\\", "");
                for (DownloadLink dl : links) {
                    String fileid = new Regex(dl.getDownloadURL(), FILEIDREGEX).getMatch(0);
                    if (fileid == null) {
                        logger.warning("Filepost availablecheck is broken!");
                        return false;
                    }
                    Regex theData = new Regex(correctedBR, ";\" target=\"_blank\">http://filepost\\.com/files/" + fileid + "/([\\w\\.]+)/</a></td>nttt<td>(.*?)</td>nttt<td>ntttt<span class=\"(x|v)\">Activettt</td>");
                    String filename = theData.getMatch(0);
                    String filesize = theData.getMatch(1);
                    if (filename == null || filesize == null || ("x".equals(theData.getMatch(2)))) {
                        dl.setAvailable(false);
                        continue;
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.setName(filename);
                    dl.setDownloadSize(SizeFormatter.getSize(filesize));
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(>Your IP address is already downloading a file at the moment|>Please wait till the download completion and try again)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
        String premiumlimit = br.getRegex(">Files over (.*?) can be downloaded by premium members only").getMatch(0);
        if (premiumlimit != null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.filepostcom.only4premium", "Files over " + premiumlimit + " are only downloadable for premium users"));
        // Errorhandling in case their linkchecker lies
        if (br.containsHTML("(<title>FilePost\\.com: Download  \\- fast \\&amp; secure\\!</title>|>File not found<|>It may have been deleted by the uploader or due to the received complaint|<div class=\"file_info file_info_deleted\">)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String waittime = br.getRegex("var wait_time = \\'(\\-?\\d+)\\';").getMatch(0);
        int wait = 30;
        if (waittime != null) {
            if (waittime.contains("-")) waittime = "0";
            wait = Integer.parseInt(waittime);
        }
        sleep(wait * 1001l, downloadLink);
        br.postPage("http://filepost.com/files/get/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "code=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0) + "&pass=undefined");
        String correctedBR = br.toString().replace("\\", "");
        String dllink = new Regex(correctedBR, "\"answer\":\\{\"link\":\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = new Regex(correctedBR, "\"(http://fs\\d+\\.filepost\\.com/get_file/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies, because no multiple downloads possible when always
            // loggin in for every download
            br.setCookiesExclusive(false);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("SSL") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.postPage("https://filepost.com/general/login_form/?JsHttpRequest=" + System.currentTimeMillis() + "-xml", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&recaptcha_response_field=");
            if (br.getCookie(MAINPAGE, "u") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
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
        br.getPage(MAINPAGE);
        if (!br.containsHTML("<li>Accout type: <span>Premium</span>")) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex("<li>Used storage: <span>(.*?)</span></li>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        String filesNum = br.getRegex("<li>Active files: <span>(\\d+)</span>").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<li>Valid until: <span>(.*?)</span>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MMMM dd, yyyy", null));
            account.setValid(true);
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<button onclick=\"download_file\\(\\'(http://.*?)\\'\\)").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://fs\\d+\\.filepost\\.com/get_file/.*?)\\'").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}