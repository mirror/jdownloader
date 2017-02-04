//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vvvvid.it" }, urls = { "http://vvvviddecrypted\\.it/\\d+" })
public class VvvvidIt extends PluginForHost {

    public VvvvidIt(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.vvvvid.it/#register");
    }

    @Override
    public String getAGBLink() {
        return "http://vvvvid.it/";
    }

    /* Example hls master: http://vvvvid-vh.akamaihd.net/i/Dynit/KekkaiSensen/KekkaiSensen_Ep01m.mp4/master.m3u8 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = link.getStringProperty("filename", null);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* 2016-08-02: psp: stream_master string is crypted. We need to decode this to get the plugin working again! */
        String stream_master = downloadLink.getStringProperty("directlink", null);
        if (stream_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (stream_master.contains(".f4m")) {
            /* HDS */
            /*
             * E.g.
             * 'http://vvvvid-vh.akamaihd.net/z/Dynit/Durarara/DurararaX2_Ep01_H26phJw2RNlnBURem.mp4/manifest.f4m?g=WBTXLPEAWKUL&hdcore=3.6.0&plugin=aasp-3.6.0.50.41'
             */
            stream_master += "?hdcore=3.6.0&plugin=aasp-3.6.0.50.41";
            br.getPage(stream_master);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                /** TODO: This is missing: */
                /*
                 * https://www.vvvvid.it/kenc?action=kt&conn_id=xoNcWLuE8Y18&url=http%3A%2F%2Fvvvvid-vh.akamaihd.net%2Fz%2FDynit2%2FJojo%2F
                 * JojoDiamond_S04Ep03_NCULxv3jGQXwPyIhm.mp4%2Fmanifest.f4m
                 */
                /*
                 * http://vvvvid-vh.akamaihd.net/z/Dynit2/Jojo/JojoDiamond_S04Ep03_NCULxv3jGQXwPyIhm.mp4/manifest.f4m?hdnts=exp=1462316725~acl
                 * =/z/Dynit2/Jojo/JojoDiamond_S04Ep03_NCULxv3jGQXwPyIhm.mp4/*~hmac=4d264
                 * a58f22fa5277f15676afd27731484d42cc42b8c5d1e82e89931a86801a4&g=TSVEFKWLFPKQ&hdcore=3.6.0&plugin=aasp-3.6.0.50.41
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 30 * 60 * 1000l);
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String finallink = null;
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
            NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
            Node media;
            for (int j = 0; j < mediaUrls.getLength(); j++) {
                media = mediaUrls.item(j);

                String temp = getAttByNamedItem(media, "url");
                if (temp != null) {
                    finallink = temp;
                    break;
                }
            }
            dl = new HDSDownloader(downloadLink, br, finallink);
            dl.startDownload();
        } else {
            /* HLS */
            if (!stream_master.startsWith("http")) {
                /* Workaround for very old content e.g. http://www.vvvvid.it/#!show/52/gto-great-teacher-onizuka/50/392657/lesson-1 */
                /*
                 * Thanks:
                 * http://andrealazzarotto.com/2014/02/22/scaricare-i-contenuti-audio-e-video-presenti-nelle-pagine-web/comment-page-7/
                 */
                /* http://wowzaondemand.top-ix.org/videomg/_definst_/mp4:Dynit/GTO/GTO_Ep01.mp4/playlist.m3u8 */
                stream_master = "http://wowzaondemand.top-ix.org/videomg/_definst_/mp4:" + stream_master + "/playlist.m3u8";
            } else {
                /* No need to change our hls urls */
            }
            this.br.getPage(stream_master);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked");
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        }
    }

    /**
     * lets try and prevent possible NPE from killing the progress.
     *
     * @author raztoki
     * @param n
     * @param item
     * @return
     */
    private String getAttByNamedItem(final Node n, final String item) {
        final String t = n.getAttributes().getNamedItem(item).getTextContent();
        return (t != null ? t.trim() : null);
    }

    private static Object LOCK = new Object();

    public static void login(Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                jd.plugins.decrypter.VvvvidIt.prepBR(br);
                String conn_id = getConnIDFromAccount(account);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && conn_id != null) {
                    br.setCookies(account.getHoster(), cookies);
                    jd.plugins.decrypter.VvvvidIt.getConnID(br);
                    if (PluginJSonUtils.getJsonValue(br, "id") != null) {
                        account.saveCookies(br.getCookies(account.getHoster()), "");
                        return;
                    }
                    br = jd.plugins.decrypter.VvvvidIt.prepBR(new Browser());
                }
                conn_id = jd.plugins.decrypter.VvvvidIt.getConnID(br);
                if (conn_id == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String postdata = "{\"data\":{},\"action\":\"login\",\"email\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\",\"facebookParams\":\"\",\"mobile\":false,\"hls\":true,\"flash\":true,\"isIframe\":false,\"login_type\":\"force\",\"reminder\":true,\"conn_id\":\"" + conn_id + "\"}";
                br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                br.getHeaders().put("Content-Type", "application/json");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getHeaders().put("Referer", "https://www." + account.getHoster() + "/");
                br.postPageRaw("https://www." + account.getHoster() + "/user/login", postdata);
                if (br.getCookie(account.getHoster(), "reminder") == null || PluginJSonUtils.getJsonValue(br, "id") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.setProperty("conn_id", conn_id);
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        doFree(link);
    }

    public static String getConnIDFromAccount(final Account acc) {
        return acc.getStringProperty("conn_id", null);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

}