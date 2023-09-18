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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "turboimagehost.com", "turboimagehost.com" }, urls = { "https?://(?:www\\.)?turboimagehost\\.com/p/\\d+/[^/]+\\.html|https?://[a-z0-9\\-]+\\.turboimg\\.net/t1/\\d+_[^/]+", "https?://(?:www\\.)?turboimagehost\\.com/album/\\d+/[^/]+" })
public class TurboimagehostCom extends PluginForDecrypt {
    public TurboimagehostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches("(?i).*/album/\\d+/.+")) {
            return crawlAlbum(param);
        } else {
            return crawlImage(param);
        }
    }

    private ArrayList<DownloadLink> crawlAlbum(CryptedLink param) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        checkOffline(br);
        String galleryTitle = br.getRegex("class\\s*=\\s*\"galleryTitle\">\\s*<h1>\\s*(.*?)\\s*</h1>").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (galleryTitle == null) {
            galleryTitle = new Regex(param.getCryptedUrl(), "/album/(\\d+)").getMatch(0);
        }
        fp.setName(galleryTitle);
        final Set<String> pages = new HashSet<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        while (!isAbort()) {
            final String links[] = br.getRegex("(https?://(?:www\\.)?turboimagehost\\.com/p/\\d+/[^/]+\\.html)").getColumn(0);
            for (String link : links) {
                final DownloadLink downloadLink = createDownloadlink(link);
                fp.add(downloadLink);
                distribute(downloadLink);
                decryptedLinks.add(downloadLink);
            }
            final String nextPage = br.getRegex("label\\s*=\\s*\"Next\"\\s*href\\s*=\\s*\"(\\?p=\\d+)\"").getMatch(0);
            if (nextPage != null && pages.add(nextPage)) {
                br.getPage(nextPage);
            } else {
                break;
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlImage(final CryptedLink param) throws Exception {
        final Regex thumbnail = new Regex(param.getCryptedUrl(), "https?://[a-z0-9\\-]+\\.turboimg\\.net/t1/(\\d+)_([^/]+)");
        final String contenturl;
        if (thumbnail.patternFind()) {
            /* Change thumbnail --> Normal URL --> Then we can crawl the fullsize URL. */
            contenturl = "https://www." + this.getHost() + "/p/" + thumbnail.getMatch(0) + "/" + thumbnail.getMatch(1) + ".html";
        } else {
            contenturl = param.getCryptedUrl();
        }
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        checkOffline(br);
        final String finallink = this.br.getRegex("\"(https?://s\\d+d\\d+\\.(?:turboimagehost\\.com|turboimg\\.net)/sp/[a-z0-9]+/.*?)\"").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final DownloadLink link = createDownloadlink(finallink);
        String filename = getFileNameFromURL(new URL(finallink));
        if (filename != null) {
            filename = filename.replaceFirst("\\.html?$", "");
            link.setName(filename);
        }
        link.setAvailable(true);
        link.setContentUrl(contenturl);
        ret.add(link);
        return ret;
    }

    private void checkOffline(final Browser br) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().matches("^/?$")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }
}
