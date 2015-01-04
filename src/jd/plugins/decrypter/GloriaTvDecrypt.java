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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gloria.tv" }, urls = { "http://(www\\.)?gloria\\.tv/media/[A-Za-z0-9]+" }, flags = { 0 })
public class GloriaTvDecrypt extends PluginForDecrypt {

    public GloriaTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("class=\"missing\"")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String videotitle = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (videotitle == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        videotitle = Encoding.htmlDecode(videotitle).trim();
        String[][] finfo = null;
        final String fid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        if (getUserLogin(false)) {
            br.getPage("http://gloria.tv/?media=" + fid + "&action=download&language=KiaLEJq2fBR&particular=&_=" + System.currentTimeMillis());
            finfo = br.getRegex("\"(http://[a-z0-9\\-\\.]+\\.gloria.tv/[a-z0-9\\-]+/mediafile[^<>\"]*?)\".*?>Download (video|audio), ([a-z0-9:]+).*?, (\\d+\\.\\d{2} MB)</a>").getMatches();
        } else {
            finfo = new String[3][4];
            final String resolutions[] = { "686x432", "458x288", "228x144" };
            final String[] qualities = br.getRegex("\"(http://[a-z0-9\\-\\.]+\\.gloria\\.tv/[a-z0-9\\-]+/mediafile[^<>\"]*?)\"").getColumn(0);
            int counter = 0;
            for (String qualityurl : qualities) {
                final String qualinfo[] = new String[4];
                qualityurl = Encoding.htmlDecode(qualityurl);
                qualinfo[0] = qualityurl;
                qualinfo[1] = "video";
                qualinfo[2] = resolutions[counter];
                qualinfo[3] = "-1";
                finfo[counter] = qualinfo;
                counter++;
                if (counter == 3) {
                    break;
                }
            }
        }
        if (finfo == null || finfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String sinfo[] : finfo) {
            String ext;
            final String finallink = Encoding.htmlDecode(sinfo[0]);
            final String type = sinfo[1];
            final String lengtscale = sinfo[2];
            final String fsize = sinfo[3];
            String filename = videotitle + "_" + type + "_" + lengtscale;
            if (type.equals("audio")) {
                ext = ".mp3";
            } else {
                ext = ".mp4";
            }
            filename += ext;
            final DownloadLink dl = createDownloadlink("http://gloriadecrypted.tv/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setProperty("free_directlink", finallink);
            dl.setProperty("mainlink", parameter);
            dl.setFinalFileName(filename);
            if (!fsize.equals("-1")) {
                dl.setDownloadSize(SizeFormatter.getSize(fsize));
            }
            dl.setProperty("decryptedfilesize", fsize);

            dl.setProperty("decryptedfilename", filename);
            dl.setProperty("LINKDUPEID", "gloriatv_" + fid + "_" + filename);

            try {
                dl.setContentUrl(parameter);
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
                dl.setBrowserUrl(parameter);
            }

            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        // final FilePackage fp = FilePackage.getInstance();
        // fp.setName(videotitle);
        // fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings("deprecation")
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("gloria.tv");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            logger.info("There is no account available...");
            return false;
        }
        try {
            ((jd.plugins.hoster.GloriaTv) hostPlugin).login(this.br, aa, force);
        } catch (final PluginException e) {
            aa.setValid(false);
            return false;
        }
        return true;
    }

}
