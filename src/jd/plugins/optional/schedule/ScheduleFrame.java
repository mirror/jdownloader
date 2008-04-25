package jd.plugins.optional.schedule;

import java.awt.*;
import java.awt.event.*;
import jd.config.Configuration;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDUtilities;

public class ScheduleFrame extends Frame implements ActionListener{
    
    javax.swing.Timer t = new javax.swing.Timer(10000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
               JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, Integer.parseInt(maxspeed.getText()));
               JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, Integer.parseInt(maxdls.getText()));
               JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.getState());
               JDUtilities.getSubConfig("DOWNLOAD").save();
               JDUtilities.saveConfig();
               SimpleGUI.CURRENTGUI.updateStatusBar();
               start.setLabel("Start");
           }
    });
    
    TextField maxdls = new TextField();
    TextField maxspeed = new TextField();
    Checkbox premium = new Checkbox();
    
    TextField hrs = new TextField("0");
    TextField min = new TextField("0");
    TextField sek = new TextField("0");
    
    Button start = new Button("Start");
    Button close = new Button("Close");
    
    Label remain = new Label();
    
    public ScheduleFrame() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                
                setVisible(false);

            }
        });
        
        setLayout(new GridLayout(6,2));
        add(new Label("max. Downloads"));
        add(maxdls);
        add(new Label("max. DownloadSpeed"));
        add(maxspeed);
        add(new Label("Premium ?"));
        add(premium);
      
        add(new Label("Enter Time:"));
        
        Panel time = new Panel();
        time.setLayout(new GridLayout(1,1));
        time.add(hrs);
        time.add(min);
        time.add(sek);
        
        add(time);
        add(start);
        add(close);
        add(remain);

        start.addActionListener(this);
        close.addActionListener(this);
        t.setRepeats(false);
    }
    
    public void actionPerformed(ActionEvent e) {
  
         if (e.getSource() == start) {
            if (t.isRunning() == false){
                this.start.setLabel("Stop");
                int var = parsetime();
                t.setInitialDelay(var);
                t.start();
                
            }
            else {
                this.start.setLabel("Start");
                t.stop();
                
            }
        }
        
        if (e.getSource() == close) {
            
            this.setVisible(false);
        }
    }
    
    public int parsetime () {
        
        int hrs1 = Integer.parseInt(this.hrs.getText());
        int min1 = Integer.parseInt(this.min.getText());
        int sek1 = Integer.parseInt(this.sek.getText());
        int mili = hrs1 * 3600 * 1000 + min1 * 60 * 1000 + sek1 * 1000;
        return mili;    
    }    
}
