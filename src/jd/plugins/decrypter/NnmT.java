//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3,

names = { "anonym.to", "hiderefer.com", "blankrefer.com", "anon.click" },

urls = { "https?://[\\w\\.]*?anonym\\.to/\\?.+", "https?://[\\w\\.]*?hiderefer\\.com/\\?.+", "https?://[\\w\\.]*?blankrefer\\.com/\\?.+", "https?://[\\w\\.]*?anon\\.click/.+" },

flags = { 0 })
public class NnmT extends PluginForDecrypt {

    public NnmT(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String url = new Regex(parameter, "https?://[^/]+/\\??(.+)").getMatch(0);
        // some allow www. or domain without any protocol prefixes.
        if (!url.matches("^(?:https?|ftp)://.+$")) {
            url = new Regex(parameter, "^https?://").getMatch(-1) + url;
        }
        links.add(createDownloadlink(url));
        return links;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }

}