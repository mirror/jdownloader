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
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgbox.com" }, urls = { "https?://(www\\.)?imgbox\\.com/(g/)?[A-Za-z0-9]+" })
public class ImgBoxComGallery extends PluginForDecrypt {
    public ImgBoxComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static final String GALLERYLINK    = "(?i)https?://(www\\.)?imgbox\\.com/g/([A-Za-z0-9]+)";
    private static final String PICTUREOFFLINE = "The image in question does not exist|The image has been deleted due to a DMCA complaint";
    private static final String INVALIDLINKS   = "(?i)https?://(www\\.)?imgbox\\.com/(help|login|privacy|register|tos|images|dmca|gallery|assets)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl();
        if (contenturl.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + contenturl);
            return ret;
        }
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.containsHTML(">The page you (are|were) looking for") || br.getURL().contains("imgbox.com/login")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (contenturl.matches(GALLERYLINK)) {
            if (br.containsHTML("The specified gallery could not be found")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String fpName = br.getRegex("<h1 style=\"padding\\-left:15px;\">(.*?)</h1>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<h1>([^<>\"]+)- \\d+ images(?:\\s+images)?</h1>").getMatch(0);
            }
            if (fpName == null) {
                fpName = "imgbox.com gallery " + new Regex(contenturl, "imgbox\\.com/g/(.+)").getMatch(0);
            }
            final String[] uids = br.getRegex("(?i)<a href=(\"|')/([a-zA-Z0-9]+)\\1><img alt=(\"|')\\2[^'\"]*\\3").getColumn(1);
            if (uids == null || uids.length == 0) {
                if (br.containsHTML(" 0 images\\s*</h1>")) {
                    // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
            }
            for (final String uid : uids) {
                final DownloadLink dl = createDownloadlink("http://imgbox.com/" + uid);
                if (fp != null) {
                    fp.add(dl);
                }
                ret.add(dl);
            }
        } else {
            if (br.containsHTML(PICTUREOFFLINE)) {
                logger.info("Link offline: " + contenturl);
                return ret;
            }
            final DownloadLink dl = crawlSingle(br);
            ret.add(dl);
        }
        return ret;
    }

    private DownloadLink crawlSingle(final Browser br) throws PluginException {
        final String finallink = br.getRegex("\"(https?://(i|[a-z0-9\\-]+)\\.imgbox\\.com/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final DownloadLink ret = createDownloadlink(DirectHTTP.createURLForThisPlugin(Encoding.htmlDecode(finallink)));
        final String imageContainer = br.getRegex("class\\s*=\\s*\"image-container\"[^>]*>\\s*(.*?)\\s*</div>").getMatch(0);
        final String title = new Regex(imageContainer, "title\\s*=\\s*\"(.*?)\"").getMatch(0);
        if (StringUtils.isNotEmpty(title)) {
            ret.setFinalFileName(Encoding.htmlDecode(title));
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}