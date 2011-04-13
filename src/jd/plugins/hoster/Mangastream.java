package jd.plugins.hoster;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 1 $", interfaceVersion = 2, names = { "mangastream.com" }, urls = { "mangastream:///read/.*?/\\d+/\\d+" }, flags = { 0 })
public class Mangastream extends PluginForHost {

    public Mangastream(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public String getAGBLink() {
        return "http://mangastream.com/content/privacy";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://mangastream.com" + downloadLink.getDownloadURL().substring(14));
        if (br.containsHTML("We couldn't find the page you were looking for")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        try {
            final URL url = new URL("http://mangastream.com" + downloadLink.getDownloadURL().substring(14));
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            BufferedImage buffer = null;
            String line;
            boolean a = false; // Set to true when we get the dimensions of the
            // whole picture, we do not get get an exception
            // if the page is ill-formed
            Graphics2D g = null;

            downloadLink.getLinkStatus().setStatusText("Working...");
            downloadLink.getLinkStatus().setStatus(LinkStatus.PLUGIN_IN_PROGRESS);
            while ((line = reader.readLine()) != null) {
                if (line.contains("<div style=\"position:relative;width:")) {
                    // Data about the whole picture. Should be matched before
                    // the others and only once

                    int width = Integer.parseInt(line.split(":")[2].split("px")[0]);
                    int height = Integer.parseInt(line.split(":")[3].split("px")[0]);
                    buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    g = buffer.createGraphics();
                    a = true;
                } else if (a && line.contains("<div style=\"position:absolute;z-index:")) {
                    // A chunk of the whole picture

                    URL src = new URL(line.split("<img src=\"")[1].split("\"")[0]);
                    BufferedImage chunk = ImageIO.read(src);
                    int offset_left = Integer.parseInt(line.split("left:")[1].split("px")[0]);
                    int offset_top = Integer.parseInt(line.split("top:")[1].split("px")[0]);

                    g.drawImage(chunk, offset_left, offset_top, null);

                } else if (a && line.contains("</div>")) {
                    // The end of the picture. Should be matched after the
                    // others and only once
                    boolean success = ImageIO.write(buffer, "PNG", new File(downloadLink.getFileOutput()));
                    downloadLink.getLinkStatus().setStatusText(success ? "Finished" : "Error saving the file");
                    downloadLink.getLinkStatus().setStatus(success ? LinkStatus.FINISHED : LinkStatus.ERROR_LOCAL_IO);
                    return;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
