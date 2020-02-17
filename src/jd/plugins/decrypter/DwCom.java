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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dw.com" }, urls = { "https?://(?:www\\.)?dw\\.com/[a-z]{2}/([^/]+)/av-(\\d+)" })
public class DwCom extends PluginForDecrypt {
    public DwCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String lid = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        br.getPage("https://www." + this.getHost() + "/playersources/v-" + lid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String url_title = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        url_title = Encoding.htmlDecode(url_title);
        final String[] links = br.getRegex("(http[^\"]+\\.mp4)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String title_for_filename = url_title.replace("-", " ");
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
            final String quality_part = new Regex(singleLink, "(_[a-z]+_[a-z]+\\.mp4)").getMatch(0);
            if (quality_part != null) {
                dl.setFinalFileName(title_for_filename + quality_part);
            }
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_title);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
