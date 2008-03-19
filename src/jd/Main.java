package jd;

import java.awt.Graphics;
import java.awt.Image;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JWindow;

import jd.captcha.JACController;
import jd.config.Configuration;
import jd.controlling.JDController;
import jd.controlling.interaction.Interaction;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import jd.utils.Upload;

/**
 * @author astaldo/JD-Team
 */

public class Main {

    private static Logger logger = JDUtilities.getLogger();

    public static void main(String args[]) {
        
        //
     
        if(Runtime.getRuntime().maxMemory()<100000000){
            JDUtilities.restartJD();
        }

        
        
        if( System.getProperty("os.name").toLowerCase().indexOf("mac")>=0){
            logger.info("apple.laf.useScreenMenuBar=true");
            logger.info("com.apple.mrj.application.growbox.intrudes=false");
            logger.info("com.apple.mrj.application.apple.menu.about.name=jDownloader");
           
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "jDownloader");
            System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
      
        JDLocale.setLocale("english");
        JDTheme.setTheme("default");
        boolean stop = false;
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if (string.equals("--help") || string.equals("-h")) {
                String[][] help = new String[][] { { JDUtilities.getJDTitle(), "Coalado::Astaldo::DwD::Botzi GPL" }, { "http://jdownloader.ath.cx/", "http://www.the-lounge.org/viewforum.php?f=217" + System.getProperty("line.separator") }, { "-h, --help", "Print help for jDownloader" }, { "-s --show", "Open a menu to show a JAC prepared captcha" }, { "-t --train", "Open a menu to train a JAC method" } };
                for (int j = 0; j < help.length; j++) {
                    System.out.println(help[j][0] + "\t" + help[j][1]);
                }
                System.exit(0);
            }
            else if (string.equals("--show") || string.equals("-s")) {
                JACController.showDialog(false);
                stop = true;
                break;
            }
            else if (string.equals("--train") || string.equals("-t")) {
                JACController.showDialog(true);
                stop = true;
                break;
            }
           
        }

logger.info(System.getProperty("java.class.path"));
        // rausgenommen verlängert nur den startvorgang
        // if (SingleInstance<Controller.isApplicationRunning()) {
        // JOptionPane.showMessageDialog(null,
        // JDLocale.L("sys.warning.multiple_instance"),
        // JDLocale.L("sys.header.jdownloader","JDownloader runs already"),
        // JOptionPane.WARNING_MESSAGE);
        // System.exit(0);
        // return;
        // }
        // SingleInstanceController.bindRMIObject(new
        // SingleInstanceController());
        if (!stop) {
            Main main = new Main();
            main.go();
         
          
        }
    }
  
    @SuppressWarnings("unchecked")
    private void go() {

        JDInit init = new JDInit();

        
 
       logger.info("Registriere Plugins");
        init.init();
        init.loadImages();
        JWindow window = new JWindow() {
            public void paint(Graphics g) {
                Image splashImage = JDUtilities.getImage("jd_logo_large");
                g.drawImage(splashImage, 0, 0, this);
            }
        };

        window.setSize(450, 100);
        window.setLocationRelativeTo(null);
        
        if(JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty(SimpleGUI.PARAM_SHOW_SPLASH, true)){
       window.setVisible(true);
        }
     
        init.loadConfiguration();
        /*
         * Übergangsfix. Die Interactiosn wurden in eine subconfig verlegt. dieser teil kopiert bestehende events in die neue configfile       
         */
  
        if(JDUtilities.getConfiguration().getInteractions().size()>0&& JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS,null)==null){
            JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).setProperty(Configuration.PARAM_INTERACTIONS,JDUtilities.getConfiguration().getInteractions());
            JDUtilities.getConfiguration().setInteractions(new Vector<Interaction>());
            JDUtilities.saveConfig();
        }
        final JDController controller = init.initController();
        if (init.installerWasVisible()) {
            init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1),true);

        }
        else {
            init.initGUI(controller);

         

            init.initPlugins();
            init.loadDownloadQueue();
            init.loadModules();
            init.checkUpdate();
            if (JDUtilities.getRunType() == JDUtilities.RUNTYPE_LOCAL_JARED) {

                init.doWebupdate(JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID, -1),false);
            }
        }
        controller.setInitStatus(JDController.INIT_STATUS_COMPLETE);

       

             
      //init.createQueueBackup();

        window.dispose();
        controller.getUiInterface().onJDInitComplete();
        Properties pr = System.getProperties();
        TreeSet propKeys = new TreeSet(pr.keySet());  
        for (Iterator it = propKeys.iterator(); it.hasNext(); ) {
            String key = (String)it.next();
           logger.finer("" + key + "=" + pr.get(key));
        }
        logger.info("jd.revision="+JDUtilities.getJDTitle());
        logger.info("jd.run="+JDUtilities.getRunType());
        logger.info("jd.lastAuthor="+JDUtilities.getLastChangeAuthor());   
        logger.info("jd.appDir="+JDUtilities.getCurrentWorkingDirectory(null));

     
   
    }
    

   

}
