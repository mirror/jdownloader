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
//

package jd.plugins.hoster;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hotfile.com" }, urls = { "http://[\\w\\.]*?hotfile\\.com/dl/\\d+/[0-9a-zA-Z]+/(.*?/|.+)" }, flags = { 2 })
public class HotFileCom extends PluginForHost {
    private String ua = RandomUserAgent.generate();

    public HotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hotfile.com/register.html?reff=274657");
        setConfigElements();
    }

    private static final String UNLIMITEDMAXCON = "UNLIMITEDMAXCON";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), UNLIMITEDMAXCON, JDL.L("plugins.hoster.HotFileCom.SetUnlimitedConnectionsForPremium", "Allow more than 5 connections per file for premium (default maximum = 5). Enabling this can cause errors!!")).setDefaultValue(false);
        config.addEntry(cond);
    }

    @Override
    public String getAGBLink() {
        return "http://hotfile.com/terms-of-service.html";
    }

    private HashMap<String, String> callAPI(Browser br, String action, Account account, HashMap<String, String> addParams) throws Exception {
        if (action == null || action.length() == 0) return null;
        Browser tbr = br;
        if (tbr == null) {
            tbr = new Browser();
        }
        tbr.setDebug(true);
        tbr.getPage("http://api.hotfile.com");
        Form form = new Form();
        form.setAction("/");
        form.setMethod(MethodType.POST);
        form.put("action", action);
        if (account != null) {
            String pwMD5 = JDHash.getMD5(account.getPass());
            form.put("username", Encoding.urlEncode(account.getUser()));
            form.put("passwordmd5", pwMD5);
        }
        if (addParams != null) {
            for (String param : addParams.keySet()) {
                form.put(param, addParams.get(param));
            }
        }
        tbr.submitForm(form);
        HashMap<String, String> ret = new HashMap<String, String>();
        ret.put("httpresponse", tbr.toString());
        String vars[][] = tbr.getRegex("(.*?)=(.*?)(&|$)").getMatches();
        for (String var[] : vars) {
            ret.put(var[0] != null ? var[0].trim() : null, var[1]);
        }
        return ret;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (account.getUser().trim().equalsIgnoreCase("cookie")) {
            account.setValid(false);
            ai.setStatus("Cookie login no longer possible! API does not support it!");
            return ai;
        }
        HashMap<String, String> info = callAPI(null, "getuserinfo", account, null);
        if (!info.containsKey("is_premium") || !"1".equalsIgnoreCase(info.get("is_premium"))) {
            account.setValid(false);
            if (info.get("httpresponse").contains("invalid username")) {
                ai.setStatus("invalid username or password");
            } else {
                ai.setStatus("No Premium Account");
            }
            return ai;
        }
        String validUntil = info.get("premium_until");
        if (validUntil == null) {
            account.setValid(false);
        } else {
            account.setValid(true);
            validUntil = validUntil.replaceAll(":|T", "");
            validUntil = validUntil.replaceFirst("-", "");
            validUntil = validUntil.replaceFirst("-", "");
            ai.setValidUntil(Regex.getMilliSeconds(validUntil, "yyyyMMddHHmmssZ", null));
            ai.setStatus("Premium");
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("link", Encoding.urlEncode(downloadLink.getDownloadURL() + "\n\r"));
        HashMap<String, String> info = callAPI(null, "getdirectdownloadlink", account, params);
        if (info.get("httpresponse").contains("file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (info.get("httpresponse").contains("premium required")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        String finalUrl = info.get("httpresponse").trim();
        if (finalUrl == null || !finalUrl.startsWith("http://")) {
            logger.severe(finalUrl);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalUrl, true, getPluginConfig().getBooleanProperty(UNLIMITEDMAXCON, false) == true ? 0 : -5);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Invalid link")) {
                String newLink = br.getRegex("href=\"(http://.*?)\"").getMatch(0);
                if (newLink != null) {
                    /* set new downloadlink */
                    logger.warning("invalid link -> use new link");
                    downloadLink.setUrlDownload(newLink.trim());
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            logger.severe(finalUrl);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* filename workaround , MAYBE no longer needed because of api */
        String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
        urlFileName = Encoding.htmlDecode(urlFileName);
        downloadLink.setFinalFileName(urlFileName);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        /* workaround as server does not send correct encoding information */
        br.setCustomCharset("UTF-8");
        br.setCookie("http://hotfile.com", "lang", "en");
        br.getHeaders().put("User-Agent", ua);
        br.getPage(parameter.getDownloadURL());
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String filename = br.getRegex("Downloading <b>(.+?)</b>").getMatch(0);
        if (filename == null) {
            /* polish users get this */
            filename = br.getRegex("Downloading:</strong>(.*?)<").getMatch(0);
        }
        String filesize = br.getRegex("<span class=\"size\">(.*?)</span>").getMatch(0);
        if (filesize == null) {
            /* polish users get this */
            filesize = br.getRegex("Downloading:</strong>.*?span.*?strong>(.*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        /*
         * for free users we dont use api filecheck, cause we have to call
         * website anyway
         */
        requestFileInformation(link);
        br.setDebug(true);
        if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
        if (br.containsHTML("starthtimer\\(\\)")) {
            String waittime = br.getRegex("starthtimer\\(\\).*?timerend=.*?\\+(\\d+);").getMatch(0);
            if (Long.parseLong(waittime.trim()) > 0) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Long.parseLong(waittime.trim())); }
        }
        Form[] forms = br.getForms();
        Form form = forms[1];
        long sleeptime = 0;
        try {
            sleeptime = Long.parseLong(br.getRegex("timerend=d\\.getTime\\(\\)\\+(\\d+);").getMatch(0)) + 1;
            // for debugging purposes
            logger.info("Regexed waittime is " + sleeptime + " seconds");
        } catch (Exception e) {
            logger.info("WaittimeRegex broken");
            logger.info(br.toString());
            sleeptime = 60 * 1000l;
        }
        // Reconnect if the waittime is too big!
        if (sleeptime > 100 * 1000l) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sleeptime);
        this.sleep(sleeptime, link);
        br.submitForm(form);
        // captcha
        if (!br.containsHTML("Click here to download")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, link);
            rc.setCode(c);
            if (!br.containsHTML("Click here to download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dl_url = br.getRegex("<h3 style='margin-top: 20px'><a href=\"(.*?)\">Click here to download</a>").getMatch(0);
        if (dl_url == null) dl_url = br.getRegex("table id=\"download_file\".*?<a href=\"(.*?)\"").getMatch(0);/* polish */
        if (dl_url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dl_url, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Invalid link")) {
                String newLink = br.getRegex("href=\"(http://.*?)\"").getMatch(0);
                if (newLink != null) {
                    /* set new downloadlink */
                    logger.warning("invalid link -> use new link");
                    link.setUrlDownload(newLink.trim());
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
            if (br.containsHTML("You are currently downloading")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* filename workaround */
        String urlFileName = Plugin.getFileNameFromURL(new URL(br.getURL()));
        urlFileName = Encoding.htmlDecode(urlFileName);
        link.setFinalFileName(urlFileName);
        dl.startDownload();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("pl.hotfile.com", "hotfile.com"));
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            StringBuilder sbIDS = new StringBuilder();
            StringBuilder sbKEYS = new StringBuilder();
            HashMap<String, String> params = new HashMap<String, String>();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                params.clear();
                while (true) {
                    if (index == urls.length || links.size() > 25) break;
                    links.add(urls[index]);
                    index++;
                }
                sbIDS.delete(0, sbIDS.capacity());
                sbKEYS.delete(0, sbKEYS.capacity());
                sbIDS.append("");
                sbKEYS.append("");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) {
                        sbIDS.append(",");
                        sbKEYS.append(",");
                    }
                    String id = new Regex(dl.getDownloadURL(), "/dl/(\\d+)/").getMatch(0);
                    String key = new Regex(dl.getDownloadURL(), "/dl/\\d+/([0-9a-zA-Z]+)").getMatch(0);
                    sbIDS.append(id);
                    sbKEYS.append(key);
                    c++;
                }
                /* we want id,status,name, size , md5 and sha1 info */
                params.put("fields", "id,status,name,size,md5,sha1");
                params.put("ids", sbIDS.toString());
                params.put("keys", sbKEYS.toString());
                HashMap<String, String> info = callAPI(null, "checklinks", null, params);
                String response = info.get("httpresponse");
                for (DownloadLink dl : links) {
                    String id = new Regex(dl.getDownloadURL(), "/dl/(\\d+)").getMatch(0);
                    String[] dat = new Regex(response, id + ",(\\d+),(.*?),(\\d+),(.*?),(.*?)(\n|$)").getRow(0);
                    if (dat != null) {
                        dl.setName(dat[1]);
                        dl.setDownloadSize(Long.parseLong(dat[2]));
                        dl.setMD5Hash(dat[3]);
                        // SHA1 hashes seems to be wrong sometimes
                        // dl.setSha1Hash(dat[4]);
                        if ("1".equalsIgnoreCase(dat[0])) {
                            dl.setAvailable(true);
                        } else if ("0".equalsIgnoreCase(dat[0])) {
                            dl.setAvailable(false);
                        } else if ("2".equalsIgnoreCase(dat[0])) {
                            dl.setAvailable(true);
                            dl.getLinkStatus().setStatusText("HotLink");
                        } else {
                            dl.setAvailable(false);
                            dl.getLinkStatus().setStatusText("Unknown FileStatus " + dat[0]);
                        }
                    } else {
                        dl.setAvailable(false);
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
