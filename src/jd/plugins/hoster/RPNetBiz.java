//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.simplejson.JSonArray;
import org.appwork.storage.simplejson.JSonFactory;
import org.appwork.storage.simplejson.JSonNode;
import org.appwork.storage.simplejson.JSonObject;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rpnet.biz" }, urls = { "http://(www\\.)?dl[^\\.]*.rpnet\\.biz/download/.*/([^/\\s]+)?" }, flags = { 0 })
public class RPNetBiz extends PluginForHost {

    private static final String mName    = "rpnet.biz";
    private static final String mProt    = "http://";
    private static final String mPremium = "https://premium.rpnet.biz/";
    private static Object       LOCK     = new Object();

    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mProt + mName + "/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("http://www.", "http://"));
    }

    @Override
    public String getAGBLink() {
        return mPremium + "tos.php";
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.getHeaders().put("User-Agent", "JDOWNLOADER");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // tested with 20 seems fine.
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception, PluginException {
        handleDL(link, link.getDownloadURL());
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        URLConnectionAdapter con = dl.getConnection();
        List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
        if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
            if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            link.setName(getFileNameFromHeader(con));
            link.setDownloadSize(con.getLongContentLength());
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
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
        br.getPage(mPremium + "usercp.php");
        String expirationDate = br.getRegex("Your premium account will expire in: <u>.*</u> \\(([^\\)]*)\\)").getMatch(0);
        account.setValid(true);
        account.setConcurrentUsePossible(true);
        account.setMaxSimultanDownloads(-1);
        expirationDate = expirationDate.replaceFirst("1st", "1").replaceAll("nd", "").replaceAll("rd", "").replaceAll("th", "");
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expirationDate, "dd MMM, yyyy", null));

        // get the supported hosts
        String hosts = br.getPage(mPremium + "hostlist.php");
        if (hosts != null) {
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts.split(",")));
            ai.setProperty("multiHostSupport", supportedHosts);
            ai.setStatus("Premium User - " + supportedHosts.size() + " hosts available!");
        } else {
            ai.setStatus("Premium User - 0 Hosts available!");
        }
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        // Temporary workaround for bitshare. Can be removed when rpnet accepts bitshare shorthand links.
        String downloadURL = null;
        if (link.getDownloadURL().contains("bitshare.com/?f=")) {
            Browser newBr = new Browser();
            newBr.getPage(link.getDownloadURL());
            String rex = newBr.getRegex("Download:</td>[^\"]*<td><input type=\"text\" value=\"([^\"]+)\"").getMatch(0);
            if (rex == null) {
                logger.warning("Could not find 'rex'");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadURL = rex;
        } else {
            downloadURL = link.getDownloadURL();
        }
        // end of workaround
        showMessage(link, "Generating Link");
        /* request Download */
        prepBrowser();
        String apiDownloadLink = mPremium + "client_api.php?username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&action=generate&links=" + Encoding.urlEncode(downloadURL);
        br.getPage(apiDownloadLink);
        JSonObject node = (JSonObject) new JSonFactory(br.toString().replaceAll("\\\\/", "/")).parse();
        JSonArray links = (JSonArray) node.get("links");

        // for now there is only one generated link per api call, could be changed in the future, therefore iterate anyway
        for (JSonNode linkNode : links) {
            JSonObject linkObj = (JSonObject) linkNode;
            JSonNode errorNode = linkObj.get("error");
            if (errorNode != null) {
                // shows a more detailed error message returned by the API, especially if the DL Limit is reached for a host
                String msg = errorNode.toString();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg);
            }

            // Only ID given? => request the download from rpnet hdd
            JSonNode idNode = linkObj.get("id");
            String generatedLink = null;
            if (idNode != null) {
                String id = idNode.toString();

                int progress = 0;
                int tryNumber = 0;

                while (tryNumber <= 30) {
                    br.getPage(mPremium + "client_api.php?username=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&action=downloadInformation&id=" + Encoding.urlEncode(id));
                    JSonObject node2 = (JSonObject) new JSonFactory(br.toString().replaceAll("\\\\/", "/")).parse();
                    JSonObject downloadNode = (JSonObject) node2.get("download");
                    String tmp = downloadNode.get("status").toString();
                    progress = Integer.parseInt(tmp.substring(1, tmp.length() - 1));

                    showMessage(link, "Waiting for upload to rpnet HDD - " + progress + "%");

                    // download complete?
                    if (progress == 100) {
                        String tmp2 = downloadNode.get("rpnet_link").toString();
                        generatedLink = tmp2.substring(1, tmp2.length() - 1);
                        break;
                    }

                    Thread.sleep(10000);
                    tryNumber++;
                }
            } else {
                String tmp = ((JSonObject) linkNode).get("generated").toString();
                generatedLink = tmp.substring(1, tmp.length() - 1);
            }
            // download the file
            if (generatedLink == null || generatedLink.isEmpty()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            showMessage(link, "Download begins!");

            try {
                handleDL(link, generatedLink);
                return;
            } catch (PluginException e1) {
            }
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Link generation failed.");
    }

    private void handleDL(DownloadLink link, String dllink) throws Exception {
        /* we want to follow redirects in final stage */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().isContentDisposition()) {
            /* contentdisposition, lets download it */
            dl.startDownload();
            return;
        } else {
            /*
             * download is not contentdisposition, so remove this host from premiumHosts list
             */
            br.followConnection();
        }

        /* temp disabled the host */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCookiesExclusive(true);
                prepBrowser();
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(mPremium, key, value);
                        }
                        return;
                    }
                }
                URLConnectionAdapter postback = br.openPostConnection(mPremium + "login.php", "username=" + account.getUser() + "&password=" + account.getPass() + "&login=");
                if (postback.getResponseCode() != 302) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(mPremium);
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
}