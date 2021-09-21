//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.RuTubeVariant;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rutube.ru" }, urls = { "https?://rutube\\.ru/video/([a-f0-9]{32})" })
public class RuTubeRu extends PluginForHost {
    public RuTubeRu(final PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    public static final String PROPERTY_PRIVATEVALUE     = "privatevalue";
    public static final String PROPERTY_TITLE            = "title";
    public static final String PROPERTY_UPLOADER         = "uploader";
    public static final String PROPERTY_DURATION         = "duration";
    public static final String PROPERTY_EXPIRE_TIMESTAMP = "expire_timestamp";
    public static final String PROPERTY_INTERNAL_VIDEOID = "internal_videoid";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([a-f0-9]{32})$").getMatch(0);
    }

    @Override
    public String getAGBLink() {
        return "https://rutube.ru/agreement.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        download(link);
    }

    @Override
    public LinkVariant getActiveVariantByLink(final DownloadLink link) {
        return link.getVariant(RuTubeVariant.class);
    }

    @Override
    public void setActiveVariantByLink(final DownloadLink link, LinkVariant variant) {
        link.setDownloadSize(-1);
        super.setActiveVariantByLink(link, variant);
    }

    @Override
    public List<? extends LinkVariant> getVariantsByLink(DownloadLink link) {
        return link.getVariants(RuTubeVariant.class);
    }

    private void download(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String directurl = getStoredDirecturl(link);
        if (directurl == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (directurl.contains(".m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, directurl);
            dl.startDownload();
        } else {
            final Browser brc = cloneBrowser(br);
            dl = new HDSDownloader(link, brc, directurl);
            dl.startDownload();
        }
    }

    /** Returns stored directurl for current variant. */
    private String getStoredDirecturl(final DownloadLink link) {
        final String newHandlingValue = link.getStringProperty("directurl_" + link.getVariant(RuTubeVariant.class).getStreamID());
        if (newHandlingValue != null) {
            return newHandlingValue;
        } else {
            return link.getStringProperty("f4vUrl");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        final RuTubeVariant var = link.getVariant(RuTubeVariant.class);
        br.setCustomCharset("utf-8");
        /* URLs added via crawler revision 45007 or lower don't have this property! */
        if (link.hasProperty(PROPERTY_TITLE)) {
            link.setFinalFileName(link.getStringProperty(PROPERTY_TITLE) + "_" + var.getHeight() + "p" + ".mp4");
        }
        final String storedDirecturl = getStoredDirecturl(link);
        if (storedDirecturl == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2021-09-21: Directurls are valid regardless of cookies and IP! */
        final long expireTimestamp = link.getLongProperty(PROPERTY_EXPIRE_TIMESTAMP, 0);
        if (expireTimestamp > System.currentTimeMillis()) {
            logger.info("Directurl still valid until: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(expireTimestamp)));
            return AvailableStatus.TRUE;
        } else {
            logger.info("Directurl needs refresh");
            final PluginForDecrypt decrypter = this.getNewPluginForDecryptInstance(this.getHost());
            final CryptedLink param;
            if (link.hasProperty(PROPERTY_INTERNAL_VIDEOID)) {
                /* Crawler will be faster if internal videoID is given inside URL already! */
                param = new CryptedLink("http://" + this.getHost() + "/play/embed/" + link.getStringProperty(PROPERTY_INTERNAL_VIDEOID), link);
            } else {
                param = new CryptedLink(link.getPluginPatternMatcher(), link);
            }
            final ArrayList<DownloadLink> results = decrypter.decryptIt(param, null);
            if (results.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Single video crawler failure");
            }
            link.setProperties(results.get(0).getProperties());
        }
        return AvailableStatus.TRUE;
    }

    private Browser cloneBrowser(final Browser br) {
        final Browser ajax = br.cloneBrowser();
        // rv40.0 don't get "video_balancer".
        ajax.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:50.0) Gecko/20100101 Firefox/50.0");
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("X-Requested-With", "ShockwaveFlash/22.0.0.209");
        return ajax;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }
}