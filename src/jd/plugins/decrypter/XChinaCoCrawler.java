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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.XChinaCo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XChinaCoCrawler extends PluginForDecrypt {
    public XChinaCoCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xchina.co" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/(photo|video)/id-([a-f0-9]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String contenturl = param.getCryptedUrl();
        final Regex urlinfo = new Regex(contenturl, this.getSupportedLinks());
        final String urltype = urlinfo.getMatch(0);
        final String contentID = urlinfo.getMatch(1);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = null;
        if (urltype.equalsIgnoreCase("photo")) {
            final HashSet<String> dupes = new HashSet<String>();
            final String[] urls = br.getRegex("(https?://img\\.[^<>\"\\']+)").getColumn(0);
            for (String url : urls) {
                if (!dupes.add(url)) {
                    continue;
                }
                /* Change thumbnail URL to fullsize image-URL. */
                url = url.replaceFirst("_600x0\\.jpg$", ".jpg");
                final DownloadLink photo = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                photo.setAvailable(true);
                ret.add(photo);
            }
            title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        } else {
            /* Selfhosted content / stream which will be handled by hosterplugin. */
            final DownloadLink video = this.createDownloadlink(contenturl);
            XChinaCo.scanFileInfo(br, video);
            String filesizeStr = null;
            final String torrentDownloadurl = br.getRegex("\"(/torrent/id-[a-f0-9]+\\.html)\"").getMatch(0);
            if (torrentDownloadurl != null) {
                logger.info("Looking for external downloadurls");
                // final String titleFromHTML2 = br.getRegex("class=\"title\">([^<]+)</div>").getMatch(0);
                /* Grab external downloadlinks and filesize */
                br.getPage(torrentDownloadurl);
                filesizeStr = br.getRegex("class=\"fa fa-file\" title=\"文件大小\"></i>([^<]+)</div>").getMatch(0);
                final String stepContinueToExternalDownloadurlURL = br.getRegex("\"(/download/id-[a-f0-9]+\\.html)\"").getMatch(0);
                if (stepContinueToExternalDownloadurlURL != null) {
                    br.getPage(stepContinueToExternalDownloadurlURL);
                    final String externalDownloadlink = this.br.getRegex("class=\"_download\"><div><a href=\"(https?://[^\"]+)\"").getMatch(0);
                    if (externalDownloadlink != null) {
                        logger.info("Found external downloadurl: " + externalDownloadlink);
                        ret.add(createDownloadlink(externalDownloadlink));
                    } else {
                        logger.warning("Failed to find external downloadurl");
                    }
                } else {
                    logger.warning("Failed to find stepContinueToExternalDownloadurlURL");
                }
            }
            if (filesizeStr != null) {
                video.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            video.setAvailable(true);
            ret.add(video);
            final String titleFromHTML = video.getStringProperty(XChinaCo.PROPERTY_TITLE);
            title = titleFromHTML;
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        } else {
            /* Fallback */
            title = contentID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
