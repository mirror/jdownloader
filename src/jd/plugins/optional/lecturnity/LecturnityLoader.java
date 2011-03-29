package jd.plugins.optional.lecturnity;

import java.io.File;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.http.URLConnectionAdapter;
import jd.nutils.io.JDIO;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class LecturnityLoader extends PluginForHost {

    public LecturnityLoader(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        /*
         * Check file status again. (Time can have passed ...)
         */
        requestFileInformation(link);

        /*
         * Reset download directory. (dirty hack)
         */
        FilePackage fp = link.getFilePackage();
        fp.setDownloadDirectory(link.getStringProperty(LecturnityDownloaderExtension.PROPERTY_DOWNLOADDIR, fp.getDownloadDirectory()));

        /*
         * We know the Download-URL, so simply start the download.
         */
        dl = BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 1);
        dl.startDownload();

        /*
         * If the file ends with .ram, we have to manipulate the content to
         * enable offline-viewing.
         */
        String fileName = link.getFileOutput();
        if (fileName.endsWith(".ram")) {
            logger.info("Lecturnity: Manipulating " + fileName);
            File file = new File(fileName);
            try {
                String content = "file://./" + file.getName().replace(".ram", ".rm");
                JDIO.writeLocalFile(file, content);
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        URLConnectionAdapter urlConnection = null;
        try {
            urlConnection = br.openGetConnection(parameter.getDownloadURL());
            parameter.setDownloadSize(urlConnection.getLongContentLength());
            return AvailableStatus.TRUE;
        } catch (Exception e) {
            JDLogger.exception(e);
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}