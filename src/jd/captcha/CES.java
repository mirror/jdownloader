package jd.captcha;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.config.Configuration;
import jd.gui.skins.simple.CaptchaDialog;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.Plugin;
import jd.plugins.host.Rapidshare;
import jd.utils.CESClient;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;


public class CES {
   private static boolean enabled = false;
private static CESClient ces;
   private static String loginDialog()
   {
       return new Dialog(((SimpleGUI) JDUtilities.getGUI()).getFrame()) {
           JTextField user = new JTextField();
           JTextField pass = new JTextField();
           String ret = null;
           /**
            * 
            */
           private static final long serialVersionUID = -5144850223169000644L;

           String init() {
               setLayout(new BorderLayout());
               setModal(true);
               setTitle(JDLocale.L("jac.CES.login.title", "CaptchaExchangeServer login"));
               setAlwaysOnTop(true);
               setSize(400, 120);
               setLocation(JDUtilities.getCenterOfComponent(getOwner(), this));
               JPanel panel = new JPanel(new GridBagLayout());
               addWindowListener(new WindowListener() {

                   public void windowActivated(WindowEvent e) {
                      

                   }

                   public void windowClosed(WindowEvent e) {
                      

                   }

                   public void windowClosing(WindowEvent e) {
                       dispose();

                   }

                   public void windowDeactivated(WindowEvent e) {
                      

                   }

                   public void windowDeiconified(WindowEvent e) {
                      

                   }

                   public void windowIconified(WindowEvent e) {
                      

                   }

                   public void windowOpened(WindowEvent e) {
                      

                   }
               });
               user = new JTextField(100);
               
               pass = new JTextField(100);
               panel.setLayout(new GridLayout(2, 2));
               panel.add(new JLabel(JDLocale.L("jac.CES.login.user", "Benutzername:"), JLabel.CENTER));
               panel.add(user);
               panel.add(new JLabel(JDLocale.L("jac.CES.login.pass", "Passwort:"), JLabel.CENTER));
               panel.add(pass);
               
               JButton btnOK = new JButton(JDLocale.L("gui.btn_continue", "OK"));
               btnOK.addActionListener(new ActionListener() {

                   public void actionPerformed(ActionEvent e) {
                       String un = user.getText();
                       String pw = pass.getText();
                       if(un!=null && !un.matches("[\\s]*") && pw!=null && !pw.matches("[\\s]*") )
                       {
                           ret="user="+JDUtilities.urlEncode(un)+"&pass="+JDUtilities.urlEncode(pw);
                       }
                       dispose();
                   }

               });

               add(panel, BorderLayout.NORTH);
               add(btnOK, BorderLayout.SOUTH);
               setVisible(true);
               return ret;
           }

       }.init();
   }
   public static String getLonginString()
   {
       String loginstring =  JDUtilities.getConfiguration().getStringProperty(Configuration.JAC_CES_LOGIN, null);
       if(loginstring==null)
       {
           loginstring=loginDialog();
           JDUtilities.getConfiguration().setProperty(Configuration.JAC_CES_LOGIN, loginstring);
       }
       return loginstring;
   }
   public static boolean isEnabled()
   {
       return enabled;
   }
   public static void setEnabled(boolean bool)
   {
       enabled=bool;
       if(bool){
           ces= new CESClient();
           ces.setLogins(JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_USER),JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_PASS));
           new Thread(){
               public void run(){
                   
          
           ces.enterQueueAndWait();
               }
           }.start();
       }else if(ces!=null){
           ces.abortReceiving();
           
       }
   }
   public static boolean toggleActivation()
   {
       setEnabled(!enabled);
       
     
       return enabled;
   }
   public static String getCesImageString() {
       if (CES.isEnabled())
           return JDTheme.V("gui.images.ceson");
       else
           return JDTheme.V("gui.images.cesoff");

   }
   public static ImageIcon getImage()
   {
       return new ImageIcon(JDUtilities.getImage(getCesImageString()));
   }
   public static String captchaDialog(Plugin plugin, File captcha)
   {
       CaptchaDialog captchaDialog = new CaptchaDialog(((SimpleGUI) JDUtilities.getGUI()).getFrame(), plugin, captcha, "");
       captchaDialog.countdown=30;
       // frame.toFront();
      // captchaDialog.
       captchaDialog.setVisible(true);
       return captchaDialog.getCaptchaText();
   }
public static void requestCode(File captchaFile, String specs, Rapidshare rapidshare) {
   
    
}
}
