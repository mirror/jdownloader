//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RomHustlerCrawler extends PluginForDecrypt {
    public RomHustlerCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "romhustler.org", "romhustler.net" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/rom/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.getCryptedUrl();
        final PluginForHost rhPlugin = this.getNewPluginForHostInstance(this.getHost());
        ((jd.plugins.hoster.RomHustler) rhPlugin).prepBrowser(this.br);
        br.getPage(addedurl);
        if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">404 \\- Page got lost|>\\s*This is a ESA protected rom|>Administrators only")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlSlug = new Regex(addedurl, "/rom/(.+)").getMatch(0);
        String fpName = br.getRegex("<h1 [^>]*?itemprop=\"name\"[^>]*?>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = urlSlug;
        }
        final String[] results = br.getRegex("<a[^>]+(/roms/(?:file|download)/[^>]*/[A-Za-z0-9/\\+=%]*)\"[^>]+>").getColumn(0);
        // TODO: add support for multiple parts
        if (results == null || results.length == 0 || fpName == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int counter = 1;
        for (final String result : results) {
            final String name = fpName + "_" + counter;
            final DownloadLink dl = createDownloadlink(br.getURL(result).toString());
            dl.setName(name);
            dl.setProperty("decrypterLink", addedurl);
            if (counter > 1) {
                dl.setProperty("splitlink", true);
            }
            dl.setAvailable(true);
            ret.add(dl);
            counter++;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        return ret;
    }
}