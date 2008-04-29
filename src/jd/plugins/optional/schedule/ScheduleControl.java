package jd.plugins.optional.schedule;

import java.awt.FlowLayout;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import jd.utils.JDLocale;
import java.util.Vector;

public class ScheduleControl extends JDialog implements ActionListener {
    
    JButton add = new JButton(JDLocale.L("addons.schedule.menu.add","Add"));
    JButton remove = new JButton(JDLocale.L("addons.schedule.menu.remove","Remove"));
    JButton show = new JButton(JDLocale.L("addons.schedule.menu.edit","Edit"));    
    Timer status = new Timer(1000,this);
    JPanel status_panel = new JPanel();
    JPanel menu = new JPanel();

    Choice list = new Choice();
    Vector v = new Vector();
    boolean visible = false;
    
    rsswitcher sw = new rsswitcher();
    JButton swa = new JButton("Start RS.com P/HH"); 
    
    public ScheduleControl(){
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {                
                    setVisible(false);
                }
            });
            
            this.setTitle("Scheduler by Tudels");
            this.setSize(450, 300);
            this.setResizable(false);
            this.setLocation(300, 300);
            
            this.menu.setLayout(new FlowLayout());
            this.menu.add(this.list);
            this.list.add(JDLocale.L("addons.schedule.menu.create","Create"));
            this.menu.add(this.show);
            this.menu.add(this.add);
            this.menu.add(this.remove);            
            this.menu.add(this.swa);
            
            this.swa.setToolTipText("This little plugin is for Premium Users. It checks if it is HappyHour and switches to FreeMode, and back if HappyHour stops.");
            
            this.getContentPane().setLayout(new FlowLayout());
            this.getContentPane().add(menu);
            this.getContentPane().add(status_panel);

            
            this.add.addActionListener(this);
            this.remove.addActionListener(this);
            this.show.addActionListener(this);
            this.swa.addActionListener(this);
                       
            SwingUtilities.updateComponentTreeUI(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        boolean ba = false;
        if(e.getSource() == add){
            int a = v.size() + 1;
            this.v.add(new ScheduleFrame(" Schedule " + a));
            reloadList();
            SwingUtilities.updateComponentTreeUI(this);           
        }
        if(e.getSource() == remove){
            try{
                ScheduleFrame s = (ScheduleFrame) v.elementAt(list.getSelectedIndex());
            
                this.v.remove(list.getSelectedIndex());
                this.reloadList();
                this.status_panel.removeAll();
                renameLabels();
                SwingUtilities.updateComponentTreeUI(this);
            }catch(Exception ex){}
        }
        if (e.getSource() == show){
            try{
                int item = this.list.getSelectedIndex();
                ScheduleFrame sched;
            
                if(visible == false){
                    this.status.stop();
                    this.status_panel.removeAll();
                    sched = (ScheduleFrame) v.elementAt(item);
                    visible = true;
                    this.status_panel.add(sched);
                    this.show.setText(JDLocale.L("addons.schedule.menu.close","Close"));
                    this.controls(false);
                }
                else{
                    visible = false;
                    this.show.setText(JDLocale.L("addons.schedule.menu.edit","Edit"));
                    this.status_panel.removeAll();
                    this.status.start();
                    this.controls(true);
                }
                SwingUtilities.updateComponentTreeUI(this);
            }catch(Exception ex){}
        }
        if (e.getSource() == status){
            
            int size = v.size();
            
            this.status_panel.removeAll();
            this.status_panel.setLayout(new GridLayout(size,1));
            for(int i = 0; i < size; ++i){
                ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
                int a = i+1;
                this.status_panel.add(new JLabel("Schedule "+a+" Status: "+s.status.getText()));
            }
            SwingUtilities.updateComponentTreeUI(this);
            
        }
        if(e.getSource() == swa){
            if (this.sw.my_t_running == true){
                this.sw.my_t.stop();
                this.sw.my_t_running = false;
                this.swa.setText("Start RS.com P/HH");
            }
            else{
                this.sw.my_t.start();
                this.sw.my_t_running = true;
                this.swa.setText("Stop RS.com P/HH");
            }
            }
        }
    
    
    public void reloadList(){
        
        this.list.removeAll();
        int size = v.size();
        
        String[] s = new String[size+10];
        
        for(int i = 1; i <= size; ++i){
            s[i] = " Schedule "+i;
            this.list.add(s[i]);
        }
        if(size == 0){
            this.list.add("Create");
        }

    }
    public void renameLabels(){
        int size = v.size();
        for(int i = 0; i < size; ++i){
            ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
            s.label.setText(list.getItem(i));
        }
        
    }
    public void controls(boolean b){

            this.add.setEnabled(b);
            this.remove.setEnabled(b);
            this.list.setEnabled(b);

    }
}
