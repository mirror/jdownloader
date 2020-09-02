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
import java.util.HashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer;
import jd.plugins.decrypter.BrightcoveDecrypter.BrightcoveEdgeContainer.Protocol;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "oracle.com" }, urls = { "https?://learn\\.oracle\\.com/ols/course/[a-z0-9\\-]+/\\d+/\\d+/\\d+|https?://learn\\.oracle\\.com/ords/training/DL4_EKITDOCUMENT\\.getPDF\\?p_url=[^\\&]+" })
public class OracleCom extends PluginForDecrypt {
    public OracleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
        // if (acc == null) {
        // throw new AccountRequiredException();
        // }
        /* Login if possible */
        if (acc != null) {
            final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.OracleCom) plg).login(acc, false);
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (parameter.contains(".getPDF")) {
            /* Crawl "flippdf" PDFs --> As single images */
            final String urlparam_p_url = UrlQuery.parse(parameter).get("p_url");
            final String[] jsURLs = br.getRegex("<script src=\"([^\"]+)\"></script>").getColumn(0);
            String largePath = null;
            String[] pages = null;
            String titleOfFirstPage = null;
            for (final String jsURL : jsURLs) {
                br.getPage(jsURL);
                if (largePath == null) {
                    largePath = br.getRegex("bookConfig\\.largePath=\"([^\"]+)\"").getMatch(0);
                }
                if (pages == null || pages.length == 0) {
                    pages = br.getRegex("page:\"(\\d+)\"").getColumn(0);
                    if (pages.length > 0) {
                        titleOfFirstPage = br.getRegex("caption:\"([^\"]+)\"").getMatch(0);
                    }
                }
            }
            if (largePath == null || pages == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final FilePackage fp = FilePackage.getInstance();
            if (titleOfFirstPage != null) {
                fp.setName(titleOfFirstPage);
            } else {
                /* Fallback */
                fp.setName(urlparam_p_url);
            }
            largePath = br.getURL(largePath).toString();
            for (final String page : pages) {
                final String url = largePath + page + ".jpg";
                final DownloadLink dl = this.createDownloadlink("directhttp://" + url);
                dl.setFinalFileName(page + ".jpg");
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
            }
        } else {
            final String json = br.getRegex("var globalConsData =\\s*(\\{.+\\});").getMatch(0);
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final String title = (String) entries.get("name");
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("components");
            /* Grab http qualities only! */
            final ArrayList<Protocol> allowedProtocols = new ArrayList<Protocol>();
            allowedProtocols.add(Protocol.HTTP);
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            for (final Object videoO : ressourcelist) {
                entries = (Map<String, Object>) videoO;
                final String playerAccountId = (String) entries.get("playerAccountId");
                final String videoId = (String) entries.get("videoId");
                final HashMap<String, BrightcoveEdgeContainer> qualities = jd.plugins.decrypter.BrightcoveDecrypter.findAllQualitiesNew(this.br, br.getHost(), null, playerAccountId, videoId);
                final BrightcoveEdgeContainer best = jd.plugins.decrypter.BrightcoveDecrypter.findBESTBrightcoveEdgeContainer(qualities);
                final DownloadLink dl = this.createDownloadlink("directhttp://" + best.getDownloadURL());
                dl.setFinalFileName(best.getStandardFilename());
                if (best.getFilesize() > 0) {
                    dl.setDownloadSize(best.getFilesize());
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                if (this.isAbort()) {
                    break;
                }
            }
        }
        return decryptedLinks;
    }
}
