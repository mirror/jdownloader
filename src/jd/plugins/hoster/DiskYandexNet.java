//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "disk.yandex.net" }, urls = { "https?://(www\\.)?((((mail|disk)\\.)?yandex\\.(net|com|com\\.tr|ru|ua)/(disk/)?public/(\\?hash=[A-Za-z0-9%/\\+=]+|#[A-Za-z0-9%\\/+=]+))|(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+)" }, flags = { 0 })
public class DiskYandexNet extends PluginForHost {

    private static final String primaryURLs = "https?://(www\\.)?((mail|disk)\\.)?yandex\\.(net|com|com\\.tr|ru|ua)/(disk/)?public/(\\?hash=[A-Za-z0-9%/\\+=\\&]+|#[A-Za-z0-9%\\/+=]+)";
    private static final String shortURLs   = "https?://(www\\.)?(yadi\\.sk|yadisk\\.cc)/d/[A-Za-z0-9\\-_]+";

    public DiskYandexNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://disk.yandex.net/";
    }

    public void correctDownloadLink(DownloadLink link) throws PluginException {
        link.setUrlDownload(link.getDownloadURL().replace("mail.yandex.ru/", "disk.yandex.net/").replace("#", "?hash="));
        if (!link.getDownloadURL().matches("(" + shortURLs + ")")) {
            String protocol = new Regex(link.getDownloadURL(), "(https?)://").getMatch(0);
            String hashID = new Regex(link.getDownloadURL(), "hash=(.+)$").getMatch(0);
            if (protocol == null || hashID == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            link.setUrlDownload(protocol + "://disk.yandex.com/public/?hash=" + hashID);
        }

    }

    private String getHashID(DownloadLink link) throws PluginException {
        String hashID = new Regex(link.getDownloadURL(), "hash=(.+)$").getMatch(0);
        if (hashID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        hashID = Encoding.urlDecode(hashID, false);
        hashID = hashID.replaceAll(" ", "+");
        return hashID;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setFollowRedirects(true);
        // redirect links
        if (link.getDownloadURL().matches(shortURLs)) {
            br.getPage(link.getDownloadURL());
            if (link.getDownloadURL().matches(shortURLs)) {
                if (br.containsHTML("This link was removed or not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String newUrl = Encoding.htmlDecode(br.getURL()).replace("&locale=ru", "");
            if (!newUrl.matches(primaryURLs)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setUrlDownload(new Regex(newUrl, "(" + primaryURLs + ")").getMatch(0));
            // lets format back into single url format, as short links redirect
            // to different the different domains depending on geolcoation
            // and we need English for Error catching!
            correctDownloadLink(link);
        }
        br.getPage(link.getDownloadURL() + "&locale=en");
        String filename;
        String filesize;
        if (br.getURL().contains("&final=true")) {
            // prob not needed
            final String xml = br.getRegex("<script id=\"xml\\-data\">(.*?)</script>").getMatch(0);
            if (xml == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getRequest().setHtmlCode(Encoding.htmlDecode(xml));
            if (br.containsHTML(">resource not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            filename = parse("name");
            filesize = parse("size");
            if (filesize != null) filesize = filesize + "b";
        } else {
            if (br.containsHTML("(<title>The file you are looking for could not be found\\.|>Nothing found</span>|<title>Nothing found — Yandex\\.Disk</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            filename = br.getRegex("<title>(.*?) \\— Yandex\\.Disk</title>").getMatch(0);
            if (filename == null) filename = br.getRegex("b\\-text_title\">(.*?)</span>").getMatch(0);
            filesize = br.getRegex("<span class=\"b\\-text\">(.*?), uploaded").getMatch(0);
            if (filesize == null) filesize = br.getRegex("(\\d+(\\.\\d+)? ?(KB|MB|GB))").getMatch(0);
        }

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String ckey = br.getRegex("\"ckey\":\"([^\"]+)\"").getMatch(0);
        if (ckey == null) {
            logger.warning("Could not find ckey");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (br.getURL().contains("&final=true")) {
            // this is prob wrong
            // br.postPage("/handlers.jsx",
            // "_c&public_url=1&_handlers=disk-file-info&_locale=en&_page=disk-share&_service=disk&hash="
            // +
            // Encoding.urlEncode(getHashID(downloadLink)));
            logger.warning("Component disabled. Please report the source URL to JDownloader Development Team so we can fix!");
        } else {
            br.postPage("/handlers.jsx", "_ckey=" + ckey + "&_name=getLinkFileDownload&hash=" + Encoding.urlEncode(getHashID(downloadLink)));
        }
        String dllink = parse("url");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (dllink.startsWith("//")) dllink = "http:" + dllink;
        // Don't do htmldecode as the link will be invalid then
        dllink = dllink.replace("amp;", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String parse(String var) {
        if (var == null) return null;
        String result = br.getRegex("<" + var + ">([^<>\"]*?)</" + var + ">").getMatch(0);
        if (result == null) result = br.getRegex("\"" + var + "\":\"([^\"]+)").getMatch(0);
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}