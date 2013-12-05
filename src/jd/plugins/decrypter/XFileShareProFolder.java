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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "XFileShareProFolder" }, urls = { "https?://(www\\.)?(terafile\\.co|(exclusivefaile\\.com|exclusiveloader\\.com)|shareblue\\.eu|fileparadox\\.in|sharesix\\.com|ex-load\\.com|hulkload\\.com|sharingmaster\\.com|videofrog\\.eu|wallobit\\.com|mediafire\\.bz|maskfile\\.com|anafile\\.com|koofile\\.com|vidx\\.to|lovevideo\\.tv|isavelink\\.com|fastsonic\\.net|kingfiles\\.net|usefile\\.com|megarelease\\.org|sizfile\\.com|bestreams\\.net|filekai\\.com|redload\\.net|videodd\\.net|uncapped\\-downloads\\.com|jumbofiles\\.cc|vodlocker\\.com|powvideo\\.net|vidspot\\.net|videofox\\.net|upshared\\.com|rapidstation\\.com|mydisc\\.net|nirafile\\.com|filewe\\.com|dogupload\\.com|med1fire\\.com|videopremium\\.(net|tv)|vozupload\\.com|up\\.msrem\\.com|lunaticfiles\\.com|uploadinc\\.com|expressleech\\.com|swankshare\\.com|youwatch\\.org|enjoybox\\.in|(fileplaneta\\.com|fileplanet\\.com\\.ua)|junocloud\\.me|wooupload\\.com|filebulk\\.com|lomafile\\.com|streamratio\\.com|vshare\\.eu|vidplay\\.net|filepurpose\\.com|livecloudz\\.com|zingload\\.com|dumpfiles\\.org|treefiles\\.com|up\\.media1fire\\.com|sinhro\\.net|salefiles\\.com|tusfiles\\.net|ortofiles\\.com|filegag(plus)?\\.com|verzend\\.be|lemuploads\\.com|shareprofi\\.com|(mediafilestorage\\.net|multishared\\.me)|interfile\\.net|filezy\\.net|epicshare\\.net|fileom\\.com|goldbytez\\.com|sanshare\\.com|dupload\\.org|tishare\\.com|restfile\\.(ws|ca|co|com)|rapidstone\\.com|akafile\\.com|storagely\\.com|mightyupload\\.com|sube\\.me|lalata\\.info|guizmodl\\.net|(4upfiles\\.com|4up\\.(im|me))|senseless\\.tv|dynaupload\\.com|thefile\\.me|free\\-uploading\\.com|oteupload\\.com|iperupload\\.com|megafiles\\.se|filevice\\.com|filexb\\.com|wizzfile\\.com|rapidfileshare\\.net|rd\\-fs\\.com|hostinoo\\.com|fireget\\.com|creafile\\.net|4savefile\\.com|bitupload\\.com|galaxy\\-file\\.com|aa\\.vg|allbox4\\.com|ishareupload\\.com|project\\-free\\-upload\\.com|upfile\\.biz|gorillavid\\.in|ezzfile\\.(com|it|co\\.nz)|foxishare\\.com|your\\-filehosting\\.com|mp3the\\.net|kongsifile\\.com|mooshare\\.biz|xenubox\\.com|mixshared\\.com|longfiles\\.com|helluploads\\.com|novafile\\.com|saryshare\\.com|orangefiles\\.com|ufile\\.eu|qtyfiles\\.com|pizzaupload\\.com|filesbb\\.com|free\\-uploading\\.com|megaul\\.com|megaup1oad\\.net|fireuploads\\.net|(igetfile\\.com|pandamemo\\.com)|free\\-uploading\\.com|uload\\.to|cosmobox\\.org|uploadjet\\.net|fileove\\.com|rapidapk\\.com|hyshare\\.com|(uppit\\.com)|nosupload\\.com|potload\\.com|uploadbaz\\.com|simpleshare\\.org|ryushare\\.com|lafiles\\.com|clicktoview\\.org|lumfiles\\.com|gigfiles\\.net|shareonline\\.org|downloadani\\.me|movdivx\\.com|filenuke\\.com|((flashstream\\.in|sharefiles4u\\.com)|xvidstage\\.com|vidstream\\.in)|vidbux\\.com|queenshare\\.com|filesabc\\.com|((fiberupload|bulletupload)\\.com)|edoc\\.com|filesabc\\.com|henchfile\\.com|cometfiles\\.com)/(users/[a-z0-9_]+/[^\\?\r\n]+|folder/\\d+/[^\\?\r\n]+)|https?://(www\\.)?lafile\\.com/f/[a-z0-9]+|https?://(w(ww)?\\.)?hulkfile\\.(com|eu)/(users/[a-z0-9_]+/[^\\?\r\n]+|folder/\\d+/[^\\?\r\n]+)|https?://(www\\.)?fileom\\.com/go/[a-zA-Z0-9]+/.*\\.html|http?://(www\\.)?musickapoz\\.se/users/[a-z0-9]+" }, flags = { 0 })
@SuppressWarnings("deprecation")
public class XFileShareProFolder extends PluginForDecrypt {

    // DEV NOTES
    // other: keep last /.+ for fpName. Not needed otherwise.
    // other: group sister sites or aliased domains together, for easy
    // maintenance.
    // TODO: remove old xfileshare folder plugins after next major update.

    private String HOST      = null;
    private String parameter = null;

    /**
     * @author raztoki
     * */
    public XFileShareProFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> dupe = new HashSet<String>();
        parameter = param.toString();
        HOST = new Regex(parameter, "https?://(www\\.)?([^:/]+)").getMatch(1);
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
        dupe.add(parameter);
        parsePage(dupe, decryptedLinks);
        parseNextPage(dupe, decryptedLinks);

        if (fpName != null) {
            fpName = "Folder - " + (Encoding.urlDecode(fpName, false));
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(HashSet<String> dupe, ArrayList<DownloadLink> ret) throws PluginException {
        final String[] links = br.getRegex("href=\"(https?://(www\\.)?" + HOST + "/[a-z0-9]{12})(\"|/)").getColumn(0);
        if (links != null && links.length > 0) {
            for (String dl : links) {
                if (dupe.add(dl)) ret.add(createDownloadlink(dl));
            }
        }
        String folders[] = br.getRegex("folder.?\\.gif.*?<a href=\"(.+?" + HOST + "[^\"]+users/[^\"]+)").getColumn(0);
        if (folders != null && folders.length > 0) {
            for (String dl : folders) {
                if (dupe.add(dl)) ret.add(createDownloadlink(dl));
            }
        }
    }

    private boolean parseNextPage(HashSet<String> dupe, ArrayList<DownloadLink> ret) throws IOException, PluginException {
        // not sure if this is the same for normal folders, but the following
        // picks up users/username/*
        String nextPage = br.getRegex("<div class=\"paging\">[^\r\n]+<a href='([^']+&amp;page=\\d+)'>Next").getMatch(0);
        if (nextPage != null) {
            nextPage = HTMLEntities.unhtmlentities(nextPage);
            if (dupe.add(nextPage)) {
                br.getPage(parameter + nextPage);
                parsePage(dupe, ret);
                parseNextPage(dupe, ret);
                return true;
            }
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}