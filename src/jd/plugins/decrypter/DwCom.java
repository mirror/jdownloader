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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.GenericM3u8;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dw.com" }, urls = { "https?://(?:www\\.)?dw\\.com/[a-z]{2}/([^/]+)/av-(\\d+)" })
public class DwCom extends PluginForDecrypt {
    public DwCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.getCryptedUrl();
        br.setFollowRedirects(true);
        br.getPage(addedurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        if (StringUtils.isEmpty(title)) {
            /* Fallback */
            String url_title = new Regex(addedurl, this.getSupportedLinks()).getMatch(0);
            url_title = Encoding.htmlDecode(url_title);
            title = url_title.replace("-", " ").trim();
        }
        final String[] hlsmasters = br.getRegex("src=\"(https?://[^\"]+master\\.m3u8)\" type=\"application/x-mpegURL\"").getColumn(0);
        for (final String hlsmaster : hlsmasters) {
            final DownloadLink hls = this.createDownloadlink(hlsmaster);
            /* Important as this will be used in filenames later. */
            hls.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, title);
            ret.add(hls);
        }
        if (ret.isEmpty()) {
            /* Old handling */
            final String lid = new Regex(addedurl, this.getSupportedLinks()).getMatch(1);
            br.getPage("/playersources/v-" + lid);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] links = br.getRegex("(http[^\"]+\\.mp4)").getColumn(0);
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String singleLink : links) {
                final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
                final String quality_part = new Regex(singleLink, "(_[a-z]+_[a-z]+\\.mp4)").getMatch(0);
                if (quality_part != null) {
                    dl.setFinalFileName(title + quality_part);
                }
                ret.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
