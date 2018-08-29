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
import java.util.regex.Pattern;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dramagalaxy.tv" }, urls = { "https?://www.dramagalaxy.tv/[A-Za-z0-9_\\-/?=]+" })
public class DramaGalaxy extends PluginForDecrypt {
    public DramaGalaxy(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String parameterBase = parameter.replaceFirst("\\?page\\=\\d+", "");
        if (new Regex(parameterBase, "/(biography|category|thumbs|sitemap|img|xmlrpc|fav|images|ads|gga)/").count() == 0) {
            br.setFollowRedirects(true);
            Request request = br.createGetRequest(parameter);
            request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            String page = br.getPage(request);
            String title = br.getRegex("<title>(.*?)[^|]+</title>").getMatch(0);
            if (title != null) {
                title = title.toString().replace("|", "").trim();
            }
            // Handle tabs in case the video is split into multiple parts (JD default behavior would grab only the currently visible tab).
            int beginContent = page.indexOf("<div id=\"content\">");
            int beginFooter = page.indexOf("<div id=\"footer\">", beginContent);
            String contentBlock = page.substring(beginContent, beginFooter);
            String[][] regExMatches = new Regex(contentBlock, ">[\r\n ]*<li>[\r\n ]*<a href=\"(.+?)\"[\\ >]").getMatches();
            if (regExMatches.length > 0) {
                for (String[] regExMatch : regExMatches) {
                    final String matchedURL = Encoding.htmlDecode(regExMatch[0]);
                    decryptedLinks.add(createDownloadlink(matchedURL));
                }
            }
            // Handle currently visible IFrame videos while we're already here anyway.
            regExMatches = br.getRegex("><iframe src=\"(.+?)\"[\\ >]").getMatches();
            if (regExMatches.length > 0) {
                for (String[] regExMatch : regExMatches) {
                    final String matchedURL = Encoding.htmlDecode(regExMatch[0]);
                    decryptedLinks.add(createDownloadlink(matchedURL));
                }
            }
            // The HTML5 player version has the file URLs embedded in a struct
            String[][] videoURLMatches = br.getRegex("file:\"(.+?)\"").getMatches();
            for (String[] videoURLMatch : videoURLMatches) {
                String videoURL = Encoding.htmlDecode(videoURLMatch[0]);
                decryptedLinks.add(createDownloadlink(videoURL));
            }
            regExMatches = br.getRegex(Pattern.quote(parameterBase) + "/[A-Za-z0-9_\\-]+").getMatches();
            if (regExMatches.length > 0) {
                // On index pages, grab everything resembling listed episodes
                for (String[] regExMatch : regExMatches) {
                    final String matchedURL = Encoding.htmlDecode(regExMatch[0]);
                    // We only want episodes for this show, none of the other crud.
                    decryptedLinks.add(createDownloadlink(matchedURL));
                }
                // Handle possible pagination of the episode list
                regExMatches = br.getRegex("href=\"" + Pattern.quote(parameterBase) + "(.*?)\" onclick=\"window\\.location\\.href").getMatches();
                for (String[] regExMatch : regExMatches) {
                    final String matchedURL = Encoding.htmlDecode(regExMatch[0]);
                    // We only want episodes for this show, none of the other crud.
                    decryptedLinks.add(createDownloadlink(matchedURL));
                }
            }
            if (!title.isEmpty()) {
                final FilePackage filePackage = FilePackage.getInstance();
                filePackage.setName(Encoding.htmlDecode(title));
                filePackage.setComment(title);
                filePackage.addLinks(decryptedLinks);
            }
        }
        //
        return decryptedLinks;
    }
}