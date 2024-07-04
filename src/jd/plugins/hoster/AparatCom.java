//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.AparatComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AparatCom extends PluginForHost {
    public AparatCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    public static List<String[]> getPluginDomains() {
        return AparatComCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            /* No regex as items get added exclusively via crawler. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/community-guideline";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        return this.getHost() + "://" + link.getProperty(PROPERTY_VIDEOID) + "/quality/" + link.getProperty(PROPERTY_QUALITY_HEIGHT);
    }

    public static final String PROPERTY_VIDEOID           = "videoid";
    public static final String PROPERTY_QUALITY_HEIGHT    = "height";
    public static final String PROPERTY_QUALITY_DIRECTURL = "directurl";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        final String extDefault = ".mp4";
        this.setBrowserExclusive();
        final String dllink = link.getStringProperty(PROPERTY_QUALITY_DIRECTURL);
        boolean success = false;
        if (dllink != null) {
            try {
                basicLinkCheck(br, br.createHeadRequest(dllink), link, null, extDefault, FILENAME_SOURCE.FINAL);
                success = true;
            } catch (final PluginException e) {
                logger.log(e);
                logger.info("Looks like directurl expired, refresh needed");
            }
        }
        if (!success) {
            logger.info("Obtaining fresh directurl");
            final AparatComCrawler crawler = (AparatComCrawler) this.getNewPluginForDecryptInstance(this.getHost());
            final ArrayList<DownloadLink> results = crawler.crawlSingleVideo(dllink, link.getIntegerProperty(PROPERTY_QUALITY_HEIGHT, -1));
            final DownloadLink fresh = results.get(0);
            final String dllinkFresh = fresh.getStringProperty(PROPERTY_QUALITY_DIRECTURL);
            link.setProperty(PROPERTY_QUALITY_DIRECTURL, dllinkFresh);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String dllink = link.getStringProperty(PROPERTY_QUALITY_DIRECTURL);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(link, null));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}