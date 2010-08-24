//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Paylesssofts.net" }, urls = { "http://[\\w\\.]*?paylesssofts\\.net/((rs/\\?id\\=)|(\\?))[\\w]+" }, flags = { 0 })
public class PlsssftsNt extends PluginForDecrypt {

    public PlsssftsNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String param = parameter.toString();
        br.setFollowRedirects(true);
        br.getPage(param);
        param = br.getURL();
        br.setFollowRedirects(false);

        String[] worker = param.split("=");
        String code = worker[1].substring(0, 3);
        String id = worker[1].substring(3);
        LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
        post.put("code", code);
        post.put("id", id);
        String rsOrMega = new Regex(param, "\\.net\\/(.*?)\\/").getMatch(0);
        br.postPage("http://www.paylesssofts.net/" + rsOrMega + "/fdngenerate.php", post);

        // If Uploader posted not existent URL, Decrypter reports a warning
        // which we catch here to get the link
        String failedUrl = br.getRegex("<b>Warning<\\/b>\\:  file\\((.*?)\\)\\:").getMatch(0);
        if (failedUrl != null) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));

        br.getPage("http://www.paylesssofts.net/" + rsOrMega + "/fdngetfile.php");
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://www.paylesssofts.net")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String finalurl = br.getRegex("<INPUT type=hidden value=(.*?) name=link>").getMatch(0);
        if (finalurl == null) return null;
        decryptedLinks.add(createDownloadlink(finalurl));
        return decryptedLinks;
    }

}
