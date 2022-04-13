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

import org.appwork.utils.Files;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sex.com" }, urls = { "https?://(?:www\\.)?sex\\.com/(?:pin/\\d+|picture/\\d+|video/\\d+|galleries/[a-z0-9\\-_]+/\\d+|link/out\\?id=\\d+)" })
public class SexCom extends PornEmbedParser {
    public SexCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private static final String TYPE_VIDEO           = "https?://(www\\.)?sex\\.com/video/\\d+.*?";
    private static final String TYPE_EXTERN_REDIRECT = "https?://(?:www\\.)?sex\\.com/link/out\\?id=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String externID;
        String filename;
        br.setAllowedResponseCodes(502);
        String redirect = null;
        if (param.getCryptedUrl().matches(TYPE_EXTERN_REDIRECT)) {
            br.setFollowRedirects(false);
            br.getPage(param.getCryptedUrl());
            redirect = this.br.getRedirectLocation();
            if (this.canHandle(redirect)) {
                return null;
            }
            decryptedLinks.add(this.createDownloadlink(redirect));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        redirect = br.getRegex("onclick=\"window\\.location\\.href=\\'(/[^<>\"]*?)\\'").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        if (br.getURL().matches(TYPE_VIDEO) || br.containsHTML("<h1>\\s*Video\\s*.*?Pin")) {
            decryptedLinks.addAll(this.findLink());
        } else {
            filename = br.getRegex("<title>([^<>\"]*?) \\| Sex Videos and Pictures \\| Sex\\.com</title>").getMatch(0);
            if (filename == null || filename.length() <= 2) {
                filename = br.getRegex("addthis:title=\"([^<>\"]*?)\"").getMatch(0);
            }
            if (filename == null || filename.length() <= 2) {
                filename = br.getRegex("property=\"og:title\" content=\"([^<>]*?)\\-  Pin #\\d+ \\| Sex\\.com\"").getMatch(0);
            }
            if (filename == null || filename.length() <= 2) {
                filename = br.getRegex("<div class=\"pin\\-header navbar navbar\\-static\\-top\">[\t\n\r ]+<div class=\"navbar\\-inner\">[\t\n\r ]+<h1>([^<>]*?)</h1>").getMatch(0);
            }
            if (filename == null || filename.length() <= 2) {
                filename = new Regex(param.getCryptedUrl(), "(\\d+)/?$").getMatch(0);
            }
            filename = Encoding.htmlDecode(filename.trim());
            filename = filename.replace("#", "");
            externID = br.getRegex("<div class=\"from\">From <a rel=\"nofollow\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
            externID = br.getRegex("<link rel=\"image_src\" href=\"(http[^<>\"]*?)\"").getMatch(0);
            // For .gif images
            if (externID == null) {
                externID = br.getRegex("<div class=\"image_frame\"[^<>]*>\\s*(?:<[^<>]*>)?\\s*<img alt=[^<>]*?src=\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                final String filePath = new URL(externID).getPath();
                dl.setContentUrl(param.getCryptedUrl());
                dl.setFinalFileName(filename + "." + Files.getExtension(filePath));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            if (externID == null) {
                throw new DecrypterException("Decrypter broken for link: " + param.getCryptedUrl());
            }
        }
        return decryptedLinks;
    }

    @Override
    protected boolean isOffline(final Browser br) {
        final int responseCode = br.getHttpConnection().getResponseCode();
        if (responseCode == 404 || responseCode == 502) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<DownloadLink> findLink() throws Exception {
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)-  Pin #\\d+ \\| Sex\\.com\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\| Sex\\.com</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        }
        if (Encoding.isHtmlEntityCoded(filename)) {
            filename = Encoding.htmlDecode(filename);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        final String embedLink = br.getRegex("\"(/video/embed[^<>\"]*?)\"").getMatch(0);
        if (embedLink != null) {
            br.getPage(embedLink);
        }
        String externID = br.getRegex("(file|src):\\s*(\"|')(/video/stream[^<>\"]*?)(\"|')").getMatch(2);
        if (externID == null) {
            externID = br.getRegex("file:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
        }
        if (externID == null) {
            externID = br.getRegex("src: '([^<>']+)',\\s*type: 'video/mp4'").getMatch(0);
        }
        if (externID != null) {
            final DownloadLink fina = createDownloadlink("directhttp://" + br.getURL(externID).toString());
            fina.setContentUrl(br.getURL());
            fina.setFinalFileName(filename + ".mp4");
            decryptedLinks.add(fina);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(/link/out\\?id=\\d+)\" data\\-hostname").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("href=\"([^<>\"]+)\" data-rel=\"source\"").getMatch(0); // Picture
        }
        if (externID != null) {
            decryptedLinks.add(this.createDownloadlink(br.getURL(externID).toString()));
            return decryptedLinks;
        }
        return null;
    }
}