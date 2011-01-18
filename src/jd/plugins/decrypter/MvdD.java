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
import java.util.Random;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.decrypter.TbCm.DestinationFormat;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myvideo.de" }, urls = { "http://[\\w\\.]*?myvideo\\.de/watch/[0-9]+/" }, flags = { 0 })
public class MvdD extends PluginForDecrypt {

    public MvdD(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private void addLink(final ArrayList<DownloadLink> decryptedLinks, final String parameter, final String link, final String name, final DestinationFormat convertTo) {
        final DownloadLink thislink = this.createDownloadlink(link);
        thislink.setProperty("ALLOW_DUPE", true);
        final FilePackage filePackage = FilePackage.getInstance();
        filePackage.setName("MyVideo " + convertTo.getText() + "(" + convertTo.getExtFirst() + ")");
        thislink.setFilePackage(filePackage);
        thislink.setBrowserUrl(parameter);
        thislink.setFinalFileName(name + ".tmp");
        thislink.setSourcePluginComment("Convert to " + convertTo.getText());
        thislink.setProperty("convertto", convertTo.name());
        decryptedLinks.add(thislink);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {

        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        this.br.setFollowRedirects(true);
        this.br.getPage(parameter);
        final String videoid = new Regex(parameter, "myvideo\\.de/watch/(\\d+)").getMatch(0);
        String serverpath = this.br.getRegex(Pattern.compile("<link rel='image_src'.*?href='(.*?)thumbs/.*?'.*?/><link", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (serverpath == null) {
            serverpath = this.br.getRegex("\"(http://is[0-9]+\\.myvideo\\.de/de/movie[0-9]+/.*?/)thumbs\"").getMatch(0);
        }
        if (videoid == null || serverpath == null) { return null; }
        final String link = serverpath + videoid + ".flv";
        String name = this.br.getRegex(Pattern.compile("<h1 class='globalHd'>(.*?)</h1>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (name == null) {
            name = Encoding.htmlDecode(this.br.getRegex(Pattern.compile("name='title' content='(.*?)Video - MyVideo'", Pattern.CASE_INSENSITIVE)).getMatch(0));
        }
        if (name == null) {
            name = "Video" + new Random().nextInt(10);
        }
        name = name.trim();

        this.addLink(decryptedLinks, parameter, link, name, DestinationFormat.AUDIOMP3);
        this.addLink(decryptedLinks, parameter, link, name, DestinationFormat.VIDEOFLV);

        return decryptedLinks;
    }

}
