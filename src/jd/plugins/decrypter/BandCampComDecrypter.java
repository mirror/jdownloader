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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bandcamp.com" }, urls = { "http://(www\\.)?[a-z0-9\\-]+\\.bandcamp\\.com/album/[a-z0-9\\-_]+" }, flags = { 0 })
public class BandCampComDecrypter extends PluginForDecrypt {

    public BandCampComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FASTLINKCHECK = "FASTLINKCHECK";
    private static final String GRABTHUMB     = "GRABTHUMB";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig("bandcamp.com");
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, that something isn\\'t here\\.<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        final Regex inforegex = br.getRegex("<title>(.*?) \\| (.*?)</title>");
        final String[][] links = br.getRegex("\"(/track/[a-z0-9\\-]+)\" itemprop=\"url\"><span itemprop=\"name\">([^<>\"]*?)</span>").getMatches();
        String artist = inforegex.getMatch(1);
        String albumname = inforegex.getMatch(0);
        final String date = br.getRegex("<meta itemprop=\"datePublished\" content=\"(\\d+)\"/>").getMatch(0);
        if (links == null || links.length == 0 || artist == null || albumname == null || date == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        artist = Encoding.htmlDecode(artist.trim());
        albumname = Encoding.htmlDecode(albumname.trim());
        DecimalFormat df = new DecimalFormat("0");
        if (links.length > 999)
            df = new DecimalFormat("0000");
        else if (links.length > 99)
            df = new DecimalFormat("000");
        else if (links.length > 9) df = new DecimalFormat("00");
        int trackcounter = 1;
        final String bcLink = new Regex(parameter, "(http://.*?\\.bandcamp\\.com)/album/").getMatch(0);
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("bandcamp.com");
        for (final String linkinfo[] : links) {
            final String dllink = bcLink + linkinfo[0];
            final String fname = Encoding.htmlDecode(linkinfo[1].trim());
            final DownloadLink dl = createDownloadlink(dllink);
            dl.setProperty("fromdecrypter", true);
            dl.setProperty("directdate", date);
            dl.setProperty("directartist", artist);
            dl.setProperty("directalbum", albumname);
            dl.setProperty("directname", fname);
            dl.setProperty("type", "mp3");
            dl.setProperty("directtracknumber", df.format(trackcounter));
            final String formattedFilename = ((jd.plugins.hoster.BandCampCom) hostPlugin).getFormattedFilename(dl);
            dl.setName(formattedFilename);
            if (cfg.getBooleanProperty(FASTLINKCHECK, false)) dl.setAvailable(true);
            decryptedLinks.add(dl);
            trackcounter++;
        }

        final boolean decryptThumb = cfg.getBooleanProperty(GRABTHUMB, false);
        final String thumbnail = br.getRegex("artFullsizeUrl: \"(https?://[^<>\"]*?)\"").getMatch(0);
        if (decryptThumb && thumbnail != null) {
            final DownloadLink thumb = createDownloadlink("directhttp://" + thumbnail);
            decryptedLinks.add(thumb);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(artist + " - " + albumname);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}