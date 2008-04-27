package jd.plugins.optional.schedule;

import java.awt.FlowLayout;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import java.util.Vector;

public class ScheduleControl extends JDialog implements ActionListener {
    
    JButton add = new JButton("Add");
    JButton remove = new JButton("Remove");
    JButton show = new JButton("Show");    
    Timer status = new Timer(1000,this);
    JPanel lpanel = new JPanel();
    JPanel menu = new JPanel();
    Choice list = new Choice();
    Vector v = new Vector();
    
    public ScheduleControl(){
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {                
                    setVisible(false);
                    int size = v.size();
                    for(int i = 0; i < size; ++i){
                        ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
                        s.setVisible(false);
                    }
                }
            });
            
            this.setTitle("Scheduler by Tudels");
            this.setSize(400, 300);
            this.setResizable(false);
            this.setLocation(300, 300);
            
            this.menu.setLayout(new FlowLayout());
            this.menu.add(this.list);
            this.list.add("Create");
            this.menu.add(this.show);
            this.menu.add(this.add);
            this.menu.add(this.remove);            
            this.menu.add(this.lpanel);
            
            this.getContentPane().setLayout(new FlowLayout());
            this.getContentPane().add(menu);
            this.getContentPane().add(lpanel);
                       
            this.add.addActionListener(this);
            this.remove.addActionListener(this);
            this.show.addActionListener(this);
            this.status.addActionListener(this);
                       
            SwingUtilities.updateComponentTreeUI(this);
    }
    
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == add){
            int size = v.size()+1;
            this.v.add(new ScheduleFrame("Schedule " + size));
            reloadList();
            SwingUtilities.updateComponentTreeUI(this);           
        }
        if(e.getSource() == remove){
            
            ScheduleFrame s = (ScheduleFrame) v.elementAt(list.getSelectedIndex());
            s.setVisible(false);
            this.v.remove(list.getSelectedIndex());
            reloadList();
            SwingUtilities.updateComponentTreeUI(this);
            renameWindows();
        }
        if (e.getSource() == show){
            ScheduleFrame s = (ScheduleFrame) v.elementAt(list.getSelectedIndex());
            s.setVisible(true);
            s.repaint();
            SwingUtilities.updateComponentTreeUI(this);
        }
        if (e.getSource() == status){
            
            int size = v.size();
            
            this.lpanel.removeAll();
            this.lpanel.setLayout(new GridLayout(size,1));
            for(int i = 0; i < size; ++i){
                ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
                int a = i+1;
                this.lpanel.add(new JLabel("Schedule "+a+" Status: "+s.status.getText()));
            }
            SwingUtilities.updateComponentTreeUI(this);
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
    
    public void renameWindows(){
        int size = v.size();
        for(int i = 0; i < size; ++i){
            ScheduleFrame s = (ScheduleFrame) v.elementAt(i);
            s.setTitle(list.getItem(i));
        }
        
    }
}
