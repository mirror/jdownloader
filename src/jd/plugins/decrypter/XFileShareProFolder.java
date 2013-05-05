//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "XFileShareProFolder" }, urls = { "https?://(www\\.)?(tishare\\.com|restfile\\.(ws|ca|co|com)|rapidstone\\.com|98file\\.com|farmupload\\.com|akafile\\.com|storagely\\.com|mightyupload\\.com|sube\\.me|lalata\\.info|guizmodl\\.net|(4upfiles\\.com|4up\\.(im|me))|senseless\\.tv|ufox\\.com|dynaupload\\.com|thefile\\.me|free\\-uploading\\.com|oteupload\\.com|iperupload\\.com|megafiles\\.se|uploadboxs\\.com|hotfiles\\.ws|cyberlocker\\.ch|filevice\\.com|filexb\\.com|wizzfile\\.com|rapidfileshare\\.net|rd\\-fs\\.com|hostinoo\\.com|fireget\\.com|filedefend\\.com|creafile\\.net|247upload\\.com|dippic\\.com|4savefile\\.com|fileprohost\\.com|bitupload\\.com|galaxy\\-file\\.com|aa\\.vg|allbox4\\.com|ishareupload\\.com|project\\-free\\-upload\\.com|upfile\\.biz|syfiles\\.com|gorillavid\\.in|ezzfile\\.(com|it)|foxishare\\.com|your\\-filehosting\\.com|mp3the\\.net|kongsifile\\.com|gbitfiles\\.com|ddl\\.mn|spaceha\\.com|mooshare\\.biz|flashdrive\\.it|zooupload\\.com|xenubox\\.com|mixshared\\.com|longfiles\\.com|helluploads\\.com|novafile\\.com|vidpe\\.com|saryshare\\.com|orangefiles\\.com|ufile\\.eu|filesega\\.com|qtyfiles\\.com|pizzaupload\\.com|filesbb\\.com|free\\-uploading\\.com|megaul\\.com|megaup1oad\\.net|fireuploads\\.net|filestay\\.com|(igetfile\\.com|pandamemo\\.com)|free\\-uploading\\.com|uload\\.to|cosmobox\\.org|uploadjet\\.net|fileove\\.com|rapidapk\\.com|hyshare\\.com|(uppit\\.com)|nosupload\\.com|idup\\.in|potload\\.com|uploadbaz\\.com|simpleshare\\.org|ryushare\\.com|lafiles\\.com|clicktoview\\.org|lumfiles\\.com|gigfiles\\.net|shareonline\\.org|downloadani\\.me|movdivx\\.com|filenuke\\.com|((flashstream\\.in|sharefiles4u\\.com)|xvidstage\\.com|vidstream\\.in)|ginbig\\.com|vidbux\\.com|queenshare\\.com|filesabc\\.com|((fiberupload|bulletupload)\\.com)|edoc\\.com|filesabc\\.com|fileduct\\.com|henchfile\\.com|xtilourbano\\.info)/(users/[a-z0-9_]+/.+|folder/\\d+/.+)|https?://(www\\.)?lafile\\.com/f/[a-z0-9]+" }, flags = { 0 })
public class XFileShareProFolder extends PluginForDecrypt {

    // DEV NOTES
    // other: keep last /.+ for fpName. Not needed otherwise.
    // other: group sister sites or aliased domains together, for easy maintenance.
    // TODO: add spanning folders + page support, at this stage it's not important.
    // TODO: remove old xfileshare folder plugins after next major update.

    public XFileShareProFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String HOST = new Regex(parameter, "https?://([^:/]+)").getMatch(0);
        if (HOST == null) {
            logger.warning("Failure finding HOST : " + parameter);
            return null;
        }
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie("http://" + HOST, "lang", "english");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("No such user exist")) {
            logger.warning("Incorrect URL or Invalid user : " + parameter);
            return null;
        }
        final String[] links = br.getRegex("href=\"(http://(www\\.)?" + HOST + "/[a-z0-9]{12})").getColumn(0);
        if (links != null && links.length > 0) {
            for (String dl : links) {
                decryptedLinks.add(createDownloadlink(dl));
            }
        }
        String folders[] = br.getRegex("folder.?\\.gif.*?<a href=\"(.+?" + HOST + "[^\"]+users/[^\"]+)").getColumn(0);
        if (folders != null && folders.length > 0) {
            for (String dl : folders) {
                decryptedLinks.add(createDownloadlink(dl));
            }
        }
        // name isn't needed, other than than text output for fpName.
        String fpName = new Regex(parameter, "folder/\\d+/.+/(.+)").getMatch(0); // name
        if (fpName == null) {
            fpName = new Regex(parameter, "folder/\\d+/(.+)").getMatch(0); // id
            if (fpName == null) {
                fpName = new Regex(parameter, "users/[a-z0-9_]+/.+/(.+)").getMatch(0); // name
                if (fpName == null) {
                    fpName = new Regex(parameter, "users/[a-z0-9_]+/(.+)").getMatch(0); // id
                }
            }
        }

        if (fpName != null) {
            fpName = "Folder - " + (Encoding.urlDecode(fpName, false));
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}