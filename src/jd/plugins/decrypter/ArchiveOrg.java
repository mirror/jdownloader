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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.ArchiveOrgConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "archive.org" }, urls = { "https?://(?:www\\.)?archive\\.org/(details|download)/(?!copyrightrecords)@?[A-Za-z0-9_\\-\\.]+$" })
public class ArchiveOrg extends PluginForDecrypt {
    public ArchiveOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return ArchiveOrgConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("://www.", "://");
        /*
         * 2017-01-25: We do not (yet) have to be logged in here. We can always see all items and their information but some may be limited
         * to premium users only
         */
        // final Account aa = AccountController.getInstance().getValidAccount(JDUtilities.getPluginForHost(this.getHost()));
        // if (aa != null) {
        // jd.plugins.hoster.ArchiveOrg.login(this.br, aa, false);
        // }
        br.getPage(parameter);
        if (StringUtils.containsIgnoreCase(parameter, "/details/")) {
            int page = 2;
            while (!isAbort()) {
                if (br.containsHTML("This item is only available to logged in Internet Archive users")) {
                    decryptedLinks.add(createDownloadlink(parameter.replace("/details/", "/download/")));
                    break;
                }
                final String showAll = br.getRegex("href=\"(/download/[^\"]*?)\">SHOW ALL").getMatch(0);
                if (showAll != null) {
                    decryptedLinks.add(createDownloadlink(br.getURL(showAll).toString()));
                    break;
                }
                final String[] details = br.getRegex("<div class=\"item-ia\".*? <a href=\"(/details/[^\"]*?)\" title").getColumn(0);
                if (details == null || details.length == 0) {
                    break;
                }
                for (final String detail : details) {
                    final DownloadLink link = createDownloadlink(br.getURL(detail).toString());
                    decryptedLinks.add(link);
                    if (isAbort()) {
                        break;
                    } else {
                        distribute(link);
                    }
                }
                br.getPage("?page=" + (page++));
            }
        } else {
            if (br.containsHTML(">The item is not available")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (!br.containsHTML("\"/download/")) {
                logger.info("Maybe invalid link or nothing there to download: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final boolean preferOriginal = PluginJsonConfig.get(ArchiveOrgConfig.class).isPreferOriginal();
            final String fpName = br.getRegex("<h1>Index of [^<>\"]+/([^<>\"/]+)/?</h1>").getMatch(0);
            // New way
            final String[][] finfo = br.getRegex("<a href=\"([^<>\"]*?)\">[^<>\"]*?</a>[^<>\"]*?(\\d+\\.\\d+(?:K|M|G|B))").getMatches();
            String filesXML = null;
            for (final String[] finfosingle : finfo) {
                final String filename = finfosingle[0];
                if (StringUtils.endsWithCaseInsensitive(filename, "_files.xml")) {
                    final Browser brc = br.cloneBrowser();
                    filesXML = brc.getPage(brc.getURL() + "/" + filename);
                    continue;
                }
            }
            for (final String[] finfosingle : finfo) {
                final String filename = Encoding.urlDecode(finfosingle[0], false);
                if (StringUtils.endsWithCaseInsensitive(filename, "_files.xml") || StringUtils.endsWithCaseInsensitive(filename, "_meta.sqlite") || StringUtils.endsWithCaseInsensitive(filename, "_meta.xml")) {
                    continue;
                } else if (filesXML != null && preferOriginal) {
                    if (!new Regex(filesXML, "<file name=\"" + Pattern.quote(filename) + "\" source=\"original\"").matches()) {
                        continue;
                    }
                }
                final String fsize = finfosingle[1] + "b";
                final DownloadLink fina = createDownloadlink(br.getURL() + "/" + filename);
                fina.setDownloadSize(SizeFormatter.getSize(fsize));
                fina.setAvailable(true);
                fina.setFinalFileName(filename);
                if (filesXML != null) {
                    final String sha1 = new Regex(filesXML, "<file name=\"" + Pattern.quote(filename) + "\".*?<sha1>([a-f0-9]{40})</sha1>").getMatch(0);
                    if (sha1 != null) {
                        fina.setSha1Hash(sha1);
                    }
                    final String size = new Regex(filesXML, "<file name=\"" + Pattern.quote(filename) + "\".*?<size>(\\d+)</size>").getMatch(0);
                    if (size != null) {
                        fina.setVerifiedFileSize(Long.parseLong(size));
                    }
                }
                decryptedLinks.add(fina);
            }
            final FilePackage fp = FilePackage.getInstance();
            if (fpName != null) {
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}