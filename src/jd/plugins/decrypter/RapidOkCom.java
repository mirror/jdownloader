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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidok.com" }, urls = { "http://(www\\.)?rapidok\\.com/download/[A-Za-z0-9_\\-]+" }, flags = { 0 })
public class RapidOkCom extends PluginForDecrypt {

    public RapidOkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("<b>Title:</b>([^<>\"\\']+)<br").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<p class=\"s_result\"><span>Download:([^<>\"\\']+)</span></p>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>Download ([^<>\"\\']+) from .*?</title>").getMatch(0);
            }
        }
        br.setFollowRedirects(false);
        int counter = 1;
        String lastFinallink = null;
        while (true) {
            br.postPage(br.getURL(), "part=" + counter + "&hash=" + new Regex(parameter, "rapidok\\.com/download/(.+)").getMatch(0));
            final String finallink = br.getRedirectLocation();
            if (finallink == null) break;
            if (finallink.equals(lastFinallink)) break;
            decryptedLinks.add(createDownloadlink(finallink));
            lastFinallink = finallink;
            counter++;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}