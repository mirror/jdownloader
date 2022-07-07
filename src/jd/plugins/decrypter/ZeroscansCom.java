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

import java.net.URL;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
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
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zeroscans.com" }, urls = { "https?://(?:www\\.)?zeroscans\\.com/comics/([a-z0-9\\-]+)/(\\d+)" })
public class ZeroscansCom extends PluginForDecrypt {
    public ZeroscansCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        String fpName = br.getRegex("<title>Zero Scans \\- ([^<>\"]+)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback */
            fpName = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        }
        final String urlsJson = br.getRegex("good_quality:\\[([^\\]]+)").getMatch(0);
        final String[] links = PluginJSonUtils.unescape(urlsJson).replace("\"", "").split(",");
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int padLength = StringUtils.getPadLength(links.length);
        int position = 1;
        for (String singleLink : links) {
            if (!singleLink.startsWith("http") && !singleLink.startsWith("/")) {
                singleLink = "https://" + br.getHost() + "/storage/" + singleLink;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + singleLink);
            dl.setFinalFileName(StringUtils.formatByPadLength(padLength, position) + "_" + Plugin.getFileNameFromURL(new URL(singleLink)));
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            position++;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
