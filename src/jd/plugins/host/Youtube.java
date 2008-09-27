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
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
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
import jd.utils.JDMediaConvert;

public class Youtube extends PluginForHost {
    static private final String CODER = "JD-Team";

    static private final String AGB = "http://youtube.com/t/terms";

    public Youtube(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium(1);
    }

    @Override
    public String getAGBLink() {
        return AGB;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        br.setFollowRedirects(true);
        if (br.openGetConnection(downloadLink.getDownloadURL()).getResponseCode() == 200) {
            return true;
        } else
            return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(getHost() + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));
            return;
        }
        br.getHttpConnection().disconnect();
        dl = new RAFDownload(this, downloadLink, br.createGetRequest(downloadLink.getDownloadURL()));
        dl.setChunkNum(1);
        dl.setResume(false);
        if (dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                ConversionMode convertto = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                ConversionMode InType = ConversionMode.VIDEOFLV;
                if (convertto.equals(ConversionMode.VIDEOMP4) || convertto.equals(ConversionMode.VIDEO3GP)) {
                    InType = convertto;
                }

                if (!JDMediaConvert.ConvertFile(downloadLink, InType, convertto)) {
                    logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        login(account);
        if (!br.getRegex("<title>YouTube - Mein YouTube: " + account.getUser() + "</title>").matches()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }

        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(getHost() + " " + JDLocale.L("plugins.host.server.unavailable", "Serverfehler"));
            return;
        }
        br.getHttpConnection().disconnect();
        dl = new RAFDownload(this, downloadLink, br.createGetRequest(downloadLink.getDownloadURL()));
        dl.setChunkNum(1);
        dl.setResume(false);
        if (dl.startDownload()) {
            if (downloadLink.getProperty("convertto") != null) {
                ConversionMode convertto = ConversionMode.valueOf(downloadLink.getProperty("convertto").toString());
                ConversionMode InType = ConversionMode.VIDEOFLV;
                if (convertto.equals(ConversionMode.VIDEOMP4) || convertto.equals(ConversionMode.VIDEO3GP)) {
                    InType = convertto;
                }

                if (!JDMediaConvert.ConvertFile(downloadLink, InType, convertto)) {
                    logger.severe("Video-Convert failed!");
                }
            }
        }
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void login(Account account) throws IOException {
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage("http://www.youtube.com/signup?next=/index");
        Form login = br.getFormbyName("loginForm");
        login.put("username", account.getUser());
        login.put("password", account.getPass());
        br.submitForm(login);
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        login(account);
        if (!br.getRegex("<title>YouTube - Mein YouTube: " + account.getUser() + "</title>").matches()) {
            ai.setValid(false);
            ai.setStatus("Account invalid. Logins wrong?");
            return ai;
        }

        ai.setStatus("Account is ok.");
        ai.setValidUntil(-1);
        ai.setValid(true);

        return ai;
    }
}
