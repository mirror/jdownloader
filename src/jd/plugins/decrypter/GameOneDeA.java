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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gameone.de" }, urls = { "http://((www|m)\\.)?gameone\\.de/(?!playtube/)(tv/\\d+(\\?part=\\d+)?|blog/\\d+/\\d+/.+|playtube/[\\w\\-]+/\\d+(/(sd|hd))?)|http://feedproxy.google.com/~r/mtvgameone/.*\\.mp3" }, flags = { 0 })
public class GameOneDeA extends PluginForDecrypt {

    public class ReplacerInputStream extends InputStream {

        private final byte[]      REPLACEMENT = "amp;".getBytes();
        private final byte[]      readBuf     = new byte[REPLACEMENT.length];
        private final Deque<Byte> backBuf     = new ArrayDeque<Byte>();
        private final InputStream in;

        /**
         * Replacing & to {@literal &amp;} in InputStreams
         * 
         * @author mhaller
         * @see <a href="http://stackoverflow.com/a/4588005">http://stackoverflow.com/a/4588005</a>
         */
        public ReplacerInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            if (!backBuf.isEmpty()) { return backBuf.pop(); }
            int first = in.read();
            if (first == '&') {
                peekAndReplace();
            }
            return first;
        }

        private void peekAndReplace() throws IOException {
            int read = super.read(readBuf, 0, REPLACEMENT.length);
            for (int i1 = read - 1; i1 >= 0; i1--) {
                backBuf.push(readBuf[i1]);
            }
            for (int i = 0; i < REPLACEMENT.length; i++) {
                if (read != REPLACEMENT.length || readBuf[i] != REPLACEMENT[i]) {
                    for (int j = REPLACEMENT.length - 1; j >= 0; j--) {
                        // In reverse order
                        backBuf.push(REPLACEMENT[j]);
                    }
                    return;
                }
            }
        }

    }

    private Document doc;

    public GameOneDeA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.startsWith("http://feedproxy.google.com")) {
            br.getPage(parameter);
            parameter = br.getRedirectLocation();
            if (parameter != null) {
                decryptedLinks.add(createDownloadlink(parameter));
            }
            return null;
        } else {
            setBrowserExclusive();
            br.setFollowRedirects(false);
            br.setReadTimeout(60 * 1000);
            br.getPage(parameter);
            br.setFollowRedirects(true);
            if (br.getRedirectLocation() != null) {
                if (br.getHttpConnection().getResponseCode() == 302) {
                    logger.info("Link offline or no downloadable content found: " + parameter);
                    return decryptedLinks;
                } else if (br.getHttpConnection().getResponseCode() == 301) {
                    br.getPage(parameter);
                }
            }

            // if (!br.containsHTML("\"player_swf\"")) {
            // logger.info("Wrong/Unsupported link: " + parameter);
            // return decryptedLinks;
            // }

            String dllink, filename;
            boolean newEpisode = true;
            final String episode = new Regex(parameter, "http://(www\\.)?gameone\\.de/tv/(\\d+)").getMatch(1);
            if (episode != null && Integer.parseInt(episode) < 102) {
                newEpisode = false;
            }

            final String[] mrssUrl = br.getRegex("\\.addVariable\\(\"mrss\"\\s?,\\s?\"(http://.*?)\"").getColumn(0);
            String fpName = br.getRegex("<title>(.*?)( \\||</title>)").getMatch(0);
            fpName = fpName == null ? br.getRegex("<h2>\n?(.*?)\n?</h2>").getMatch(0) : fpName;

            if (fpName == null) return null;

            fpName = fpName.replaceAll(" (-|~) Teil \\d+", "");
            fpName = fpName.replaceAll("\\.", "/");
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());

            /* audio, pictures */
            if (mrssUrl == null || mrssUrl.length == 0) {
                if (br.containsHTML("><img src=\"/images/dummys/dummy_agerated\\.jpg\"")) {
                    UserIO.getInstance().requestMessageDialog(UserIO.STYLE_HTML, "Link momentan inaktiv", "<b><font color=\"red\">" + parameter + "</font></b><br /><br />Vollstängiger Inhalt zu o.g. Link ist zwischen 22:00 und 06:00 Uhr verfügbar!");
                    return null;
                }
                String[] pictureOrAudio = null;
                if (br.containsHTML("<div class=\'gallery\' id=\\'gallery_\\d+\'")) {
                    pictureOrAudio = br.getRegex("<div class=\'gallery_image\'>.*?<a href=\"(http://.*?/gallery_pictures/.*?/large/.*?)\"").getColumn(0);
                } else if (br.containsHTML("class=\"flash_container_audio\"")) {
                    pictureOrAudio = br.getRegex("<a href=\"(http://[^<>]+\\.mp3)").getColumn(0);
                    if (pictureOrAudio == null || pictureOrAudio.length == 0) {
                        pictureOrAudio = br.getRegex(",\\s?file:\\s?\"(http://[^<>\",]+)").getColumn(0);
                    }
                }
                if (pictureOrAudio == null || pictureOrAudio.length == 0) {
                    logger.warning("Decrypter out of date or no downloadable content found for link: " + parameter + ". Please check the Website!");
                    return decryptedLinks;
                }
                if (pictureOrAudio.length <= 10) {
                    newEpisode = false;
                }
                for (final String ap : pictureOrAudio) {
                    final DownloadLink dlLink = createDownloadlink(ap);
                    if (newEpisode) {
                        dlLink.setAvailable(true);
                    }
                    fp.add(dlLink);
                    decryptedLinks.add(dlLink);
                }
                return decryptedLinks;
            }

            /* video: blog, tv, playtube */
            for (String startUrl : mrssUrl) {
                startUrl = startUrl.replaceAll("http://(.*?)/", "http://www.gameone.de/api/mrss/");

                XPath xPath = xmlParser(startUrl);
                NodeList linkList, partList;
                XPathExpression expr = null;
                try {
                    filename = xPath.evaluate("/rss/channel/item/title", doc);
                    expr = xPath.compile("/rss/channel/item/group/content[@type='text/xml']/@url");
                    partList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                    if (partList == null || partList.getLength() == 0) throw new Exception("PartList empty");
                } catch (final Throwable e) {
                    return null;
                }
                final DecimalFormat df = new DecimalFormat("000");
                for (int i = 0; i < partList.getLength(); ++i) {
                    final Node partNode = partList.item(i);
                    startUrl = partNode.getNodeValue();
                    if (startUrl == null) continue;
                    /* Episode 1 - 101 */
                    startUrl = startUrl.replaceAll("media/mediaGen\\.jhtml\\?uri.*?\\.de:", "flv/flvgen.jhtml?vid=");

                    xPath = xmlParser(startUrl);
                    try {
                        expr = xPath.compile("/package/video/item/src|//rendition/src");
                        linkList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                        if (linkList == null || linkList.getLength() == 0) throw new Exception("LinkList empty");
                    } catch (final Throwable e) {
                        continue;
                    }
                    for (int j = 0; j < linkList.getLength(); ++j) {
                        final Node node = linkList.item(j);
                        dllink = node.getTextContent();
                        if (dllink == null) continue;
                        String q = new Regex(dllink, "(\\d+)k_").getMatch(0);
                        q = q == null ? "" : quality(Integer.parseInt(q));
                        String ext = dllink.substring(dllink.lastIndexOf("."));
                        ext = ext == null || ext.length() > 4 ? ".flv" : ext;

                        /* Episode > 102 */
                        dllink = dllink.replaceAll("^.*?/r2/", "http://cdn.riptide-mtvn.com/r2/");
                        /* Fallback */
                        dllink = dllink.replace("rtmp", "gameonertmp");

                        dllink = dllink.startsWith("http") ? "directhttp://" + dllink : dllink;
                        final DownloadLink dlLink = createDownloadlink(dllink);
                        if (!newEpisode) {
                            dlLink.setFinalFileName(filename + "_Part_" + df.format(i + 1) + "@" + q + ext);
                        } else {
                            dlLink.setFinalFileName(filename + "@" + q + ext);
                        }
                        fp.add(dlLink);
                        decryptedLinks.add(dlLink);
                    }
                }
            }
            if (decryptedLinks == null || decryptedLinks.size() == 0) {
                logger.warning("Decrypter out of date for link: " + parameter);
                return null;
            }
        }
        return decryptedLinks;
    }

    private String quality(final int q) {
        if (q <= 350) { return "LOW"; }
        if (q <= 850) { return "MEDIUM"; }
        return "HIGH";
    }

    private XPath xmlParser(final String linkurl) throws Exception {
        try {
            final URL url = new URL(linkurl);
            final InputStream stream = url.openStream();
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            try {
                doc = parser.parse(new ReplacerInputStream(stream));
                return xPath;
            } finally {
                try {
                    stream.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final Throwable e2) {
            return null;
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}