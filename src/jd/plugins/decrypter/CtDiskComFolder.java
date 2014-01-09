//  jDownloader - Downloadmanager
//  Copyright (C) 2012  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ctdisk.com" }, urls = { "https?://(www\\.)?(ctdisk|400gb|pipipan|t00y)\\.com/u/\\d{6,7}(/\\d{6,7})?" }, flags = { 0 })
public class CtDiskComFolder extends PluginForDecrypt {

    // DEV NOTES
    // protocol: no https.
    // t00y doesn't seem to work as alias but ill add it anyway.

    private static final String domains = "(ctdisk|400gb|pipipan|t00y)\\.com";
    // user unique id
    private String              uuid    = null;
    // folder unique id
    private String              fuid    = null;
    private static String       agent   = null;
    private static Object       LOCK    = new Object();

    public CtDiskComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser prepBrowser(Browser prepBr) {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 1000);
        prepBr.setCookiesExclusive(true);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        prepBr.setReadTimeout(3 * 60 * 1000);
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
        return prepBr;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("https://", "http://").replaceAll(domains + "/", "400gb.com/");
        prepBrowser(br);
        // lock to one thread!
        synchronized (LOCK) {
            getPage(br, parameter);
            uuid = new Regex(parameter, domains + "/u/(\\d+)").getMatch(1);
            fuid = new Regex(parameter, domains + "/u/\\d+/(\\d+)").getMatch(1);
            if (fuid == null) fuid = "0";
            if (br.containsHTML("(Due to the limitaion of local laws, this url has been disabled!<|该用户还未打开完全共享\\。|您目前无法访问他的资源列表\\。)")) {
                logger.info("Invalid URL: " + parameter);
                return decryptedLinks;
            }

            String fpName = null;
            if (!"0".equals(fuid)) {
                // covers sub directories. /u/uuid/fuid/
                fpName = br.getRegex("href=\"/u/" + uuid + "/" + fuid + "\">(.*?)</a>").getMatch(0);
                // fail over
                if (fpName == null && uuid != null) fpName = "User " + uuid + " - Sub Directory " + fuid;
            }
            // covers base /u/\d+ directories,
            // no fpName for these as results of base directory returns subdirectories.

            parsePage(decryptedLinks, parameter);

            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        }
    }

    private Browser prepAjax(Browser prepBr) {
        prepBr.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        prepBr.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        prepBr.getHeaders().put("Accept-Charset", null);
        return prepBr;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String parameter) throws Exception {
        String ajaxSource = br.getRegex("\"sAjaxSource\": \"(/iajax_guest\\.php\\?item=file_act&action=file_list&folder_id=" + fuid + "&uid=" + uuid + "&task=file_list&t=\\d+&k=[a-f0-9]{32})\"").getMatch(0);
        if (ajaxSource == null) {
            logger.warning("Can not find 'ajax source' : " + parameter);
            return;
        }
        Browser ajax = br.cloneBrowser();
        prepAjax(ajax);
        getPage(ajax, ajaxSource);
        ajax.getHttpConnection().getRequest().setHtmlCode(ajax.toString().replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\""));
        final String[][] results = ajax.getRegex("(href=\"([^\"]+/file/\\d+)\">(.*?)</a>(\\\\t){1,}\",\"([\\d\\.]+ [KMGT]?B)\")").getMatches();
        // export folders back into decrypter again.
        final String[] folders = ajax.getRegex("<a href=\"(/u/" + uuid + "/\\d+)\">").getColumn(0);
        if ((folders == null || folders.length == 0) && (results == null || results.length == 0)) {
            if (ajax.containsHTML("\"iTotalRecords\":\"0\"")) {
                logger.info("Link offline (empty): " + parameter);
                return;
            }
            ret = null;
            return;
        }
        if (results != null && results.length != 0) {
            for (String[] args : results) {
                DownloadLink dl = createDownloadlink(args[1]);
                if (args[1] != null) {
                    dl.setName(unescape(args[2]));
                    if (args[4] != null) dl.setDownloadSize(SizeFormatter.getSize(args[4]));
                    dl.setAvailable(true);
                }
                ret.add(dl);
            }
        }
        if (folders != null && folders.length != 0) {
            final String host = new Regex(br.getURL(), "(https?://(www\\.)?" + domains + ")").getMatch(0);
            if (host == null) {
                logger.info("Could not determine Host :: " + parameter);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String folder : folders) {
                ret.add(createDownloadlink(host + folder));
            }
        }

    }

    /**
     * site has really bad connective issues, this method helps retry over throwing exception at the first try
     * 
     * @author raztoki
     * */
    private boolean getPage(Browser ibr, final String url) throws Exception {
        if (ibr == null || url == null) return false;
        final Browser obr = ibr.cloneBrowser();
        boolean failed = false;
        int repeat = 4;
        for (int i = 0; i <= repeat; i++) {
            if (failed) {
                long meep = 0;
                while (meep == 0)
                    meep = new Random().nextInt(4) * 1371;
                Thread.sleep(meep);
                failed = false;
                ibr = obr.cloneBrowser();
            }
            try {
                ibr.getPage(url);
                break;
            } catch (IOException e) {
                if (i == (repeat - 1)) {
                    logger.warning("Exausted retry getPage count");
                    throw e;
                }
                failed = true;
                continue;
            }
        }
        return true;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    private static boolean pluginloaded = false;

    private static synchronized String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        if (pluginloaded == false) {
            final PluginForHost plugin = JDUtilities.getPluginForHost("youtube.com");
            if (plugin == null) throw new IllegalStateException("youtube plugin not found!");
            pluginloaded = true;
        }
        return jd.plugins.hoster.Youtube.unescape(s);
    }

}