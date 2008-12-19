//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class SpiegelDe extends PluginForDecrypt {

    private static final Pattern PATTERN_SUPPORTED_VIDEO = Pattern.compile("http://[\\w\\.]*?spiegel\\.de/video/video-(\\d+).html", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SUPPORED_FOTOSTRECKE = Pattern.compile("http://[\\w\\.]*?spiegel\\.de/fotostrecke/fotostrecke-\\d+(-\\d+)?.html", Pattern.CASE_INSENSITIVE);

    // Patterns für Vidos

    private static final Pattern PATTERN_THEMA = Pattern.compile("<headline>(.+?)</headline>");
    private static final Pattern PATTERN_HEADLINE = Pattern.compile("<thema>(.+?)</thema>");
    private static final Pattern PATTERN_TEASER = Pattern.compile("<teaser>(.+?)</teaser>");

    /*
     * Type 1: h263 flv Type 2: flv mid (VP6) Type 3: h263 low Type 4: flv low
     * (VP6) Type 5: flv high (VP6) (680544) Type 6: h263 3gp Type 7: h263 3gp
     * low Type 8: iphone mp4 Type 9: podcast mp4 640480
     */

    private static final Pattern PATTERN_FILENAME = Pattern.compile("<filename>(.+?)</filename>");
    private static final Pattern PATTERN_FILENAME_T5 = Pattern.compile("^\\s+<type5>\\s+$\\s+^\\s+" + PATTERN_FILENAME.toString(), Pattern.MULTILINE);
    private static final Pattern PATTERN_FILENAME_T6 = Pattern.compile("^\\s+<type6>\\s+$\\s+^\\s+" + PATTERN_FILENAME.toString(), Pattern.MULTILINE);
    private static final Pattern PATTERN_FILENAME_T8 = Pattern.compile("^\\s+<type8>\\s+$\\s+^\\s+" + PATTERN_FILENAME.toString(), Pattern.MULTILINE);
    private static final Pattern PATTERN_FILENAME_T9 = Pattern.compile("^\\s+<type9>\\s+$\\s+^\\s+" + PATTERN_FILENAME.toString(), Pattern.MULTILINE);

    // Patterns für Fotostrecken

    private static final Pattern PATTERN_IMG_URL = Pattern.compile("<a id=\"spFotostreckeControlImg\" href=\"(/fotostrecke/fotostrecke-\\d+-\\d+.html)\"><img src=\"(http://www.spiegel.de/img/.+?(\\.\\w+?))\"");
    private static final Pattern PATTERN_IMG_TITLE = Pattern.compile("<meta name=\"description\" content=\"(.+?)\" />");

    public SpiegelDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (new Regex(cryptedLink.getCryptedUrl(), PATTERN_SUPPORTED_VIDEO).matches()) {
            String id = new Regex(cryptedLink.getCryptedUrl(), PATTERN_SUPPORTED_VIDEO).getMatch(0);
            String xmlEncodings = br.getPage("http://video.spiegel.de/flash/" + id + ".xml");
            String xmlInfos = br.getPage("http://www1.spiegel.de/active/playlist/fcgi/playlist.fcgi/asset=flashvideo/mode=id/id=" + id);
            String name = new Regex(xmlInfos, PATTERN_THEMA).getMatch(0) + "-" + new Regex(xmlInfos, PATTERN_HEADLINE).getMatch(0);
            String comment = new Regex(xmlInfos, PATTERN_TEASER).getMatch(0);

            Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
            possibleconverts.add(ConversionMode.VIDEOFLV);
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);
            possibleconverts.add(ConversionMode.VIDEO3GP);
            possibleconverts.add(ConversionMode.VIDEOMP4);
            possibleconverts.add(ConversionMode.PODCAST);
            possibleconverts.add(ConversionMode.IPHONE);
            ConversionMode convertTo = Plugin.DisplayDialog(possibleconverts.toArray(), name, cryptedLink);

            if (convertTo != null) {
                DownloadLink downloadLink = null;
                String fileName;
                if (convertTo == ConversionMode.VIDEO3GP) {
                    // type 6
                    fileName = new Regex(xmlEncodings, PATTERN_FILENAME_T6).getMatch(0);
                    downloadLink = createDownloadlink("http://video.promobil2spiegel.netbiscuits.com/" + fileName);
                    downloadLink.setFinalFileName(name + ".3gp");
                } else if (convertTo == ConversionMode.IPHONE) {
                    // type 8
                    fileName = new Regex(xmlEncodings, PATTERN_FILENAME_T8).getMatch(0);
                    downloadLink = createDownloadlink("http://video.promobil2spiegel.netbiscuits.com/" + fileName);
                    downloadLink.setFinalFileName(name + ".mp4");
                } else if (convertTo == ConversionMode.VIDEOMP4 || convertTo == ConversionMode.PODCAST) {
                    // type9
                    fileName = new Regex(xmlEncodings, PATTERN_FILENAME_T9).getMatch(0);
                    downloadLink = createDownloadlink("http://video.promobil2spiegel.netbiscuits.com/" + fileName);
                    downloadLink.setFinalFileName(name + ".mp4");
                } else {
                    // type5
                    fileName = new Regex(xmlEncodings, PATTERN_FILENAME_T5).getMatch(0);
                    downloadLink = createDownloadlink("http://video.spiegel.de/flash/" + fileName);
                    downloadLink.setFinalFileName(name + ".tmp");
                }
                downloadLink.setSourcePluginComment(comment);
                downloadLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                downloadLink.setProperty("convertto", convertTo.name());
                decryptedLinks.add(downloadLink);
            }
        } else if (new Regex(cryptedLink.getCryptedUrl(), PATTERN_SUPPORED_FOTOSTRECKE).matches()) {
            String group3 = new Regex(cryptedLink.getCryptedUrl(), PATTERN_SUPPORED_FOTOSTRECKE).getMatch(0);
            if (group3 != null) {
                // Sicherstellen, dass mit dem 1. Bild begonnen wird!
                decryptedLinks.add(createDownloadlink(cryptedLink.getCryptedUrl().replaceAll(group3 + "\\.html", ".html")));
                return decryptedLinks;
            }

            String url = cryptedLink.getCryptedUrl();
            String title = new Regex(br.getPage(url), PATTERN_IMG_TITLE).getMatch(0);
            int count = 1;
            FilePackage filePackage = new FilePackage();
            filePackage.setName(title);
            while (url != null) {
                String page = br.getPage(url);
                Regex regex = new Regex(page, PATTERN_IMG_URL);
                url = regex.getMatch(0);
                url = url != null ? "http://www.spiegel.de" + url : null;
                String imgLink = regex.getMatch(1);
                String ending = regex.getMatch(2);
                if (imgLink != null) {
                    DownloadLink dlLink = createDownloadlink(imgLink);
                    dlLink.setFilePackage(filePackage);
                    dlLink.setFinalFileName(title + "-" + count + ending);
                    dlLink.setName(dlLink.getFinalFileName());
                    if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER, false) == false) {
                        dlLink.addSubdirectory("spiegel.de - fotostrecken");
                    }
                    decryptedLinks.add(dlLink);
                    count++;
                }

            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
