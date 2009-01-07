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
import java.util.HashMap;
import java.util.Map.Entry;

import jd.utils.JDUtilities;

import jd.PluginWrapper;
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

public class MeinUpload extends PluginForHost {

    private static final String AGB_LINK = "http://meinupload.com/#help.html";

    public MeinUpload(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://meinupload.com/register.php?g=2");
    }
    @SuppressWarnings("unchecked")
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.getPage(downloadLink.getDownloadURL());

        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.meinupload.error." + error, error));
        }
        br.setDebug(true);
        Form form = br.getFormbyValue("Free Download");

        if (form != null) {
            form.remove("method_premium");
            br.submitForm(form);
            if(br.toString().contains("(Or wait"))
            {
                String[] timestr = br.getRegex("\\(Or wait (\\d+) minutes, (\\d+) seconds\\)").getRow(0);
                long waitTime = (Long.parseLong(timestr[0])*60000)+(Long.parseLong(timestr[1])*1000);
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
            }
            String[][] Str = br.getRegex("position:absolute;padding-left:(\\d+)[^>]+>(\\d+)").getMatches();
            HashMap<Integer, Integer> gr = new HashMap<Integer, Integer>();
            for (String[] strings : Str) {
                try {
                    gr.put(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            gr = (HashMap<Integer, Integer>) JDUtilities.revSortByKey(gr);
            String code = "";
            for (Entry<Integer, Integer> entry : gr.entrySet()) {
                code+=entry.getValue();
            }

            Form captcha = br.getForms()[1];      
            captcha.put("code", code);
            captcha.put("down_script", "1");
            this.sleep((Integer.parseInt(br.getRegex("(\\d+)</span> Sekunden</span>").getMatch(0))*1000),downloadLink );
            br.openDownload(downloadLink, captcha);
        }
   

        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.host.meinupload.serverdefect", "Serivce not available"), 10 * 60 * 1000l);

        }
        dl.startDownload();
    }

    private void login(Account account) throws IOException {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.clearCookies(getHost());

        br.getPage("http://meinupload.com/index.php");
        Form login = br.getFormbyValue("Login");
        login.put("act", "login");
        login.put("user", account.getUser());
        login.put("pass", account.getPass());
        br.submitForm(login);
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);

        login(account);
        br.getPage("http://meinupload.com/members.php");
        String expire = br.getRegex("<b>Packet runs out on</b></td>.*?<td align=.*?>(.*?)</td>").getMatch(0);
        if (expire == null) {
            ai.setValid(false);
            ai.setStatus("Account invalid. Logins wrong?");
            return ai;
        }

        String points = br.getRegex("Bonuspoints overall</b></td>.*?<td align=.*?>(\\d+?)&nbsp;\\(([\\d\\.]+?)&#x80;\\)</t").getMatch(0);
        String cash = br.getRegex("Bonuspoints overall</b></td>.*?<td align=.*?>(\\d+?)&nbsp;\\(([\\d\\.]+?)&#x80;\\)</t").getMatch(1);
        String files = br.getRegex("Hosted Files</b></td>.*?<td align=.*?>(.*?)  <a href").getMatch(0);

        ai.setStatus("Account is ok.");
        ai.setValidUntil(Regex.getMilliSeconds(expire, "MM/dd/yy", null));

        ai.setPremiumPoints(Integer.parseInt(points));
        ai.setAccountBalance(Integer.parseInt(cash.replaceAll("\\.", "")));
        ai.setFilesNum(Integer.parseInt(files));

        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        login(account);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.meinupload.error." + error, error));

        }

        String url = br.getRegex("document\\.location=\"(.*?)\"").getMatch(0);

        dl = new RAFDownload(this, downloadLink, br.createRequest(url));
        dl.setChunkNum(1);
        dl.setResume(true);
        dl.connect();
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
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.getPage(downloadLink.getDownloadURL());

        if (br.getRedirectLocation() != null) {
            String error = br.getRegex("code=(.*)").getMatch(0);
            downloadLink.getLinkStatus().setErrorMessage(error);
            return false;

        }
        String filename = br.getRegex("Datei: <font color=\"#\\d+\">([^<]*)</font>").getMatch(0);
        downloadLink.setName(filename);
        try {
            String s = br.getRegex("Datei: <font color=\"#\\d+\">[^<]*</font> \\(([^\\)]*)\\)").getMatch(0);
            long size = Regex.getSize(s);
            if (size > 0) downloadLink.setDownloadSize(size);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}