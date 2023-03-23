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
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.Open3dlabCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { Open3dlabCom.class })
public class Open3dlabComCrawler extends PluginForDecrypt {
    public Open3dlabComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return Open3dlabCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/project/(\\d+)/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String projectID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (projectID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Open3dlabCom hosterplugin = (Open3dlabCom) this.getNewPluginForHostInstance(this.getHost());
        String title = br.getRegex("\"name\"\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else {
            fp.setName(projectID);
        }
        final String[] dlHTMLs = br.getRegex("<td class=\"text-wrap-word js-edit-input\"(.*?)</div>\\s*</div>\\s*</td>\\s*</tr>").getColumn(0);
        if (dlHTMLs == null || dlHTMLs.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String dlHTML : dlHTMLs) {
            /* Find all mirrors */
            String filename = new Regex(dlHTML, "span class=\"js-edit-input__wrapper\"><strong>([^<]+)</strong>").getMatch(0);
            if (filename == null) {
                filename = new Regex(dlHTML, "(?i)You are about to download \"([^\"]+)").getMatch(0);
            }
            if (filename != null) {
                filename = Encoding.htmlDecode(filename).trim();
            }
            final String filesizeStr = new Regex(dlHTML, "<td>(\\d+[^<]+)</td>\\s*</tr>").getMatch(0);
            long filesize = -1;
            if (filesizeStr != null) {
                filesize = SizeFormatter.getSize(filesizeStr);
            }
            final String[] urls = HTMLParser.getHttpLinks(dlHTML, br.getURL());
            for (final String url : urls) {
                if (hosterplugin.canHandle(url)) {
                    final DownloadLink dl = this.createDownloadlink(url);
                    dl._setFilePackage(fp);
                    dl.setAvailable(true);
                    if (filename != null) {
                        dl.setName(filename);
                    }
                    if (filesize != -1) {
                        dl.setDownloadSize(filesize);
                    }
                    ret.add(dl);
                }
            }
        }
        return ret;
    }
}
