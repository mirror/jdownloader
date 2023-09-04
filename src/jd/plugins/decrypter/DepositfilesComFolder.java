//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DepositFiles;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { DepositFiles.class })
public class DepositfilesComFolder extends PluginForDecrypt {
    private static String MAINPAGE = null;

    public DepositfilesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return DepositFiles.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]+/)?folders/(.+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (MAINPAGE == null) {
            /* we first have to load the plugin, before we can reference it */
            final PluginForHost depositfilesPlugin = this.getNewPluginForHostInstance(this.getHost());
            ((jd.plugins.hoster.DepositFiles) depositfilesPlugin).setMainpage();
            MAINPAGE = jd.plugins.hoster.DepositFiles.MAINPAGE.get();
            if (MAINPAGE == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        String url = param.getCryptedUrl();
        // final String folderSlug = new Regex(url, this.getSupportedLinks()).getMatch(0);
        int pagecount = 1;
        final String folderID = new Regex(url, "(?i)folders/(.+)").getMatch(0);
        url = MAINPAGE + "/de/folders/" + folderID;
        // Get Pagecount //
        if (url.contains("page")) {
            url = url.split("\\?")[0];
        }
        br.getPage(url);
        if (br.containsHTML("\\&gt;\\&gt;\\&gt;")) {
            final String[] pages = br.getRegex("\\?page=(\\d+)\">\\d+</a>").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String currentPage : pages) {
                    if (Integer.parseInt(currentPage) > pagecount) {
                        pagecount = Integer.parseInt(currentPage);
                    }
                }
            }
        }
        String folderTitle = br.getRegex("Dateien festlegen\\s*:\\s*<b>([^<]+)</b>").getMatch(0);
        FilePackage fp = null;
        if (folderTitle != null) {
            folderTitle = Encoding.htmlDecode(folderTitle).trim();
            fp = FilePackage.getInstance();
            fp.setName(folderTitle);
        }
        for (int x = 1; x <= pagecount; x++) {
            br.getPage(url + "?page=" + x + "&format=text");
            final String[] urls = br.getRegex("(https?://[^/]+/files/[0-9a-z]+)").getColumn(0);
            for (String data : urls) {
                final DownloadLink link = createDownloadlink(data);
                if (fp != null) {
                    link._setFilePackage(fp);
                }
                ret.add(link);
                distribute(link);
            }
        }
        if (ret.isEmpty()) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}