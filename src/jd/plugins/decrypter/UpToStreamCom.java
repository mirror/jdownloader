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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "uptostream.com" }, urls = { "https?://(?:www\\.)?uptostream\\.com/(?:iframe/)?[a-z0-9]{12}|https?://(?:www\\.)?uptobox\\.com/\\?op=user_public\\&hash=[a-f0-9]{16}\\&folder=\\d+" })
public class UpToStreamCom extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public UpToStreamCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOMAIN = "uptostream.com";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter;
        /* Load sister-host plugin */
        JDUtilities.getPluginForHost(DOMAIN);
        final ArrayList<String> dupecheck = new ArrayList<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.toString().contains("user_public")) {
            parameter = param.toString();
            final Regex urlInfo = new Regex(parameter, "hash=([a-f0-9]{16})\\&folder=(\\d+)");
            final String hash = urlInfo.getMatch(0);
            final String folderID = urlInfo.getMatch(1);
            br.getHeaders().put("Accept", "application/json, text/plain, */*");
            int pageMax = 1;
            int pageCurrent = 0;
            int offset = 0;
            do {
                pageCurrent++;
                /* 2018-10-18: default = "limit=10" */
                br.getPage("https://uptobox.com/api/user/public?folder=" + folderID + "&hash=" + hash + "&orderBy=file_name&dir=asc&limit=100&offset=" + offset);
                final String errormessage = PluginJSonUtils.getJson(br, "message");
                if (!StringUtils.isEmpty(errormessage) && !StringUtils.equalsIgnoreCase(errormessage, "Success")) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    break;
                }
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("data");
                if (pageCurrent == 1) {
                    /* Set maxPage on first request */
                    pageMax = (int) JavaScriptEngineFactory.toLong(entries.get("pageCount"), 0);
                }
                logger.info("Processing loop " + pageCurrent + " of " + pageMax);
                final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("list");
                for (final Object fileO : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) fileO;
                    final String linkid = (String) entries.get("file_code");
                    final String filename = (String) entries.get("file_name");
                    if (StringUtils.isEmpty(linkid) || StringUtils.isEmpty(filename)) {
                        continue;
                    }
                    /* Extra failsafe - avoid infinite loop! */
                    if (dupecheck.contains(linkid)) {
                        logger.info("Found dupe");
                        break;
                    }
                    final DownloadLink dl = this.createDownloadlink("https://" + br.getHost() + "/" + linkid);
                    dl.setName(filename);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    distribute(dl);
                    dupecheck.add(linkid);
                    offset++;
                }
            } while (pageCurrent < pageMax && !this.isAbort());
        } else {
            final String main_id = new Regex(param.toString(), "([a-z0-9]{12})").getMatch(0);
            final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
            if (this.getPluginConfig().getBooleanProperty(jd.plugins.hoster.UpToStreamCom.PROPERTY_SSL, false)) {
                parameter = "https://uptostream.com/" + main_id;
            } else {
                parameter = "http://uptostream.com/" + main_id;
            }
            final String url_uptobox = "http://uptobox.com/" + main_id;
            final String nicehost = new Regex(parameter, "http://(?:www\\.)?([^/]+)").getMatch(0);
            final String decryptedhost = "http://" + nicehost + "decrypted";
            final boolean fastcheck = cfg.getBooleanProperty(jd.plugins.hoster.UpToStreamCom.PROPERTY_FAST_LINKCHECK, false);
            br.setFollowRedirects(true);
            try {
                br.getPage(parameter);
            } catch (final Throwable e) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String fpName = br.getRegex("id=\"titleVid\">([^<>\"]*?)<").getMatch(0);
            if (fpName == null) {
                fpName = main_id;
            }
            fpName = Encoding.htmlDecode(fpName).trim();
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("404 \\(File not found\\)")) {
                /* Simply add the main host url, maybe it is still online on there. */
                decryptedLinks.add(this.createDownloadlink(url_uptobox));
                return decryptedLinks;
            }
            String[] videosinfo = br.getRegex("\\'(http://[^/]*?uptostream\\.com/[^/]+/\\d+/\\d+)\\'").getColumn(0);
            HashMap<String, DownloadLink> foundLinks_all = new HashMap<String, DownloadLink>();
            /* parse flash url */
            ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
            for (final String videourl : videosinfo) {
                final String quality = new Regex(videourl, "(\\d+)/\\d+$").getMatch(0);
                if (quality == null) {
                    return null;
                }
                final DownloadLink dl = this.createDownloadlink(decryptedhost + "/" + main_id + "_" + quality);
                dl.setFinalFileName(fpName + "_" + quality + "p.mp4");
                dl.setProperty("directlink", videourl);
                dl.setProperty("mainlink", parameter);
                if (fastcheck) {
                    dl.setAvailable(true);
                }
                dl.setContentUrl(parameter);
                foundLinks_all.put(quality, dl);
            }
            final Iterator<Entry<String, DownloadLink>> it = foundLinks_all.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, DownloadLink> next = it.next();
                final String qualityInfo = next.getKey();
                final DownloadLink dl = next.getValue();
                if (cfg.getBooleanProperty(qualityInfo, true)) {
                    newRet.add(dl);
                }
            }
            final String url_subtitle = this.br.getRegex("\\'(https?://[^<>\"]*?\\.srt)\\'").getMatch(0);
            if (url_subtitle != null && cfg.getBooleanProperty(jd.plugins.hoster.UpToStreamCom.PROPERTY_SUBTITLE, true)) {
                final DownloadLink dl = this.createDownloadlink(decryptedhost + "/" + main_id + "_" + jd.plugins.hoster.UpToStreamCom.PROPERTY_SUBTITLE);
                dl.setFinalFileName(fpName + "_subtitle.srt");
                dl.setProperty("directlink", url_subtitle);
                dl.setProperty("mainlink", parameter);
                if (fastcheck) {
                    dl.setAvailable(true);
                }
                dl.setContentUrl(parameter);
                decryptedLinks.add(dl);
            }
            if (cfg.getBooleanProperty(jd.plugins.hoster.UpToStreamCom.PROPERTY_ORIGINAL, true)) {
                final DownloadLink dl = this.createDownloadlink(url_uptobox);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(newRet);
            decryptedLinks.addAll(newRet);
        }
        return decryptedLinks;
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}