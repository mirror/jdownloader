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

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MegaDpUaFolder extends PluginForDecrypt {
    public MegaDpUaFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mega.dp.ua" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:[a-z]{2}/)?([A-Za-z0-9]{3,})");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        final String parameter = param.toString();
        br.getPage(parameter);
        if (jd.plugins.hoster.MegaDpUa.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String passCode = null;
        if (this.getPasswordProtectedForm() != null) {
            final Form pwform = this.getPasswordProtectedForm();
            passCode = getUserInput("Password?", param);
            pwform.put("pass", Encoding.urlEncode(passCode));
            br.submitForm(pwform);
            if (this.getPasswordProtectedForm() != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        final String[] folderHTMLs = br.getRegex("<tr><td><div class=\"urlfile\"(.*?)</td></tr>").getColumn(0);
        for (final String folderHTML : folderHTMLs) {
            final String filename = new Regex(folderHTML, ">([^<>\"]+)</div>").getMatch(0);
            final String filesize = new Regex(folderHTML, "<td style=[^>]*>([^<>\"]+)</td>").getMatch(0);
            final String url = new Regex(folderHTML, plg.getSupportedLinks()).getMatch(-1);
            final String directurl = new Regex(folderHTML, "class=\"hidden-link\"[^>]*data-link=\"(https://[^<>\"]+)").getMatch(0);
            if (url == null) {
                /* Skip invalid items */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink(url);
            if (filename != null) {
                /* 2021-02-26: They're tagging their filenames -> Prefer the ones we find here */
                dl.setFinalFileName(Encoding.htmlDecode(filename).trim());
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            dl.setAvailable(true);
            if (passCode != null) {
                dl.setDownloadPassword(passCode);
            }
            /* Saving this directurl will help us later so we can skip the password form ;) */
            if (directurl != null) {
                dl.setProperty("free_directlink", directurl);
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private Form getPasswordProtectedForm() {
        return br.getFormbyKey("pass");
    }
}
