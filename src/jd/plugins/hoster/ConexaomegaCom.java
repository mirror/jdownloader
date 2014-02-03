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

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "conexaomega.com" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" }, flags = { 2 })
public class ConexaomegaCom extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static Object                                  LOCK               = new Object();
    private static final String                            COOKIE_HOST        = "http://conexaomega.com";

    /**
     * Important notes: 1.conexaomega.com and conexaomega.com.r are two DIFFERENT hosts! 2.Download never worked for me, I always got server
     * errors, limits are also untested
     */

    public ConexaomegaCom(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(1 * 1000l);
        this.enablePremium("http://www.conexaomega.com/planos");
    }

    @Override
    public String getAGBLink() {
        return "http://www.conexaomega.com/";
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        return 20;
    }

    @SuppressWarnings("unchecked")
    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load cookies */
                br.setCustomCharset("utf-8");
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return true;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.conexaomega.com/login");
                br.postPage("http://www.conexaomega.com/login", "email=" + Encoding.urlEncode(account.getUser()) + "&senha=" + Encoding.urlEncode(account.getPass()) + "&remember=1&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100));
                if (br.getCookie(COOKIE_HOST, "cm_auth") == null) return false;
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return true;
            } catch (final Exception e) {
                account.setProperty("cookies", Property.NULL);
                return false;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        if (!login(account, true)) {
            account.setValid(false);

            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        ac.setProperty("multiHostSupport", Property.NULL);
        // check if account is valid
        br.getPage("http://www.conexaomega.com/gerador");
        final String expireDays = br.getRegex(">Seu plano expira em (\\d+) dias\\.</strong>").getMatch(0);
        if (expireDays == null) {
            account.setValid(false);

            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nAccount invalid!\r\nAccount ungültig!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }

        ac.setUnlimitedTraffic();

        // now let's get a list of all supported hosts:
        br.getPage("http://www.conexaomega.com/");
        ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[][] hostsList = { { "Uploaded", "uploaded.net" }, { "Jumbofiles", "jumbofiles.org" }, { "i-FileZ", "ifilez.co" }, { "DepFile", "depfile.com" }, { "SendSpace", "sendspace.com" }, { "Bayfiles", "bayfiles.net" }, { "FileJungle", "filejungle.com" }, { "VideoBB", "videobb.com" }, { "FilePost", "filepost.com" }, { "FileFactory", "filefactory.com" }, { "Uploading", "uploading.com" }, { "Bitshare", "bitshare.com" }, { "Rapidshare", "rapidshare.com" }, { "FreakShare", "freakshare.net" }, { "4shared", "4shared.com" }, { "Mediafire", "mediafire.com" }, { "Crocko", "crocko.com" }, { "Netload", "netload.in" }, { "Extabit", "extabit.com" }, { "RapdiGator", "rapidgator.net" } };
        for (final String[] hostSet : hostsList) {
            if (br.containsHTML(hostSet[0] + ": Disponível")) {
                supportedHosts.add(hostSet[1]);
            }
        }
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

        if (supportedHosts.size() == 0) {
            ac.setStatus("Account valid: 0 Hosts via conexaomega.com available");
        } else {
            ac.setStatus("Account valid: " + supportedHosts.size() + " Hosts via conexaomega.com available");
            ac.setProperty("multiHostSupport", supportedHosts);
        }
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    /** no override to keep plugin compatible to old stable */
    public void handleMultiHost(final DownloadLink link, final Account acc) throws Exception {
        login(acc, false);
        final String url = Encoding.urlEncode(link.getDownloadURL());
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        showMessage(link, "Generating downloadlink...");
        br.getPage("http://www.conexaomega.com/_gerar?link=" + url + "&rnd=" + System.currentTimeMillis());
        final String dllink = br.getRegex("\"(http://cdn\\.conexaomega\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Erro \\d+")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: " + br.toString().trim(), 30 * 60 * 1000l); }
            logger.info("Unhandled download error on conexaomega.com: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void tempUnavailableHoster(final Account account, final DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) {
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

    private void showMessage(final DownloadLink link, final String message) {
        link.getLinkStatus().setStatusText(message);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}