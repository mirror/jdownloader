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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "libgen.org" }, urls = { "https?://(www\\.)?(libgen\\.org|gen\\.lib\\.rus\\.ec|libgen\\.in)/book/index\\.php\\?md5=[A-F0-9]{32}" }, flags = { 0 })
public class LibGen extends PluginForDecrypt {

    public LibGen(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load hostplugin */
        JDUtilities.getPluginForHost("libgen.info");
        String libgen_url = null;
        String libgen_server_filename = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String host = new Regex(parameter, "(https?://[^/]+)").getMatch(0);
        br.setCookie(host, "lang", "en");
        br.setCustomCharset("utf-8");
        /* Allow redirects to other of their domains */
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("entry not found in the database")) {
            logger.info("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (fpName != null) {
            fpName = Encoding.htmlDecode(fpName).trim();
        }

        String[] links = br.getRegex("<url\\d+>(https?://[^<]+)</url\\d+>").getColumn(0);
        // Hmm maybe just try to get all mirrors
        if (links == null || links.length == 0) {
            links = br.getRegex("<td align='center' width='11,1%'><a href=\\'(http[^<>\"]*?)'").getColumn(0);
        }
        if (links == null || links.length == 0) {
            return null;
        }
        if (links != null && links.length != 0) {
            for (final String dl : links) {
                if (dl.matches(jd.plugins.hoster.LibGenInfo.type_libgen_in)) {
                    libgen_url = dl;
                } else {
                    decryptedLinks.add(createDownloadlink(dl));
                }
            }
        }

        if (libgen_url != null) {
            final DownloadLink libgen_dl = createDownloadlink(libgen_url);
            URLConnectionAdapter con = null;
            final Browser br2 = br.cloneBrowser();
            try {
                try {
                    con = openConnection(br2, libgen_url);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    libgen_server_filename = Encoding.htmlDecode(getFileNameFromHeader(con));
                    libgen_dl.setDownloadSize(con.getLongContentLength());
                    libgen_dl.setFinalFileName(libgen_server_filename);
                } else {
                    libgen_dl.setAvailable(false);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            decryptedLinks.add(libgen_dl);
        }

        final String cover_url = br.getRegex("(?:\\'|\")(https?://libgen\\.(?:in|info|net)/covers/\\d+/[^<>\"\\']*?\\.(?:jpg|jpeg|png|gif))").getMatch(0);
        if (cover_url != null && libgen_url != null) {
            final DownloadLink dl = createDownloadlink(cover_url);
            final String ext = cover_url.substring(cover_url.lastIndexOf("."));
            String filename_cover = null;
            if (libgen_server_filename != null) {
                filename_cover = libgen_server_filename.substring(0, libgen_server_filename.lastIndexOf("."));
            } else if (fpName != null) {
                filename_cover = encodeUnicode(fpName);
            }
            filename_cover += ext;
            dl.setFinalFileName(filename_cover);
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (isJDStable()) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}