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

package jd.plugins.host;

import java.io.File;
import java.net.URI;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Encoding;
import jd.http.GetRequest;
import jd.http.PostRequest;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

public class RapidShareDe extends PluginForHost {

    public RapidShareDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapidshare.de/en/premium.html");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }

        LinkStatus linkStatus = downloadLink.getLinkStatus();

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setFollowRedirects(false);
        Form[] forms = br.getForms(downloadLink.getDownloadURL());
        if (forms.length < 2) {
            logger.severe("konnte den Download nicht finden");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        Form form = forms[1];
        form.remove("dl.start");
        form.put("dl.start", "Free");
        br.submitForm(form);

        long waittime;
        try {
            waittime = Long.parseLong(new Regex(br, "<script>var.*?\\= ([\\d]+)").getMatch(0)) * 1000;

            this.sleep((int) waittime, downloadLink);
        } catch (Exception e) {
            try {
                waittime = Long.parseLong(new Regex(br, "Oder warte (\\d*?) Minute").getMatch(0)) * 60000;
                linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                linkStatus.setValue(waittime);
                return;
            } catch (Exception es) {
                logger.severe("kann wartezeit nicht setzen");
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage("Waittime could not be set");
                return;
            }
        }

        String ticketCode = Encoding.htmlDecode(new Regex(br, "unescape\\(\\'(.*?)\\'\\)").getMatch(0));

        form = Form.getForms(ticketCode)[0];
        File captchaFile = Plugin.getLocalCaptchaFile(this, ".png");
        String captchaAdress = new Regex(ticketCode, "<img src=\"(.*?)\">").getMatch(0);
        logger.info("CaptchaAdress:" + captchaAdress);
        br.getDownload(captchaFile, captchaAdress);
        if (!captchaFile.exists() || captchaFile.length() == 0) {
            logger.severe("Captcha not found");
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        String code = null;

        code = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
        form.put("captcha", code);

        br.openDownload(downloadLink, form).startDownload();

        File l = new File(downloadLink.getFileOutput());
        if (l.length() < 10240) {
            String local = JDIO.getLocalFile(l);
            if (Regex.matches(local, "Zugriffscode falsch")) {
                l.delete();
                l.deleteOnExit();
                linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                return;
            }

        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }

        String user = account.getUser();
        String pass = account.getPass();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setFollowRedirects(false);
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String formatPass = "";
        for (int i = 0; i < pass.length(); i++) {
            formatPass += "%" + Integer.toString(pass.charAt(i), 16);
        }

        String path = new URI(downloadLink.getDownloadURL()).getPath();
        PostRequest r = new PostRequest("http://rapidshare.de");
        r.setPostVariable("uri", Encoding.urlEncode(path));
        r.setPostVariable("dl.start", "PREMIUM");
        r.getCookies().add(new Cookie(getHost(), "user", user + "-" + formatPass));

        String page = r.load();
        if (page.contains("Premium-Cookie nicht gefunden")) {
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            linkStatus.setErrorMessage("Account not found or password wrong");
            return;

        }
        String error = new Regex(page, "alert\\(\"(.*)\"\\)<\\/script>").getMatch(0);
        if (error != null) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(JDLocale.L("plugins.host.rapidshareDE.errors." + JDHash.getMD5(error), error));
            return;

        }
        String url = new Regex(page, "\\:<\\/b> <a href\\=\"([^\"].*)\">.*?.rapidshare.de").getMatch(0);

        URLConnectionAdapter urlConnection;
        GetRequest req = new GetRequest(url);
        r.getCookies().add(new Cookie(getHost(), "user", user + "-" + formatPass));
        dl = RAFDownload.download(downloadLink, req, true, 0);
        dl.connect(br);
        urlConnection = req.getHttpConnection();
        if (urlConnection.getHeaderField("content-disposition") == null) {
            page = req.read();
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(page);

        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://rapidshare.de/de/faq.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return false;
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            Form[] forms = br.getForms();
            if (forms.length < 2) { return false; }

            br.submitForm(forms[1]);

            String[][] regExp = new Regex(br, "<p>Du hast die Datei <b>(.*?)</b> \\(([\\d]+)").getMatches();
            downloadLink.setDownloadSize(Integer.parseInt(regExp[0][1]) * 1024);
            downloadLink.setName(regExp[0][0]);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
