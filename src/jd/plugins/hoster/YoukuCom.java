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

package jd.plugins.hoster;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DownloadWatchDog;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream;
import org.jdownloader.translate._JDT;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youku.com" }, urls = { "http://v\\.youku.com/v_show/id_.*?\\.html" }, flags = { 0 })
public class YoukuCom extends PluginForHost {

    private URLConnectionAdapter        DL;
    private MeteredThrottledInputStream INPUTSTREAM;
    private byte[]                      BUFFER;
    private long                        BYTESLOADED;
    private long                        BYTES2DO        = -1;
    private BufferedOutputStream        FILEOUT;
    private boolean                     CONNECTIONCLOSE = false;
    private int                         FAILCOUNTER     = 0;
    private double                      SEED            = 0;
    private HashMap<String, String>     fileDesc;
    private TreeMap<Integer, String[]>  videoParts;

    public YoukuCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String cg_fun(final String arg0) {
        final String tmpKey = cg_hun();
        final String[] _loc_2 = arg0.split("\\*");
        String _loc_3 = "";
        int _loc_4 = 0;
        while (_loc_4 < _loc_2.length - 1) {
            _loc_3 = _loc_3 + tmpKey.charAt(Integer.parseInt(_loc_2[_loc_4]));
            _loc_4++;
        }
        return _loc_3;
    }

    private String cg_hun() {
        String _cgStr = "";
        StringBuffer initKey = new StringBuffer("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/\\:._-1234567890");
        final int j = initKey.length();
        int i = 0;
        while (i < j) {
            final double pos = cg_run() * initKey.length();
            _cgStr = _cgStr + initKey.charAt((int) pos);
            initKey = initKey.deleteCharAt((int) pos);
            i++;
        }
        return _cgStr;
    }

    private double cg_run() {
        SEED = (SEED * 211 + 30031) % 65536;
        return SEED / 65536;
    }

    public void closeConnections() {
        CONNECTIONCLOSE = true;
        try {
            INPUTSTREAM.close();
        } catch (final Throwable e) {
        } finally {
            INPUTSTREAM = null;
        }
        try {
            DL.disconnect();
        } catch (final Throwable e) {
        }
        logger.info("Closed connection before closing file");
    }

