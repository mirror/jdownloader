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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "viva.tv", "mtviggy.com", "southpark.de", "southpark.cc.com", "vh1.com" }, urls = { "http://www\\.viva\\.tv/charts/16\\-viva\\-top\\-100", "http://www\\.mtviggy\\.com/videos/[a-z0-9\\-]+/|http://www\\.mtvdesi\\.com/(videos/)?[a-z0-9\\-]+|http://www\\.mtvk\\.com/videos/[a-z0-9\\-]+", "http://www\\.southpark\\.de/alle\\-episoden/s\\d{2}e\\d{2}[a-z0-9\\-]+", "http://southpark\\.cc\\.com/full\\-episodes/s\\d{2}e\\d{2}[a-z0-9\\-]+", "http://www\\.vh1.com/(shows/[a-z0-9\\-_]+/[a-z0-9\\-_]+/.+|video/play\\.jhtml\\?id=\\d+|video/[a-z0-9\\-_]+/\\d+/[a-z0-9\\-_]+\\.jhtml|events/[a-z0-9\\-_]+/videos/[a-z0-9\\-_]+/\\d+/)" }, flags = { 0, 0, 0, 0, 0 })
public class VivaTvDecrypt extends PluginForDecrypt {

    public VivaTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Tags: Viacom International Media Networks Northern Europe, mrss, gameone.de */
    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/mtv.py */
    /* Additional information/methods can be found in the VivaTv host plugin */

    private static final String     type_viva                 = "http://www\\.viva\\.tv/.+";
    private static final String     type_mtviggy              = "http://www\\.mtviggy\\.com/videos/[a-z0-9\\-]+/";
    private static final String     type_mtvdesi              = "http://www\\.mtvdesi\\.com/(videos/)?[a-z0-9\\-]+";
    private static final String     type_mtvk                 = "http://www\\.mtvk\\.com/videos/[a-z0-9\\-]+";

    private static final String     type_southpark_de_episode = "http://www\\.southpark\\.de/alle\\-episoden/.+";
    private static final String     type_southpark_cc_episode = "http://southpark\\.cc\\.com/full\\-episodes/.+";

    private static final String     type_vh1                  = "http://www\\.vh1.com/.+";
    private static final String     subtype_vh1_episodes      = "http://www\\.vh1\\.com/shows/.+";
    private static final String     subtype_vh1_videos        = "http://www\\.vh1\\.com/video/.+";
    private static final String     subtype_vh1_events        = "http://www\\.vh1\\.com/events/.+";

