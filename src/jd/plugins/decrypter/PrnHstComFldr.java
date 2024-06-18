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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhost.com" }, urls = { "https?://(www\\.)?pornhost\\.com/([0-9]+.*|embed/\\d+)" })
public class PrnHstComFldr extends PluginForDecrypt {
    public PrnHstComFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        final PluginForHost hosterplugin = this.getNewPluginForHostInstance(this.getHost());
        if (hosterplugin.canHandle(contenturl) && StringUtils.startsWithCaseInsensitive(contenturl, ".html")) {
            ret.add(createDownloadlink(contenturl));
            return ret;
        }
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("gallery not found") || br.containsHTML("You will be redirected to")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("(moviecontainer|flashmovie|play this movie|createPlayer|>The movie needs to be converted first|jwplayer\\(\"div_video\"\\)\\.setup\\(\\{)") || br.getURL().contains(".com/embed/")) {
            /* Looks like single video */
            // final String finallink = br.getURL();
            ret.add(createDownloadlink(contenturl));
        } else {
            String[] urls = br.getRegex("class=\"thumb\">\\s*<a href=\"(.*?)\">").getColumn(0);
            if (urls.length == 0) {
                urls = br.getRegex("\"(https?://(?:www\\.)?pornhost\\.com/[0-9]+/[0-9]+\\.html)\"").getColumn(0);
            }
            if (urls == null || urls.length == 0) {
                /* Probably single video */
                ret.add(this.createDownloadlink(contenturl));
                return ret;
            }
            String fpName = br.getRegex("<title>pornhost\\.com - free file hosting with a twist - gallery(.*?)</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("id=\"url\" value=\"http://(www\\.)?pornhost\\.com/(.*?)/\"").getMatch(1);
            }
            for (String url : urls) {
                url = br.getURL(url).toExternalForm();
                ret.add(createDownloadlink(url));
            }
            // If the plugin knows the name/number of the gallery we can
            // add all pics to one package...looks nicer and makes it easier
            // for the user
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName("Gallery " + fpName.trim());
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}