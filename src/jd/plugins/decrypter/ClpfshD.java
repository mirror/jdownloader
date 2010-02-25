//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "clipfish.de" }, urls = { "http://[\\w\\.]*?clipfish\\.de/(.*?channel/\\d+/video/\\d+|video/\\d+(/.+)?|special/.*?/video/\\d+)" }, flags = { 0 })
public class ClpfshD extends PluginForDecrypt {

    private static final Pattern PATTERN_CAHNNEL_VIDEO = Pattern.compile("http://[\\w\\.]*?clipfish\\.de/.*?channel/\\d+/video/(\\d+)");
    private static final Pattern PATTERN_STANDARD_VIDEO = Pattern.compile("http://[\\w\\.]*?clipfish\\.de/video/(\\d+)(/.+)?");
    private static final Pattern PATTERN_SPECIAL_VIDEO = Pattern.compile("clipfish\\.de/special/.*?/video/(\\d+)");
    private static final Pattern PATTERN_FLV_FILE = Pattern.compile("&url=(http://.+?\\.flv)&", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_TITEL = Pattern.compile("<title>(.+?)</title>", Pattern.CASE_INSENSITIVE);
    private static final String XML_PATH = "http://www.clipfish.de/video_n.php?vid=";

    public ClpfshD(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        Regex regexInfo = new Regex(br.getPage(cryptedLink.getCryptedUrl()), PATTERN_TITEL);
        StringTokenizer tokenizer = new StringTokenizer(regexInfo.getMatch(0), "-");
        String name = "";
        int i = 0;
        while (tokenizer.hasMoreTokens() && i < 2) {
            i++;
            name += tokenizer.nextToken("-");
        }

        ArrayList<ConversionMode> possibleconverts = new ArrayList<ConversionMode>();
        int vidId = -1;
        if (new Regex(cryptedLink.getCryptedUrl(), PATTERN_STANDARD_VIDEO).matches()) {
            vidId = Integer.parseInt(new Regex(cryptedLink.getCryptedUrl(), PATTERN_STANDARD_VIDEO).getMatch(0));
        } else if (new Regex(cryptedLink.getCryptedUrl(), PATTERN_CAHNNEL_VIDEO).matches()) {
            vidId = Integer.parseInt(new Regex(cryptedLink.getCryptedUrl(), PATTERN_CAHNNEL_VIDEO).getMatch(0));
        } else if (new Regex(cryptedLink.getCryptedUrl(), PATTERN_SPECIAL_VIDEO).matches()) {
            vidId = Integer.parseInt(new Regex(cryptedLink.getCryptedUrl(), PATTERN_SPECIAL_VIDEO).getMatch(0));
        } else {
            logger.severe("No VidID found");
            return decryptedLinks;
        }
        String page = br.getPage(XML_PATH + vidId);
        String pathToflv = new Regex(page, PATTERN_FLV_FILE).getMatch(0);
        if (pathToflv == null) return null;
        DownloadLink downloadLink = createDownloadlink(pathToflv);
        possibleconverts.add(ConversionMode.VIDEOFLV);
        possibleconverts.add(ConversionMode.AUDIOMP3);
        possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);

        ConversionMode convertTo = Plugin.showDisplayDialog(possibleconverts, name, cryptedLink);
        downloadLink.setFinalFileName(name + ".tmp");
        downloadLink.setBrowserUrl(cryptedLink.getCryptedUrl());
        downloadLink.setSourcePluginComment("Convert to " + convertTo.getText());
        downloadLink.setProperty("convertto", convertTo.name());
        decryptedLinks.add(downloadLink);

        return decryptedLinks;
    }

}
