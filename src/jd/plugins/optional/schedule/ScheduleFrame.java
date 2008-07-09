package jd.plugins.optional.schedule;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import jd.config.Configuration;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ScheduleFrame extends JPanel implements ActionListener{
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Objekte werden erzeugt
    Timer t = new Timer(10000,this); 
    Timer c = new Timer(1000,this);
    JSpinner maxdls = new JSpinner(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,2),1,10,1));
    JSpinner maxspeed = new JSpinner(new SpinnerNumberModel(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED),0,50000,50));
    JCheckBox premium = new JCheckBox();
    JCheckBox reconnect = new JCheckBox();
    JCheckBox stop_start = new JCheckBox();
    
    SpinnerDateModel date_model = new SpinnerDateModel();
    JSpinner time = new JSpinner(date_model);
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
        
        this.add(new JLabel(JDLocale.L("addons.schedule.menu.maxdl"," max. Downloads")));
        this.add(this.maxdls);
        
        this.add(new JLabel(JDLocale.L("addons.schedule.menu.maxspeed"," max. DownloadSpeed")));
        this.add(this.maxspeed);
        
        this.add(new JLabel("Premium"));
        this.add(this.premium);
        
        this.add(new JLabel(JDLocale.L("addons.schedule.menu.reconnect"," Reconnect ?")));
        this.add(this.reconnect);
        
        this.add(new JLabel(JDLocale.L("addons.schedule.menu.start_stop"," Start/Stop DL ?")));
        this.add(this.stop_start);
        
        this.add(new JLabel(JDLocale.L("addons.schedule.menu.time"," Select Time:")));      
        this.add(this.time);
        
        this.add(new JLabel(JDLocale.L("addons.schedule.menu.redo"," Redo in h:")));      
        this.add(this.repeat);
        
        this.label = new JLabel(title);
        this.add(this.label);
        this.add(this.start);
        
        this.add(this.status);
        
        this.start.addActionListener(this);
        this.t.setRepeats(false);        
    }
    
    //ActionPerformed e 
    public void actionPerformed(ActionEvent e) {     
        int var = (int) parsetime();
        
            if (var > 0 && e.getSource() == start) {
                if (t.isRunning() == false || c.isRunning() == false){
                    this.start.setText(JDLocale.L("addons.schedule.menu.stop","Stop"));               
                    this.t.setInitialDelay(var);
                    this.t.start();
                    this.c.start();
                    this.status.setText("Started!");
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
            else{this.status.setText(JDLocale.L("addons.schedule.menu.p_time"," Select positive time!"));}
            
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
                    Date new_time = date_model.getDate();
                    long var2 = new_time.getTime();
                    var2 = var2 + r * 3600000;
                    new_time.setTime(var2);
                    this.date_model.setValue(new_time);
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
                String remainString = JDUtilities.formatSeconds(var/1000);
                String remain = JDLocale.L("addons.schedule.menu.remain","Remaining:") + " " + remainString;
                this.status.setText(remain);
            }  
        
    }
    
    //Berechnen der TimerZeit
    public double parsetime () {
        Calendar cal = Calendar.getInstance();
        Date start_time = cal.getTime();
        Date end_time = this.date_model.getDate();
        return end_time.getTime() - start_time.getTime();
    }
}
