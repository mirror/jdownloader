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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ypaste.com" }, urls = { "http://(www\\.)?ypaste\\.com/\\d+(/\\d+)?" }, flags = { 0 })
public class YPasteCom extends PluginForDecrypt {

    public YPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** TODO: Maybe implement API: http://ypaste.com/doc/api/ */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">The contents of this paste have been hidden<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">This paste is password protected<")) {
            for (int i = 1; i <= 3; i++) {
                final String pass = getUserInput(null, param);
                if (pass == null || pass.equals("")) continue;
                br.postPage(br.getURL(), "password=" + Encoding.urlEncode(pass));
                if (br.containsHTML(">This paste is password protected<")) continue;
                break;
            }
            if (br.containsHTML(">This paste is password protected<")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        br.getPage(parameter + "/raw/");
        final String[] links = HTMLParser.getHttpLinks(br.toString(), "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String linkPart = new Regex(parameter, "(ypaste\\.com/.+)").getMatch(0);
        for (final String singleLink : links) {
            if (!singleLink.contains(linkPart)) decryptedLinks.add(createDownloadlink(singleLink));
        }
        return decryptedLinks;
    }

}
