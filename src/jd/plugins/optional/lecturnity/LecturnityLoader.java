package jd.plugins.optional.lecturnity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.JDLogger;
import jd.nutils.io.JDIO;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

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
        FilePackage fp = link.getFilePackage();
        fp.setDownloadDirectory(link.getStringProperty(LecturnityDownloader.PROPERTY_DOWNLOADDIR, fp.getDownloadDirectory()));

        dl = BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 1);
        dl.startDownload();

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
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null) return false;

        HashMap<String, ArrayList<DownloadLink>> map = new HashMap<String, ArrayList<DownloadLink>>();

        String dir;
        ArrayList<DownloadLink> links;
        for (DownloadLink dLink : urls) {
            dir = getDir(dLink);

            links = map.get(dir);
            if (links == null) map.put(dir, links = new ArrayList<DownloadLink>());

            links.add(dLink);
        }

        String[][] sizeArray;
        HashMap<String, Long> sizeMap = new HashMap<String, Long>();
        Long downloadSize;
        for (Entry<String, ArrayList<DownloadLink>> entry : map.entrySet()) {
            try {
                br.getPage(entry.getKey());

                sizeArray = br.getRegex(Pattern.compile("<a href=\".*?\">(.*?)</a>[ ]+.*?[ ].*?[ ]+(\\d+\\.?\\d?[K|M|G]?)", Pattern.CASE_INSENSITIVE)).getMatches();
                sizeMap.clear();

                for (String[] size : sizeArray) {
                    sizeMap.put(size[0], getSize(size[1]));
                }

                for (DownloadLink dLink : entry.getValue()) {
                    downloadSize = sizeMap.get(dLink.getName());
                    if (downloadSize == null) {
                        dLink.setAvailable(false);
                    } else {
                        dLink.setDownloadSize(downloadSize);
                        dLink.setAvailable(true);
                    }
                }
            } catch (Exception e) {
                JDLogger.exception(e);
                for (DownloadLink dLink : entry.getValue()) {
                    dLink.setAvailable(false);
                }
            }
        }

        return true;
    }

    private static final long getSize(String string) {
        double res = Double.parseDouble(string.replaceAll("[K|M|G]", ""));

        if (string.contains("K")) {
            res *= 1024;
        } else if (string.contains("M")) {
            res *= 1024 * 1024;
        } else if (string.contains("G")) {
            res *= 1024 * 1024 * 1024;
        }

        return Math.round(res);
    }

    private static final String getDir(DownloadLink dLink) {
        String url = dLink.getDownloadURL();
        return url.substring(0, url.lastIndexOf('/') + 1);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        checkLinks(new DownloadLink[] { parameter });

        if (!parameter.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        return parameter.getAvailableStatus();
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