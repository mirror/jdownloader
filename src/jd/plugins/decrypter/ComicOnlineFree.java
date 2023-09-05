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
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ComicOnlineFree extends antiDDoSForDecrypt {
    public ComicOnlineFree(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "comiconlinefree.net", "comiconlinefree.com" });
        ret.add(new String[] { "viewcomics.org", "viewcomics.co", "viewcomics.me" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[\\w\\-]+/issue-[\\w\\-]+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        String url = param.getCryptedUrl();
        br.setFollowRedirects(true);
        if (!new Regex(url, "(?i)/(comic/|issue-)").patternFind()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (new Regex(url, "(?i)/[^/]+/issue-\\d+$").patternFind() && !url.endsWith("/full")) {
            url += "/full";
        }
        getPage(url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>\\s*([^<]+)Comic\\s*-\\s*Read\\s*[^<]+\\s+Online\\s+For\\s+Free").getMatch(0);
        if (StringUtils.isEmpty(fpName)) {
            fpName = br.getRegex("<title>\\s*([^>]+)\\s+-\\s+Read\\s+[^<]+\\s+Online\\s+").getMatch(0);
        }
        FilePackage fp = null;
        if (StringUtils.isNotEmpty(fpName)) {
            fpName = Encoding.htmlDecode(fpName).trim();
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        } else {
            /* Fallback */
            final String urlPath = br._getURL().getPath();
            fpName = urlPath.substring(1).replace("-", " ");
            fp = FilePackage.getInstance();
            fp.setName(fpName);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String[] links = br.getRegex("<a[^>]+class\\s*=\\s*\"ch-name\"[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                final DownloadLink dl = createDownloadlink(link);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
            }
        }
        String[] images = br.getRegex("<img[^>]+class\\s*=\\s*\"[^\"]+chapter_img\"[^>]+data-original\\s*=\\s*\"([^\"]+)\"[^>]*>").getColumn(0);
        if (images == null || images.length == 0) {
            /* 2023-08-30: viewcomics.co */
            images = br.getRegex("<img src=\"(https?://[^\"]+)\" alt=\"").getColumn(0);
        }
        if (images != null && images.length > 0) {
            final String chapter_name = fpName;
            final int padlength = StringUtils.getPadLength(images.length);
            int page = 1;
            for (String imageURL : images) {
                String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page++);
                imageURL = Encoding.htmlDecode(imageURL);
                final DownloadLink dl = createDownloadlink(DirectHTTP.createURLForThisPlugin(imageURL));
                String ext = Plugin.getFileNameExtensionFromURL(imageURL, ".jpg");
                if (chapter_name != null) {
                    dl.setFinalFileName(chapter_name + "_" + page_formatted + ext);
                } else {
                    dl.setFinalFileName(page_formatted + ext);
                }
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                dl.setAvailable(true);
                ret.add(dl);
            }
        }
        if (ret.isEmpty()) {
            if (!br.containsHTML("class=\"chapter_select_col\"")) {
                /* Empty page e.g. https://comiconlinefree.net/moon-knight-2016/issue-190 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
        }
        return ret;
    }
}