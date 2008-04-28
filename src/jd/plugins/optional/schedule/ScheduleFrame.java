package jd.plugins.optional.schedule;

import java.awt.*;
import java.awt.event.*;
import jd.config.Configuration;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import javax.swing.*;
import java.util.Date;
import java.util.Calendar;

public class ScheduleFrame extends JPanel implements ActionListener{
    
    //Objekte werden erzeugt
    Timer t = new Timer(10000,this); 
    Timer c = new Timer(1000,this);
    JSpinner maxdls = new JSpinner(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN),1,10,1));
    JSpinner maxspeed = new JSpinner(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED),0,5000,50));
    JCheckBox premium = new JCheckBox();
    JCheckBox reconnect = new JCheckBox();
    JCheckBox stop_start = new JCheckBox();
    
    SpinnerDateModel baba = new SpinnerDateModel();
    JSpinner time = new JSpinner(baba);
    String dateFormat = "HH:mm:ss | dd.MM.yy";
    
    JSpinner repeat = new JSpinner(new SpinnerNumberModel(0,0,24,1));
    JLabel label;
    JButton start = new JButton(JDLocale.L("addons.schedule.menu.start","Start"));        
    JLabel status = new JLabel(JDLocale.L("addons.schedule.menu.running"," Not Running!"));    
    
    boolean visible = false;
    
    //Konstruktor des Fensters und Aussehen
    public ScheduleFrame(String title) {
        
        this.start.setBorderPainted(false);
        this.start.setFocusPainted(false);
        this.maxdls.setBorder(BorderFactory.createEmptyBorder());
        this.maxspeed.setBorder(BorderFactory.createEmptyBorder());
        this.time.setToolTipText("Select your time. Format: HH:mm:ss | dd.MM.yy");
        this.time.setEditor(new JSpinner.DateEditor(time,dateFormat));
        this.time.setBorder(BorderFactory.createEmptyBorder());
        this.repeat.setBorder(BorderFactory.createEmptyBorder());
        this.repeat.setToolTipText("Enter h | 0 = disable");
        this.premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM));
        this.reconnect.setSelected(!(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT)));
        
        setLayout(new GridLayout(9,2));        
        add(new JLabel(JDLocale.L("addons.schedule.menu.maxdl"," max. Downloads")));
        add(this.maxdls);
        add(new JLabel(JDLocale.L("addons.schedule.menu.maxspeed"," max. DownloadSpeed")));
        add(this.maxspeed);
        add(new JLabel(" Premium ?"));
        add(this.premium);
        add(new JLabel(JDLocale.L("addons.schedule.menu.reconnect"," Reconnect ?")));
        add(this.reconnect);
        add(new JLabel(JDLocale.L("addons.schedule.menu.start_stop"," Start/Stop DL ?")));
        add(this.stop_start);
        add(new JLabel(JDLocale.L("addons.schedule.menu.time"," Select Time:")));      
        add(this.time);
        add(new JLabel(JDLocale.L("addons.schedule.menu.redo"," Redo in h:")));      
        add(this.repeat);
        this.label = new JLabel(title);
        add(this.label);
        add(this.start);     
        add(this.status);
        
        this.start.addActionListener(this);
        this.t.setRepeats(false);        
    }
    
    //ActionPerformed e 
    public void actionPerformed(ActionEvent e) {     
        int var = (int) parsetime();  
        if (var < 0 & e.getSource() == start){
            this.status.setText(JDLocale.L("addons.schedule.menu.p_time"," Select positive time!"));     
        }
        else{
            if (e.getSource() == start) {
                if (t.isRunning() == false || c.isRunning() == false){
                    this.start.setText(JDLocale.L("addons.schedule.menu.stop","Stop"));               
                    this.t.setInitialDelay(var);
                    this.t.start();
                    this.c.start();
                    this.status.setText(" Started!");
                    this.time.setEnabled(false);
                }
                else {
                    this.start.setText(JDLocale.L("addons.schedule.menu.start","Start"));
                    this.t.stop();
                    this.c.stop();
                    this.status.setText(JDLocale.L("addons.schedule.menu.abort"," Aborted!"));
                    this.time.setEnabled(true);
                }
            }              
            if (e.getSource() == t) {
                
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, maxspeed.getValue());
                JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls.getValue());
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, !(reconnect.isSelected()));
                JDUtilities.getSubConfig("DOWNLOAD").save();
                JDUtilities.saveConfig();
                if (this.stop_start.isSelected() == true){
                    JDUtilities.getController().toggleStartStop();           
                }
                if((Integer) this.repeat.getValue() > 0){
                    int r = (Integer) this.repeat.getValue();
                    Date new_time = baba.getDate();
                    long var2 = new_time.getTime();
                    var2 = var2 + r * 3600000;
                    new_time.setTime(var2);
                    baba.setValue(new_time);
                    var = (int) parsetime(); 
                    this.t.setInitialDelay(var);
                    this.t.start();
                }
                else{
                    this.start.setText(JDLocale.L("addons.schedule.menu.start","Start"));
                    this.c.stop();
                    this.status.setText(JDLocale.L("addons.schedule.menu.finished"," Finished!"));
                    this.time.setEnabled(true);
                }
            }       
            if (e.getSource() == c){
                String ba = JDUtilities.formatSeconds(var/1000);
                String remain = JDLocale.L("addons.schedule.menu.remain"," Remaining: ") + ba;
                this.status.setText(remain);
            }  
        }
    }
    
    //Berechnen der TimerZeit
    public double parsetime () {
        Calendar cal = Calendar.getInstance();
        Date start_time = cal.getTime();
        Date end_time = baba.getDate();
        return end_time.getTime() - start_time.getTime();
    }
}
