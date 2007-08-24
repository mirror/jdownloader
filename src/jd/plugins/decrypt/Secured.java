package jd.plugins.decrypt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import jd.JDUtilities;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

/**
 * DecryptPlugin für secured.in Links
 *  
 */
public class Secured extends PluginForDecrypt {

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://secured.in/download.*");
    static private final Pattern PAT_FILE_ID = Pattern.compile("accessDownload\\([^']*'([^']*)");
    static private final Pattern PAT_CAPTCHA = Pattern.compile("<img src=\"(captcha-[^\"]*)");
        
    static private final String HOST = "secured.in";
    static private final String PLUGIN_NAME = "secured.in";
    static private final String PLUGIN_VERSION = "0";
    static private final String PLUGIN_ID = PLUGIN_NAME+"-"+VERSION;
    static private final String CODER = "olimex";

    static private final String AJAX_URL = "http://secured.in/ajax-handler.php";
    static private final String DOWNLOAD_CMD = "downloaditfgh4w5z4e5";

    public Secured() {
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override public String getCoder()                { return CODER;        }
    @Override public String getPluginName()           { return HOST;             }
    @Override public Pattern getSupportedLinks()      { return PAT_SUPPORTED; }
    @Override public String getHost()                 { return HOST;             }
    @Override public boolean isClipboardEnabled()     { return true;             }
    @Override public String getVersion()              { return PLUGIN_VERSION;          }
    @Override public String getPluginID()             { return PLUGIN_ID; }

    @Override
    public Vector<String> decryptLink(String cryptedLink) {
        logger.finest("Decrypt: "+cryptedLink);

        Vector<String> fileUrls = new Vector<String>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo = getRequest(url);

            String html = requestInfo.getHtmlCode();

            for(;;) { // for() läuft bis kein Captcha mehr abgefragt wird
                Matcher matcher = PAT_CAPTCHA.matcher(html);
                
                if (matcher.find()) {
                    logger.finest("Captcha Protected");
                    String capHash = matcher.group(1).substring(8);
                    capHash = capHash.substring(0, capHash.length()-4);
                    
                    String capTxt = JDUtilities.getCaptcha(null, this, "http://"+HOST+"/"+matcher.group(1));
                    
                    String postData = "captcha_key="+capTxt+"&captcha_hash="+capHash;
                    
                    requestInfo = postRequest(url, postData);
                    html = requestInfo.getHtmlCode();
                } else {
                    break;
                }
            }
            
            // Alle File ID aus dem HTML-Code ziehen
            Matcher matcher = PAT_FILE_ID.matcher(html);

            while(matcher.find()) {
                // ..und URLs erzeugen und anfügen
                String fileUrl = decryptId(matcher.group(1));
                fileUrls.add(fileUrl);
                logger.finest("ID: "+matcher.group(1)+" URL:"+fileUrl);
            }
        } catch (Exception e) {
            logger.warning("Exception: "+e);
        }    
        logger.finest("URL#: "+fileUrls.size());
        return fileUrls;
    }

    /**
     * Eine Secured ID in eine URL übersetzen
     * @param id Secured ID
     * @return URL als String
     * @throws IOException
     */
    public String  decryptId(String id) throws IOException {
        URLConnection con = new URL(AJAX_URL).openConnection();
        con.setDoOutput(true);

        OutputStreamWriter wr = new OutputStreamWriter(con
                .getOutputStream());
        wr.write("cmd="+DOWNLOAD_CMD+"&download_id="+id);
        wr.flush();

        InputStreamReader in = new InputStreamReader(con
                .getInputStream());

        StringBuffer sb = new StringBuffer();
        while (in.ready()) {
            sb.append((char)in.read());
        }


        return sb.toString();
    }

    @Override
    public PluginStep getNextStep(Object parameter) {
        return currentStep;
    }
}
