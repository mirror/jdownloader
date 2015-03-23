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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "asianspankee.com", "mediaboom.org", "bookgn.com", "vip-files.net", "url4u.org", "tunesies.com", "xtragfx.com", "psdkeys.com" },

urls = { "http://asianspankee\\.com/\\?goto=[A-Za-z0-9=%]+", "http://(www\\.)?mediaboom\\.org/engine/go\\.php\\?url=[A-Za-z0-9=%]+", "http://(www\\.)?bookgn\\.com/engine/go\\.php\\?url=[A-Za-z0-9=%]+", "http://(www\\.)?vip\\-files\\.net/download\\.php\\?e=[A-Za-z0-9=%]+", "http://www\\.url4u\\.org/[A-Za-z0-9=%]+", "https?://(www\\.)?tunesies\\.com/go/[a-zA-Z0-9_/\\+\\=\\-]+", "https?://(www\\.)?xtragfx\\.com/engine/go\\.php\\?url=[A-Za-z0-9=%]+", "https?://(www\\.)?psdkeys\\.com/engine/go\\.php\\?url=[A-Za-z0-9=%]+" },

flags = { 0, 0, 0, 0, 0, 0, 0, 0 })
public class BasicBase64Decrypter extends PluginForDecrypt {

    public BasicBase64Decrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Decrypts base64 strings...nothing spectacular here! */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String b64 = Encoding.htmlDecode(new Regex(parameter, "[A-Za-z]+=([A-Za-z0-9=%]+)$").getMatch(0));
        if (b64 == null) {
            b64 = Encoding.htmlDecode(new Regex(parameter, "([A-Za-z0-9=%]+)$").getMatch(0));
        }
        final String finallink = Encoding.Base64Decode(b64);
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}