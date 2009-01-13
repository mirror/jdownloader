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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class MeinUpload extends PluginForHost {

    private static final String AGB_LINK = "http://meinupload.com/tos.html";
    private int simultanpremium = 1;

    public MeinUpload(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://meinupload.com/register.php?g=2");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.meinupload.error." + error, error));
        }
        handleFree0(downloadLink);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    public void handleFree0(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());
        Form form = br.getFormbyValue("Free Download");

        if (form != null) {
            form.remove("method_premium");
            br.submitForm(form);
            if (br.toString().contains("(Or wait")) {
                String[] timestr = br.getRegex("\\(Or wait (\\d+) minutes, (\\d+) seconds\\)").getRow(0);
                long waitTime = (Long.parseLong(timestr[0]) * 60000) + (Long.parseLong(timestr[1]) * 1000);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
            }
            String[][] Str = br.getRegex("position:absolute;padding-left:(\\d+)[^>]+>(\\d+)").getMatches();
            HashMap<Integer, Integer> gr = new HashMap<Integer, Integer>();
            for (String[] strings : Str) {
                try {
                    gr.put(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
                } catch (Exception e) {
                }
            }
            gr = (HashMap<Integer, Integer>) JDUtilities.revSortByKey(gr);
            String code = "";
            for (Entry<Integer, Integer> entry : gr.entrySet()) {
                code += entry.getValue();
            }
            Form captcha = br.getFormbyName("F1");
            captcha.put("code", code);
            captcha.put("down_script", "1");
            this.sleep((Integer.parseInt(br.getRegex("(\\d+)</span> Sekunden</span>").getMatch(0)) * 1000), downloadLink);
            br.setDebug(true);
            br.openDownload(downloadLink, captcha);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.meinupload.serverdefect", "Serivce not available"), 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.clearCookies("mein-upload.com");
        br.getPage("http://www.mein-upload.com/");
        Form login = br.getForm(0);
        login.put("login", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(login);
        String cookie = br.getCookie("http://www.mein-upload.com/", "xfss");
        if (cookie == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        br.setCookie("http://www.meinupload.com/", "xfss", cookie);
    }

    private boolean isPremium() throws IOException {
        br.getPage("http://www.mein-upload.com/?op=my_account");
        if (br.containsHTML("Kein Premium Account")) return false;
        return true;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            ai.setStatus("Free Member Account");
            ai.setValid(true);
            return ai;
        }
        String expire = br.getRegex("<td><strong>G&uuml;ltig bis:</strong>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setValid(false);
            ai.setStatus("Expired?");
            return ai;
        }
        expire = expire.trim();
        String points = br.getRegex("<strong>Gesammelte Premium-Punkte:</strong> (\\d+)</td>").getMatch(0);
        ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", Locale.UK));
        ai.setPremiumPoints(Integer.parseInt(points));
        return ai;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        getFileInformation(downloadLink);
        login(account);
        if (!this.isPremium()) {
            simultanpremium = 1;
            handleFree0(downloadLink);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.getPage(downloadLink.getDownloadURL());
        Form form = br.getForm(0);
        br.submitForm(form);
        String url = br.getRegex(Pattern.compile("Dieser Direktlink.*?href=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, url, true, 0);
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.meinupload.serverdefect", "Serivce not available"), 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.clearCookies("mein-upload.com");
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            downloadLink.getLinkStatus().setErrorMessage(error);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("Datei: <font color=\"#\\d+\">([^<]*)</font>").getMatch(0);
        String filesize = br.getRegex("Datei: <font color=\"#\\d+\">[^<]*</font> \\(([^\\)]*)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}