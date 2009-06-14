package tests.singletests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import jd.DecryptPluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.utils.TestUtils;

public class Decrypter {
	private String[] urls = {};

    @Before
    public void setUp() {
        TestUtils.mainInit();
        TestUtils.initDecrypter();
        TestUtils.initHosts();
        TestUtils.finishInit();
    }

    @Test
    public void decrypt() {
        for(String url : urls) {
	        boolean found = false;
	        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
	            if (pd.canHandle(url)) {
	                found = true;
	                PluginForDecrypt plg = (PluginForDecrypt) pd.getNewPluginInstance();
	
	                CryptedLink[] d = plg.getDecryptableLinks(url);
	
	                try {
	                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], new ProgressController("test", 10));
	                    
	                    if(a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null))
	                    	assertTrue(true);
	                    else {
	                    	TestUtils.log("Error with url: " + url);
	                    	assertTrue(false);
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	                break;
	            }
	        }
	        if (!found) {
	        	TestUtils.log("Url not found: " + url);
	        	fail();
	        }
        }
    }

    @After
    public void tearDown() throws Exception {
        // JDUtilities.getController().exit();
    }
}