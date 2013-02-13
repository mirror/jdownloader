//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vip-file.com" }, urls = { "http://(u\\d+\\.)?vip\\-file\\.com/download(lib)?/[^<>\"/]*?/[^<>\"/]*?\\.html" }, flags = { 2 })
public class Vipfilecom extends PluginForHost {

    public static final String  FREELINKREGEX = "\"(http://vip\\-file\\.com/download([0-9]+)/.*?)\"";
    private static final String APIKEY        = "VjR1U3JGUkNx";
    private static final String APIPAGE       = "http://api.letitbit.net/";
    private static final String FILEOFFLINE   = "(This file not found|\">File not found)";
    private static Object       LOCK          = new Object();

    public Vipfilecom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        enablePremium("http://vip-file.com/tmpl/premium_en.php");
    }

    @Override
    public String getAGBLink() {
        return "http://vip-file.com/page/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /**
     * Important: Always sync this code with the vip-file.com, shareflare.net
     * and letitbit.net plugins Limits: 20 * 50 = 1000 links per minute
     * */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /*
                     * we test 50 links at once (probably we could check even
                     * more)
                     */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("r=" + Encoding.urlEncode("[\"" + Encoding.Base64Decode(APIKEY) + "\""));
                for (final DownloadLink dl : links) {
                    sb.append(",[\"download/info\",{\"link\":\"" + dl.getDownloadURL() + "\"}]");
                }
                sb.append("]");
                br.setReadTimeout(2 * 60 * 60);
                br.setConnectTimeout(2 * 60 * 60);
                br.postPage(APIPAGE, sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    final Regex fInfo = br.getRegex("\"name\":\"([^<>\"]*?)\",\"size\":\"(\\d+)\",\"uid\":\"" + fid + "\",\"project\":\"(letitbit\\.net|shareflare\\.net|vip\\-file\\.com)\",\"md5\":\"([a-z0-9]{32}|0)\"");
                    if (br.containsHTML("\"data\":\\[\\[\\]\\]")) {
                        dllink.setAvailable(false);
                    } else {
                        final String md5 = fInfo.getMatch(3);
                        dllink.setFinalFileName(Encoding.htmlDecode(fInfo.getMatch(0)));
                        dllink.setDownloadSize(Long.parseLong(fInfo.getMatch(1)));
                        dllink.setAvailable(true);
                        if (!md5.equals("0")) dllink.setMD5Hash(md5);
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) { return AvailableStatus.UNCHECKED; }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/(\\d+\\-)?([^<>\"/]*?)/[^<>\"/]*?\\.html").getMatch(1);
    }

    // private AvailableStatus oldAvailableCheck(final DownloadLink
    // downloadLink) throws IOException, PluginException {
    // br.setFollowRedirects(true);
    // br.getPage(downloadLink.getDownloadURL());
    // br.setFollowRedirects(false);
    // if (br.containsHTML(FILEOFFLINE)) throw new
    // PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // String fileSize =
    // br.getRegex("name=\"sssize\" value=\"(.*?)\"").getMatch(0);
    // if (fileSize == null) fileSize =
    // br.getRegex("<p>Size of file: <span>(.*?)</span>").getMatch(0);
    // String fileName =
    // br.getRegex("<input type=\"hidden\" name=\"realname\" value=\"(.*?)\" />").getMatch(0);
    // if (fileSize == null || fileName == null) throw new
    // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
    // downloadLink.setName(fileName);
    // String link =
    // Encoding.htmlDecode(br.getRegex(Pattern.compile(FREELINKREGEX,
    // Pattern.CASE_INSENSITIVE)).getMatch(0));
    // if (link == null) {
    // downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink",
    // "No free download link for this file"));
    // return AvailableStatus.TRUE;
    // }
    // return AvailableStatus.TRUE;
    // }

    private String getJson(final String parameter) {
        return br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String link = getLinkViaSkymonkDownloadMethod(downloadLink.getDownloadURL());
        boolean skymonk = link == null ? false : true;
        if (link == null) {
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            br.setFollowRedirects(false);
            if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            /* DownloadLink holen, 2x der Location folgen */
            /* we have to wait little because server too buggy */
            sleep(2000, downloadLink);
            link = Encoding.htmlDecode(br.getRegex(Pattern.compile(FREELINKREGEX, Pattern.CASE_INSENSITIVE)).getMatch(0));
            if (link == null) {
                try {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } catch (final Throwable e) {
                    if (e instanceof PluginException) throw (PluginException) e;
                }
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));
            }
            br.setDebug(true);
            /* SpeedHack */
            br.setFollowRedirects(false);
            br.getPage(link);
            link = br.getRedirectLocation();
        }
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (!skymonk) {
            if (!link.contains("vip-file.com")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.vipfilecom.errors.nofreedownloadlink", "No free download link for this file"));
        }
        // link = link.replaceAll("file.com.*?/", "file.com:8080/");
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("Finallink doesn't lead to a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getLinkViaSkymonkDownloadMethod(String s) throws IOException {
        Browser skymonk = new Browser();
        skymonk.setCustomCharset("UTF-8");
        skymonk.getHeaders().put("Pragma", null);
        skymonk.getHeaders().put("Cache-Control", null);
        skymonk.getHeaders().put("Accept-Charset", null);
        skymonk.getHeaders().put("Accept-Encoding", null);
        skymonk.getHeaders().put("Accept", null);
        skymonk.getHeaders().put("Accept-Language", null);
        skymonk.getHeaders().put("User-Agent", null);
        skymonk.getHeaders().put("Referer", null);
        skymonk.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");

        int rd = (int) Math.random() * 6 + 1;
        skymonk.postPage("http://api.letitbit.net/internal/index4.php", "action=LINK_GET_DIRECT&link=" + s + "&free_link=1&sh=" + JDHash.getMD5(String.valueOf(Math.random())) + rd + "&sp=" + (49 + rd) + "&appid=" + JDHash.getMD5(String.valueOf(Math.random())) + "&version=2.12");
        String[] result = skymonk.getRegex("([^\r\n]+)").getColumn(0);
        if (result == null || result.length == 0) return null;

        if ("NO".equals(result[0].trim())) {
            if (result.length > 1) {
                if ("activation".equals(result[1].trim())) {
                    logger.warning("SkyMonk activation not completed!");
                }
            }
        }

        ArrayList<String> res = new ArrayList<String>();
        for (String r : result) {
            if (r.startsWith("http")) {
                res.add(r);
            }
        }
        if (res.size() > 1) return res.get(1);
        return res.size() == 1 ? res.get(0) : null;
    }

    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            // Reset stuff because it can only be checked while downloading ;
            ai.setStatus("Status can only be checked while downloading!");
            account.setValid(true);
            return ai;
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        br.postPage(APIPAGE, "r=[\"" + Encoding.Base64Decode(APIKEY) + "\",[\"download/direct_links\",{\"link\":\"" + downloadLink.getDownloadURL() + "\",\"pass\":\"" + account.getPass() + "\"}]]");
        if (br.containsHTML("data\":\"bad password\"")) {
            logger.info("Wrong password, disabling the account!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (br.containsHTML("\"data\":\"no mirrors\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("\"data\":\"file is not found\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dlUrl = br.getRegex("\"(http:[^<>\"]*?)\"").getMatch(0);
        if (dlUrl != null)
            dlUrl = dlUrl.replace("\\", "");
        else
            dlUrl = handleOldPremiumPassWay(account, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("Finallink doesn't lead to a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
        logger.info("no working link found");
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    // NOTE: Old, tested 15.11.12, works!
    private String handleOldPremiumPassWay(final Account account, final DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.containsHTML(FILEOFFLINE)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Form[] allForms = br.getForms();
        if (allForms == null || allForms.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form premiumform = null;
        for (Form singleForm : allForms) {
            if (singleForm.containsHTML("pass") && singleForm.containsHTML("sms/check2.php")) {
                premiumform = singleForm;
                break;
            }
        }
        if (premiumform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        premiumform.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(premiumform);
        // Try to find the remaining traffic, 1 Point = 1 GB
        String trafficLeft = br.getRegex("\">Points:</acronym> ([0-9\\.]+)</li>").getMatch(0);
        if (trafficLeft != null && !trafficLeft.equals("")) {
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) ai = new AccountInfo();
            ai.setTrafficLeft(SizeFormatter.getSize(trafficLeft + "GB"));
            ai.setStatus("Premium User");
            account.setAccountInfo(ai);
        }
        String expireDate = br.getRegex(">Period of validity:</acronym> (.*?) \\[<acronym").getMatch(0);
        if (expireDate != null) {
            AccountInfo ai = account.getAccountInfo();
            if (ai == null) ai = new AccountInfo();
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd", null));
            ai.setStatus("Premium User");
            account.setAccountInfo(ai);
            if (ai.isExpired()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        }
        String urls[] = br.getRegex(Pattern.compile("title=\"Link to the file download\" href=\"(http://[^<>\"\\']+)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
        if (urls == null) {
            urls = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/f/[a-z0-9]+/[^<>\"\\'/]+)\"").getColumn(0);
        }
        if (urls == null && br.containsHTML("(Wrong password|>This password expired<)")) {
            logger.info("Downloadpassword seems to be wrong, disabeling account now!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (urls == null) {
            if (br.containsHTML("(Your premium access is about to be over|Amount of Your points is close to zero\\.)")) {
                logger.info("Password is wrong!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return urls[urls.length - 1];

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}