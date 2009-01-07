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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.XPath;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class BluehostTo extends PluginForHost {

    public BluehostTo(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium();
    }

    private void correctUrl(DownloadLink downloadLink) {
        String url = downloadLink.getDownloadURL();
        url = url.replaceFirst("\\?dl=", "dl=");
        downloadLink.setUrlDownload(url);
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.setDebug(true);
        // br.setFollowRedirects(true);
        br.getPage("http://bluehost.to/index.php");

        Form login = br.getForm(0);

        login.setVariable(0, account.getUser());
        login.setVariable(1, account.getPass());

        br.submitForm(login);

        if (!br.getRedirectLocation().contains("interface")) throw new PluginException(LinkStatus.ERROR_PREMIUM);
        br.setFollowRedirects(true);
        br.getPage((String) null);
        String trafficLeft = br.getXPathElement("/html/body/div/div/ul[2]/div/div").trim();
        XPath path = new XPath(br.toString(), "/html/body/div/div/ul[2]/div[4]/center");
        double traffic = Double.parseDouble(trafficLeft) * 1000 * 1024 * 1024;
        ai.setTrafficLeft((long) traffic);
        ArrayList<String> matches = path.getMatches();
        ai.setPremiumPoints(JDUtilities.filterInt(matches.get(0)));
        ai.setAccountBalance((int) (Float.parseFloat(Encoding.filterString(matches.get(1), "1234567890.,").replaceAll("\\,", ".")) * 100.0));
        ai.setExpired(false);
        ai.setValidUntil(System.currentTimeMillis() + 60 * 60 * 1000);

        return ai;
    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        this.setBrowserExclusive();
        br.forceDebug(true);
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage("http://bluehost.to");

        Form login = br.getForm(5);
        login.put("loginname", account.getUser());
        login.put("loginpass", account.getPass());
        br.submitForm(login);

        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL(), true, 0);
        if (dl.getConnection().getContentType().contains("text")) {
            String page = br.loadConnection(dl.getConnection());
            br.getRequest().setHtmlCode(page);
            Form download = br.getFormbyName("download");
            if (download == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "Premium Error"); }
            dl = br.openDownload(downloadLink, download, true, 0);
            if (dl.getConnection().getContentType().contains("text")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Premium Error"); }
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());

        String[] dat = page.split("\\, ");

        downloadLink.setName(dat[0]);
        downloadLink.setDownloadSize(Integer.parseInt(dat[2]));
        if (Integer.parseInt(dat[3]) > 0) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, Integer.parseInt(dat[3]) * 1000l); }

        br.getPage(downloadLink.getDownloadURL());

        Form[] forms = br.getForms();

        // br.cloneBrowser().getPage("http://bluehost.to/style.css");
        // br.cloneBrowser().getPage("http://bluehost.to/css/autosuggest_inquisitor.css");

        Form dlForm = forms[2];
        dlForm.remove("UPLOADSCRIPT_LOGSESSION");
        br.clearCookies("bluehost.to");
        br.submitForm(dlForm);
        br.getPage(downloadLink.getDownloadURL());
        forms = br.getForms();
        // String url = br.getRegex("var url=\\'(.*?)\\'\\;").getMatch(0);
        br.openDownload(downloadLink, forms[1]);
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://bluehost.to/agb.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        correctUrl(downloadLink);
        // String page;
        // dateiname, dateihash, dateisize, dateidownloads, zeit bis
        // happyhour
        //           
        String page = br.getPage("http://bluehost.to/fileinfo/urls=" + downloadLink.getDownloadURL());

        String[] dat = page.split("\\, ");

        downloadLink.setMD5Hash(dat[5].trim());
        downloadLink.setName(dat[0]);
        downloadLink.setDownloadSize(Integer.parseInt(dat[2]));
        if (Integer.parseInt(dat[3]) > 0) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, Integer.parseInt(dat[3])); }

        return true;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
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
