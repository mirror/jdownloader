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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 35010 $", interfaceVersion = 3, names = { "kickassanime.io" }, urls = { "https://www\\d*.kickassanime.io/(?:anime)/[A-Za-z0-9\\-]+\\-\\d+/episode\\-[0-9\\-/]+" })
public class KickAssAnime extends PluginForDecrypt {
    public KickAssAnime(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:").replaceAll("[/]+$", "");
        br.setFollowRedirects(true);
        final String page = br.getPage(parameter);
        String showTitle = br.getRegex("<a href=\"https://www\\d*.kickassanime.io/(?:anime)/[A-Za-z0-9\\-]+\\-\\d+\" title=\"(.+?)\">").getMatch(0);
        String episodeTitle = br.getRegex(">([^>]+)</h1>").getMatch(0);
        String[][] iframeURLMatches = br.getRegex("https://animo-pace-stream.io/[a-zA-Z0-9]+/player.php[^\"]+").getMatches();
        // Get the IFrame details, which might contain our target URLs
        for (String[] iframeURLMatch : iframeURLMatches) {
            String iframeURL = iframeURLMatch[0];
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(iframeURL)));
            if (iframeURL != null) {
                final Browser brIframe = br.cloneBrowser();
                brIframe.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                String iframePage = brIframe.getPage(iframeURL);
                if (brIframe.getHttpConnection().getResponseCode() == 403) {
                    brIframe.getPage(Encoding.htmlDecode(iframeURL));
                    if (brIframe.getHttpConnection().getResponseCode() == 403) {
                        throw new DecrypterException("Decrypter broken for link: " + parameter);
                    }
                }
                final Browser br2 = br.cloneBrowser();
                String videoURL = brIframe.getRegex("window.location = '(.+?)'").getMatch(0); // Fed to the IFrame's JWPlayer plugin
                if (videoURL != null) {
                    br2.getPage(videoURL);
                    String dc = Encoding.Base64Decode(br2.getRegex("Base64.decode\\(\"([^>\"]+)\"").getMatch(0));
                    videoURL = new Regex(dc, "src=([^<>\"]+) allowfullscreen").getMatch(0);
                    if (videoURL == null) {
                        videoURL = new Regex(dc, "file:\"([^<>\"]+)\"").getMatch(0);
                    }
                    if (videoURL != null) {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(videoURL)));
                    }
                }
                String dataURL = brIframe.getRegex("data=(.+?)&").getMatch(0);
                if (dataURL != null) {
                    br2.getPage(dataURL);
                    String dc = Encoding.Base64Decode(br2.getRegex("Base64.decode\\(\"([^>\"]+)\"").getMatch(0));
                    if (dc != null) {
                        decryptedLinks.add(createDownloadlink(dc));
                    }
                }
            }
        }
        // In some cases we have player HTML with a dropdown list to switch between different hoster implementations.
        String[][] selectOptionMatches = br.getRegex("<option value=\"https://animo-pace-stream\\.io/axplayer/player\\.php\\?[^>]+").getMatches();
        if (selectOptionMatches.length > 0) {
            for (String[] selectOptionMatch : selectOptionMatches) {
                String selectOptionURL = Encoding.htmlDecode(new Regex(selectOptionMatch[0], "\"(.+?)$").getMatch(0)).trim();
                decryptedLinks.add(createDownloadlink(selectOptionURL));
            }
        }
        // In some we have DDL links that will redirect to the target.
        String[][] directDownloadMatches = br.getRegex("https://animo-pace-stream\\.io/redirector\\.php\\?[^\"]+").getMatches();
        if (directDownloadMatches.length > 0) {
            for (String[] directDownloadMatch : directDownloadMatches) {
                String directDownloadURL = Encoding.htmlDecode(directDownloadMatch[0].trim());
                decryptedLinks.add(createDownloadlink(directDownloadURL));
            }
        }
        // In some cases we can just translate the target URL from Base64-encoded URL parameters
        directDownloadMatches = br.getRegex("link=(.+?)[\"/&]").getMatches();
        if (directDownloadMatches.length > 0) {
            for (String[] directDownloadMatch : directDownloadMatches) {
                String directDownloadURL = Encoding.Base64Decode(directDownloadMatch[0]);
                if (directDownloadURL != null && StringUtils.containsIgnoreCase(directDownloadURL, "http")) {
                    decryptedLinks.add(createDownloadlink(directDownloadURL));
                }
            }
        }
        // Sometimes the hoster selection is in a HTML table
        directDownloadMatches = br.getRegex("data-video=\"(.+?)\">").getMatches();
        if (directDownloadMatches.length > 0) {
            for (String[] directDownloadMatch : directDownloadMatches) {
                String directDownloadURL = Encoding.htmlDecode(directDownloadMatch[0]);
                if (directDownloadURL != null) {
                    directDownloadURL = directDownloadURL.replaceAll("^[/]+", "");
                    decryptedLinks.add(createDownloadlink(directDownloadURL));
                }
            }
        }
        // Special view sometimes used for self-embeds
        if (!parameter.contains("episode/addWatch")) {
            decryptedLinks.add(createDownloadlink(parameter + "/episode/addWatch"));
        }
        if (!episodeTitle.isEmpty()) {
            for (DownloadLink decryptedLink : decryptedLinks) {
                decryptedLink.setForcedFileName(episodeTitle.trim() + ".mp4");
                decryptedLink.setComment(decryptedLink.getContentUrlOrPatternMatcher());
            }
        }
        if (!showTitle.isEmpty()) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(showTitle);
            filePackage.addLinks(decryptedLinks);
        }
        //
        for (DownloadLink decryptedLink : decryptedLinks) {
            getLogger().info(decryptedLink.getContentUrlOrPatternMatcher());
        }
        return decryptedLinks;
    }
}