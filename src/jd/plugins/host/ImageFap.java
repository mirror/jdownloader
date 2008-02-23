package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ImageFap extends PluginForHost {
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?imagefap.com/gallery.php\\?.*", Pattern.CASE_INSENSITIVE);
    static private final String HOST = "imagefap.com";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.1";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "JD-Team";
    static private final Pattern LINKS = Pattern.compile("(?s)<a href=\"image.php\\?id=([0-9]+).*?\"><img border=0 src='http://images.imagefap.com/images/thumb/[0-9\\/]+\\.jpg\\'><\\/a>", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAGES = Pattern.compile("<a class=link[0-9]+ href=\"gallery.php(.*?)\">[0-9]+</a>", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAGES2 = Pattern.compile("<a class=link[0-9]+ onclick=\"requestGallery\\(\\'(.*?)\\'\\)\\; return (true|false);\" href=\"#\">[0-9]+</a>", Pattern.CASE_INSENSITIVE);
    static private final Pattern NAME = Pattern.compile("<td style=\"background: url\\(img/.*?\\) .*?;\"><font face=verdana color=white size=4><b>(.*)", Pattern.CASE_INSENSITIVE);

    public ImageFap() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }
    @Override
    public String getCoder() {
        return CODER;
    }
    @Override
    public String getPluginName() {
        return HOST;
    }
    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }
    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }
    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        RequestInfo requestInfo;
        try {
            if (step.getStep() == PluginStep.STEP_DOWNLOAD) {
                requestInfo = getRequest(new URL(parameter.getDownloadURL()));
                Vector<String> Images = new Vector<String>();
                Vector<Vector<String>> matches = getAllSimpleMatches(requestInfo.getHtmlCode(), LINKS);
                Vector<Vector<String>> pagematches = getAllSimpleMatches(requestInfo.getHtmlCode(), PAGES);
                Vector<Vector<String>> pagematches2 = getAllSimpleMatches(requestInfo.getHtmlCode(), PAGES2);
                String name = getFirstMatch(requestInfo.getHtmlCode(), NAME, 1);
                for (int i = 0; i < matches.size(); i++) {
                    Images.add(matches.get(i).get(0) + ".jpg");

                }
                for (int i = 0; i < pagematches.size(); i++) {
                    requestInfo = getRequest(new URL("http://" + HOST + "/" + pagematches.get(i).get(0)));
                    matches = getAllSimpleMatches(requestInfo.getHtmlCode(), LINKS);
                    for (int j = 0; j < matches.size(); j++) {
                        Images.add(matches.get(j).get(0) + ".jpg");
                    }

                }
                for (int i = 0; i < pagematches2.size(); i++) {
                    requestInfo = getRequest(new URL("http://" + HOST + "/gallery.php?" + pagematches2.get(i).get(0)));
                    matches = getAllSimpleMatches(requestInfo.getHtmlCode(), LINKS);
                    for (int j = 0; j < matches.size(); j++) {
                        Images.add(matches.get(j).get(0) + ".jpg");
                    }

                }

                File file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), name);
                file.mkdir();
                for (int i = 1; i < Images.size(); i++) {
                    requestInfo = postRequestWithoutHtmlCode(new URL("http://85.17.40.49/full/getimg.php?img=" + Images.get(i)), null, null, null, true);
                    parameter.setName(Images.get(i));
                    download(parameter, (URLConnection) requestInfo.getConnection());
                    new File(parameter.getFileOutput()).renameTo(new File(file, Images.get(i)));
                }
                step.setStatus(PluginStep.STATUS_DONE);
                parameter.setStatus(DownloadLink.STATUS_DONE);
                return step;

            }
        } catch (IOException e) {
             e.printStackTrace();
            return null;
        }
        return null;
    }
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    @Override
    public void reset() {
        // this.url = null;
    }
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {

            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
            String name = getFirstMatch(requestInfo.getHtmlCode(), NAME, 1);
            downloadLink.setName(name);
            /*
             * 
             * Vector<String> link = matches.get(id);
             * downloadLink.setName(link.get(1)); if (link != null) { try { int
             * length = (int) (Double.parseDouble(link.get(2)) * 1024 * 1024);
             * downloadLink.setDownloadMax(length); } catch (Exception e) { } }
             */
            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return true;
    }
    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://imagefap.com/faq.php";
    }
}
