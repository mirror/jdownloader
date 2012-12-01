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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cosyupload.com" }, urls = { "https?://(www\\.)?cosyupload.com/uploads/[a-z0-9]+" }, flags = { 0 })
public class CosyUploadCom extends PluginForDecrypt {

    public CosyUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();

        String uploadId = parameter.substring(parameter.lastIndexOf("/") + 1);
        br.setFollowRedirects(true);
        br.getPage(parameter);

        if (br.containsHTML(">Page Not Found|>Sorry, but the page you were trying to")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);

        /* container */
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        ArrayList<DownloadLink> tempLinks = new ArrayList<DownloadLink>();
        for (String singleLink : br.getRegex("\"(/uploads/create_container\\?server=[a-z0-9\\-_]+\\&amp;upload_id=[a-z0-9]+)\"").getColumn(0)) {
            tempLinks = loadcontainer(br, "https://cosyupload.com" + Encoding.htmlDecode(singleLink));
            if (tempLinks != null && tempLinks.size() != 0) {
                for (final DownloadLink dl : tempLinks) {
                    decryptedLinks.add(dl);
                }
            }
        }

        if (decryptedLinks.size() == 0) {
            /* links */
            for (String link : br.getRegex("\'link_for_upload_" + uploadId + "_([^\']+)").getColumn(0)) {
                br.getPage("https://cosyupload.com/uploads/open_link/?upload_id=" + uploadId + "&server=" + link);
                String finallink = br.getRegex("\"link\":\"(http[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) continue;
                decryptedLinks.add(createDownloadlink(finallink));
            }

        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /** by jiaz */
    private ArrayList<DownloadLink> loadcontainer(final Browser br, final String dlclinks) throws IOException, PluginException {
        final Browser brc = br.cloneBrowser();

        if (dlclinks == null) { return new ArrayList<DownloadLink>(); }
        String test = Encoding.htmlDecode(dlclinks);
        File file = null;
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(dlclinks);
            if (con.getResponseCode() == 200) {
                if (con.isContentDisposition()) {
                    test = Plugin.getFileNameFromDispositionHeader(con.getHeaderField("Content-Disposition"));
                } else {
                    String s = new Regex(test, "server=(.*?)\\&").getMatch(0);
                    test = JDHash.getMD5(s) + "_" + s + ".dlc";
                }
                file = JDUtilities.getResourceFile("tmp/cosyupload/" + test);
                if (file == null) { return new ArrayList<DownloadLink>(); }
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                final ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return new ArrayList<DownloadLink>();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

}