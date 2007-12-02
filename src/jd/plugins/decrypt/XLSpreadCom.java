package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class XLSpreadCom extends PluginForDecrypt {

    final static String host             = "xlspread.com";

    private String      version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]xlspread.com/download.html\\?id=[+]");

    public XLSpreadCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }

    @Override
    public String getCoder() {
        return "Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Xlspread.com-2.0.0.";
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

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<String> decryptedLinks = new Vector<String>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "downlink.php?id=°&amp;hoster=°','download'");
    			
    			// Anzahl der Links zählen
    			for(int i=0; i<links.size(); i++) {
	    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true) && links.get(i).get(1).equals("rapidshare")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true) && links.get(i).get(1).equals("uploaded")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true) && links.get(i).get(1).equals("netload")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_MEINUPLOAD",true) && links.get(i).get(1).equals("meinupload")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE",true) && links.get(i).get(1).equals("share-online")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",true) && links.get(i).get(1).equals("simpleupload")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_BLUEHOST",true) && links.get(i).get(1).equals("bluehost")) {
	    				count++;
	    			}
    			}
    			progress.setRange( count);
    			
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("rapidshare"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("uploaded"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("netload"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEINUPLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("meinupload"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("share-online"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_BLUEHOST",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("bluehost"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(1).equalsIgnoreCase("simpleupload"))
    				    {
    				        decryptedLinks.add(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(0)+"&hoster=" + links.get(i).get(1))).getHtmlCode(), "iframe src=\"", "\""));
        				progress.increase(1);
    				    }
    				}
    			}
    			
    			// Decrypt abschliessen
    			
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }

    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_MEINUPLOAD", "MeinUpload.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SHAREONLINE", "Share-Online.biz"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SIMPLEUPLOAD", "SimpleUpload.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_BLUEHOST", "BlueHost.to"));
        cfg.setDefaultValue(false);
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}