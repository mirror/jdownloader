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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "publicfeet.com" }, urls = { "https?://(?:www\\.)?publicfeet\\.com/posts/([a-z0-9\\-]+)" })
public class PublicfeetCom extends PluginForDecrypt {
    public PublicfeetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("(?i)>\\s*No such post|This room does not exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to mainpage --> Content looks to be offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String[] images = br.getRegex("\"(https?://[^/]+/public/media/front/post/img/\\d+[^<>\"\\']+)\"").getColumn(0);
        final String[] videos = br.getRegex("\"(https?://[^/]+/storage/app/[^<>\"\\']+\\.mp4)\"").getColumn(0);
        if (images.length == 0 && videos.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String urlSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        String galleryTitle = br.getRegex("class=\"gallery_name\"[^>]*><h1>([^<]+)</h1>").getMatch(0);
        if (galleryTitle != null) {
            galleryTitle = Encoding.htmlDecode(galleryTitle).trim();
        }
        String uploaderName = br.getRegex("(?i)>\\s*Room Owner\\s*:\\s*</h1>\\s*<p class=\"post_details post_detls_inline\">([^<]+)</p>").getMatch(0);
        if (uploaderName != null) {
            uploaderName = Encoding.htmlDecode(uploaderName).trim();
        }
        final String galleryID = br.getRegex("id=\"galry_detl_id\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (uploaderName != null && galleryTitle != null) {
            fp.setName(uploaderName + " - " + galleryTitle);
        } else if (galleryTitle != null) {
            fp.setName(galleryTitle);
        } else if (galleryID != null) {
            fp.setName(galleryID);
        } else {
            /* Final fallback */
            fp.setName(Encoding.htmlDecode(urlSlug).replace("-", " ").trim());
        }
        for (final String singleLink : images) {
            ret.add(createDownloadlink(singleLink));
        }
        for (final String singleLink : videos) {
            ret.add(createDownloadlink(singleLink));
        }
        for (final DownloadLink result : ret) {
            result.setAvailable(true);
            result._setFilePackage(fp);
        }
        return ret;
    }
}
