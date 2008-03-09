package jd.plugins.decrypt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.controlling.JDController;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://crypt-it.com/s/BXYMBR

public class CryptItCom extends PluginForDecrypt {

    static private final String HOST             = "crypt-it.com";
    private String              VERSION          = "0.1.0";
    private String              CODER            = "jD-Team";
    
    static private final Pattern patternSupported = getSupportPattern("(http|ccf)://[*]crypt-it.com/(s|e|d)/[a-zA-Z0-9]+");

    public CryptItCom() {
    	
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginID() {
        return HOST+"-"+VERSION;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            
    		// surpress jd warning
    		Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		step.setParameter(decryptedLinks);
    		
    		parameter = parameter.replace("/s/", "/d/");
    		parameter = parameter.replace("/e/", "/d/");
    		parameter = parameter.replace("ccf://", "http://");
    		
    		try {
    			
    			requestInfo = postRequestWithoutHtmlCode(new URL(parameter), null, null, "", true);
    			HTTPConnection urlConnection = requestInfo.getConnection();
    			
    			String folder = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
    			String name = this.getFileNameFormHeader(urlConnection);
    			
    			if ( name.equals("redir.ccf") || !name.contains(".ccf") ) {
    				
    				logger.severe("Container not found");
            		step.setStatus(PluginStep.STATUS_ERROR);
            		return null;
            		
    			}
    			
    			// download
    			File file = new File(folder, name);
    			int i = 0;
    			
    			while ( file.exists() ) {
    				
    				String newName = name.substring(0, name.length()-4)+"-"+String.valueOf(i)+".ccf";
    				file = new File(folder, newName);
    				i++;
    				
    			}
    			
    			logger.info("Download container: "+file.getAbsolutePath());
    			JDUtilities.download(file, urlConnection);
                
    			// read container
                JDController controller = JDUtilities.getController();
                controller.loadContainerFile(file);
                
                // delete container
                file.delete();
                
			} catch (MalformedURLException e) { e.printStackTrace();
			} catch (FileNotFoundException e) { e.printStackTrace();
			} catch (IOException e) { e.printStackTrace(); }
    		
    	}
    	
    	return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    
}
