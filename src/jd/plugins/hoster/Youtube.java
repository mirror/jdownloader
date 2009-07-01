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
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDMediaConvert;
import jd.utils.locale.JDL;

@HostPlugin(name = "youtube.com", urls = "http://[\\w\\.]*?youtube\\.com/get_video\\?video_id=.+&t=.+(&fmt=\\d+)?", flags = 2)
public class Youtube extends PluginForHost {

    public Youtube(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.youtube.com/login?next=/index");
    }

    // @Override
    public String getAGBLink() {
        return "http://youtube.com/t/terms";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        downloadLink.setFinalFileName(downloadLink.getStringProperty("finalname", "video.tmp"));
        // br.setFollowRedirects(true);
        // URLConnectionAdapter tmp =
        // br.openGetConnection(downloadLink.getDownloadURL());
        // if (tmp.getResponseCode() == 200) {
        // tmp.disconnect();
        // return true;
        // }
        // tmp.disconnect();
        return AvailableStatus.TRUE; /*
                                      * warum sollte ein video das der decrypter
                                      * sagte es sei online, offline sein ;)
                                      */
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL());
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

    // @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        login(account);
        dl = br.openDownload(downloadLink, downloadLink.getDownloadURL());
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

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("http://www.youtube.com/");
        br.getPage("http://www.youtube.com/signup?next=/index");
        br.postPage("https://www.google.com/accounts/ServiceLoginAuth?service=youtube", "ltmpl=sso&continue=http%3A%2F%2Fwww.youtube.com%2Fsignup%3Fhl%3Den_US%26warned%3D%26nomobiletemp%3D1%26next%3D%2Findex&service=youtube&uilel=3&ltmpl=sso&hl=en_US&ltmpl=sso&GALX=IKTS6-HeUug&Email=" + Encoding.urlEncode(account.getUser()) + "&Passwd=" + Encoding.urlEncode(account.getPass()) + "&PersistentCookie=yes&rmShown=1&signIn=Sign+in&asts=");
        br.getPage("http://www.youtube.com/index?hl=en-GB");
        if (!br.getRegex("<title>YouTube - " + account.getUser() + "'s YouTube</title>").matches()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
    }

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.youtube.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        ai.setValid(true);

        return ai;
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
