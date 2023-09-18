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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class JumploadsComFolder extends PluginForDecrypt {
    public JumploadsComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "jumploads.com", "goloady.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/([A-Za-z0-9]+)/([^/]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* 2021-04-20: Main domain has changed from goloady.com to jumploads.com */
        final String oldDomain = Browser.getHost(param.getCryptedUrl());
        final String contenturl = param.getCryptedUrl().replace(oldDomain + "/", this.getHost() + "/");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String titleFromURL = Encoding.htmlDecode(new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(1)).trim();
        final String[] htmls = br.getRegex("<li>.*?</li>").getColumn(-1);
        if (htmls == null || htmls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleHTML : htmls) {
            String url = new Regex(singleHTML, "href=\"(https?://(?:www\\.)?[^/]+/(?:file|folder)/[^\"]+)").getMatch(0);
            final String title = new Regex(singleHTML, "class=\"inlineblock\">([^<>\"]+)<div").getMatch(0);
            String filesize = new Regex(singleHTML, "class=\"[^\"]+color777\">([^<>\"]+)</div>").getMatch(0);
            if (url == null || filesize == null) {
                /* Skip invalid objects */
                continue;
            }
            filesize = Encoding.htmlDecode(filesize);
            if (filesize.equals("--")) {
                /*
                 * 2019-08-14: Workaround for website bug: folders are also listed as '/file/' URLs but folders have no filesize displayed
                 * this is how we can recognize them!
                 */
                url = url.replace("/file/", "/folder/");
                filesize = null;
            }
            final DownloadLink dl = createDownloadlink(url);
            dl.setAvailable(true);
            if (title != null) {
                dl.setName(Encoding.htmlDecode(title));
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(titleFromURL);
        fp.addLinks(ret);
        return ret;
    }
}
