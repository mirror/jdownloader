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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.BaseShareCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { BaseShareCom.class })
public class BaseShareComCrawler extends PluginForDecrypt {
    public BaseShareComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return BaseShareCom.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[\\w\\-]+/mixtapes/[\\w\\-]+/\\d+/");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || !this.canHandle(br.getURL())) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_artist = new Regex(param.getCryptedUrl(), "baseshare\\.com/([A-Za-z0-9\\-_]+)/mixtapes").getMatch(0);
        String artist = br.getRegex("<h1>([^<>]*?)</h1>").getMatch(0);
        String title = br.getRegex("<h2>([^<>]*?)</h2>").getMatch(0);
        String fpName = null;
        if (artist != null && title != null) {
            artist = Encoding.htmlDecode(artist).trim();
            title = Encoding.htmlDecode(title).trim();
            fpName = artist + " - " + title;
        }
        final String jstext = br.getRegex("<div id=\"content\">.*?<script>(.*?)</script>").getMatch(0);
        final String[] links = jstext.split("function ");
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            final String[][] linkinfo = new Regex(singleLink, "updateSong\\(\\'\\s*(http://baseshare\\.com/uploads[^<>\"]*?\\.mp3)\\'\\s*,\\s*\\'/uploads/waves/[a-z0-9]+\\.png\\'\\s*,\\s*\\'([^<>\"]*?)\\'\\s*,\\s*\\'([^<>\"]*?)\\'\\s*,\\s*(\\d+)\\);").getMatches();
            if (linkinfo != null && linkinfo.length == 1) {
                final String thisurl = linkinfo[0][0];
                final String thisartist = linkinfo[0][1];
                final String thistitle = linkinfo[0][2];
                final String thisid = linkinfo[0][3];
                final String thisartisturl = thisartist.replaceAll("(\\-|\\.|_)", "").replace(" ", "-");
                final String thistitleurl = thistitle.replaceAll("(\\-|\\.|_)", "").replace(" ", "-");
                final String songurl = "http://baseshare.com/" + url_artist + "/songs/" + thisartisturl + "-" + thistitleurl + "/" + thisid + "/";
                final DownloadLink dl = createDownloadlink(songurl);
                dl.setProperty("directlink", thisurl);
                dl.setName(thisartist + " - " + thistitle + ".mp3");
                dl.setAvailable(true);
                ret.add(dl);
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(ret);
        }
        return ret;
    }

    private PluginForHost plugin = null;

    private void getPage(final String parameter) throws Exception {
        getPage(br, parameter);
    }

    private void getPage(final Browser br, final String parameter) throws Exception {
        loadPlugin();
        ((jd.plugins.hoster.BaseShareCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.BaseShareCom) plugin).getPage(parameter);
    }

    public void loadPlugin() throws PluginException {
        if (plugin == null) {
            plugin = getNewPluginForHostInstance(this.getHost());
        }
    }
}
