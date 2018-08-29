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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 35010 $", interfaceVersion = 3, names = { "getcomics.info" }, urls = { "https://getcomics\\.info/[A-Za-z0-9_\\-]+/[A-Za-z0-9_\\-]+" })
public class GetComics extends PluginForDecrypt {
    public GetComics(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // Load page
        br.setFollowRedirects(true);
        Request request = br.createGetRequest(parameter);
        request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String page = br.getPage(request).toString();
        final String title = br.getRegex("<title>(.+?) &ndash; GetComics").getMatch(0);
        //
        int sectionStart = page.indexOf("<section class=\"post-contents\">");
        int sectionEnd = page.indexOf("<strong>Screenshots", sectionStart);
        if (sectionEnd < sectionStart) {
            sectionEnd = page.indexOf("<strong>Notes", sectionStart);
        }
        if (sectionEnd < sectionStart) {
            sectionEnd = page.indexOf("<section class=\"post-footer\">", sectionStart);
        }
        String section = page.substring(sectionStart, sectionEnd);
        String[][] regExMatches = new Regex(section, "a href=\"(.+?)\"").getMatches();
        for (String[] regExMatch : regExMatches) {
            String matchedURL = Encoding.htmlDecode(regExMatch[0]);
            decryptedLinks.add(createDownloadlink(matchedURL));
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(Encoding.htmlDecode(title));
            filePackage.addLinks(decryptedLinks);
        }
        //
        return decryptedLinks;
    }
}