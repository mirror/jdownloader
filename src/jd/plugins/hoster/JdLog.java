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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.HTACCESSController;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;

/**
 * alternative log downloader
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jdlog" }, urls = { "jdlog://(\\d+)" }) 
public class JdLog extends PluginForHost {

    private String  dllink     = null;
    private String  uid        = null;
    private boolean isNewLogin = false;

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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getResponseCode() == 401) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
        }
        if (isNewLogin) {
            HTACCESSController.getInstance().addValidatedAuthentication(dllink, basicauthInfo[1], basicauthInfo[2]);
        }
        org.jdownloader.auth.AuthenticationController.getInstance().validate(new org.jdownloader.auth.BasicAuth(basicauthInfo[1], basicauthInfo[2]), dllink);
        dl.startDownload();
        // the following determine is its empty container.
        final File file = new File(dl.getDownloadable().getFileOutput());
        if (file.exists()) {
            final long dlsize = dl.getDownloadable().getDownloadTotalBytes();
            // limit the check because it will use less memory than placing 100+MiB log to String
            if (dlsize < 50) {
                final String out = parseLocalFile(file);
                if (StringUtils.equals(out, "LogID: " + uid + "\r\n\r\n")) {
                    // set as offline
                    downloadLink.setProperty("offlineByRegexConfirmation", true);
                    // delete method
                    downloadLink.getDownloadLinkController().getJobsAfterDetach().add(new DownloadWatchDogJob() {

                        @Override
                        public void interrupt() {
                        }

                        @Override
                        public void execute(DownloadSession currentSession) {
                            final ArrayList<DownloadLink> delete = new ArrayList<DownloadLink>();
                            delete.add(downloadLink);
                            DownloadWatchDog.getInstance().delete(delete, null);
                        }

                        @Override
                        public boolean isHighPriority() {
                            return false;
                        }
                    });
                    // not set as offline! have to throw exception!!
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } else if (dlsize < 125) {
                // delete method
                downloadLink.getDownloadLinkController().getJobsAfterDetach().add(new DownloadWatchDogJob() {

                    @Override
                    public void interrupt() {
                    }

                    @Override
                    public void execute(DownloadSession currentSession) {
                        final ArrayList<DownloadLink> delete = new ArrayList<DownloadLink>();
                        delete.add(downloadLink);
                        DownloadWatchDog.getInstance().delete(delete, null);
                    }

                    @Override
                    public boolean isHighPriority() {
                        return false;
                    }
                });
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // dllink = "http://update3.jdownloader.org/jdserv/UploadInterface/logsorted?" + uid;
            }
        }
    }

    private String[] getBasicAuth(final DownloadLink link) throws PluginException {
        org.jdownloader.auth.Login logins = org.jdownloader.auth.AuthenticationController.getInstance().getBestLogin(dllink);
        if (logins == null) {
            logins = requestLogins(org.jdownloader.translate._JDT.T.DirectHTTP_getBasicAuth_message(), link);
            isNewLogin = true;
        }
        return new String[] { logins.toBasicAuth(), logins.getUsername(), logins.getPassword() };
    }

    private String parseLocalFile(final File file) {
        final BufferedReader f;
        final StringBuffer buffer = new StringBuffer();
        try {
            f = new BufferedReader(new FileReader(file));

            String line;

            while ((line = f.readLine()) != null) {
                buffer.append(line + "\r\n");
            }
            f.close();
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

}