//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import jd.PluginWrapper;
import jd.nutils.FtpEvent;
import jd.nutils.FtpListener;
import jd.nutils.JDHash;
import jd.nutils.SimpleFTP;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.RAFDownload;
import jd.plugins.download.DownloadInterface.Chunk;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ftp" }, urls = { "ftp://.+/.+" }, flags = { 0 })
public class Ftp extends PluginForHost {

    public Ftp(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://jdownloader.org";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        SimpleFTP ftp = new SimpleFTP();
        try {
            URL url = new URL(downloadLink.getDownloadURL());
            ftp.connect(url);

            String[] list = ftp.getFileInfo(Encoding.urlDecode(url.getPath(), false));
            if (list == null) return AvailableStatus.FALSE;
            downloadLink.setDownloadSize(Long.parseLong(list[4]));
            downloadLink.setFinalFileName(list[6]);
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            ftp.disconnect();
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        download(downloadLink.getDownloadURL(), downloadLink);
    }

    public void download(String ftpurl, final DownloadLink downloadLink) throws IOException, PluginException {
        SimpleFTP ftp = new SimpleFTP();
        try {
            if (new File(downloadLink.getFileOutput()).exists()) throw new PluginException(LinkStatus.ERROR_ALREADYEXISTS);
            URL url = new URL(ftpurl);
            ftp.connect(url);
            String[] list = ftp.getFileInfo(Encoding.urlDecode(url.getPath(), false));
            if (list == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setDownloadSize(Long.parseLong(list[4]));
            String path = Encoding.urlDecode(url.getPath().substring(0, url.getPath().lastIndexOf("/")), false);
            if (path.length() > 0) ftp.cwd(path);
            ftp.bin();
            ftp.getBroadcaster().addListener(new FtpListener() {

                private long last = 0;
                private long lastTime = System.currentTimeMillis();

                public void onDownloadProgress(FtpEvent event) {
                    downloadLink.setDownloadCurrent(event.getProgress());

                    if (System.currentTimeMillis() - lastTime > 250) {
                        DownloadInterface dli = downloadLink.getDownloadInstance();
                        /* TODO: change ftp to meteredinputStream */
                        // if (dli != null)
                        // dli.getChunks().get(0).getSpeedMeter().addSpeedValue((event.getProgress()
                        // - last), System.currentTimeMillis() - lastTime);
                        downloadLink.requestGuiUpdate();
                        last = event.getProgress();
                        lastTime = System.currentTimeMillis();
                        downloadLink.setChunksProgress(new long[] { last });
                    }
                }

            });

            File tmp;
            dl = new RAFDownload(this, downloadLink, null);
            dl.setResume(false);

            downloadLink.setDownloadInstance(dl);
            dl.addChunksDownloading(1);
            Chunk ch = dl.new Chunk(0, 0, null, null);
            ch.setInProgress(true);
            dl.getChunks().add(ch);
            String name = new Regex(ftpurl, ".*/(.+)").getMatch(0);
            if (name == null) {
                logger.severe("could not get filename from ftpurl");
                name = downloadLink.getName();
            }
            name = Encoding.urlDecode(name, false);
            downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            try {
                ftp.download(name, tmp = new File(downloadLink.getFileOutput() + ".part"));
            } finally {
                downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
                downloadLink.setDownloadInstance(null);
                ch.setInProgress(false);
            }
            if (tmp.length() != downloadLink.getDownloadSize()) {
                tmp.delete();
                throw new PluginException(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE);
            }

            if (downloadLink.getMD5Hash() != null && !downloadLink.getMD5Hash().equalsIgnoreCase(JDHash.getMD5(tmp))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " CRC error"); }

            if (downloadLink.getSha1Hash() != null && !downloadLink.getSha1Hash().equalsIgnoreCase(JDHash.getSHA1(tmp))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " CRC error"); }

            if (!tmp.renameTo(new File(downloadLink.getFileOutput()))) { throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED, " Rename failed. file exists?"); }
            downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
        } finally {
            ftp.disconnect();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
