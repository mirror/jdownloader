package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class RsLayerCom extends PluginForDecrypt {

    final static String host             = "rs-layer.com";

    private String      version          = "0.1.1";
    
    private static String strCaptchaPattern = "<img src=\"(captcha-[^\"]*\\.png)\" ";
    private Pattern     patternSupported = getSupportPattern("http://[*]rs-layer\\.com/[+]\\.html");
    
    private static Pattern linkPattern= Pattern.compile("onclick=\"getFile\\('([^;]*)'\\)");

    public RsLayerCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "eXecuTe";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host+"-"+version;
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
    public PluginStep doStep(PluginStep step, String parameter) {
        if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();

            try {
                if ( parameter.indexOf("rs-layer.com/link-") != -1 ) {
                    URL url = new URL(parameter);
                    RequestInfo reqinfo = getRequest(url);
                    
                    String link = getBetween(reqinfo.getHtmlCode(),"<iframe src=\"", "\" ");
                    link = decryptEntities(link);
                    
                    progress.setRange(1);
                    decryptedLinks.add(this.createDownloadlink(link));
                    progress.increase(1);
                    step.setParameter(decryptedLinks);
                } else if ( parameter.indexOf("rs-layer.com/directory-") != -1 ) {
                    URL url = new URL(parameter);
                    requestInfo = getRequest(url);
                    
                    //determin if this page has a form we can enter a capta value in
                    Form[] forms = Form.getForms(requestInfo);
                    if( null == forms || 0 == forms.length || null == forms[0]){
                        logger.info("Unable to find form for captcha entry");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return null;
                    }
                    Form captchaForm = forms[0];

                    //find the captcha url
                    String captchaFileName = new Regexp(requestInfo.getHtmlCode(), strCaptchaPattern).getFirstMatch(1);
                    if( null == captchaFileName){
                        logger.info("Unable to fetch captcha file from \"" + parameter + "\"");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return null;
                    }

                    //download the captcha
                    String captchaUrl =  "http://" + host +"/"+ captchaFileName;
                    File captchaFile = getLocalCaptchaFile(this, ".png");
                    boolean fileDownloaded = JDUtilities.download(captchaFile,
                                    getRequestWithoutHtmlCode(new URL(captchaUrl),
                                    requestInfo.getCookie(), null, true).getConnection()
                                    );
                    
                    if(!fileDownloaded){
                        logger.info("Unable the fetch captcha from determined captcha url");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return null;
                    }
                    
                    String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                    if(null == captchaCode || captchaCode.isEmpty()){
                        logger.info("User did not enter a valid captcha code");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return null;
                    }
                    
                    captchaForm.put("captcha_input", captchaCode);
                
                    RequestInfo reqInfo = readFromURL((HttpURLConnection)captchaForm.getConnection());
                    String  captchaResponseHtml = reqInfo.getHtmlCode();
                    
                    if( captchaResponseHtml.contains("Sicherheitscode<br />war nicht korrekt")){
                        //TODO open simple DialogBox to tell the user what went wrong
                        logger.info("Captcha was wrong");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return null;
                    }
                    
                    if( captchaResponseHtml.contains("Gültigkeit für den<br> Sicherheitscode<br>ist abgelaufen")){
                        //TODO open simple DialogBox to tell the user what went wrong
                        logger.info("Captcha expired");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return null;
                    }

                    Vector<String> layerLinks = getAllSimpleMatches(captchaResponseHtml, linkPattern, 1);
                    progress.setRange(layerLinks.size());

                    for(String fileId : layerLinks){
                        String layerLink = "http://rs-layer.com/link-"+fileId + ".html";

                        RequestInfo request2 = getRequest(new URL(layerLink));
                        String link = getBetween(request2.getHtmlCode(),"<iframe src=\"", "\" ");

                        decryptedLinks.add(this.createDownloadlink(link));
                        progress.increase(1);
                    }
                    //attach the links
                    step.setParameter(decryptedLinks);

                }
            } catch(IOException e) {
                 e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
    // Zeichencode-Entities (&#124 etc.) in normale Zeichen umwandeln
    private String decryptEntities(String str) {
        Vector<Vector<String>> codes = getAllSimpleMatches(str,"&#°;");
        String decodedString = "";
        
        for( int i=0; i<codes.size(); i++ ) {
            int code = Integer.parseInt(codes.get(i).get(0));
            char[] asciiChar = {(char)code};
            decodedString += String.copyValueOf(asciiChar);
        }
        return decodedString;
    }   
}