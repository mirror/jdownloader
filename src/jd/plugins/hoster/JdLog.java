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
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.auth.AuthenticationController;
import org.jdownloader.jdserv.JDServUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.http.AuthenticationFactory;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

/**
 * Alternative AppWork log downloader
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jdlog" }, urls = { "jdlog://(\\d+)" })
public class JdLog extends PluginForHost {
    @Override
    public String getAGBLink() {
        return "https://jdownloader.org/impressum";
    }

    public JdLog(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.INTERNAL };
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final String uid = new Regex(link.getDownloadURL(), this.getSupportedLinks()).getMatch(0);
        link.setFinalFileName(uid + ".log");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String uid = new Regex(link.getDownloadURL(), this.getSupportedLinks()).getMatch(0);
        final String url = JDServUtils.BASE + "logunsorted?" + uid;
        final List<AuthenticationFactory> authenticationFactories = AuthenticationController.getInstance().buildAuthenticationFactories(URLHelper.createURL(url), null);
        for (final AuthenticationFactory authenticationFactory : authenticationFactories) {
            br.setCustomAuthenticationFactory(authenticationFactory);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
            if (dl.getConnection().getResponseCode() == 401) {
                dl.getConnection().disconnect();
            } else {
                break;
            }
        }
        if (dl == null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, false, 1);
        }
        if (dl.getConnection().getResponseCode() == 401) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, JDL.L("plugins.hoster.httplinks.errors.basicauthneeded", "BasicAuth needed"));
        } else if (dl.getConnection().getCompleteContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
                    link.setProperty("offlineByRegexConfirmation", true);
                    // delete method
                    link.getDownloadLinkController().getJobsAfterDetach().add(new DownloadWatchDogJob() {
                        @Override
                        public void interrupt() {
                        }

                        @Override
                        public void execute(DownloadSession currentSession) {
                            final ArrayList<DownloadLink> delete = new ArrayList<DownloadLink>();
                            delete.add(link);
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
                link.getDownloadLinkController().getJobsAfterDetach().add(new DownloadWatchDogJob() {
                    @Override
                    public void interrupt() {
                    }

                    @Override
                    public void execute(DownloadSession currentSession) {
                        final ArrayList<DownloadLink> delete = new ArrayList<DownloadLink>();
                        delete.add(link);
                        DownloadWatchDog.getInstance().delete(delete, null);
                    }

                    @Override
                    public boolean isHighPriority() {
                        return false;
                    }
                });
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
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