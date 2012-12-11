//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cineon.com" }, urls = { "http://(www\\.)?c1neon\\.com/download\\-[\\w\\-]+\\d+\\.html" }, flags = { 0 })
public class CineonCom extends PluginForDecrypt {

    public CineonCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        br.setFollowRedirects(false);
        String fpName = br.getRegex("title=\'([^\']+)").getMatch(0);
        if (fpName != null) fpName = Encoding.htmlDecode(fpName);
        String allLinks = br.getRegex("var subcats = \\{([^;]+)").getMatch(0);
        if (allLinks != null) {
            allLinks = allLinks.replaceAll("\\\\|\"", "");
            for (String q[] : new Regex(allLinks, "(XVID|720p|1080p):\\{(.*?)\\}\\}\\}").getMatches()) {
                for (String s[] : new Regex(q[1], "([\\w\\-]+):\\[(\\[.*?\\])\\]").getMatches()) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName + "@" + s[0].trim() + "__" + q[0]);
                    for (String ss[] : new Regex(s[1], "\\[(\\d),(\\w+),.*?,(.*?),\\d+\\]").getMatches()) {
                        String link = ss[2].trim();
                        if ("redirect".equalsIgnoreCase(ss[1].trim())) link = getRedirectUrl(link);
                        final DownloadLink dl = createDownloadlink(link);
                        fp.add(dl);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            /* does not exist in 09581 */
                        }
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private String getRedirectUrl(String s) throws Exception {
        br.getPage(s);
        return br.getRedirectLocation();
    }

}