    private ArrayList<DownloadLink> decryptedLinks            = new ArrayList<DownloadLink>();
    private String                  default_ext               = null;
    private String                  parameter                 = null;
    private String                  mgid                      = null;
    private String                  fpName                    = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* we first have to load the plugin, before we can reference it */
        JDUtilities.getPluginForHost("viva.tv");
        default_ext = jd.plugins.hoster.VivaTv.default_ext;
        parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            if (parameter.matches(type_viva)) {
                decryptVivaTvCharts();
            } else if (parameter.matches(type_mtviggy) || parameter.matches(type_mtvdesi) || parameter.matches(type_mtvk)) {
                decryptMtviggy();
            } else if (parameter.matches(type_southpark_de_episode)) {
                decryptSouthparkDe();
            } else if (parameter.matches(type_southpark_cc_episode)) {
                decryptSouthparkCc();
            } else if (parameter.matches(type_vh1)) {
                decryptVh1();
            } else {
                /* Probably unsupported linktype */
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        } catch (final BrowserException e) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    private void decryptVivaTvCharts() throws DecrypterException, IOException {
        br.getPage(parameter);
        fpName = br.getRegex("<h1 class=\\'title\\'>([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String allvids = br.getRegex("<div class=\\'related_episodes\\'>(.*?)</ul>[\t\n\r ]+</div>").getMatch(0);
        String[] chartinfo = new Regex(allvids, "<li (.*?)</a></li>").getColumn(0);
        for (final String chartnumber : chartinfo) {
            String title = new Regex(chartnumber, "title=\"([^<>]*?)\"").getMatch(0);
            final String url = new Regex(chartnumber, "data\\-url=\\'(https?://www\\.viva.tv/musikvideo/[^<>\"]*?)\\'").getMatch(0);
            if (title == null || url == null) {
                continue;
            }
            title = doFilenameEncoding(title) + default_ext;
            final DownloadLink dl = createDownloadlink(url);
            if (title.contains("Video nicht verfügbar")) {
                dl.setAvailable(false);
                title = title.replace("Video nicht verfügbar: ", "");
                dl.setFinalFileName(title);
            } else {
                dl.setAvailable(true);
                dl.setName(title);
            }
            decryptedLinks.add(dl);
        }
        if (decryptedLinks.size() == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final FilePackage fp = FilePackage.getInstance();
        fpName = Encoding.htmlDecode(fpName.trim());
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
    }

    private void decryptMtviggy() throws IOException {
        if (parameter.matches(type_mtvdesi) || parameter.matches(type_mtvk)) {
            parameter = "http://www.mtviggy.com/videos/" + new Regex(parameter, "([a-z0-9\\-]+)$").getMatch(0) + "/";
        }
        br.getPage(parameter);
        final String vevo = br.getRegex("(http://videoplayer\\.vevo\\.com/embed/embedded\\?videoId=[A-Za-z0-9]+)").getMatch(0);
        if (vevo != null) {
            logger.info("Current link is a VEVO link");
            decryptedLinks.add(createDownloadlink(vevo));
            return;
        }
        logger.info("Current link is NO VEVO link");
        final DownloadLink main = createDownloadlink(parameter.replace("mtviggy.com/", "mtviggy_jd_decrypted_jd_.com/"));
        if (!br.containsHTML("class=\"video\\-box\"") || br.getHttpConnection().getResponseCode() == 404) {
            main.setAvailable(false);
        } else {
            String filename = jd.plugins.hoster.VivaTv.getFilenameMTVIGGY(this.br);
            if (filename != null) {
                main.setName(filename + default_ext);
                main.setAvailable(true);
            }
        }
        decryptedLinks.add(main);

    }

    private void decryptSouthparkDe() throws IOException, DecrypterException {
        br.getPage(parameter);
        this.mgid = br.getRegex("media\\.mtvnservices\\.com/(mgid[^<>\"]*?)\"").getMatch(0);
        if (this.mgid == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String feedURL = String.format(getFEEDURL("southpark.de"), this.mgid);
        br.getPage(feedURL);
        fpName = getXML("title");
        decryptFeed();
        fpName = new Regex(parameter, "episoden/(s\\d{2}e\\d{2})").getMatch(0) + " - " + fpName;
        final FilePackage fp = FilePackage.getInstance();
        fpName = Encoding.htmlDecode(fpName.trim());
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
    }

    private void decryptSouthparkCc() throws IOException, DecrypterException {
        br.getPage(parameter);
        this.mgid = br.getRegex("data\\-mgid=\"(mgid[^<>\"]*?)\"").getMatch(0);
        if (this.mgid == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String feedURL = String.format(getFEEDURL("southpark.cc.com"), this.mgid);
        br.getPage(feedURL);
        fpName = getFEEDtitle(br.toString());
        decryptFeed();
        fpName = new Regex(parameter, "episodes/(s\\d{2}e\\d{2})").getMatch(0) + " - " + fpName;
        final FilePackage fp = FilePackage.getInstance();
        fpName = Encoding.htmlDecode(fpName.trim());
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
    }

    private void decryptFeed() throws DecrypterException {
        final String[] items = br.getRegex("<item>(.*?)</item>").getColumn(0);
        if (items == null || items.length == 0 || fpName == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        int counter = 0;
        for (final String item : items) {
            String title = getFEEDtitle(item);
            final String item_mgid = new Regex(item, "scheme=\"urn:mtvn:id\">(mgid[^<>\"]*?)</media:category>").getMatch(0);
            final String mediagen_url = new Regex(item, "url=\"(http://[^<>\"]+/mediagen[^<>\"]*?)\"").getMatch(0);
            if (title == null || item_mgid == null || mediagen_url == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            /* We don't need the intro - it's always the same! */
            if (counter == 0 && title.contains("Intro")) {
                continue;
            }
            title = doFilenameEncoding(title);
            String final_url = String.format(getEMBEDURL("ALL_OTHERS"), item_mgid);
            /* Dirty workaround to display the correct domain in JD. */
            if (parameter.matches(type_southpark_cc_episode)) {
                final_url = final_url.replace("video:southparkstudios.com:", "video:southparkstudios_jd_decrypted_jd_.com:");
            }
            final DownloadLink dl = createDownloadlink(final_url);
            dl.setName(title + this.default_ext);
            dl.setAvailable(true);
            dl.setProperty("mainlink", parameter);
            try {
                dl.setContentUrl(parameter);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                dl.setBrowserUrl(parameter);
            }
            decryptedLinks.add(dl);
        }
    }

    private void decryptVh1() throws DecrypterException, IOException {
        br.getPage(parameter);
        final String vevo_ID = br.getRegex("MTVN\\.Player\\.vevoVideoId = \"([A-Za-z0-9]+)\";").getMatch(0);
        if (vevo_ID != null) {
            logger.info("Current link is a VEVO link");
            decryptedLinks.add(createDownloadlink("http://www.vevo.com/watch/" + vevo_ID));
            return;
        }
        logger.info("Current link is NO VEVO link");
        final DownloadLink main = createDownloadlink(parameter.replace("vh1.com/", "vh1_jd_decrypted_jd_.com/"));
        if (!br.containsHTML("MTVN\\.VIDEO\\.PLAYER\\.instance") || br.getHttpConnection().getResponseCode() == 404) {
            main.setAvailable(false);
        } else {
            String filename = jd.plugins.hoster.VivaTv.getFilenameVH1(this.br);
            if (filename != null) {
                main.setName(filename + default_ext);
                main.setAvailable(true);
            }
        }
        decryptedLinks.add(main);
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "[^<]*?>([^<>]*?)</" + parameter + ">").getMatch(0);
    }

    private String getFEEDURL(final String domain) {
        return jd.plugins.hoster.VivaTv.feedURLs.get(domain);
    }

    private String getEMBEDURL(final String domain) {
        return jd.plugins.hoster.VivaTv.embedURLs.get(domain);
    }

    private String getFEEDtitle(final String source) {
        return jd.plugins.hoster.VivaTv.feedGetTitle(source);
    }

    private String doFilenameEncoding(final String filename) {
        return jd.plugins.hoster.VivaTv.doFilenameEncoding(filename);
    }

}
