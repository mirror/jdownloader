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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ThisvidComAlbums extends PluginForDecrypt {
    public ThisvidComAlbums(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "thisvid.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(/albums/(?!most-popular)([a-z0-9\\-]+)/|/playlist/\\d+/videos?/[^/\\?#]+/?)");
        }
        return ret.toArray(new String[0]);
    }

    private static final String PATTERN_DIRECT_URL = "(?i)https?://media\\.thisvid\\.com/contents/albums/main/(\\d+x\\d+)/(\\d+)/(\\d+)/(\\d+)\\.jpg";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches("(?i).*/playlist/\\d+/.+")) {
            final String videoURL = parameter.replaceFirst("/playlist/\\d+/videos?/", "/videos/");
            final DownloadLink playlistVideo = createDownloadlink(videoURL);
            decryptedLinks.add(playlistVideo);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String mainImageURL = br.getRegex("<div class=\"photo-holder\">\\s*<img src=\"(https?://[^\"]+)\"").getMatch(0);
        final String higherResolution;
        if (mainImageURL != null && mainImageURL.matches(PATTERN_DIRECT_URL)) {
            higherResolution = new Regex(mainImageURL, PATTERN_DIRECT_URL).getMatch(0);
        } else {
            /* Fallback */
            higherResolution = "700x525";
        }
        String fpName = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0).replace("-", " ");
        final String[] thumbnailURLs = br.getRegex("class=\"tumbpu\">\\s*<img src=\"(https?://[^\"]+)\"").getColumn(0);
        if (thumbnailURLs == null || thumbnailURLs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int imageNumber = 1;
        for (String directLink : thumbnailURLs) {
            final String currentResolution = new Regex(directLink, PATTERN_DIRECT_URL).getMatch(0);
            if (currentResolution != null) {
                /* Alter thumbnail URLs to fullsize image URLs. */
                directLink = directLink.replace(currentResolution, higherResolution);
            }
            final DownloadLink direct = createDownloadlink(directLink);
            direct.setFinalFileName(fpName + "_" + imageNumber + ".jpg");
            direct.setAvailable(true);
            decryptedLinks.add(direct);
            imageNumber += 1;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
