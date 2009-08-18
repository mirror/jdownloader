//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "counterstrike.de" }, urls = { "http://[\\w\\.]*?4players\\.de/\\S*/download/[0-9]+/([01]/)?index\\.html?" }, flags = { 0 })
public class CntrstrkD extends PluginForDecrypt {

    public CntrstrkD(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        // Get fileid
        String fileid = new Regex(param.toString(), "http://[\\w\\.]*?4players\\.de/\\S*/download/([0-9]+)/([01]/)?index\\.html?").getMatch(0);

        // Open URL which redirects immediately to the file
        br.setFollowRedirects(false);
        br.getPage("http://www.4players.de/cs.php/download_start/-/download/" + fileid + "/1/index.html");

        // Add to decrypted links - we use http://ftp.freenet instead of
        // ftp://freenet which works
        decryptedLinks.add(createDownloadlink(br.getRedirectLocation().replaceFirst("ftp", "httpviajd")));

        return decryptedLinks;
    }

    // @Override

}
