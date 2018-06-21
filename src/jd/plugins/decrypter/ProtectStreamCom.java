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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-stream.com" }, urls = { "https?://(www\\.)?protect\\-stream\\.com/(PS_(DL|SM)|VID)_[A-Za-z0-9\\-_]+" })
public class ProtectStreamCom extends PluginForDecrypt {
    public ProtectStreamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String prtcid = new Regex(parameter, "protect\\-stream\\.com/PS_(?:DL|SM)_([A-Za-z0-9\\-_]+)").getMatch(0);
        if (parameter.contains("VID")) {
            br.getPage(parameter);
            br.followRedirect();
            final String cheap = br.getRegex("var k=\"([^<>\"]*?)\";").getMatch(0);
            if (cheap == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            this.sleep(5 * 1000l, param);
            br.postPage("/secur2.php", "k=" + Encoding.urlEncode(cheap));
        } else if (parameter.contains("PS_SM")) {
            br.getPage(parameter);
            br.followRedirect();
            final String cheap = br.getRegex("var k=\"([^<>\"]*?)\";").getMatch(0);
            if (cheap == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            this.sleep(10 * 1000l, param);
            br.postPage("/secur2_sm.php", "k=" + Encoding.urlEncode(cheap));
        } else {
            // br.getPage(parameter);
            // br.getPage("http://www.protect-stream.com/frame.php?u=" + prtcid);
            br.getPage("https://www.protect-stream.com/w.php?u=" + prtcid);
            br.followRedirect();
            final String cheap = br.getRegex("var k=\"([^<>\"]*?)\";").getMatch(0);
            if (cheap == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            this.sleep(10 * 1000l, param);
            br.postPage("/secur.php", "k=" + Encoding.urlEncode(cheap));
        }
        String finallink = br.getRegex("class=\"button\"\\s*(?:href|src)\\s*=\\s*\"(https?[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("(?:href|src)\\s*=\\s*\"(https?[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
