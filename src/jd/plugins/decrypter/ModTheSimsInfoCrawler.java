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
import java.util.List;
import java.util.regex.Pattern;

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
import jd.plugins.PluginForHost;
import jd.plugins.hoster.ModTheSimsInfo;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ModTheSimsInfoCrawler extends PluginForDecrypt {
    public ModTheSimsInfoCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return ModTheSimsInfo.getPluginDomains();
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/d/(\\d+)/([\\w\\-]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final URL addedlink = new URL(param.getCryptedUrl());
        br.getPage(param.getCryptedUrl().replaceFirst(addedlink.getHost(), this.getHost()));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String slug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(slug.replace("-", " ").trim());
        final PluginForHost hosterPlugin = this.getNewPluginForHostInstance(this.getHost());
        final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final String url : urls) {
            if (!hosterPlugin.canHandle(url)) {
                continue;
            }
            final String quotedURL = Pattern.quote(url);
            final DownloadLink file = this.createDownloadlink(url);
            file.setAvailable(true);
            final String filename = br.getRegex(quotedURL + "\"[^>]*>([^<]+)</a>").getMatch(0);
            if (filename != null) {
                file.setName(Encoding.htmlDecode(filename).trim());
            }
            final String filesizeStr = br.getRegex(quotedURL + "\">\\s*Download</a>\\s*</td>\\s*<td class=\"hidden-phone hidden-tablet\">([^<]+)</td>").getMatch(0);
            if (filesizeStr != null) {
                file.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            file._setFilePackage(fp);
            ret.add(file);
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }
}
