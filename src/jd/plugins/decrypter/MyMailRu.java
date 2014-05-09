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
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "my.mail.ru" }, urls = { "http://(www\\.)?my\\.mail\\.ru(decrypted)?/[^<>/\"]+/[^<>/\"]+/photo(\\?album_id=[a-z0-9\\-_]+)?" }, flags = { 0 })
public class MyMailRu extends PluginForDecrypt {

    public MyMailRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        boolean setPackagename = true;
        if (parameter.contains("mail.rudecrypted/")) {
            setPackagename = false;
            parameter = parameter.replace("my.mail.rudecrypted/", "my.mail.ru/");
        }

        br.getPage(parameter);
        final String username = new Regex(parameter, "http://(www\\.)?my.mail.ru/[^<>/\"]+/([^<>/\"]+)/.+").getMatch(1);
        final String dirname = new Regex(parameter, "http://(www\\.)?my.mail.ru/([^<>/\"]+)/[^<>/\"]+/.+").getMatch(1);
        if (parameter.matches("http://(www\\.)?my\\.mail\\.ru/[^<>/\"]+/[^<>/\"]+/photo\\?album_id=[a-z0-9\\-_]+")) {
            // Decrypt an album
            if (br.containsHTML("class=oranzhe><b>Ошибка</b>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<h1 class=\"l\\-header1\">([^<>\"]*?)</h1>").getMatch(0);
            int offset = 0;
            final Regex parameterStuff = new Regex(parameter, "http://(www\\.)?my\\.mail\\.ru/[^<>/\"]+/([^<>/\"]+)/photo\\?album_id=(.+)");
            final String albumID = parameterStuff.getMatch(2);
            final double maxPicsPerSegment = Double.parseDouble(getData("imagesOffset"));

            final int imgCount = Integer.parseInt(getData("imagesTotal"));
            final int segmentCount = (int) StrictMath.ceil(imgCount / maxPicsPerSegment);
            int segment = 1;
            while (decryptedLinks.size() != imgCount) {
                logger.info("Decrypting segment " + segment + " of maybe " + segmentCount + " segments...");
                if (offset > 0) {
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage("http://my.mail.ru/" + dirname + "/" + username + "/ajax?ajax_call=1&func_name=photo.photostream&mna=false&mnb=false&encoding=windows-1251&arg_offset=" + offset + "&arg_marker=" + new Random().nextInt(1000) + "&arg_album_id=" + albumID);
                    br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                }
                final String[] items = br.getRegex("(<div class=\"l\\-catalog_item\" data\\-bubble\\-config=.*?</div>)").getColumn(0);
                for (final String item : items) {
                    final String url = new Regex(item, "style=\"background\\-image:url\\((http://content[a-z0-9\\-_\\.]+\\.my\\.mail\\.ru/[^<>\"]+p\\-\\d+\\.jpg)\\);").getMatch(0);
                    final String mainlink = new Regex(item, "\"(http://my\\.mail\\.ru/[^<>\"]+/photo/\\d+/\\d+\\.html)\"").getMatch(0);
                    final String ending = url.substring(url.lastIndexOf("."));
                    final DownloadLink dl = createDownloadlink("http://my.mail.ru/jdeatme" + System.currentTimeMillis() + new Random().nextInt(100000));
                    dl.setProperty("mainlink", mainlink);
                    dl.setProperty("ext", ending);
                    dl.setFinalFileName(new Regex(mainlink, "(\\d+)\\.html").getMatch(0) + ending);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
                offset += maxPicsPerSegment;
                segment++;
            }
            if (fpName != null && setPackagename) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
        } else {
            final String albumsAllText = br.getRegex("\"albumsAll\": \\[(.*?)\\]").getMatch(0);
            if (albumsAllText == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String[] albumsAll = new Regex(albumsAllText, "\"([^<>\"]*?)\"").getColumn(0);
            if (albumsAll == null || albumsAll.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String albumid : albumsAll) {
                final DownloadLink dl = createDownloadlink("http://my.mail.rudecrypted/" + dirname + "/" + username + "/photo?album_id=" + albumid);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setProperty("ALLOW_MERGE", true);
            fp.setName(username);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getData(final String parameter) {
        return br.getRegex("\"" + parameter + "\": (\")?([^<>\"]*?)(\"|,)").getMatch(1);
    }

    /** old album decrypt */

    // final int albumShowed = Integer.parseInt(getData("albumShowed"));
    // final int albumTotal = Integer.parseInt(getData("albumTotal"));
    // int segment = 1;
    // final int segmentCount = (int) StrictMath.ceil(albumTotal / albumShowed);
    // while (decryptedLinks.size() != albumTotal) {
    // logger.info("Decrypting segment " + segment + " of maybe " + segmentCount
    // + " segments...");
    // String[] albums = null;
    // if (segment == 1) {
    // albums =
    // br.getRegex("\"(http://(www\\.)?my\\.mail\\.ru/[^<>/\"]+/[^<>/\"]+/photo\\?album_id=[a-z0-9\\-_]+)\"").getColumn(0);
    // } else {
    // br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    // br.getPage("http://my.mail.ru/" + dirname + "/" + username +
    // "/ajax?ajax_call=1&func_name=photo.get_albums&mna=false&mnb=false&encoding=windows-1251&arg_album_ids=%5B%22"
    // + albumsAll[albumsAll.length - 2] + "%22%2C%22" +
    // albumsAll[albumsAll.length - 1] + "%22%5D");
    // albums =
    // br.getRegex("\"(http://(www\\.)?my\\.mail\\.ru/[^<>/\"]+/[^<>/\"]+/photo\\?album_id=[a-z0-9\\-_]+)\\\\\"").getColumn(0);
    // }
    // if (albums == null || albums.length == 0) {
    // logger.warning("Decrypter broken for link: " + parameter);
    // return null;
    // }
    // final FilePackage fp = FilePackage.getInstance();
    // fp.setProperty("ALLOW_MERGE", true);
    // fp.setName(username);
    // for (final String album : albums) {
    // final DownloadLink dl = createDownloadlink(album);
    // fp.add(dl);
    // try {
    // distribute(dl);
    // } catch (final Exception e) {
    // // Not available in old 0.9.851 Stable
    // }
    // decryptedLinks.add(dl);
    // }
    // segment++;
    // }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}