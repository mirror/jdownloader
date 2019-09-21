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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "voirfilms.ec" }, urls = { "https?://(?:www\\.)?voirfilms\\.ec/[^/]+\\.html?" })
public class VoirFilms extends PluginForDecrypt {
    public VoirFilms(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Load page
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>(?:\\s*film\\s+)?([^<]+)(?:\\s+streaming vf\\s*)").getMatch(0);
        final String[] links = br.getRegex("(?:href|data-src)\\s*=\\s*\"((?!javascript)[^\"]+)\"[^>]+target\\s*=\\s*\"filmPlayer").getColumn(0);
        if (links != null && links.length > 0) {
            for (String link : links) {
                link = Encoding.htmlDecode(link);
                if (link.contains("/video.php?")) {
                    final Browser br2 = br.cloneBrowser();
                    br2.getPage(link);
                    String linkRedirect = br2.getRedirectLocation();
                    String[] contentLinks = HTMLParser.getHttpLinks(StringUtils.valueOrEmpty(br2.getRegex("<meta[^>]+http-equiv\\s*=\\s*\"refresh\"([^>]+)>").getMatch(0)), null);
                    String linkContent = contentLinks.length > 0 ? contentLinks[0] : null;
                    link = firstNotEmpty(linkRedirect, linkContent, link);
                }
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (fpName != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(Encoding.htmlDecode(fpName));
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public static String firstNotEmpty(String... strings) {
        for (String s : strings) {
            if (StringUtils.isNotEmpty(s)) {
                return s;
            }
        }
        return null;
    }
}