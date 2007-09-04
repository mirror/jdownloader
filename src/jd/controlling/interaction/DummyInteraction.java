package jd.controlling.interaction;

import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.router.RouterData;

/**
 * Diese Klasse führt eine Test INteraction durch
 * 
 * @author coalado
 */
public class DummyInteraction extends Interaction{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1332164738388120767L;
    private static final String NAME ="Dummy";

    
    @Override
    public boolean interact() {    
        logger.info("Starting Dummy");
        return JOptionPane.showConfirmDialog(new JFrame(), "Dummy Interaction bestätigen?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0;
    }
    public String toString(){
        return NAME;
    }
    @Override
    public String getName() {
    
        return NAME;
    }
}
