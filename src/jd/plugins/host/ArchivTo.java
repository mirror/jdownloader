package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.utils.JDUtilities;

//http://archiv.to/Get/?System=Download&Hash=FILE4799F3EC23328
// http://archiv.to/?Module=Details&HashID=FILE4799F3EC23328

public class ArchivTo extends PluginForHost {

    private static final String  HOST             = "archiv.to";

    private static final String  VERSION          = "1.2.0";

    static private final Pattern patternSupported = Pattern.compile("http://.*?archiv\\.to/\\?Module\\=Details\\&HashID\\=.*", Pattern.CASE_INSENSITIVE);

    static private final String  FILESIZE         = "<td width=\".*\">: ([0-9]+) Byte";

    static private final String  FILENAME         = "<td width=\".*\">Original-Dateiname</td>\n	<td width=\".*\">: <a href=\".*\" style=\".*\">(.*?)</a></td>";

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public ArchivTo() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public PluginStep doStep(PluginStep step, final DownloadLink downloadLink) {
        if (aborted) {
            logger.warning("Plugin aborted");
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            step.setStatus(PluginStep.STATUS_TODO);
            return step;
        }
        try {
            String url = downloadLink.getDownloadURL();

            requestInfo = getRequestWithoutHtmlCode(new URL("http://archiv.to/Get/?System=Download&Hash=" + new Regexp(url, ".*HashID=(.*)").getFirstMatch()), null, url, true);

            HTTPConnection urlConnection = requestInfo.getConnection();
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            final long length = downloadLink.getDownloadMax();
            downloadLink.setName(getFileNameFormHeader(urlConnection));

            Download dl = new Download(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            return step;

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = getRequest(new URL(url));
            downloadLink.setName(new Regexp(requestInfo.getHtmlCode(), FILENAME).getFirstMatch());
            if (!requestInfo.getHtmlCode().contains(":  Bytes (~ 0 MB)")) {
                downloadLink.setDownloadMax(Integer.parseInt(new Regexp(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch()));
            }
            else
                return false;
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {}

    @Override
    public void resetPluginGlobals() {}

    @Override
    public String getAGBLink() {
        return "http://archiv.to/?Module=Policy";
    }
}
