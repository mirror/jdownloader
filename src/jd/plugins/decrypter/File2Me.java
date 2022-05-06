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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class File2Me extends PluginForDecrypt {
    public File2Me(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "file2.me" });
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
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/d/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*업로드 파일이 없습니다")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String passCode = null;
        if (getPasswordProtectedForm(br) != null) {
            for (int i = 0; i <= 2; i++) {
                final Form pwform = this.getPasswordProtectedForm(br);
                /* Password InputField key is different each time --> We need to find it */
                final InputField pwfield = pwform.getInputFieldByNameRegex("dcode.+");
                if (pwfield == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                passCode = getUserInput("Password?", param);
                pwfield.setValue(Encoding.urlEncode(passCode));
                br.submitForm(pwform);
                if (this.getPasswordProtectedForm(br) == null) {
                    break;
                }
            }
            if (this.getPasswordProtectedForm(br) != null) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String title = br.getRegex("<h5 class=\"title\">\\s*([^<]+)\\s*</h5>").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        if (title != null) {
            fp.setName(Encoding.htmlDecode(title).trim());
        } else {
            /* Fallback */
            fp.setName(contentID);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String table = br.getRegex("<tbody>(.*?)</table>").getMatch(0);
        final String[] tableRows = table.split("</td>\\s*</tr>");
        for (final String tableRow : tableRows) {
            final String[] textColumns = new Regex(tableRow, "<td class=\"visible-md[^\"]+\"[^>]*>([^<]+)</td>").getColumn(0);
            if (textColumns.length == 0) {
                if (decryptedLinks.isEmpty()) {
                    /* Fatal error */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    /* Last item, can be corrupted */
                    break;
                }
            }
            final String filename = Encoding.htmlDecode(textColumns[1]).trim();
            final String fileID = new Regex(tableRow, "(?i)/Act/fileDown/([a-f0-9]{32})").getMatch(0);
            final DownloadLink link = this.createDownloadlink("directhttp://" + br.getURL("/Act/fileDown/" + fileID).toString());
            link.setFinalFileName(filename);
            link.setProperty(DirectHTTP.FIXNAME, filename);
            link.setDownloadSize(SizeFormatter.getSize(textColumns[2]));
            link.setAvailable(true);
            link.setContentUrl(br.getURL());
            if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            /* Set linkID as same file can e.g. be available via different links/different subdomains. */
            link.setLinkID(this.getHost() + "://" + fileID);
            link._setFilePackage(fp);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }

    private Form getPasswordProtectedForm(final Browser br) {
        return br.getFormbyKey("check_pass");
    }
}
