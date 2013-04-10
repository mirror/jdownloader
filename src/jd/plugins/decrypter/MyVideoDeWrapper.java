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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "http://(www\\.)?myvideo\\.(de|at)/watch/\\d+(/\\w+)?" }, flags = { 0 })
public class MyVideoDeWrapper extends PluginForDecrypt {

    public MyVideoDeWrapper(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String nextLink = br.getRegex("<object type=(\'|\")application/x\\-shockwave\\-flash(\'|\") data=(\'|\")(http://[^\'\"]+)").getMatch(3);
        if (nextLink != null && nextLink.contains("sevenload.com")) {
            br.setFollowRedirects(false);
            br.getPage(nextLink);
            String redirect = br.getRedirectLocation();
            if (redirect != null) {
                redirect = new Regex(redirect, "configPath=(.*?)$").getMatch(0);
                if (redirect != null) {
                    br.getPage(Encoding.htmlDecode(redirect));
                    nextLink = br.getRegex("<link target=\"_blank\">(http://[^<]+)").getMatch(0);
                    if (nextLink != null) decryptedLinks.add(createDownloadlink(nextLink));
                }
            }
        } else {
            if (nextLink != null) logger.info("MyVideoDeWrapper: nextLink = " + nextLink);
            decryptedLinks.add(createDownloadlink(parameter.replaceFirst("http", "fromDecrypter")));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}