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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.appwork.storage.TypeRef;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
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
        return -5;
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
            /* Extension is not always given but we can be sure that this filehost is only hosting video content! */
            filename = this.applyFilenameExtension(filename, ".mp4");
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
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        if (storedDirecturl != null) {
            logger.info("Attempting to re-use stored directurl");
            /* This header is very important. Without it server will return error 404. */
            br.getHeaders().put("Referer", storedDirecturl);
            /* Alternative which also works: */
            // br.getHeaders().put("Referer", "https://" + this.getHost());
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, storedDirecturl, this.isResumeable(link, null), this.getMaxChunks(null));
        } else {
            requestFileInformation(link);
            final String nextStepURL = "/d/" + this.getFID(link);
            if (!br.containsHTML(nextStepURL)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
            final int fontSizeCaptchaDescription = 9;
            graphic.setFont(new Font(ImageProvider.getDrawFontName(), Font.BOLD, fontSizeCaptchaDescription));
            final String captchaExplanationText;
            final String[] textRows;
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                captchaExplanationText = "Klicke auf alle Zeichen aus dem Beispielbild und bestätige durch Klick auf 'Senden'.";
                textRows = new String[] { "Klicke die Zeichen aus dem", "Beispielbild der Reihenfolge", "nach an und bestätige mit Ok.", "<--- Beispielbild" };
            } else {
                captchaExplanationText = "Click the characters in order and confirm by clicking on 'Send'.";
                textRows = new String[] { "Click on all characters you can", "see inside the example image", "in order. Hit Ok to confirm.", "<--- Example image" };
            }
            try {
                int textRowPosition = 1;
                for (final String textRow : textRows) {
                    graphic.drawString(textRow, exampleImage.getWidth() + 1, fontSizeCaptchaDescription * textRowPosition);
                    textRowPosition++;
                }
            } catch (final Throwable e) {
                /* User doesn't have any fonts installed on OS. */
            }
            final byte[] image = IconIO.toJpgBytes(stichedImageBuffer);
            if (image == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final File stitchedImageOutput = this.getLocalCaptchaFile(".png");
            IO.writeToFile(stitchedImageOutput, image);
            final MultiClickedPoint c = this.getMultiCaptchaClickedPoint(stitchedImageOutput, link, captchaExplanationText);
            final int[] x = c.getX();
            final int[] y = c.getY();
            logger.info("User has clicked " + x.length + " times");
            final StringBuilder sb = new StringBuilder();
            for (int index = 0; index < x.length; index++) {
                final int coordX = x[index];
                /* Correct Y position as our example image is part of the clickable area. */
                final int coordY = y[index] - exampleImage.getHeight();
                if (coordY < 0) {
                    logger.info("Invalid captcha answer: User clicked in area of example image. This answer cannot be correct!");
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(Integer.toString(coordX));
                sb.append(",");
                sb.append(Integer.toString(coordY));
            }
            final UrlQuery query = new UrlQuery();
            query.add("dots", Encoding.urlEncode(sb.toString()));
            query.add("key", Encoding.urlEncode(captcha_key));
            query.add("v", Long.toString(System.currentTimeMillis()));
            brc.postPage("/captcha", query);
            if (brc.getHttpConnection().getResponseCode() == 400) {
                /* Comes with json response: {"msg":"Verification failed"} */
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            // Valid answer: {"token":"<hash[a-f]{32}>"}
            /* Access that same URL. Now we should get downloadable direct-urls. */
            br.getPage(nextStepURL);
            final String[][] qualities = br.getRegex("\"(https?://[^\"]+)\"[^>]*>(\\d+)p</a>").getMatches();
            if (qualities == null || qualities.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String dllink = null;
            int maxHeight = 0;
            for (final String[] qualityInfo : qualities) {
                final int height = Integer.parseInt(qualityInfo[1]);
                if (dllink == null || height > maxHeight) {
                    dllink = qualityInfo[0];
                    maxHeight = height;
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Chosen qualily: " + maxHeight + "p");
            dllink = Encoding.htmlOnlyDecode(dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(null));
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Failed to re-use stored directurl");
            } else {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc != null && acc.getType() == AccountType.PREMIUM) {
            return false;
        } else {
            return true;
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