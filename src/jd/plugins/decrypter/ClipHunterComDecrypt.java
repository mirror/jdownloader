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
import java.util.LinkedHashMap;
import java.util.Random;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cliphunter.com" }, urls = { "http://(www\\.)?cliphunter\\.com/w/\\d+/\\w+" }, flags = { 0 })
public class ClipHunterComDecrypt extends PluginForDecrypt {

    public ClipHunterComDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // [0] =
    final public static String[][] qualities = { { "_h.flv", "540p.flv" }, { "_p.mp4", "_p480.mp4", "480p.mp4" }, { "_l.flv", "_p360.mp4", "360pflv.flv" }, { "_i.mp4", "360p.mp4" } };

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie("cliphunter.com", "qchange", "h");
        br.getPage(parameter);
        if (br.getURL().contains("error/missing") || br.containsHTML("(>Ooops, This Video is not available|>This video was removed and is no longer available at our site|<title></title>)")) {
            final DownloadLink dl = createDownloadlink("http://cliphunterdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
            dl.setName(new Regex(parameter, "cliphunter\\.com/w/\\d+/(.+)").getMatch(0));
            dl.setAvailable(false);
            dl.setProperty("offline", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>(.*?) \\-.*?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 style=\"font\\-size: 2em;\">(.*?) </h1>").getMatch(0);
        }
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());

        final LinkedHashMap<String, String> foundQualities = findAvailableVideoQualities();
        if (foundQualities == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        /** Decrypt qualities, selected by the user */
        final ArrayList<String> selectedQualities = new ArrayList<String>();
        final SubConfiguration cfg = SubConfiguration.getConfig("cliphunter.com");
        boolean q360p = cfg.getBooleanProperty("ALLOW_360P", false);
        boolean q360pflv = cfg.getBooleanProperty("ALLOW_360PFLV", false);
        boolean q480p = cfg.getBooleanProperty("ALLOW_480P", false);
        boolean q540p = cfg.getBooleanProperty("ALLOW_540P", false);
        if (q360p == false && q360pflv == false && q480p == false && q540p == false) {
            q360p = true;
            q360pflv = true;
            q480p = true;
            q540p = true;
        }
        if (q360p) selectedQualities.add("_i.mp4");
        if (q360pflv) selectedQualities.add("_l.flv");
        if (q480p) selectedQualities.add("_p.mp4");
        if (q540p) selectedQualities.add("_h.flv");
        final boolean fastLinkcheck = cfg.getBooleanProperty("FASTLINKCHECK", false);
        if (cfg.getBooleanProperty("ALLOW_BEST", false)) {
            String dllink = null;
            for (final String quality[] : qualities) {
                dllink = foundQualities.get(quality[0]);
                if (dllink != null) {
                    final DownloadLink dl = createDownloadlink("http://cliphunterdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                    dl.setFinalFileName(filename + "_" + quality[1]);
                    dl.setProperty("directlink", dllink);
                    dl.setProperty("originallink", parameter);
                    dl.setProperty("selectedquality", quality[0]);
                    if (fastLinkcheck) dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    break;
                }
            }
        } else {
            for (final String selectedQuality : selectedQualities) {
                final String dllink = foundQualities.get(selectedQuality);
                for (final String[] quality : qualities) {
                    final String currentQualityValue = quality[0];
                    if ((currentQualityValue == selectedQuality) && dllink != null) {
                        final DownloadLink dl = createDownloadlink("http://cliphunterdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(100000));
                        dl.setFinalFileName(filename + "_" + quality[1]);
                        dl.setProperty("directlink", dllink);
                        dl.setProperty("originallink", parameter);
                        dl.setProperty("selectedquality", quality[0]);
                        if (fastLinkcheck) dl.setAvailable(true);
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(filename);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /**
     * Same function in hoster and decrypterplugin, sync it!!
     * 
     * @throws IOException
     */
    private LinkedHashMap<String, String> findAvailableVideoQualities() throws IOException {
        // parse decryptalgo
        final String jsUrl = br.getRegex("<script.*src=\"(http://s\\.gexo.*?player\\.js)\"").getMatch(0);
        final String[] encryptedUrls = br.getRegex("var pl_fiji(_p|_i)? = \\'(.*?)\\'").getColumn(1);
        if (jsUrl == null || encryptedUrls == null || encryptedUrls.length == 0) return null;
        final Browser br2 = br.cloneBrowser();
        br2.getPage(jsUrl);
        String decryptAlgo = new Regex(br2, "decrypt\\:\\s?function(.*?\\})(,|;)").getMatch(0);
        if (decryptAlgo == null) return null;
        // Find available links
        final LinkedHashMap<String, String> foundQualities = new LinkedHashMap<String, String>();
        decryptAlgo = "function decrypt" + decryptAlgo + ";";
        String currentSr, tmpUrl;
        for (final String s : encryptedUrls) {
            tmpUrl = decryptUrl(decryptAlgo, s);
            currentSr = new Regex(tmpUrl, "sr=(\\d+)").getMatch(0);
            if (currentSr == null) {
                continue;
            }
            for (final String quality[] : qualities) {
                if (quality.length == 3) {
                    if (tmpUrl.contains(quality[1]) || tmpUrl.contains(quality[2])) {
                        foundQualities.put(quality[0], tmpUrl);
                        break;
                    }
                } else {
                    if (tmpUrl.contains(quality[0])) {
                        foundQualities.put(quality[0], tmpUrl);
                        break;
                    }
                }
            }
        }
        return foundQualities;
    }

    private String decryptUrl(final String fun, final String value) {
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(fun);
            result = inv.invokeFunction("decrypt", value);
        } catch (final Throwable e) {
            return null;
        }
        return result != null ? result.toString() : null;
    }

}
