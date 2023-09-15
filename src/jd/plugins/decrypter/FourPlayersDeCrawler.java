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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4players.de" }, urls = { "https?://(?:www\\.)?4players\\.de/\\S*/download/([0-9]+)/([01]/)?index\\.html?" })
public class FourPlayersDeCrawler extends PluginForDecrypt {
    public FourPlayersDeCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String fileid = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        // Open URL which redirects immediately to the file
        br.setFollowRedirects(false);
        br.getPage("https://www.4players.de/cs.php/download_start/-/download/" + fileid + "/0/index.html");
        br.getPage("/download_start/-/download/" + fileid + "/1/index.html");
        // final String finallink = br.getRedirectLocation();
        final String directlink = "https://www." + this.getHost() + "/cs.php/download_start/-/download/" + fileid + "/1/index.html";
        // Add to decrypted links - we use http://ftp.freenet instead of
        // ftp://freenet which works
        ret.add(createDownloadlink("directhttp://" + directlink));
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}