package jd.plugins.optional.schedule;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Timer;

import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.plugins.HTTP;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class rsswitcher implements ActionListener{
    
    boolean my_1;
    Timer my_t = new Timer(300000,this);
    boolean my_t_running = false;

    
    public rsswitcher(){
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == my_t){
            new Thread() {
                public void run() {
                    ProgressController progress = new ProgressController(JDLocale.L("plugins.hoster.rapidshare.com.happyHours", "Happy Hour Check"), 3);
                    try {
                        RequestInfo ri = HTTP.getRequest(new URL("http://jdownloader.org/hh.php?txt=1"));
                        if (ri.containsHTML("Hour")) {
                            my_1 = true;
                        } 
                        else {
                            my_1 = false;
                        }
                    } catch (Exception e) {}
                    progress.finalize();
                }
            }.start();
            
            if(my_1){
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, false);
                JDUtilities.saveConfig();
            }
            else{
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true);
                JDUtilities.saveConfig();
            }
        }

        
    }
}

