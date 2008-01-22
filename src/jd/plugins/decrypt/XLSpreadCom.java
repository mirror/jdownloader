package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class XLSpreadCom extends PluginForDecrypt {

    final static String host             = "xlspread.com";

    private String      version          = "1.0.0.0";
    //http://www.xlspread.com/download.html?id=b0b18a2f966cc247660845508e1111b4
    //http://www.xlspread.com/download.html?id=61a7912765cb3d04fb98ce2e7dcbb4a4
    private Pattern     patternSupported = getSupportPattern("http://[*]xlspread.com/download.html\\?id=[a-zA-Z0-9]{32}");

    public XLSpreadCom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Xlspread.com-3.0.0.";
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
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    		    
    		    
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			int count = 0;
    			Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "</td></tr><tr><td><b>°</b>°downlink.php?id=°&amp;hoster");
    			System.out.println(links.size());
    			// Anzahl der Links zählen
    			for(int i=0; i<links.size(); i++) {
	    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true) && links.get(i).get(0).equals("Rapidshare")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true) && links.get(i).get(0).equals("Uploaded")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true) && links.get(i).get(0).equals("Netload")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_MEINUPLOAD",true) && links.get(i).get(0).equals("MeinUpload")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE",true) && links.get(i).get(0).equals("Share-Online")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",true) && links.get(i).get(0).equals("Simpleupload")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_BLUEHOST",true) && links.get(i).get(0).equals("Bluehost")) {
	    				count++;
	    			}
	    			if((Boolean) this.getProperties().getProperty("USE_FASTLOAD",true) && links.get(i).get(0).equals("Fastload")) {
	    				count++;
	    			}
    			}
    			progress.setRange(count);
    			
    			if((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Rapidshare"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=rapidshare")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_UPLOADED",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Uploaded"))
    				    {
                            decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=uploaded")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_NETLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Netload"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=netload")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_MEINUPLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("MeinUpload"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=meinupload")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SHAREONLINE",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Share-Online"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=share-online")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_BLUEHOST",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Bluehost"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=bluehost")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Simpleupload"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=simpleupload")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
    				        progress.increase(1);
    				    }
    				}
    			}
    			if((Boolean) this.getProperties().getProperty("USE_FASTLOAD",true)) {
    				for( int i=0; i<links.size();i++){
    				    if(links.get(i).get(0).equalsIgnoreCase("Fastload"))
    				    {
    				        decryptedLinks.add(createDownloadlink(getBetween(getRequest(new URL("http://www.xlspread.com/downlink.php?id="+links.get(i).get(2)+"&hoster=fastload")).getHtmlCode(), "</div>\n<iframe src=\"", "\"")));
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
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
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
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_FASTLOAD", "Fast-load.net"));
        cfg.setDefaultValue(false);
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}