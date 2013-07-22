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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "heaven666.org" }, urls = { "http://(www\\.)?heaven666\\.org/[a-z0-9\\-]+\\-\\d+\\.php" }, flags = { 0 })
public class Heaven666OrgDecrypter extends PluginForDecrypt {

    public Heaven666OrgDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("flashvars\\.Title = \"([^<>\"]*?)\"").getMatch(0);
        String externID = br.getRegex(">Link: <a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("style=\"position:absolute;right:0;top:0\"><a(.*?)<div class=media\\-details><div").getMatch(0);
        if (externID != null && !externID.contains("flashvars.")) {
            final String picLinks[] = new Regex(externID, "<img src=\"(//[^<>\"]*?)\"").getColumn(0);
            if (picLinks == null || picLinks.length == 0) {
                logger.warning("Decrypzter broken for link: " + parameter);
                return null;
            }
            for (final String piclink : picLinks) {
                decryptedLinks.add(createDownloadlink("directhttp://http:" + piclink));
            }
            if (filename != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(filename.trim()));
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://player\\.vimeo\\.com/video/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://(www\\.)?youtube\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("heaven666.org/", "heaven666.orgdecrypted/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}