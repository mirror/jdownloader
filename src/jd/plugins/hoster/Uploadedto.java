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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploaded.to" }, urls = { "(http://[\\w\\.-]*?uploaded\\.to/.*?(file/|\\?id=|&id=)[\\w]+/?)|(http://[\\w\\.]*?ul\\.to/[\\w\\-]+/.+)|(http://[\\w\\.]*?ul\\.to/[\\w\\-]+/?)" }, flags = { 2 })
public class Uploadedto extends PluginForHost {
    private static final UploadedtoLinkObserver LINK_OBSERVER     = new UploadedtoLinkObserver();
    private static final String                 RECAPTCHA         = "/recaptcha/";
    private static final String                 PASSWORDPROTECTED = "<span>Passwort:</span>";

    static class UploadedtoLinkObserver extends Thread {

        private ArrayList<DownloadLink> list;
        private Object                  lock = new Object();

        public UploadedtoLinkObserver() {
            super("UploadedCRCObserver");
            list = new ArrayList<DownloadLink>();

            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_DO_CRC, true)) {
                start();
            }
        }

        @Override
        public void run() {
            ArrayList<DownloadLink> unregister = new ArrayList<DownloadLink>();
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (lock) {
                    for (DownloadLink link : list) {
                        LinkStatus ls = link.getLinkStatus();
                        if (ls.hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && ls.getValue() == LinkStatus.VALUE_FAILED_HASH) {
                            String pat = Pattern.quote(JDL.L("plugins.optional.jdunrar.crcerrorin", "Extract: failed (CRC in %s)"));
                            pat = pat.replace("%s", "\\E(.*?)\\Q");
                            if (pat == null) continue;
                            String rg = new Regex(ls.getErrorMessage(), pat).getMatch(0);
                            if (rg == null) continue;
                            if (rg.equalsIgnoreCase(link.getName())) {
                                collectCRCLinks(link, false);
                                unregister.add(link);
                            }
                        }
                    }
                    list.removeAll(unregister);
                    unregister.clear();
                }
            }
        }

        public void register(DownloadLink downloadLink) {
            synchronized (lock) {
                list.remove(downloadLink);
                list.add(downloadLink);
            }
        }

    }

    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploaded.to/ref?id=70683&r");
        setMaxConnections(20);
        this.setStartIntervall(2000l);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL();
        url = url.replaceFirst("http://.*?/", "http://uploaded.to/");
        url = url.replaceFirst("\\.to/.*?id=", ".to/file/");
        if (!url.contains("/file/")) {
            url = url.replaceFirst("uploaded.to/", "uploaded.to/file/");
        }
        String[] parts = url.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }
        link.setUrlDownload(newLink);
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.getPage("http://uploaded.to/login");

        Form login = br.getForm(0);
        login.put("email", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(login);
        if (br.getCookie("http://uploaded.to", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.containsHTML("Login failed!")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private boolean isPremium() {
        String accounttype = br.getMatch("Accounttype:</span> <span class=.*?>(.*?)</span>");
        if (accounttype != null && accounttype.equalsIgnoreCase("free")) { return false; }
        return true;
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
        if (!isPremium()) {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            ai.setAccountBalance((long) (Double.parseDouble(balance) * 100));
            ai.setPremiumPoints(Long.parseLong(points));
            ai.setValidUntil(System.currentTimeMillis() + (356 * 24 * 60 * 60 * 1000l));
            ai.setStatus("Accounttyp: Collectorsaccount");
        } else {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String opencredits = br.getMatch("Open credits:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            String traffic = br.getMatch("Traffic left: </span><span class=.*?>(.*?)</span> ");
            String expire = br.getMatch("Valid until: </span> <span class=.*?>(.*?)</span>");
            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd-MM-yyyy HH:mm", null));
            if (opencredits != null) {
                double b = (Double.parseDouble(balance) + Double.parseDouble(opencredits)) * 100;
                ai.setAccountBalance((long) b);
            } else {
                ai.setAccountBalance((long) (Double.parseDouble(balance) * 100));
            }
            ai.setTrafficLeft(Regex.getSize(traffic));
            ai.setTrafficMax(50 * 1024 * 1024 * 1024l);
            ai.setPremiumPoints(Long.parseLong(points));
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        login(account);
        if (!isPremium()) {
            logger.severe("Entered a Free-account");
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage(JDL.L("plugins.hoster.uploadedto.errors.notpremium", "This is free account"));
            linkStatus.setValue(PluginException.VALUE_ID_PREMIUM_DISABLE);
            return;
        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        getPassword(downloadLink);
        String error = new Regex(br.getRedirectLocation(), "http://uploaded.to/\\?view=(.*)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&i").getMatch(0);
        }
        if (error != null) {
            if (error.contains("error_traffic")) throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.uploadedto.errorso.premiumtrafficreached", "Traffic limit reached"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            String message = JDL.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage("ServerError: " + message);
            return;

        }

        if (br.getRedirectLocation() == null) {
            logger.info("InDirect Downloads active");
            Form form = br.getFormBySubmitvalue("Download");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.openFormConnection(form);
            if (br.getRedirectLocation() == null) {
                br.followConnection();
                logger.severe("Endlink not found");
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
                linkStatus.setErrorMessage(JDL.L("plugins.hoster.uploadedto.errors.indirectlinkerror", "Indirect link error"));
                return;
            }
        } else {
            logger.info("Direct Downloads active");
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);

        dl.setFileSizeVerified(true);
        if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(5 * 60 * 1000l);
            return;
        }

        dl.startDownload();

        String server = new Regex(dl.getConnection().getURL(), "http:/(.*?)\\..*?.to/dl\\?id=(.+)").getMatch(0);
        String id = new Regex(dl.getConnection().getURL(), "http:/(.*?)\\..*?.to/dl\\?id=(.+)").getMatch(1);
        downloadLink.setProperty("server", server);
        downloadLink.setProperty("id", id);

        if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_FAILED_HASH) {

            collectCRCLinks(downloadLink, true);
        }

    }

    private static void collectCRCLinks(DownloadLink downloadLink, boolean b) {
        String server = downloadLink.getStringProperty("server");
        String id = downloadLink.getStringProperty("id");
        try {
            Browser br = new Browser();
            br.setDebug(true);
            br.getPage("http://uploaded.to/rarerrors?auth=" + Encoding.urlEncode(id) + "&server=" + Encoding.urlEncode(server) + "&flag=" + (b ? 1 : 0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAGBLink() {
        return "http://uploaded.to/agb";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.correctDownloadLink(downloadLink);
        this.setBrowserExclusive();
        LINK_OBSERVER.register(downloadLink);
        br.setFollowRedirects(true);
        String id = new Regex(downloadLink.getDownloadURL(), "uploaded.to/file/(.*?)/").getMatch(0);
        int retry = 0;
        while (true) {
            try {
                br.getPage("http://uploaded.to/api/file?id=" + id);
                String[] lines = Regex.getLines(br + "");
                String fileName = lines[0].trim();
                long fileSize = Long.parseLong(lines[1].trim());
                downloadLink.setFinalFileName(fileName);
                downloadLink.setDownloadSize(fileSize);
                downloadLink.setSha1Hash(lines[2].trim());
                break;
            } catch (Exception e) {
                if (br.getHttpConnection().getResponseCode() == 403) {
                    if (retry < 5) {
                        retry++;
                        Thread.sleep(100 + retry * 20);
                        continue;
                    } else {
                        return AvailableStatus.UNCHECKABLE;
                    }
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("error_fileremoved")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML(PASSWORDPROTECTED)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.uploadedto.passwordprotectedlink", "This link is password protected"));
        return AvailableStatus.TRUE;
    }

    private String getPassword(DownloadLink downloadLink) throws Exception {
        String passCode = null;
        if (br.containsHTML(PASSWORDPROTECTED)) {
            logger.info("pw protected link");
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
        }
        return passCode;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        br.setCookie("http://uploaded.to/", "lang", "de");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("ist aufgebraucht")) {
            String wTime = br.getRegex("\\(Oder warten Sie (.*?)\\!\\)").getMatch(0);
            long wait = 0;
            if (wTime != null) {
                logger.info("Traffic Limit reached...." + wTime);
                wait = Regex.getMilliSeconds(wTime);
            } else {
                logger.info("Traffic Limit reached...." + br);
            }
            if (wTime == null) {
                wait = 65 * 60 * 1000l;
            } else {
                wait = wait + (5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
        }
        String error = new Regex(br.getURL(), "http://uploaded.to/\\?view=(.*)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getURL(), "\\?view=(.*?)&").getMatch(0);
        }
        if (error != null) {
            String wTime = br.getRegex("\\(Oder warten Sie (.*?)\\!\\)").getMatch(0);
            long wait = 0;
            if (error.contains("error_traffic")) {
                if (wTime != null) {
                    logger.info("Traffic Limit reached...." + wTime);
                    wait = Regex.getMilliSeconds(wTime);
                } else {
                    logger.info("Traffic Limit reached...." + br);
                }
                if (wTime == null) {
                    wait = 65 * 60 * 1000l;
                } else {
                    wait = wait + (5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait);
            }
            String message = JDL.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
            linkStatus.setErrorMessage(message);
            return;
        }

        br.setFollowRedirects(false);

        Form form = br.getFormbyProperty("name", "download_form");
        if (form == null && br.containsHTML("Versuch es sp")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.uploadedto.errors.serverproblem", "Server problem"), 10 * 60 * 1000l);
        if (form != null) {
            if (!br.containsHTML(RECAPTCHA)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            String passCode = getPassword(downloadLink);
            if (passCode != null) rc.getForm().put("file_password", passCode);
            /* Wartezeit */
            int waitThis = 30;
            String wait = br.getRegex("var secs = (\\d+)").getMatch(0);
            if (wait != null) waitThis = Integer.parseInt(wait);
            sleep(waitThis * 1001l, downloadLink);
            rc.setCode(c);
            if (br.containsHTML(PASSWORDPROTECTED)) {
                downloadLink.setProperty("pass", null);
                logger.info("The user entered a wrong password...");
                throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
            } else {
                downloadLink.setProperty("pass", passCode);
            }
            if (br.containsHTML(RECAPTCHA)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            dl = BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        } else {
            String dlLink = br.getRedirectLocation();
            if (dlLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, false, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Sie laden bereits eine Datei herunter")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.fakeContentRangeHeader(false);
        dl.setFileSizeVerified(true);
        if (dl.getConnection().getLongContentLength() == 0) {
            dl.getConnection().disconnect();
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(10 * 60 * 1000l);
            return;
        }

        dl.startDownload();
        String server = new Regex(dl.getConnection().getURL(), "http://(.*?)\\.").getMatch(0);
        String id = new Regex(downloadLink.getDownloadURL(), "http://uploaded.to/file/(.*?)/").getMatch(0);
        downloadLink.setProperty("server", server);
        downloadLink.setProperty("id", id);

        if (downloadLink.getLinkStatus().hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && downloadLink.getLinkStatus().getValue() == LinkStatus.VALUE_FAILED_HASH) {
            collectCRCLinks(downloadLink, true);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
