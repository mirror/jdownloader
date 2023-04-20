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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VidguardTo extends PluginForHost {
    public VidguardTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    @Override
    public String getAGBLink() {
        return "https://vidguard.to/terms";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vidguard.to", "vgfplay.com" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:d|e|v)/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    /**
     * Returns how many max. chunks per file are allowed for current download mode based on account availability and account type. <br />
     * Override this function to set chunks settings!
     */
    public int getMaxChunks(final Account account) {
        return 0;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getContentURL(final DownloadLink link) {
        return "https://" + this.getHost() + "/v/" + this.getFID(link);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            /* Set dummy filenames as fallback / weak filenames. */
            link.setName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(getContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. response: Not found err:1002 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h4>([^<]+)</h4>").getMatch(0);
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        }
        String filesize = br.getRegex("Download\\s*<br />([^<]+)").getMatch(0);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim();
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final String directlinkproperty) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty)) {
            requestFileInformation(link);
            final String nextStepURL = "/d/" + this.getFID(link);
            if (!br.containsHTML(nextStepURL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* 2023-04-14: This plugin hasn't been finished yet! Captcha still needs to be implemented. */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // TODO: Add captcha handling and find best quality downloadurl in the end
            br.getPage(nextStepURL);
            final Browser brc = br.cloneBrowser();
            brc.setAllowedResponseCodes(400);
            brc.getPage("/captcha?v=" + System.currentTimeMillis());
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String captcha_key = entries.get("captcha_key").toString();
            final String img_base64 = entries.get("img_base64").toString();
            final String thumb_base64 = entries.get("thumb_base64").toString();
            final String imgBase64Regex = "(?i)data:image/([a-z]+);base64,(.+)";
            final Regex imgBase64 = new Regex(img_base64, imgBase64Regex);
            final Regex thumbBase64 = new Regex(thumb_base64, imgBase64Regex);
            final String imgBase64Ext = imgBase64.getMatch(0);
            final String imgBase64Str = imgBase64.getMatch(1);
            final String thumbBase64Ext = thumbBase64.getMatch(0); // jpeg
            final String thumbBase64Str = thumbBase64.getMatch(1); // png
            if (imgBase64Str == null || thumbBase64Str == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final File exampleImageFile = getLocalCaptchaFile("." + thumbBase64Ext);
            final File mainImageFile = getLocalCaptchaFile("." + imgBase64Ext);
            IO.writeToFile(exampleImageFile, org.appwork.utils.encoding.Base64.decode(thumbBase64Str));
            IO.writeToFile(mainImageFile, org.appwork.utils.encoding.Base64.decode(imgBase64Str));
            final BufferedImage exampleImage = ImageIO.read(exampleImageFile);
            final BufferedImage mainImage = ImageIO.read(mainImageFile);
            /* Put both images together. */
            final BufferedImage stichedImageBuffer = new BufferedImage(mainImage.getWidth(), mainImage.getHeight() + exampleImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            final Graphics graphic = stichedImageBuffer.getGraphics();
            graphic.drawImage(exampleImage, 0, 0, null);
            graphic.drawImage(mainImage, 0, exampleImage.getHeight(), null);
            final byte[] image = IconIO.toJpgBytes(stichedImageBuffer);
            if (image == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final File stitchedImageOutput = this.getLocalCaptchaFile(".png");
            IO.writeToFile(stitchedImageOutput, image);
            final MultiClickedPoint c = this.getMultiCaptchaClickedPoint(stitchedImageOutput, link, "Click the characters in order and confirm.");
            final int[] x = c.getX();
            final int[] y = c.getY();
            logger.info("User has clicked " + x.length + " times");
            final StringBuilder sb = new StringBuilder();
            for (int index = 0; index < x.length; index++) {
                // TODO: Correct Y coordinates which are wrong due to the example image we've merged with the main image.
                final int coordX = x[index];
                final int coordY = y[index];
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(Integer.toString(coordX));
                sb.append(",");
                sb.append(Integer.toString(coordY));
            }
            if (true) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // TODO
            final UrlQuery query = new UrlQuery();
            query.add("dots", Encoding.urlEncode(sb.toString()));
            query.add("key", Encoding.urlEncode(captcha_key));
            query.add("v", Long.toString(System.currentTimeMillis()));
            brc.postPage("/captcha", query);
            if (brc.getHttpConnection().getResponseCode() == 400) {
                /* Somes with json response: {"msg":"Verification failed"} */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            /* Access that same URL. Now we should get downloadable direct-urls. */
            br.getPage(nextStepURL);
            final String[][] qualities = br.getRegex("\"(https?://[^\"]+=\"[^>]*>(\\d+)p</a>").getMatches();
            String dllink = br.getRegex("").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(null));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        } else {
            /* Premium accounts do not have captchas */
            return false;
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, null), this.getMaxChunks(null));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}