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
import java.util.Iterator;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fernsehkritik.tv" }, urls = { "http://(www\\.)?fernsehkritik\\.tv/folge\\-\\d+" }, flags = { 0 })
public class FernsehkritikTvA extends PluginForDecrypt {

    private static final String DL_AS_MOV = "DL_AS_MOV";
    private static final String DL_AS_MP4 = "DL_AS_MP4";
    private static final String DL_AS_FLV = "DL_AS_FLV";
    private boolean             MOV       = true;
    private boolean             MP4       = true;
    private boolean             FLV       = true;

    public FernsehkritikTvA(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        FilePackage fp;
        final String parameter = param.toString();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        final String episode = new Regex(parameter, "folge-(\\d+)").getMatch(0);
        if (episode == null) { return null; }
        if (Integer.valueOf(episode) < 69) {
            final String[] finallinks = br.getRegex("\n\\s+<a href=\"(.*?)\">.*?").getColumn(0);
            final String title = br.getRegex("<a id=\"eptitle\".*?>(.*?)<").getMatch(0);
            if (finallinks == null || finallinks.length == 0 || title == null) { return null; }
            fp = FilePackage.getInstance();
            fp.setName(title);
            for (final String finallink : finallinks) {
                // mms not supported
                if (finallink.startsWith("mms")) {
                    continue;
                }
                final DownloadLink dlLink = createDownloadlink("directhttp://" + finallink);
                if (title != null) {
                    dlLink.setFinalFileName(title + fileExtension(finallink));
                }
                fp.add(dlLink);
                decryptedLinks.add(dlLink);
            }

        } else {
            final ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts("fernsehkritik.tv");
            Account account = null;
            if (accounts != null && accounts.size() != 0) {
                Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        account = n;
                        break;
                    }
                }
            }
            if (account != null) {
                SubConfiguration cfg = SubConfiguration.getConfig("fernsehkritik.tv");
                MOV = cfg.getBooleanProperty(DL_AS_MOV, true);
                MP4 = cfg.getBooleanProperty(DL_AS_MP4, true);
                FLV = cfg.getBooleanProperty(DL_AS_FLV, true);
                if (MOV) {
                    final DownloadLink dlLink = createDownloadlink("http://couch.fernsehkritik.tv/dl/fernsehkritik" + episode + ".mov");
                    decryptedLinks.add(dlLink);
                }
                if (MP4) {
                    final DownloadLink dlLink = createDownloadlink("http://couch.fernsehkritik.tv/dl/fernsehkritik" + episode + ".mp4");
                    decryptedLinks.add(dlLink);
                }
                if (FLV) {
                    final DownloadLink dlLink = createDownloadlink("http://couch.fernsehkritik.tv/userbereich/archive#stream:" + episode);
                    decryptedLinks.add(dlLink);
                }
                if (!MOV && !MP4 && !FLV) {
                    ArrayList<DownloadLink> dllinks = getParts(parameter, FilePackage.getInstance(), episode);
                    decryptedLinks.addAll(dllinks);
                }
            } else {
                ArrayList<DownloadLink> dllinks = getParts(parameter, FilePackage.getInstance(), episode);
                decryptedLinks.addAll(dllinks);
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) { return null; }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> getParts(String parameter, FilePackage fp, String episode) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter + "/Start/");
        final String fpName = br.getRegex("var flattr_tle = \\'(.*?)\\'").getMatch(0);
        final String[] jumps = br.getRegex("url: base \\+ \\'\\d+\\-(\\d+)\\.flv\\'").getColumn(0);
        if (jumps == null || jumps.length == 0) {
            logger.warning("FATAL error, no parts found for link: " + parameter);
            return null;
        }
        ArrayList<String> parts = new ArrayList<String>();
        parts.add("1");
        for (String jump : jumps)
            if (!parts.contains(jump)) parts.add(jump);
        fp = FilePackage.getInstance();
        fp.setName(fpName);
        for (final String part : parts) {
            String partname = null;
            if (part.equals("1"))
                partname = episode + ".flv";
            else
                partname = episode + "-" + part + ".flv";
            final DownloadLink dlLink = createDownloadlink("http://fernsehkritik.tv/jdownloaderfolge" + partname);
            dlLink.setFinalFileName(fpName + "_Teil" + part + ".flv");
            fp.add(dlLink);
            dlLink.setAvailable(true);
            decryptedLinks.add(dlLink);
        }
        return decryptedLinks;
    }

    private String fileExtension(final String arg) {
        String ext = arg.substring(arg.lastIndexOf("."));
        ext = ext == null ? ".flv" : ext;
        return ext;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}