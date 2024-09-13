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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.YoutubeDashV2;

/**
 *
 * @author butkovip
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, urls = {}, names = {})
public class TopkySk extends PluginForDecrypt {
    public TopkySk(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "topky.sk" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/cl?/\\d+/\\d+/([a-zA-Z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        final String contenturl = cryptedLink.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // extract img.zoznam.sk like vids
        String[][] links = br.getRegex("fo\\.addVariable[(]\"file\", \"(.*?)\"[)]").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                if (null != link && 1 == link.length && null != link[0] && 0 < link[0].length()) {
                    ret.add(createDownloadlink(link[0]));
                }
            }
        }
        // extract youtube links
        links = br.getRegex("<PARAM NAME=\"movie\" VALUE=\"http://www.youtube.com/v/(.*?)&").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                if (null != link && 1 == link.length && null != link[0] && 0 < link[0].length()) {
                    ret.add(createDownloadlink(YoutubeDashV2.generateContentURL(link[0])));
                }
            }
        }
        // extract instagram links
        final String[] instagramlinks = br.getRegex("data-instgrm-permalink=\"(https?://[^\"]+)").getColumn(0);
        if (null != instagramlinks && 0 < instagramlinks.length) {
            for (final String instagramlink : instagramlinks) {
                ret.add(createDownloadlink(instagramlink));
            }
        }
        // extract topky.sk http vids
        final String finallink = br.getRegex("<source src=\"(http[^<>\"]*?)\"").getMatch(0);
        if (finallink != null) {
            ret.add(createDownloadlink(DirectHTTP.createURLForThisPlugin(finallink)));
        }
        /* 2022-06-14: Selfhosted hls */
        final String[] hlsplaylists = br.getRegex("(https?://img\\.topky\\.sk/video/\\d+/master\\.m3u8)").getColumn(0);
        for (final String hlsplaylist : hlsplaylists) {
            ret.add(createDownloadlink(hlsplaylist));
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String urlSlug = br.getURL().substring(br.getURL().lastIndexOf("/") + 1);
        final String title = urlSlug.replace("-", " ").trim();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}