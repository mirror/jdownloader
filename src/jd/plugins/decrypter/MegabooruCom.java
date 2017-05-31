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
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "megabooru.com" }, urls = { "https?://(?:www\\.)?megabooru\\.com/post/list/[^/]+/\\d+" })
public class MegabooruCom extends PluginForDecrypt {
    public MegabooruCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_single = this.br.getRegex("You should be redirected to <a href=\\'(/post/view/\\d+)\\'").getMatch(0);
        if (url_single != null) {
            /* 2017-05-31: 'List' URLs can redirect to single URLs */
            decryptedLinks.add(this.createDownloadlink("https://www." + this.br.getHost() + url_single));
            return decryptedLinks;
        }
        final String fpName = new Regex(parameter, "megabooru\\.com/post/list/([^/]+)/").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final String url_part = new Regex(parameter, "(https?://(?:www\\.)?megabooru\\.com/post/list/[^/]+/)\\d+").getMatch(0);
        int counter = 1;
        final int max_entries_per_page = 40;
        int entries_per_page_current = 0;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            if (counter > 1) {
                this.br.getPage(url_part + counter);
            }
            logger.info("Decrypting: " + this.br.getURL());
            final String[] linkids = br.getRegex("/post/view/(\\d+)").getColumn(0);
            if (linkids == null || linkids.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            entries_per_page_current = linkids.length;
            for (final String linkid : linkids) {
                final String link = "http://" + this.getHost() + "/post/view/" + linkid;
                final DownloadLink dl = createDownloadlink(link);
                dl.setLinkID(linkid);
                dl.setAvailable(true);
                dl.setName(linkid + ".jpg");
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
            counter++;
        } while (entries_per_page_current >= max_entries_per_page);
        return decryptedLinks;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.Danbooru;
    }
}
