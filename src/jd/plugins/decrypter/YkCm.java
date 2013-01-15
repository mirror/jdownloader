//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youku.com" }, urls = { "http://v\\.youku.com/v_show/id_.*?\\.html" }, flags = { 0 })
public class YkCm extends PluginForDecrypt {

    private double                      SEED    = 0;
    private int                         PARTS   = 0;
    private String[]                    streamTypes;
    private HashMap<String, String>     fileDesc;
    private HashMap<String, String>     streamFileId;
    private HashMap<String, String>     streamSizes;
    private HashMap<String, String[][]> videoParts;
    private String                      videoId = null;

    public YkCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String cgFun(final String arg0) {
        final String tmpKey = cgHun();
        final String[] streamingKey = arg0.split("\\*");
        String result = "";
        int i = 0;
        while (i < streamingKey.length) {
            result = result + tmpKey.charAt(Integer.parseInt(streamingKey[i]));
            i++;
        }
        return result;
    }

    private String cgHun() {
        String cgStr = "";
        StringBuffer initKey = new StringBuffer("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ/\\:._-1234567890");
        final int j = initKey.length();
        int i = 0;
        while (i < j) {
            final double pos = cgRun() * initKey.length();
            cgStr = cgStr + initKey.charAt((int) pos);
            initKey = initKey.deleteCharAt((int) pos);
            i++;
        }
        return cgStr;
    }

    private double cgRun() {
        SEED = (SEED * 211 + 30031) % 65536;
        return SEED / 65536;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);

        videoId = br.getRegex("var videoId2= \\'(.*?)\\'").getMatch(0);
        if (videoId == null) { return null; }
        // get Playlist
        final Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("Z");
        final String jsonString = br.getPage("http://v.youku.com/player/getPlayList/VideoIDS/" + videoId + "/timezone/" + sdf.format(date).substring(0, 3) + "/version/5/source/video?password=&n=3&ran=" + (int) (Math.random() * 9999));
        if (br.containsHTML("Sorry, this video can only be streamed within Mainland China\\.")) {
            logger.info("Stopping decrypt process, video only available in mainland china: " + parameter);
            return decryptedLinks;
        }
        if (!jsonParser(jsonString)) { return null; }

        progress.setRange(PARTS);
        for (String sType : streamTypes) {
            if (streamTypes.length == 1 && sType.equals("flvhd")) {
                sType = "flv";
            }
            String hd = "0", st = "flv";
            if (sType.equals("mp4")) {
                hd = "1";
                st = "mp4";
            } else if (sType.equals("hd2")) {
                hd = "2";
            }
            String fileName = fileDesc.get("title");
            final String fileSize = streamSizes.get(sType);
            if (fileName == null || fileSize == null) {
                continue;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fileName + "_" + sType.toUpperCase());
            SEED = Double.parseDouble(fileDesc.get("seed"));
            final String streamFileIds = streamFileId.get(sType);
            if (SEED == 0 || streamFileIds == null) {
                continue;
            }
            String fileId = cgFun(streamFileIds);
            final String sid = getSid();
            if (fileId.length() > 10 && videoParts != null && videoParts.size() > 0) {
                final String[][] key = videoParts.get(sType);
                for (final String[] element : key) {
                    final String part = String.format("%02x", Integer.parseInt(String.valueOf(element[0])));
                    fileId = fileId.substring(0, 8) + part.toUpperCase() + fileId.substring(10);
                    final String vPart = "http://f.youku.com/player/getFlvPath/sid/" + sid + "_" + part + "/st/" + st + "/fileid/" + fileId + "?K=" + element[3] + "&hd=" + hd + "&myp=0&ts=" + element[2];
                    final DownloadLink dlLink = createDownloadlink(vPart);
                    final String ext = sType.equals("hd2") ? "flv" : sType;
                    if (videoParts.get(sType).length > 1) {
                        dlLink.setFinalFileName(fileName + "_part-" + part + "." + ext);
                    } else {
                        dlLink.setFinalFileName(fileName + "." + ext);
                    }
                    dlLink.setDownloadSize(SizeFormatter.getSize(element[1]));
                    fp.add(dlLink);
                    decryptedLinks.add(dlLink);
                    progress.increase(1);
                }
            } else {
                logger.warning("Sorry, this video can only be streamed within Mainland China! : " + parameter);
                return null;
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) { return null; }
        return decryptedLinks;
    }

