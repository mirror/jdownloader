//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.controlling.HTACCESSController;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

/**
 * alternative log downloader
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision: 28806 $", interfaceVersion = 3, names = { "jdlog" }, urls = { "jdlog://(\\d+)" }, flags = { 0 })
public class JdLog extends PluginForHost {

    private String  dllink     = null;
    private String  uid        = null;
    private boolean isNewLogin = false; ;

    @Override
    public String getAGBLink() {
        return "";
    }

    public JdLog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        uid = new Regex(downloadLink.getDownloadURL(), this.getSupportedLinks()).getMatch(0);
        downloadLink.setFinalFileName(uid + ".log");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dllink = "http://update3.jdownloader.org/jdserv/UploadInterface/logunsorted?" + uid;
        final String[] basicauthInfo = this.getBasicAuth(downloadLink);
        if (basicauthInfo == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
        }
        final String basicauth = basicauthInfo[0];
        br.getHeaders().put("Authorization", basicauth);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getResponseCode() == 401) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
        }
        if (isNewLogin) {
            HTACCESSController.getInstance().addValidatedAuthentication(dllink, basicauthInfo[1], basicauthInfo[2]);
        }
        org.jdownloader.auth.AuthenticationController.getInstance().validate(new org.jdownloader.auth.BasicAuth(basicauthInfo[1], basicauthInfo[2]), dllink);
        dl.startDownload();
    }

    private String[] getBasicAuth(final DownloadLink link) throws PluginException {
        org.jdownloader.auth.Login logins = org.jdownloader.auth.AuthenticationController.getInstance().getBestLogin(dllink);
        if (logins == null) {
            logins = requestLogins(org.jdownloader.translate._JDT._.DirectHTTP_getBasicAuth_message(), link);
            isNewLogin = true;
        }
        return new String[] { logins.toBasicAuth(), logins.getUsername(), logins.getPassword() };
    }

    /* do not add @Override here to keep 0.* compatibility */
    public boolean hasAutoCaptcha() {
        return false;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("nopremium"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}