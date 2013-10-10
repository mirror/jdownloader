package jd.plugins.hoster;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangastream.com" }, urls = { "mangastream:///read/[a-z0-9\\-_/]+" }, flags = { 0 })
public class Mangastream extends PluginForHost {

    public Mangastream(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mangastream.com/content/privacy";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        // We create the final picture based on the informations of the page
        Regex sizes = br.getRegex("<div style=\"position:relative;width:(\\d+)px;height:(\\d+)px\">");
        if (sizes.getMatch(0) != null && sizes.getMatch(1) != null) {
            /* create final picture based on several images */
            int width = Integer.parseInt(sizes.getMatch(0));
            int height = Integer.parseInt(sizes.getMatch(1));
            BufferedImage buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buffer.createGraphics();
            sizes = null;

            downloadLink.getLinkStatus().setStatusText("Working...");
            downloadLink.getLinkStatus().setStatus(1 << 18);
            // We get every chunk of the image
            String[][] chunksData = br.getRegex("<div style=\"position:absolute;z-index:\\d+;width:\\d+px;height:\\d+px;top:(\\d+)px;left:(\\d+)px\"><a href=\"/read/.+?\"><img src=\"(http://img.mangastream.com/m/\\d+/\\d+/\\w+.(jpg|png))\" border=\"0\" /></a></div>").getMatches();

            for (String[] chunkData : chunksData) {
                int offsetTop = Integer.parseInt(chunkData[0]);
                int offsetLeft = Integer.parseInt(chunkData[1]);

                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(chunkData[2]);
                    BufferedImage chunk = ImageIO.read(con.getInputStream());

                    // We paint the chunk on the picture
                    g.drawImage(chunk, offsetLeft, offsetTop, null);
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(downloadLink.getFileOutput());
                boolean success = ImageIO.write(buffer, "PNG", fos);
                downloadLink.getLinkStatus().setStatusText(success ? "Finished" : "Error saving the file");
                downloadLink.getLinkStatus().setStatus(success ? LinkStatus.FINISHED : LinkStatus.ERROR_DOWNLOAD_FAILED);
                return;
            } finally {
                try {
                    fos.close();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* old method - or most recent one! (present in Naturo chap. 547) */
            String picUrl = br.getRegex("id=\"manga-page\"\\s+src=\"(http[^<>\"]*?)\"").getMatch(0);
            String ext = new Regex(picUrl, ".*?(\\.jpg|\\.png)").getMatch(0);
            if (picUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, picUrl, true, 1);
            try {
                dl.setAllowFilenameFromURL(false);
                if (ext != null) {
                    String name2 = downloadLink.getName();
                    if (!name2.endsWith(ext)) {
                        String name = new Regex(name2, "(.+)\\.").getMatch(0);
                        name = name + ext;
                        downloadLink.setFinalFileName(name);
                    }
                }
            } catch (final Throwable e) {
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://mangastream.com" + downloadLink.getDownloadURL().substring(14));
        if (br.containsHTML("We couldn't find the page you were looking for")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}