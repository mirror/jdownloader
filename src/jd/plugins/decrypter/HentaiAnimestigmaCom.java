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
import java.util.regex.Matcher;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40413 $", interfaceVersion = 3, names = { "hentai.animestigma.com" }, urls = { "https?://(?:www\\.)?hentai.animestigma.com.*" })
public class HentaiAnimestigmaCom extends antiDDoSForDecrypt {
    public HentaiAnimestigmaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    protected DownloadLink createDownloadlink(String url, String title) {
        final String ext = ".mp4";
        final DownloadLink dl = super.createDownloadlink(url, true);
        dl.setName(title + ext);
        dl.setAvailable(true);
        dl.setProperty("mainlink", br.getURL());
        return dl;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (handleEmbedSingle(decryptedLinks) == false) {
            if (parameter.contains("hentai-list-a-z") || parameter.contains("3d-hentai-list-a-z")) {
                handleEmbedList(decryptedLinks);
            } else if (br.toString().contains("You must be login to submit genre tags")) {
                handleEmbedFinal(decryptedLinks);
            } else {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        if (decryptedLinks.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return decryptedLinks;
    }

    private boolean handleEmbedSingle(final ArrayList<DownloadLink> decryptedLinks) {
        String downloadtitle = br.getRegex("rel=\"bookmark\" title=\"([^\"]+)\">").getMatch(0);
        String downloadlink = br.getRegex("<iframe src=\"([^\"]+)\" frameborder=\"0\" scrolling=\"no\"").getMatch(0);
        if (downloadlink != null && downloadtitle != null) {
            decryptedLinks.add(this.createDownloadlink(Encoding.htmlOnlyDecode(downloadlink), Encoding.htmlOnlyDecode(downloadtitle)));
            return true;
        } else {
            return false;
        }
    }

    private void handleEmbedList(final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        Matcher nextpage2 = br.getRegex("<a href=\"([^\"]+)\">([^<]+)</a>").getMatcher();
        while (nextpage2.find()) {
            if (nextpage2.group(1) != null && nextpage2.group(2) != null) {
                br.getPage(nextpage2.group(1));
                handleEmbedFinal(decryptedLinks);
            }
        }
        return;
    }

    private void handleEmbedFinal(final ArrayList<DownloadLink> decryptedLinks) throws Exception {
        Matcher nextpage3 = br.getRegex("<a href=\"([^\"]+)\" rel=\"bookmark\" title=\"([^\"]+)\">").getMatcher();
        while (nextpage3.find()) {
            if (nextpage3.group(1) != null && nextpage3.group(2) != null) {
                br.getPage(nextpage3.group(1));
                handleEmbedSingle(decryptedLinks);
            }
        }
        return;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}
