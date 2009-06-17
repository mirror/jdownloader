package tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import jd.DecryptPluginWrapper;
import jd.JDInit;
import jd.controlling.JDController;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Decrypter {

    @Before
    public void setUp() throws Exception {
        JDUtilities.getDatabaseConnector();
        // JDUtilities.getDownloadController();
        JDUtilities.getController();
        JDInit init = new JDInit();

        init.init();
        new JDController();
        // init.initPlugins();
        init.loadPluginForHost();
        init.loadPluginForDecrypt();
        init.initControllers();
        //blablablabla
    }

    @Test
    public void decryptUCMS() {
        String url = TestUtils.getStringProperty("UCMS_URL");
        boolean found = false;
        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pd.canHandle(url)) {
                found = true;
                PluginForDecrypt plg = (PluginForDecrypt) pd.getNewPluginInstance();

                CryptedLink[] d = plg.getDecryptableLinks(url);

                try {
                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], new ProgressController("test", 10));

                    assertTrue(a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (!found) fail();
    }

    @Test
    public void decryptSerienjunkies() {
        String url = TestUtils.getStringProperty("SERIENJUNKIES_URL");
        boolean found = false;
        for (DecryptPluginWrapper pd : DecryptPluginWrapper.getDecryptWrapper()) {
            if (pd.canHandle(url)) {
                found = true;
                PluginForDecrypt plg = (PluginForDecrypt) pd.getNewPluginInstance();

                CryptedLink[] d = plg.getDecryptableLinks(url);

                try {
                    ArrayList<DownloadLink> a = plg.decryptIt(d[0], new ProgressController("test", 10));

                    assertTrue(a.size() > 1 || (a.size() == 1 && a.get(0).getBrowserUrl() != null));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (!found) fail();
    }

    @After
    public void tearDown() throws Exception {
        // JDUtilities.getController().exit();
    }
}