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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class BluehostTo extends PluginForHost {

    public BluehostTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://bluehost.to/premium.php");
    }

    private void correctUrl(DownloadLink downloadLink) {
        String url = downloadLink.getDownloadURL();
        url = url.replaceFirst("\\?dl=", "dl=");
        downloadLink.setUrlDownload(url);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://bluehost.to", "bluehost_lang", "DE");
        br.postPage("http://bluehost.to/premiumlogin.php", "loginname=" + Encoding.urlEncode(account.getUser()) + "&loginpass=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://bluehost.to", "bluehost_premium_points") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        ai.setTrafficLeft(Long.parseLong(br.getCookie("http://bluehost.to", "bluehost_traffic_check")));
        ai.setPremiumPoints(Long.parseLong(br.getCookie("http://bluehost.to", "bluehost_premium_points")));
        ai.setAccountBalance(Long.parseLong(br.getCookie("http://bluehost.to", "bluehost_premium_cash")));
        ai.setExpired(false);
        ai.setValidUntil(-1);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        this.setBrowserExclusive();
        getFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(true);
        if (br.getCookie("http://bluehost.to", "bluehost_premium_auth") == null) {
            logger.info("Not enough Traffic left for PremiumDownload");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (Long.parseLong(br.getCookie("http://bluehost.to", "bluehost_traffic_check")) < downloadLink.getDownloadSize()) {
            logger.info("Not enough Traffic left for PremiumDownload");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            login(account);
            String trafficLeft = br.getCookie("http://bluehost.to", "bluehost_traffic_check");
            if (trafficLeft != null && trafficLeft.trim().equalsIgnoreCase("0")) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        getFileInformation(downloadLink);
        // Zeit bis free download möglich
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());
        String[] dat = page.split("\\, ");
        int time = Integer.parseInt(dat[4]);
        if (time > 0) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.hoster.bluehostto.time", "BH free only allowed from 0 PM to 10 AM"), time * 1000l);

        br.getPage(downloadLink.getDownloadURL());
        String redirect = br.getRegex("<meta http-equiv=\"refresh\" content=\"1\\; URL=(.*?)\" />").getMatch(0);
        br.getPage(redirect);
        br.clearCookies("bluehost.to");
        br.submitForm(br.getForm(1));
        br.getPage(downloadLink.getDownloadURL());
        br.openDownload(downloadLink, br.getForm(1));
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://bluehost.to/agb.php";
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null) return false;

        try {

            logger.finest("Checked Links with one request: " + urls.length);
            StringBuilder sb = new StringBuilder();
            sb.append("urls=");
            for (int i = 0; i < urls.length; i++) {
                sb.append(urls[i].getDownloadURL());
                sb.append(',');
            }
            this.setBrowserExclusive();
            br.forceDebug(true);
            br.setCookie("http://bluehost.to", "bluehost_lang", "DE");
            br.postPage("http://bluehost.to/fileinfo/multi", sb.toString());

            String[] lines = Regex.getLines(br + "");

            for (int i = 0; i < urls.length; i++) {
                String[] dat = lines[i].split("\\, ");
                try {
                    urls[i].setMD5Hash(dat[5].trim());
                    urls[i].setFinalFileName(dat[0]);
                    urls[i].setDupecheckAllowed(true);
                    urls[i].setDownloadSize(Long.parseLong(dat[2]));
                    urls[i].setAvailable(true);
                } catch (Exception e) {
                    urls[i].setAvailable(false);
                }
            }

        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        correctUrl(downloadLink);
        // dateiname, dateihash, dateisize, dateidownloads, zeit bis HH
        this.setBrowserExclusive();
        br.setCookie("http://bluehost.to", "bluehost_lang", "DE");
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());

        String[] dat = page.split("\\, ");
        try {
            downloadLink.setMD5Hash(dat[5].trim());
            downloadLink.setFinalFileName(dat[0]);
            downloadLink.setDupecheckAllowed(true);
            downloadLink.setDownloadSize(Long.parseLong(dat[2]));
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
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
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
