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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "minus.com" }, urls = { "http://([a-zA-Z0-9]+\\.)?(minus\\.com|min\\.us)/[A-Za-z0-9]{2,}" }, flags = { 0 })
public class MinUsComDecrypter extends PluginForDecrypt {

    public MinUsComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS  = "https?://([a-zA-Z0-9]+\\.)?(minus\\.com|min\\.us)/(directory|explore|httpsmobile|pref|recent|search|smedia|uploads|mobile)";
    private static final String INVALIDLINKS2 = "https?://(www\\.)?blog\\.(minus\\.com|min\\.us)/.+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // they have subdomains like userprofile.minus.com/
        String parameter = param.toString().replace("dev.min", "min").replace("min.us/", "minus.com/");

        // ignore trash here... uses less memory allocations
        if (parameter.matches(INVALIDLINKS) || parameter.matches(INVALIDLINKS2)) {
            // /uploads is not supported
            return decryptedLinks;
        }

        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(<h2>Not found\\.</h2>|<p>Our records indicate that the gallery/image you are referencing has been deleted or does not exist|The page you requested does not exist)") || br.containsHTML("\"items\": \\[\\]") || br.containsHTML("class=\"guesthomepage_cisi_h1\">Upload and share your files instantly") || br.containsHTML(">The folder you requested has been deleted or has expired") || br.containsHTML(">You\\'re invited to join Minus") || br.containsHTML("<title>Error \\- Minus</title>")) {
            final DownloadLink dl = createDownloadlink(parameter.replace("minus.com/", "minusdecrypted.com/"));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null) {
            final DownloadLink dl = createDownloadlink(parameter.replace("minus.com/", "minusdecrypted.com/"));
            dl.setAvailable(false);
            decryptedLinks.add(dl);
            return decryptedLinks;

        }
        // Get album name for package name
        String fpName = br.getRegex("<title>(.*?) \\- Minus</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("var gallerydata = \\{.+ \"name\": \"([^\"]+)").getMatch(0);
        if (fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = decodeUnicode(Encoding.htmlDecode(fpName.trim()));
        // do not catch first "name", only items within array
        String[] items = br.getRegex("\\{([^}{]*?\"name\": \"[^\"]+?\"[^}{]*?)\\}").getColumn(0);
        // fail over for single items ?. Either that or they changed website yet
        // again and do not display the full gallery array.
        if (items == null || items.length == 0) items = br.getRegex("var gallerydata = \\{(.*?)\\};").getColumn(0);
        if (items != null && items.length != 0) {
            for (String singleitems : items) {
                String filename = new Regex(singleitems, "\"name\": ?\"([^<>\"/]*?)\"").getMatch(0);
                final String filesize = new Regex(singleitems, "\"filesize_bytes\": ?(\\d+)").getMatch(0);
                final String secureprefix = new Regex(singleitems, "\"secure_prefix\": ?\"(/\\d+/[A-Za-z0-9\\-_]+)\"").getMatch(0);
                final String linkid = new Regex(singleitems, "\"id\": ?\"([A-Za-z0-9\\-_]+)\"").getMatch(0);
                if (filename == null || filesize == null || secureprefix == null || linkid == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                filename = decodeUnicode(Encoding.htmlDecode(filename.trim()));
                if (!filename.startsWith("/")) filename = "/" + filename;
                final String filelink = "http://minusdecrypted.com/l" + linkid;
                final DownloadLink dl = createDownloadlink(filelink);
                dl.setFinalFileName(filename);
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            // Only one link available, add it!
            final String filesize = br.getRegex("<div class=\"item\\-actions\\-right\">[\t\n\r ]+<a title=\"([^<>\"]*?)\"").getMatch(0);
            final DownloadLink dl = createDownloadlink(parameter.replace("minus.com/", "minusdecrypted.com/"));
            if (filesize != null) dl.setDownloadSize(SizeFormatter.getSize(filesize));
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}