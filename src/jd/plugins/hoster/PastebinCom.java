//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.decrypter.AbstractPastebinCrawler;
import jd.plugins.decrypter.PastebinComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PastebinComCrawler.class })
public class PastebinCom extends AbstractPastebinHoster {
    public PastebinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://pastebin.com/doc_terms_of_service";
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(PastebinComCrawler.getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(PastebinComCrawler.getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return PastebinComCrawler.getAnnotationUrls();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final PluginForDecrypt plg = this.getNewPluginForDecryptInstance(this.getHost());
        /* If the content is offline, the following call will throw an appropriate exception. */
        ((AbstractPastebinCrawler) plg).preProcessAndGetPlaintextDownloadLink(new CryptedLink(link.getPluginPatternMatcher(), link));
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(getDirectDownloadURL(link));
            link.setFinalFileName(Plugin.getFileNameFromHeader(con));
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, this.getDirectDownloadURL(link), false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDirectDownloadURL(final DownloadLink link) {
        return "https://" + this.getHost() + "/dl/" + this.getFID(link);
    }
}