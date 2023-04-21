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
import java.util.List;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DoodstreamCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DoodstreamComFolder extends PluginForDecrypt {
    public DoodstreamComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return DoodstreamCom.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/f/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)not_found\"|<title>Video not found|>video you are looking for is not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        String title = br.getRegex("<title>([^<]+) - DoodStream</title>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<h1 class=\"font-weight-bold text-center\"[^>]*>([^<]+)</h1>").getMatch(0);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else {
            /* Fallback */
            fp.setName(folderID);
        }
        final DoodstreamCom hosterPlugin = (DoodstreamCom) this.getNewPluginForHostInstance(this.getHost());
        final String[] additionalInfo = br.getRegex("<li class=\"d-flex flex-wrap align-items-center justify-content-between mb-2\">.*?</div>\\s*</li>").getColumn(-1);
        final List<String> detectedURLs = new ArrayList<String>();
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        for (final String url : urls) {
            if (hosterPlugin.canHandle(url) && !detectedURLs.contains(url)) {
                detectedURLs.add(url);
            }
        }
        if (detectedURLs.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String ext = ".mp4";
        int index = 0;
        for (final String url : detectedURLs) {
            final DownloadLink video = this.createDownloadlink(url);
            video.setAvailable(true);
            String filetitle = null;
            String filesizeStr = null;
            /* Get and set additional information if possible. */
            if (additionalInfo != null && additionalInfo.length == detectedURLs.size()) {
                final String html = additionalInfo[index];
                filetitle = new Regex(html, "class=\"name\"[^>]*>\\s*<h4>([^<]+)</h4>").getMatch(0);
                filesizeStr = new Regex(html, "class=\"d-inline-block mr-1\">\\s*(\\d+[^<]+)</span>").getMatch(0);
            }
            if (filetitle != null) {
                video.setName(this.applyFilenameExtension(Encoding.htmlDecode(filetitle).trim(), ext));
            } else {
                /* Fallback */
                video.setName(hosterPlugin.getFUIDFromURL(video) + ext);
            }
            if (filesizeStr != null) {
                video.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            video._setFilePackage(fp);
            ret.add(video);
            index++;
        }
        return ret;
    }
}
