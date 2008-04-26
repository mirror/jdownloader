package jd.plugins.optional.schedule;

import java.awt.*;
import java.awt.event.*;
import jd.config.Configuration;
import jd.utils.JDUtilities;
import javax.swing.*;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

public class ScheduleFrame extends JFrame implements ActionListener{
    
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
    
    JSpinner repeat = new JSpinner(new SpinnerNumberModel(0,0,24,1));
    JButton start = new JButton("Start");    
    JButton close = new JButton("Close");    
    JLabel status = new JLabel();
    
    //Konstruktor des Fensters und Aussehen
    public ScheduleFrame() {
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                
                setVisible(false);

            }
        });
        this.setSize(300, 200);
        this.setResizable(false);
        
        this.maxdls.setBorder(BorderFactory.createEmptyBorder());
        this.maxspeed.setBorder(BorderFactory.createEmptyBorder());
        this.time.setBorder(BorderFactory.createEmptyBorder());
        this.repeat.setBorder(BorderFactory.createEmptyBorder());
        this.repeat.setToolTipText("Enter h | 0 = disable");
        
        getContentPane().setLayout(new GridLayout(9,2));        
        getContentPane().add(new JLabel(" max. Downloads"));
        getContentPane().add(this.maxdls);
        getContentPane().add(new JLabel(" max. DownloadSpeed"));
        getContentPane().add(this.maxspeed);
        getContentPane().add(new JLabel(" Premium ?"));
        getContentPane().add(this.premium);
        getContentPane().add(new JLabel(" Reconnect ?"));
        getContentPane().add(this.reconnect);
        getContentPane().add(new JLabel(" Start/Stop DL ?"));
        getContentPane().add(this.stop_start);
        getContentPane().add(new JLabel(" Select Time:"));      
        getContentPane().add(this.time);
        getContentPane().add(new JLabel(" Redo in h:"));      
        getContentPane().add(this.repeat);
        getContentPane().add(this.start);
        getContentPane().add(this.close);        
        getContentPane().add(this.status);
        
        this.start.addActionListener(this);
        this.close.addActionListener(this);
        this.t.setRepeats(false);
        
    }
    
    //ActionPerformed e 
    public void actionPerformed(ActionEvent e) {
      
        int var = (int) parsetime();  
        if (var < 0 & e.getSource() == start){
          this.status.setText(" Select positive time!");     
      }
      else{
         if (e.getSource() == start) {
            if (t.isRunning() == false || c.isRunning() == false){
                this.start.setText("Stop");               
                this.t.setInitialDelay(var);
                this.t.start();
                this.c.start();
                this.status.setText(" Started!");
                this.time.setEnabled(false);
            }
            else {
                this.start.setText("Start");
                this.t.stop();
                this.c.stop();
                this.status.setText(" Aborted!");
                this.time.setEnabled(true);
            }
        }
        
        if (e.getSource() == close) {
            
            this.setVisible(false);
            
        }
        
        if (e.getSource() == t) {
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, maxspeed.getValue());
            JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls.getValue());
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, premium.isSelected());
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_DISABLE_RECONNECT, reconnect.isSelected());
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
                this.start.setText("Start");
                this.c.stop();
                this.status.setText(" Finished!");
                this.time.setEnabled(true);
            
        }}
        
        if (e.getSource() == c){
            String ba = JDUtilities.formatSeconds(var/1000);
            String remain = " Remaining: " + ba;
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
    
    //Werte neu auslesen und eintragen
    public void repaint() {
        this.maxdls.setValue(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN));
        this.maxspeed.setValue(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED));
        this.premium.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM));
        this.reconnect.setSelected(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT));
    }
    
    
}
