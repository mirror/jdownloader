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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mycloudplayers.com" }, urls = { "http://(www\\.)?mycloudplayers\\.com/.+" }, flags = { 0 })
public class MyCloudPlayersCom extends PluginForDecrypt {

    public MyCloudPlayersCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE_SONG = "http://(www\\.)?mycloudplayers\\.com/\\?id=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = Encoding.htmlDecode(param.toString());
        final String ids = new Regex(parameter, "\\&ids=([0-9,]+)\\&").getMatch(0);
        if (ids != null) {
            final String[] links = ids.split(",");
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleID : links) {
                decryptedLinks.add(createDownloadlink("http://api.soundcloud.com/tracks/" + singleID));
            }
        } else if (parameter.matches(TYPE_SINGLE_SONG)) {
            // br.getPage(parameter);
            // if (br.containsHTML("Track+Not+Found+on")) {
            // logger.info("Link offline: " + parameter);
            // return decryptedLinks;
            // }
            // final String finallink =
            // br.getRegex("\"permalink_url\":\"(http://soundcloud\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+(/[a-z0-9\\-_]+)?)\"").getMatch(0);
            // if (finallink != null) decryptedLinks.add(createDownloadlink(finallink));
            decryptedLinks.add(createDownloadlink("http://api.soundcloud.com/tracks/" + new Regex(parameter, "(\\d+)$").getMatch(0)));
        } else {
            logger.warning("Unknown linktype: " + parameter);
            return null;
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        return decryptedLinks;
    }

}
