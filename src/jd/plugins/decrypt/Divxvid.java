package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;

public class Divxvid extends PluginForDecrypt {
    static private final String host                    = "dxp.divxvid.org";

    private String              version                 = "1.0.0.0";
    static private final Pattern patternSupported = Pattern.compile("http://dxp\\.divxvid\\.org/[a-zA-Z0-9]{32}\\.html", Pattern.CASE_INSENSITIVE);

    private Pattern             gate                    = Pattern.compile("httpRequestObject.open.'POST', '([^\\s\"]*)'\\);", Pattern.CASE_INSENSITIVE);

    private Pattern             outputlocation          = Pattern.compile("rsObject = window.open.\"([/|.|a-zA-Z0-9|_|-]*)", Pattern.CASE_INSENSITIVE);

    private Pattern             premiumdownload         = Pattern.compile("value=\"download\" onclick=\"javascript:download", Pattern.CASE_INSENSITIVE);

    /*
     * ist leider notwendig da wir das Dateiformat nicht kennen!
     */
    private Pattern             premiumdownloadlocation = Pattern.compile("form name=\"dxp\" action=\"(.*)\" method=\"post\"", Pattern.CASE_INSENSITIVE);

    public Divxvid() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        return "DwD aka James";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Divxvid-1.0.0.";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String cryptedLink = (String) parameter;
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                progress.setRange(1000);
                Vector<String> decryptedLinks = new Vector<String>();
                URL url;
                try {
                    url = new URL(cryptedLink);

                    String cookie = postRequestWithoutHtmlCode(url, null, null, null, false).getCookie();

                    String hash = url.getFile();
                    hash = hash.substring(1, hash.lastIndexOf("."));
                    RequestInfo requestInfo = getRequest((new URL("http://dxp.divxvid.org/script/old_loader.php")), cookie, cryptedLink, false);

                    String input = requestInfo.getHtmlCode();
                    String strgate = getFirstMatch(input, gate, 1);
                    String outl = getFirstMatch(input, outputlocation, 1);

                    requestInfo = postRequest((new URL("http://dxp.divxvid.org/" + strgate)), null, cryptedLink, null, "hash=" + hash, false);

                    /*
                     * es werden dank divxvid.org hier nur die menge der links
                     * gezaehlt
                     */
                    int countHits = countOccurences(requestInfo.getHtmlCode(), premiumdownload);
                    progress.setRange(countHits);
                    for (int i = 0; i < countHits; i++) {
                        requestInfo = postRequestWithoutHtmlCode((new URL(getFirstMatch(getRequest((new URL("http://dxp.divxvid.org" + outl + "," + i + ",1," + hash + ".rs")), cookie, cryptedLink, true).getHtmlCode(), premiumdownloadlocation, 1))), null, null, null, false);
                        if (requestInfo != null) {
                            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                            decryptedLinks.add(requestInfo.getLocation());
                        }

                    }
                    logger.info(decryptedLinks.size() + " downloads decrypted");
                    firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                    currentStep = null;
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                step.setParameter(decryptedLinks);
                break;

        }
        return null;
    }
}
