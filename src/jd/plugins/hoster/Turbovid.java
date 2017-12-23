package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 1, names = { "turbovid.me" }, urls = { "http://turbovid\\.me/.+" })
public class Turbovid extends PluginForHost {

    private boolean              server_issues = false;
    private static final boolean RESUME        = true;
    private static final int     MAXCHUNKS     = 0;
    private static final int     WAIT_TIME     = 11;
    private static final int     NUM_RETRIES   = 5;

    public Turbovid(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://turbovid.me/tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        /* Make sure links came via the new decrpter */
        if (!downloadLink.getPluginPatternMatcher().matches("http://turbovid\\.me/.+")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        this.setBrowserExclusive();
        br.setFollowRedirects(true);

        String mainlink = downloadLink.getPluginPatternMatcher();
        br.getPage(mainlink);
        if (br.containsHTML("The file you were looking for could not be found, sorry for any inconvenience") || br.containsHTML("File Not Found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        Form form = br.getFormbyAction(mainlink);

        String filename = form.getInputFieldByName("fname").getValue();
        downloadLink.setName(Encoding.htmlDecode(filename));

        return AvailableStatus.TRUE;
    }

    private String findLink(DownloadLink downloadLink) throws PluginException, IOException {
        String mainlink = downloadLink.getPluginPatternMatcher();
        br.setFollowRedirects(true);
        br.getPage(mainlink);

        Form form = br.getFormbyAction(mainlink);

        UrlQuery query = new UrlQuery();
        query.add("op", form.getInputFieldByName("op").getValue());
        query.add("id", form.getInputFieldByName("id").getValue());
        query.add("fname", form.getInputFieldByName("fname").getValue());
        query.add("referer", form.getInputFieldByName("referer").getValue());
        query.add("hash", form.getInputFieldByName("hash").getValue());

        int again = NUM_RETRIES;
        String dllink = null;
        Matcher m = null;
        while (again > 0) {
            sleep(WAIT_TIME * 1000, downloadLink);
            String page = br.postPage(form.getInputFieldByName("id").getValue(), query);
            String decodedPage = Encoding.unicodeDecode(page);

            m = Pattern.compile("<script type='text/javascript'>.+split\\('\\|'\\)\\)\\)").matcher(decodedPage);
            if (m.find()) {
                break;
            }
            again--;
        }
        if (again == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            String[] pieces = m.group(0).split("\\|");
            Matcher link = Pattern.compile("([A-Za-z0-9]+)://([A-Za-z0-9]+)\\.([A-Za-z0-9]+)\\.([A-Za-z0-9]+)\\.([A-Za-z0-9]+)/([A-Za-z0-9]+)/v\\.([A-Za-z0-9]+)").matcher(m.group(0));
            if (!link.find()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Matcher baseMatcher = Pattern.compile("',(\\d+),\\d+,'").matcher(m.group(0));
            if (!baseMatcher.find()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            int base = Integer.parseInt(baseMatcher.group(1));

            dllink = decode(pieces, link.group(1), base) + "://" + decode(pieces, link.group(2), base) + "." + decode(pieces, link.group(3), base) + "." + decode(pieces, link.group(4), base) + "." + decode(pieces, link.group(5), base) + "/" + decode(pieces, link.group(6), base) + "/v." + decode(pieces, link.group(7), base);
        }

        return dllink;
    }

    private String decode(String[] dict, String coded, int base) {
        String ret = null;
        int i = Integer.parseInt(coded, base);
        try {
            ret = dict[i];
            if (ret.equals("")) {
                ret = coded;
            }
        } catch (IndexOutOfBoundsException e) {
            ret = coded;
        }
        return ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        String dllink = findLink(downloadLink);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, RESUME, MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
