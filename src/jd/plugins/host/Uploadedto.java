//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.http.Request;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Uploadedto extends PluginForHost {

    static private final String AGB_LINK = "http://uploaded.to/agb";

    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);

        this.enablePremium("http://uploaded.to/?id=u8mp9x&sidebarimage");
        setMaxConnections(20);

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), "PREMIUMCHUNKS", JDLocale.L("plugins.hoster.uploadedto.chunks", "Premium connections # (>1 causes higher traffic)"), 1, 20).setDefaultValue(1).setStep(1));

    }

    /**
     * Korrigiert den Downloadlink in ein einheitliches Format
     * 
     * @param parameter
     */
    private void correctURL(DownloadLink parameter) {
        String link = parameter.getDownloadURL();
        link = link.replace("/?id=", "/file/");
        link = link.replace("?id=", "file/");
        String[] parts = link.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }

        parameter.setUrlDownload(newLink);

    }

    public int getTimegapBetweenConnections() {
        return 300;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.getPage("http://uploaded.to/login");

        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());

        br.submitForm(login);
        if (br.containsHTML("Login failed!")) {
            ai.setValid(false);
            ai.setStatus("User ID or password wrong");
            return ai;

        }
        String accounttype = br.getMatch("Accounttype:</span> <span class=.*?>(.*?)</span>");
        if (accounttype != null && accounttype.equalsIgnoreCase("free")) {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            ai.setAccountBalance((int) (Double.parseDouble(balance) * 100));
            ai.setPremiumPoints(Integer.parseInt(points));
            ai.setValidUntil(System.currentTimeMillis() + (356 * 24 * 60 * 60 * 1000l));
            ai.setStatus("Accounttyp: Collectorsaccount");
        } else {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            String traffic = br.getMatch("Traffic left: </span><span class=.*?>(.*?)</span> ");
            String expire = br.getMatch("Valid until: </span> <span class=.*?>(.*?)</span>");

            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd-MM-yyyy hh:mm", null));
            ai.setAccountBalance((int) (Double.parseDouble(balance) * 100));
            ai.setTrafficLeft(Regex.getSize(traffic));
            ai.setPremiumPoints(Integer.parseInt(points));
        }

        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }

        LinkStatus linkStatus = downloadLink.getLinkStatus();

        correctURL(downloadLink);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setDebug(true);
        br.setCookie("http://uploaded.to/", "lang", "de");

        String user = account.getUser();

        logger.info("login");
        br.setFollowRedirects(true);
        br.getPage("http://uploaded.to/login");

        Form login = br.getForm(0);
        login.put("email", account.getUser());
        login.put("password", account.getPass());

        br.submitForm(login);
        if (br.containsHTML("Login failed!")) {
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage("Login Error: " + user);
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            return;

        }
        String accounttype = br.getMatch("Accounttyp:</span> <span class=.*?>(.*?)</span>");

        if (accounttype != null && accounttype.equalsIgnoreCase("free")) {
            logger.severe("Entered a Free-account");
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage("Account " + user + " is a freeaccount");
            linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            return;

        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());

        String error = new Regex(br.getRedirectLocation(), "http://uploaded.to/\\?view=(.*)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&id\\_a").getMatch(0);
        }
        if (error != null) {
            if (error.equalsIgnoreCase("error_traffic")) {
                linkStatus.setErrorMessage(JDLocale.L("plugins.errors.uploadedto.premiumtrafficreached", "Traffic limit reached"));
                linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                return;

            }
            String message = JDLocale.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(message);
            return;

        }

        if (br.getRedirectLocation() == null) {

            Form form = br.getFormbyValue("Download");

            br.openFormConnection(form);

            if (br.getRedirectLocation() == null) {

                logger.severe("Endlink not found");
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage("Indirect Link Error");

                return;

            }
        } else {
            logger.info("Direct Downloads active");

        }
        Request request = br.createGetRequest(null);
        dl = new RAFDownload(this, downloadLink, request);
        dl.setChunkNum(this.getPluginConfig().getIntegerProperty("PREMIUMCHUNKS", 1));
        dl.setResume(true);

        HTTPConnection con = dl.connect();
        if (con.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }

        dl.startDownload();

    }

    public String getAGBLink() {
        return AGB_LINK;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return true;
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        correctURL(downloadLink);

        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("ist aufgebraucht")) {
            linkStatus.setStatusText(JDLocale.L("plugins.uploadedto.message.linkgrabber.trafficlimit", "Traffic limit reached. Could not get fileinfo"));
            return true;
        }

        Regex info = br.getRegex("Dateiname:.*?</td><td><b>(.*?)</b></td></tr>.*?<tr><td style=\"padding-left:4px;\">Dateityp:.*?</td><td>(.*?)</td></tr>.*?<tr><td style=\"padding-left:4px;\">Dateig.*?</td><td>(.*?)</td>");

        String fileName = Encoding.htmlDecode(info.getMatch(0));
        if (fileName == null) return false;
        String ext = Encoding.htmlDecode(info.getMatch(1));
        String fileSize = Encoding.htmlDecode(info.getMatch(2));
        downloadLink.setName(fileName.trim() + "" + ext.trim());

        downloadLink.setDownloadSize(Regex.getSize(fileSize));

        return true;
    }

    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {

        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        br.setDebug(true);

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setCookie("http://uploaded.to/", "lang", "de");
        correctURL(downloadLink);

        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("ist aufgebraucht")) {
            long wait = Regex.getMilliSeconds(br.getRegex("\\(Oder warten Sie (.*?)\\!\\)").getMatch(0));
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            logger.info("Traffic Limit reached....");
            linkStatus.setValue(wait);
            return;
        }
        String error = new Regex(br.getURL(), "http://uploaded.to/\\?view=(.*?)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getURL(), "\\?view=(.*?)&id\\_a").getMatch(0);
        }
        if (error != null) {
            String message = JDLocale.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(message);
            return;

        }

        br.setFollowRedirects(false);

        Form form = br.getFormbyValue("Download");

        Request request = br.createFormRequest(form);
        dl = new RAFDownload(this, downloadLink, request);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        try {
            dl.connect();
        } catch (Exception e) {
            error = new Regex(request.getLocation(), "http://uploaded.to/\\?view=(.*?)").getMatch(0);
            if (error == null) {
                error = new Regex(request.getLocation(), "\\?view=(.*?)&id\\_a").getMatch(0);
            }
            if (error != null) {
                String message = JDLocale.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));

                throw new PluginException(LinkStatus.ERROR_FATAL, message);

            }

            if (request.getLocation() != null) {
                br.setRequest(request);
                request.getHttpConnection().disconnect();
                request = br.createGetRequest(null);
                dl = new RAFDownload(this, downloadLink, request);
                dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
                dl.setResume(true);
                dl.connect();
            }
        }
        if (request.getHttpConnection().getContentLength() == 0) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);

        }

        dl.startDownload();

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

}
