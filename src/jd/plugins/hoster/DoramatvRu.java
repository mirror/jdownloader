/**
 * 
 */
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

/**
 * First version of the Doramatv.ru plugin Still has to be improved according to efficiency.
 * 
 * @author Steven Kuenzel
 * 
 */
@HostPlugin(names = { "doramatv.ru" }, urls = { "http://(www\\.)?doramatv\\.ru/(.)*" }, flags = { 0 }, interfaceVersion = 0, revision = "")
public class DoramatvRu extends PluginForHost {

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public DoramatvRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#getAGBLink()
     */
    @Override
    public String getAGBLink() {
        return "http://doramatv.ru/about";
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#requestFileInformation(jd.plugins.DownloadLink)
     */
    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String[] urlParts = parameter.getDownloadURL().replace("http://", "").split("/");
        String title = "";
        String resolution = "";
        String extension = ".mp4";

        if (urlParts.length == 3) {
            title = getTitle(urlParts[1] + "_Folge_" + urlParts[2].replace("series", ""));
        }

        // load page
        br.getPage(parameter.getDownloadURL());

        // try to find any matches
        String regexString = "<input type=\"hidden\" name=\"embed_source\" class=\"embed_source\" .*?/>";
        Regex regex = br.getRegex(regexString);

        String[][] regexMatches = regex.getMatches();

        List<String> matchList = new ArrayList<String>();

        for (int i = 0; i < regex.count(); i++) {
            String match = regexMatches[i][0];

            try {
                // try to extract the download link
                match = match.split("src=&quot;")[1];
                match = match.split("&quot;")[0];

                // decode the link into "ordinary" HTML
                match = Encoding.htmlDecode(match);

                // check the real host of the video
                if (match.contains("vk.com")) {
                    // vk.com

                    matchList.add(match);
                }
            } catch (Exception e) {

            }

            // no download links were found

        }
        if (matchList.size() < 1) return AvailableStatus.FALSE;
        for (String match : matchList) {
            Browser br2 = br.cloneBrowser();
            br2.getPage(match);

            Regex regex720 = br2.getRegex("\"url720\":\".*?\"");
            Regex regex480 = br2.getRegex("\"url480\":\".*?\"");
            Regex regex360 = br2.getRegex("\"url360\":\".*?\"");
            Regex regex240 = br2.getRegex("\"url240\":\".*?\"");

            // find download link for the best resolution

            if (regex720.count() > 0) {
                resolution = "720";
                findBestDownloadLink(parameter, regex720, "url" + resolution);

            } else if (regex480.count() > 0) {
                resolution = "480";
                findBestDownloadLink(parameter, regex480, "url" + resolution);
            } else if (regex360.count() > 0) {
                resolution = "360";
                findBestDownloadLink(parameter, regex360, "url" + resolution);
            } else if (regex240.count() > 0) {
                resolution = "240";
                findBestDownloadLink(parameter, regex240, "url" + resolution);
            } else {
                continue;
            }// no download links were found return AvailableStatus.FALSE; }

            // set the file name
            parameter.setFinalFileName(title + resolution + extension);

            // try to find the file size

            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br2.openGetConnection(parameter.getDownloadURL());

                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!con.getContentType().contains("html"))
                    parameter.setDownloadSize(con.getLongContentLength());
                else

                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }

            return AvailableStatus.TRUE;
        }
        return AvailableStatus.FALSE;
    }

    private void findBestDownloadLink(DownloadLink link, Regex regex, String resolution) {
        String[][] finalMatches;
        List<String> finalList = new ArrayList<String>();

        finalMatches = regex.getMatches();

        for (int i = 0; i < regex.count(); i++) {
            String finalMatch = finalMatches[i][0];

            finalMatch = finalMatch.replace("\"" + resolution + "\":\"", "");
            finalMatch = finalMatch.replace("\"", "");
            finalMatch = finalMatch.replace("\\", "");

            finalList.add(finalMatch);
        }

        // no download links were found
        if (finalList.size() < 1) return;

        String downloadLink = finalList.get(0);

        link.setUrlDownload(downloadLink);
    }

    /**
     * Converts the title String into a more appropriate one (trivial approach)
     * 
     * @param title
     *            The title to convert
     * @return The converted title
     */
    private String getTitle(String title) {
        String result = "";

        String[] titleSplit = title.split("_");

        for (String s : titleSplit) {

            result += s.substring(0, 1).toUpperCase() + s.substring(1) + "_";
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#getDownloadLinks(java.lang.String, jd.plugins.FilePackage)
     */
    public ArrayList<DownloadLink> getDownloadLinks(final String data, final FilePackage fp) {
        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();

        if (!data.contains("series")) {
            String title = data.split("/")[data.split("/").length - 1];
            try {

                PluginForHost plugin = getLazyP().getPrototype(null);

                try {
                    br.getPage(data);

                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                int i = 0;
                while (true) {
                    i++;
                    Regex r = br.getRegex(title + "/series" + i);
                    if (r.count() > 0) {
                        final DownloadLink link = new DownloadLink(plugin, null, getHost(), data + "/series" + i, true);
                        links.add(link);

                    } else {
                        break;
                    }
                }
            } catch (Exception e) {

            }
            return links;

        } else {
            return super.getDownloadLinks(data, fp);

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#handleFree(jd.plugins.DownloadLink)
     */
    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // requestFileInformation(link);

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        dl.startDownload();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#reset()
     */
    @Override
    public void reset() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#resetDownloadlink(jd.plugins.DownloadLink)
     */
    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
