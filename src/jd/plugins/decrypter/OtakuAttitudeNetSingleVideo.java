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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OtakuAttitudeNetSingleVideo extends PluginForDecrypt {
    public OtakuAttitudeNetSingleVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "otaku-attitude.net" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://forum\\." + buildHostsPatternPart(domains) + "/video/view/([a-z0-9\\-]+)/.*");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String mainVideoPartID = UrlQuery.parse(param.getCryptedUrl()).get("vid");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(urlSlug.replace("-", " ").trim());
        fp.setPackageKey("otakuattitude://singlevideo/" + urlSlug);
        final HashSet<String> videoPartsURLs = new HashSet<String>();
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        String skipPartID = null;
        for (final String url : urls) {
            final UrlQuery query = UrlQuery.parse(url);
            final String partID = query.get("vid");
            if (partID == null) {
                continue;
            }
            if (mainVideoPartID == null && skipPartID == null) {
                logger.info("Skipping ID of current video part: " + partID);
                skipPartID = partID;
                continue;
            } else if (partID.equals(skipPartID)) {
                continue;
            }
            if (!StringUtils.equals(partID, mainVideoPartID)) {
                videoPartsURLs.add(url);
            }
        }
        final ArrayList<DownloadLink> mainVideoPartResults = findVideoLinks(br);
        for (final DownloadLink mainVideoPartResult : mainVideoPartResults) {
            mainVideoPartResult._setFilePackage(fp);
            distribute(mainVideoPartResult);
            ret.add(mainVideoPartResult);
        }
        if (videoPartsURLs.size() > 0) {
            /* Crawl other video parts */
            int index = 0;
            for (final String videoPartURL : videoPartsURLs) {
                logger.info("Crawling  video part " + index + "/" + videoPartsURLs.size() + " | " + videoPartURL);
                br.getPage(videoPartURL);
                final ArrayList<DownloadLink> thisVideoPartResults = findVideoLinks(br);
                for (final DownloadLink thisVideoPartResult : thisVideoPartResults) {
                    thisVideoPartResult._setFilePackage(fp);
                    distribute(thisVideoPartResult);
                    ret.add(thisVideoPartResult);
                }
                if (this.isAbort()) {
                    logger.info("Aborted by user");
                    break;
                }
                index++;
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> findVideoLinks(final Browser br) throws PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        if (urls == null || urls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String url : urls) {
            /**
             * Some filenames contain mp4 two times -> This regex serves dual purpose: </br>
             * 1. Find mp4 links. </br>
             * 2. Set better filenames that we would get in browser.
             */
            final String mp4Filename = new Regex(url, "(?i)/([^/]*?\\.mp4)").getMatch(0);
            if (mp4Filename != null) {
                final DownloadLink link = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                link.setFinalFileName(Encoding.htmlDecode(mp4Filename).trim());
                link.setAvailable(true);
                ret.add(link);
            }
        }
        return ret;
    }
}
