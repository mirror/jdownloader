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
import jd.plugins.event.PluginEvent;

public class XliceNet extends PluginForDecrypt {

    final static String         host                = "xlice.net";

    private static final String USE_RAPIDSHARE      = "USE_RAPIDSHARE";

    private static final String USE_UPLOADED        = "USE_UPLOADED";

    private static final String USE_FILEFACTORY     = "USE_FILEFACTORY";

    private static final String USE_OXEDION         = "USE_OXEDION";

    private static final String USE_GULLI           = "USE_GULLI";

    private static final String USE_SIMPLEUPLOAD    = "USE_SIMPLEUPLOAD";

    private static final String USE_DEPOSITFILES    = "USE_DEPOSITFILES";

    private static final String USE_FILES           = "USE_FILES";

    private String              version             = "1.0.0.0";

    private Pattern             patternSupported    = getSupportPattern("http://[*]xlice.net/f[+]/[*]");

    private Pattern             patternRapidshare   = Pattern.compile("onclick=\".*\'../1/");

    private Pattern             patternGulli        = Pattern.compile("onclick=\".*\'../2/");

    private Pattern             patternOxedion      = Pattern.compile("onclick=\".*\'go/4/");

    private Pattern             patternFiles        = Pattern.compile("onclick=\".*\'../5/");

    private Pattern             patternUploaded     = Pattern.compile("onclick=\".*\'../6/");

    private Pattern             patternSimpleupload = Pattern.compile("onclick=\".*\'../7/");

    private Pattern             patternDepositfiles = Pattern.compile("onclick=\".*\'../9/");

    private Pattern             patternFilefactory  = Pattern.compile("onclick=\".*\'../10/");

    private String              LinkFile            = "http://xlice.net/dl/";

    private String              LinkFolder          = "http://xlice.net/go/";

    /*
     * 1 Rapidshare 2 Gullishare 3 4 Oxedion 5 Files.to 6 uploaded.o 7
     * Simpleupload 8 9 Depositfiles 10 Filefactory
     */

    public XliceNet() {
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
        return "Xlice.net-1.0.0.";
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
    			RequestInfo reqinfo = getRequest(url,null,null,true);
    			int count = 0;
    			
    			// Links zählen
    			if((Boolean) this.getProperties().getProperty(USE_RAPIDSHARE,true)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternRapidshare);
    			}
    			if((Boolean) this.getProperties().getProperty(USE_UPLOADED,true)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternUploaded);
    			}
    			if((Boolean) this.getProperties().getProperty(USE_DEPOSITFILES,false)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternDepositfiles);
    			}
    			if((Boolean) this.getProperties().getProperty(USE_FILEFACTORY,false)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternFilefactory);
    			}
    			if((Boolean) this.getProperties().getProperty(USE_OXEDION,true)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternOxedion);
    			}
    			if((Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",false)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternSimpleupload);
    			}
    			if((Boolean) this.getProperties().getProperty(USE_GULLI,true)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternGulli);
    			}
    			if((Boolean) this.getProperties().getProperty(USE_FILES,false)) {
    				count = count + countOccurences(reqinfo.getHtmlCode(), patternFiles);
    			}
    			progress.setRange( count);

				RequestInfo reqhelp = null;
				Vector<Vector<String>> links;
				String link = "";
				
				if(parameter.indexOf("xlice.net/file/")>=0) {
					link = LinkFile;
				}
				if(parameter.indexOf("xlice.net/folder/")>=0) {
					link = LinkFolder;
				}
logger.info(parameter);
				// Links herausfiltern
				if( this.getProperties().getProperty(USE_RAPIDSHARE)!=null &&(Boolean) this.getProperties().getProperty(USE_RAPIDSHARE)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/1/°/\'");
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "1/" + links.get(i).get(0)),null,null,true);
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty(USE_GULLI)!=null &&(Boolean) this.getProperties().getProperty(USE_GULLI)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/2/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "2/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty(USE_OXEDION)!=null &&(Boolean) this.getProperties().getProperty(USE_OXEDION)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/4/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "4/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty(USE_FILES)!=null &&(Boolean) this.getProperties().getProperty(USE_FILES)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/5/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "5/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty(USE_UPLOADED)!=null &&(Boolean) this.getProperties().getProperty(USE_UPLOADED)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/6/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "6/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty("USE_SIMPLEUPLOAD",false)!=null &&(Boolean) this.getProperties().getProperty("USE_SIMPLEUPLOAD",false)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/7/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "7/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty(USE_DEPOSITFILES)!=null &&(Boolean) this.getProperties().getProperty(USE_DEPOSITFILES)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/9/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "9/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				
				if(this.getProperties().getProperty(USE_FILEFACTORY)!=null &&(Boolean) this.getProperties().getProperty(USE_FILEFACTORY)) {
					links = getAllSimpleMatches(reqinfo.getHtmlCode(), "/10/°/\'");
					
					for(int i=0; i<links.size(); i++) {
						reqhelp = getRequest(new URL(link + "10/" + links.get(i).get(0)));
						reqhelp = getRequest(new URL(getBetween(reqhelp.getHtmlCode(), "href=\"", "/\">geht es hier weiter")));
						if(reqhelp.getLocation() != null) {
							decryptedLinks.add(reqhelp.getLocation());
						progress.increase(1);
						}
						else {
							decryptedLinks.add("");
						progress.increase(1);
						}
					}
				}
				System.out.println(decryptedLinks.size());
    			// Decrypt abschliessen
    			
    			logger.info(decryptedLinks.size() + " downloads decrypted "+decryptedLinks);
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
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_RAPIDSHARE, "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_UPLOADED, "Uploaded.to"));
        cfg.setDefaultValue(false);
        // config.addEntry(cfg = new
        // PluginConfigEntry(PluginConfig.TYPE_CHECKBOX, getProperties(),
        // "USE_NETLOAD", "Netload.in"));
        // cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_FILEFACTORY, "Filefactory.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_OXEDION, "Oxedion.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_GULLI, "Share.Gulli.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_SIMPLEUPLOAD, "Simpleupload.net"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_DEPOSITFILES, "Depositfiles.com"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), USE_FILES, "Files.to"));
        cfg.setDefaultValue(false);
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}