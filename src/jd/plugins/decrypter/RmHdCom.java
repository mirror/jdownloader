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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "romhood.com" }, urls = { "http://(www\\.)?romhood\\.com/category/[^<>\"/]+/\\d+_\\-_[^<>\"/]+\\d+\\.html" }, flags = { 0 })
public class RmHdCom extends PluginForDecrypt {

    public RmHdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>Romhood\\.com \\- \\d+ \\- ([^<>\"]*?)</title>").getMatch(0);
        final String postLink = "http://romhood.com/?id=" + new Regex(parameter, "(\\d+)\\.html").getMatch(0) + "&location=mirror";
        final String[] postVars = br.getRegex("NAME=\"m\" VALUE=\"([^<>\"]*?)\"").getColumn(0);
        if (postVars == null || postVars.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String postVar : postVars) {
            br.postPage(postLink, "m=" + postVar);
            final String finallink = br.getRegex("<P><FORM ACTION=\"([^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
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