    public String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    @Override
    public String getAGBLink() {
        return "http://www.veoh.com/corporate/termsofuse";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getSid() {
        final Calendar c = new GregorianCalendar();
        final String sid = String.valueOf(System.currentTimeMillis()) + String.valueOf(c.get(Calendar.MILLISECOND) + 1000) + String.valueOf((int) Math.ceil(Math.random() * 9000 + 1000));
        return sid;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (fileDesc == null) {
            requestFileInformation(downloadLink);
            prepareBrowser("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        }
        br.setDebug(true);
        final ArrayList<String> content = new ArrayList<String>();
        SEED = Double.parseDouble(fileDesc.get("seed"));
        final String streamFileIds = fileDesc.get("streamfileids");
        if (SEED == 0 || streamFileIds == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        String fileId = cg_fun(streamFileIds);
        final String sid = getSid();
        // get the rest
        if (fileId.length() > 10 && videoParts != null && videoParts.size() > 0) {
            final Object[] key = videoParts.keySet().toArray();
            Arrays.sort(key);
            for (final Object element : key) {
                final String part = String.format("%02x", Integer.parseInt(String.valueOf(element)));
                fileId = fileId.substring(0, 8) + part.toUpperCase() + fileId.substring(10);
                final String vPart = "http://f.youku.com/player/getFlvPath/sid/" + sid + "_" + part + "/st/flv/fileid/" + fileId + "0?K=" + videoParts.get(element)[1] + "&hd=0&myp=0&ts=" + videoParts.get(element)[0];
                content.add(vPart);
            }
        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Sorry, this video can only be streamed within Mainland China!");
        }

        final File tmpFile = new File(downloadLink.getFileOutput() + ".part");
        // reset
        if (!tmpFile.exists()) {
            downloadLink.setProperty("bytes_loaded", Long.valueOf(0l));
            downloadLink.setProperty("parts_finished", Long.valueOf(0l));
        }
        // resuming
        BYTESLOADED = (Long) downloadLink.getProperty("bytes_loaded", Long.valueOf(0l));
        final int resume = Math.round((Long) downloadLink.getProperty("parts_finished", Long.valueOf(0l)));

        int i = 0;
        /* once init the buffer is enough */
        BUFFER = new byte[4 * 1024];
        br.setFollowRedirects(true);

        try {
            downloadLink.getLinkStatus().addStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
            /* we have to create folder structure */
            tmpFile.getParentFile().mkdirs();
            FILEOUT = new BufferedOutputStream(new FileOutputStream(tmpFile, true));
            for (i = resume; i < content.size(); i++) {
                downloadLink.getLinkStatus().setStatusText("Video Part " + (i + 1) + " @ " + String.valueOf(content.size()) + " in Progress...");
                downloadLink.requestGuiUpdate();
                final String pieces = content.get(i);
                if (pieces == null) {
                    break;
                }
                try {
                    /* always close the existing connection */
                    DL.disconnect();
                } catch (final Throwable e) {
                }
                DL = br.openGetConnection(pieces);
                if (DL.getResponseCode() != 200) {
                    if (DL.getResponseCode() == 500) {
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "ServerError(500)", 5 * 60 * 1000l);
                    } else if (DL.getResponseCode() == 400) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Decrypt failed!");
                    } else if (DL.getResponseCode() == 404) {
                        logger.warning("youku.com: Video Part " + (i + 1) + " not found! Link: " + pieces);
                        FAILCOUNTER += 1;
                        sleep(100 * 1001l, downloadLink, String.valueOf(FAILCOUNTER) + " : ");
                        i -= 1;
                        continue;
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                long partSize = DL.getLongContentLength();
                try {
                    INPUTSTREAM = new org.appwork.utils.net.throttledconnection.MeteredThrottledInputStream(DL.getInputStream(), new org.appwork.utils.speedmeter.AverageSpeedMeter(10));
                    /* add inputstream to connectionmanager */
                    DownloadWatchDog.getInstance().getConnectionManager().addManagedThrottledInputStream(INPUTSTREAM);
                } catch (final Throwable e) {
                    /* 0.95xx comp */
                }
                try {
                    int miniblock = 0;
                    int partEndByte = 0;
                    while (partSize != 0) {
                        try {
                            if (partEndByte > 0) {
                                miniblock = INPUTSTREAM.read(BUFFER, 0, (int) Math.min(BYTES2DO, BUFFER.length));
                            } else {
                                miniblock = INPUTSTREAM.read(BUFFER);
                            }
                        } catch (final SocketException e2) {
                            if (!isExternalyAborted()) { throw e2; }
                            miniblock = -1;
                            break;
                        } catch (final ClosedByInterruptException e) {
                            if (!isExternalyAborted()) {
                                logger.severe("Timeout detected");
                            }
                            miniblock = -1;
                            break;
                        } catch (final AsynchronousCloseException e3) {
                            if (!isExternalyAborted() && !CONNECTIONCLOSE) { throw e3; }
                            miniblock = -1;
                            break;
                        } catch (final IOException e4) {
                            if (!isExternalyAborted() && !CONNECTIONCLOSE) { throw e4; }
                            miniblock = -1;
                            break;
                        }
                        if (miniblock == -1) {
                            break;
                        }
                        BYTES2DO -= miniblock;
                        partSize -= miniblock;
                        FILEOUT.write(BUFFER, 0, miniblock);
                        BYTESLOADED += miniblock;
                        partEndByte += miniblock;
                        downloadLink.setDownloadCurrent(BYTESLOADED);
                        if (partEndByte > 0) {
                            BYTES2DO = partEndByte + 1;
                        }
                    }
                    if (partSize == 0) {
                        downloadLink.setProperty("parts_finished", Long.valueOf(i) + 1);
                    } else {
                        downloadLink.setProperty("parts_finished", Long.valueOf(i));
                    }
                    if (isExternalyAborted() && downloadLink.getTransferStatus().supportsResume()) {
                        downloadLink.setProperty("bytes_loaded", Long.valueOf(BYTESLOADED));
                        downloadLink.getLinkStatus().setStatus(LinkStatus.ERROR_DOWNLOAD_INCOMPLETE);
                        break;
                    }
                } finally {
                    try {
                        INPUTSTREAM.close();
                    } catch (final Throwable e) {
                    }
                    try {
                        /* remove inputstream from connectionmanager */
                        DownloadWatchDog.getInstance().getConnectionManager().removeManagedThrottledInputConnection(INPUTSTREAM);
                    } catch (final Throwable e) {
                        /* 0.95xx comp */
                    }
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } finally {
            try {
                DL.disconnect();
            } catch (final Throwable e) {
            }
            try {
                FILEOUT.close();
            } catch (final Throwable e) {
            }

            // System.out.println("SOLL: " + downloadLink.getDownloadSize() +
            // " - IST: " + BYTESLOADED);
            if (downloadLink.getDownloadSize() == BYTESLOADED || i == content.size()) {
                if (!tmpFile.renameTo(new File(downloadLink.getFileOutput()))) {
                    logger.severe("Could not rename file " + tmpFile + " to " + downloadLink.getFileOutput());
                }
            }
        }
        downloadLink.getLinkStatus().setStatusText(null);
        if (!isExternalyAborted()) {
            downloadLink.getLinkStatus().addStatus(LinkStatus.FINISHED);
            if (FAILCOUNTER > 0) {
                downloadLink.getLinkStatus().setStatusText("File(s) not found: " + FAILCOUNTER);
            }
        }
        downloadLink.getLinkStatus().removeStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS);
        downloadLink.setDownloadInstance(null);
    }

    private boolean isExternalyAborted() {
        return Thread.currentThread().isInterrupted();
    }

    private void jsonParser(String jsonString) {
        jsonString = jsonString.replaceAll("\\{\"data\":\\[\\{|\\{\"(flv|hd2|mp4)\":", "");
        final String segs = new Regex(jsonString, "\"segs\":\\[\\{(.*?)\\}\\]").getMatch(-1);
        if (segs != null) {
            final String[][] V1 = new Regex(segs, "\"no\":\"?(.*?)\"?,.*?\"seconds\":\"(.*?)\",\"k\":\"(.*?)\"\\},?\\]?").getMatches();
            jsonString = jsonString.replaceAll("\"tags\":\\[.*?],|\"segs\":\\[\\{.*?\\}\\]\\},", "");
            final String[][] V2 = new Regex(jsonString, "\"(.*?)\":\\[?\"?(.*?)\"?\\]?\\}?,").getMatches();

            fileDesc = new HashMap<String, String>();
            for (final String[] bla : V2) {
                if (bla.length != 2) {
                    continue;
                }
                fileDesc.put(bla[0], bla[1]);
            }
            videoParts = new TreeMap<Integer, String[]>();
            for (final String[] bla : V1) {
                if (bla.length != 3) {
                    continue;
                }
                videoParts.put(Integer.parseInt(bla[0]), new String[] { bla[1], bla[2] });
            }
        }
    }

    private void prepareBrowser(final String userAgent) {
        br.clearCookies("youku.com");
        br.setCookie("youku.com", "isRemoveOnPlayComplete", "true");
        br.setCookie("youku.com", "P_F", "1");
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Language", "zh-ZH");
        br.getHeaders().put("User-Agent", userAgent);
        br.getHeaders().put("Connection", null);
        br.getHeaders().put("Referer", "http://static.youku.com/v1.0.0176/v/swf/player.swf");
        br.getHeaders().put("x-flash-version", "10,3,181,34");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Dieses Video ist nicht mehr verf&uuml;gbar|AnyClip)")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        final String videoId = br.getRegex("var videoId2= \'(.*?)\'").getMatch(0);
        if (videoId == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        // get Playlist
        final String jsonString = br.getPage("http://v.youku.com/player/getPlayList/VideoIDS/" + videoId + "/timezone/+00");
        if (jsonString == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        jsonParser(jsonString);
        String fileName = fileDesc.get("title");
        final String fileSize = fileDesc.get("streamsizes");
        if (fileName == null || fileSize == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        fileName = decodeUnicode(fileName);
        downloadLink.setName(fileName.trim() + ".flv");
        downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        link.setProperty("bytes_loaded", Long.valueOf(0l));
        link.setProperty("parts_finished", Long.valueOf(0l));
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void sleep(long i, final DownloadLink downloadLink, final String message) throws PluginException {
        try {
            while (i > 0 && !isExternalyAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(message + _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(i / 1000)));
                downloadLink.requestGuiUpdate();
                synchronized (this) {
                    this.wait(1000);
                }
            }
        } catch (final InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

}
