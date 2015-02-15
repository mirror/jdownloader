//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "http://(www\\.)?myvideo\\.(de|at)/(watch/\\d+(/\\w+)?|[a-z0-9\\-]+/[a-z0-9\\-]+/[a-z0-9\\-]+\\-m\\-\\d+)" }, flags = { 0 })
public class MyVideoDeWrapper extends PluginForDecrypt {

    public MyVideoDeWrapper(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_watch   = "http://(www\\.)?myvideo\\.de/watch/\\d+(/\\w+)?";
    private static final String type_embed   = "http://(www\\.)?myvideo\\.de/embed/\\d+";
    private static final String type_special = "http://(www\\.)?myvideo\\.de/[a-z0-9\\-]+/[a-z0-9\\-]+/[a-z0-9\\-]+\\-m\\-\\d+";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Get host plugin */
        JDUtilities.getPluginForHost("myvideo.de");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("myvideo.at/", "myvideo.de/");
        if (parameter.matches(type_embed)) {
            parameter = "http://www.myvideo.de/watch/" + new Regex(parameter, "").getMatch(0);
        } else if (!parameter.matches(type_watch)) {
            parameter = "http://www.myvideo.de/watch/" + new Regex(parameter, "\\-m\\-(\\d+)$").getMatch(0);
        }
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String externID = br.getRegex("<object type=(\'|\")application/x\\-shockwave\\-flash(\'|\") data=(\'|\")(http://[^\'\"]+)").getMatch(3);
        if (externID != null && externID.contains("sevenload.com")) {
            br.setFollowRedirects(false);
            br.getPage(externID);
            String redirect = br.getRedirectLocation();
            if (redirect != null) {
                redirect = new Regex(redirect, "configPath=(.*?)$").getMatch(0);
                if (redirect != null) {
                    br.getPage(Encoding.htmlDecode(redirect));
                    externID = br.getRegex("<link target=\"_blank\">(http://[^<]+)").getMatch(0);
                    if (externID != null) {
                        decryptedLinks.add(createDownloadlink(externID));
                    }
                }
            }
        } else if (externID != null) {
            logger.info("MyVideoDeWrapper: nextLink??? = " + externID);
            return null;
        } else {
            final DownloadLink fina = createDownloadlink(parameter.replaceFirst("http", "fromDecrypter"));
            final String redirect = br.getRedirectLocation();
            if (redirect != null && (redirect.equals("http://www.myvideo.de/") || redirect.matches("http://(www\\.)?myvideo\\.de/channel/.+"))) {
                fina.setAvailable(false);
            } else if (redirect != null) {
                br.getPage(redirect);
            }
            final String filename = jd.plugins.hoster.MyVideo.getFilename(this.br);
            fina.setName(filename + ".flv");

            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}