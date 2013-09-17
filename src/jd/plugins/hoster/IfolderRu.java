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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rusfolder.ru", "ifolder.ru" }, urls = { "http://([a-z0-9\\.\\-]*?\\.)?((daoifolder|yapapka|rusfolder|ifolder)\\.(net|ru|com)|files\\.metalarea\\.org)/(files/)?\\d+", "IFOLDERISNOWRUSFOLDER" }, flags = { 0, 0 })
public class IfolderRu extends PluginForHost {

    private String              ua      = RandomUserAgent.generate();

    private static final String PWTEXT  = "Введите пароль:<br";

    private static final String CAPTEXT = "/random/images/";

    public IfolderRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        String url = new Regex(link.getDownloadURL(), "https?://.*?(/.+)").getMatch(0);
        if (url != null) url = url.replaceAll("/files/", "/");
        link.setUrlDownload("http://rusfolder.ru" + url);
    }

    @Override
    public String getAGBLink() {
        return ("http://rusfolder.ru/agreement");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* too many traffic but can we download download with ad? */
        final boolean withad = br.containsHTML(">Вы можете получить этот файл, если посетите сайт наших рекламодателей");
        if (br.containsHTML("На данный момент иностранный трафик у этого файла превышает российский") && !withad) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "At the moment foreign traffic of this file is larger than Russia's");
        br.setFollowRedirects(true);
        br.setDebug(true);
        String passCode = null;
        String domain = br.getRegex("(https?://ints\\..*?\\.[a-z]{2,3})/ints/").getMatch(0);
        String watchAd = br.getRegex("http://ints\\..*?\\.[a-z]{2,3}/ints/\\?(.*?)\"").getMatch(0);
        if (watchAd != null) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ifolderru.errors.ticketwait", "Waiting for ticket"));
            watchAd = domain + "/ints/?".concat(watchAd).replace("';", "");
            br.getPage(watchAd);
            watchAd = br.getRegex("href=(http://ints\\.rusfolder.[a-z0-9]+/ints/[^<>\"]*?)>").getMatch(0);
            // If they take the waittime out this part is optional
            if (watchAd != null) {
                br.getPage(watchAd);
                watchAd = br.getRegex("\"f_top\" src=\"(.*?)\"").getMatch(0);
                if (watchAd == null) {
                    logger.info("third watchad equals null, trying to get sessioncode");
                    String sessioncode = br.getRegex("session=([a-z0-9]+)\"").getMatch(0);
                    if (sessioncode == null) sessioncode = br.getRegex("type=hidden name=\"session\" value=([a-z0-9]+)>").getMatch(0);
                    if (sessioncode != null) watchAd = "http://ints.rusfolder.ru/?session=" + sessioncode;
                }
                if (watchAd == null) {
                    logger.warning("third watchad equals null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (!watchAd.startsWith("http") || !watchAd.contains(domain)) watchAd = domain + watchAd;
                br.getPage(watchAd);
                /* Tickettime */
                String ticketTimeS = br.getRegex("delay = (\\d+)").getMatch(0);
                if (ticketTimeS == null) {
                    logger.warning("ticketTimeS equals null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                int ticketTime = Integer.parseInt(ticketTimeS) * 1000;
                this.sleep(ticketTime + 1, downloadLink);
                /* this response comes without valid http header */
                br.getPage(watchAd);
            } else {
                logger.warning("second watchad equals null");
            }
        } else {
            logger.warning("String watchad equals null");
        }
        if (!br.containsHTML(CAPTEXT)) {
            logger.warning("Browser doesn't contain the captcha-text");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (int retry = 1; retry <= 5; retry++) {
            Form captchaForm = br.getFormbyProperty("name", "form1");
            String captchaurl = br.getRegex("(/random/images/.*?)\"").getMatch(0);
            String ints_session = br.getRegex("tag\\.value = \"(.*?)\"").getMatch(0);
            if (captchaForm == null || captchaurl == null) {
                logger.warning("captchaForm or captchaurl or ints_session equals null, stopping...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            captchaForm.put("ints_session", ints_session);
            captchaForm.setAction(br.getURL());
            if (!captchaurl.startsWith("http://")) {
                String host = new Regex(br.getURL(), "(https?://.*?)/").getMatch(0);
                captchaurl = host + captchaurl;
            }
            /* Captcha */
            String captchaCode = getCaptchaCode("ifolder.ru", captchaurl, downloadLink);
            captchaForm.put("confirmed_number", captchaCode);
            /* this hoster checks content encoding */
            captchaForm.setEncoding("application/x-www-form-urlencoded");
            Regex specialStuff = br.getRegex("\"name=\\'(.*?)\\' value=\\'(.*?)\\'");
            if (specialStuff.getMatch(0) != null && specialStuff.getMatch(1) != null) {
                captchaForm.put(specialStuff.getMatch(0), specialStuff.getMatch(1));
            } else {
                logger.info("Specialstuff is null, this could cause trouble...");
            }
            /* hidden code */
            Regex hiddenCode = br.getRegex("var c = \\[\\'(.*?)\\'\\, \\'hh([a-z0-9]+?)\\'\\];");
            if (hiddenCode.getMatch(0) != null && hiddenCode.getMatch(1) != null) {
                captchaForm.put(hiddenCode.getMatch(0), hiddenCode.getMatch(1));
            } else {
                logger.info("hidden_code is null, this could cause trouble...");
            }
            try {
                br.submitForm(captchaForm);
            } catch (Exception e) {
                e.printStackTrace();
                br.submitForm(captchaForm);
            }
            if (!br.containsHTML(CAPTEXT)) break;
        }
        if (br.containsHTML(CAPTEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        /* Is the file password protected ? */
        if (br.containsHTML(PWTEXT)) {
            for (int passwordRetry = 1; passwordRetry <= 3; passwordRetry++) {
                logger.info("This file is password protected");
                final String session = br.getRegex("name=\"session\" value=\"(.*?)\"").getMatch(0);
                final String fileID = new Regex(downloadLink.getDownloadURL(), "rusfolder\\.ru/(\\d+)").getMatch(0);
                if (session == null || fileID == null) {
                    logger.warning("The string 'session' or 'fileID' equals null, throwing exception...");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                String postData = "session=" + session + "&file_id=" + fileID + "&action=1&pswd=" + passCode;
                br.postPage(br.getURL(), postData);
                if (!br.containsHTML(PWTEXT)) break;
            }
            if (br.containsHTML(PWTEXT)) {
                downloadLink.setProperty("pass", null);
                logger.info("DownloadPW wrong!");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        String directLink = br.getRegex("id=\"download_file_href\".*?href=\"(.*?)\"").getMatch(0);
        if (directLink == null) {
            logger.warning("directLink equals null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, directLink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void prepareBrowser(Browser br) {
        if (br == null) return;
        br.getHeaders().put("User-Agent", ua);
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
        br.setReadTimeout(3 * 60 * 1000);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException, InterruptedException {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            String fileID = new Regex(downloadLink.getDownloadURL(), "https?://.*?/(.+)").getMatch(0);
            if (!br.getRedirectLocation().contains(fileID)) {
                logger.warning("The redirect location doesn't contain the fileID, stopping...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Setting new downloadUrl...");
            downloadLink.setUrlDownload(br.getRedirectLocation());
            br.getPage(downloadLink.getDownloadURL());
        }
        if (br.containsHTML("<p>Файл номер <b>\\d+</b> удален !!!</p>") || br.containsHTML("<p>Файл номер <b>\\d+</b> не найден !!!</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("Название:.*?<b>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("Размер:.*?<b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (filename.contains("..")) {
            /* because of server problems check for final filename here */
            downloadLink.setName(filename);
        } else {
            downloadLink.setFinalFileName(filename);
        }
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize.replace("Мб", "Mb").replace("кб", "Kb").replace("Гб", "Gb")));
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
        return true;
    }
}