    private String getSid() {
        final Calendar c = new GregorianCalendar();
        final String sid = String.valueOf(System.currentTimeMillis()) + String.valueOf(c.get(Calendar.MILLISECOND) + 1000) + String.valueOf((int) Math.ceil(Math.random() * 9000 + 1000));
        return sid;
    }

    private boolean jsonParser(String jsonString) {

        if (jsonString == null) { return false; }

        jsonString = jsonString.replaceAll("\\{\"data\":\\[\\{", "");
        final String sfi = new Regex(jsonString, "\"streamfileids\":\\{(.*?)\\}").getMatch(0);
        final String ss = new Regex(jsonString, "\"streamsizes\":\\{(.*?)\\}").getMatch(0);
        final String st = new Regex(jsonString, "\"streamtypes\":\\[(.*?)\\]").getMatch(0);
        final String se = new Regex(jsonString, "\"segs\":\\{(.*?)\\},\"streamsizes").getMatch(0);
        final String seed = new Regex(jsonString, "\"seed\":(\\d+)").getMatch(0);
        // for volume based videos 1-x within the series http://www.youku.com/show_page/id_zcbff19f8962411de83b1.html
        String title = new Regex(jsonString, "\"vidEncoded\":\"" + videoId + "\",\"title\":\"(.*?)\"").getMatch(0);
        // standard video and title format. http://v.youku.com/v_show/id_XNTAxODE3NDgw.html
        if (title == null) title = new Regex(jsonString, ",\"title\":\"(.*?)\"").getMatch(0);

        if (sfi != null && ss != null && st != null && se != null && seed != null && title != null) {

            final String[][] streamfileid = new Regex(sfi, "\"(.*?)\":\"(.*?)\"").getMatches();
            final String[][] streamsizes = new Regex(ss, "\"(.*?)\":\"(.*?)\"").getMatches();
            final String[][] seqs = new Regex(se, "\"(.*?)\":\\[\\{(.*?)\\}\\]").getMatches();

            fileDesc = new HashMap<String, String>();
            fileDesc.put("seed", seed);
            fileDesc.put("title", unescape(title));

            streamTypes = new Regex(st, "\"(.*?)\"").getColumn(0);

            streamFileId = new HashMap<String, String>();
            for (final String[] element : streamfileid) {
                if (element.length != 2) {
                    continue;
                }
                streamFileId.put(element[0], element[1]);
            }

            streamSizes = new HashMap<String, String>();
            for (final String[] element : streamsizes) {
                if (element.length != 2) {
                    continue;
                }
                streamSizes.put(element[0], element[1]);
            }

            if (streamFileId == null || streamFileId.size() == 0 || streamSizes == null || streamSizes.size() == 0) { return false; }

            videoParts = new HashMap<String, String[][]>();
            for (final String[] element : seqs) {
                final String[][] V1 = new Regex(element[1], "\"no\":\"?(.*?)\"?,\"size\":\"(.*?)\",\"seconds\":\"(.*?)\",\"k\":\"(.*?)\"").getMatches();
                // valid?
                if (V1 == null || V1.length == 0) {
                    continue;
                }
                final List<String[]> list = new ArrayList<String[]>(Arrays.asList(V1));
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).length != 4) {
                        list.remove(i);
                    }
                    PARTS++;
                }
                videoParts.put(element[0], list.toArray(new String[list.size()][]));
            }
            return true;
        }
        return false;
    }

    private String unescape(final String s) {
        /* we have to make sure the youtube plugin is loaded */
        JDUtilities.getPluginForHost("youtube.com");
        return jd.plugins.hoster.Youtube.unescape(s);
    }

}
