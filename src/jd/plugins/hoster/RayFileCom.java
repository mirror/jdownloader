package jd.plugins.hoster;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 12893 $", interfaceVersion = 2, names = { "rayfile.com" }, urls = { "http://[\\w]*?\\.rayfile\\.com/(.*?|zh-cn/)files/(.*?)/" }, flags = { 2 })
public class RayFileCom extends PluginForHost {

    public RayFileCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String downloadUrl = null;
        String freeDownloadLink = null;
        String vid = null;
        String vkey = null;

        // use their regex

        String regex = "var vid = \"(.*?)\"";
        Pattern p = Pattern.compile(regex);
        vid = this.br.getRegex(p).getMatch(0);

        regex = "var vkey = \"(.*?)\"";
        p = Pattern.compile(regex);
        vkey = this.br.getRegex(p).getMatch(0);

        // freeDownloadLink = this.br.getRegex(p).getMatch(0);
        freeDownloadLink = "http://www.rayfile.com/zh-cn/files/" + vid + "/" + vkey + "/";

        final Browser ajax = this.br.cloneBrowser();
        ajax.getPage(freeDownloadLink);

        regex = "downloads_url = \\['(.*?)'\\]";
        p = Pattern.compile(regex);
        downloadUrl = ajax.getRegex(p).getMatch(0);

        String cookie_key = null;
        String cookie_value = null;
        String cookie_path = null;
        String cookie_host = null;
        regex = "setCookie\\('(.*?)', '(.*?)', (.*?), '(.*?)', '(.*?)'.*?\\)";

        p = Pattern.compile(regex);
        cookie_key = ajax.getRegex(p).getMatch(0);
        cookie_value = ajax.getRegex(p).getMatch(1);
        cookie_path = ajax.getRegex(p).getMatch(3);
        cookie_host = ajax.getRegex(p).getMatch(4);

        this.br.setCookie(cookie_host, cookie_key, cookie_value);
        this.dl = jd.plugins.BrowserAdapter.openDownload(this.br, downloadLink, downloadUrl, true, 1);

        this.dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(parameter.getDownloadURL());

        if (br.containsHTML("Not HTML Code. Redirect to: ")) {

            String redirectUrl = br.getRequest().getLocation();
            // System.out.println(redirectUrl);
            parameter.setUrlDownload(redirectUrl);
            br.getPage(redirectUrl);
        }

        if (this.br.containsHTML("page404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        Regex fname = br.getRegex("var fname = \"(.*?)\";");
        Regex fsize = br.getRegex("formatsize = \"(.*?)\";");

        String filename = fname.getMatch(0);
        String filesize = fsize.getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setName(filename.trim());
        // parameter.setDownloadSize(Regex.getSize(filesize));
        parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}
