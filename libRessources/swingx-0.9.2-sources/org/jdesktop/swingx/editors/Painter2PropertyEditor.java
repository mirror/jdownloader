/*
 * Painter2PropertyEditor.java
 *
 * Created on August 2, 2006, 8:27 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.URLPainter;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;

/**
 *
 * @author joshy
 */
public class Painter2PropertyEditor extends PropertyEditorSupport {
    Painter painter = new MattePainter(Color.BLUE);
    JFileChooser picker;
    
    void log(String str) {
        //JOptionPane.showMessageDialog(picker,str);
        System.out.println(str);
    }
    /** Creates a new instance of Painter2PropertyEditor */
    public Painter2PropertyEditor() {
        picker = new JFileChooser();
        picker.setApproveButtonText("Load Painter");
        picker.setMultiSelectionEnabled(false);
        picker.setSelectedFile(new File("/Users/joshy/projects/current/AB5k/src/java/ab5k/desklet/"));
        picker.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    File file = picker.getSelectedFile();
                    painter = new URLPainter(file);
                    //painter = PainterUtil.loadPainter(file);
                    firePropertyChange();
                    log("loaded the painter: " + painter);
                } catch (Throwable ex) {
                    //System.out.println("error loading the painter: " + ex.getMessage());
                    //ex.printStackTrace();
                    StringWriter wrt = new StringWriter();
                    ex.printStackTrace(new PrintWriter(wrt));
                    JOptionPane.showMessageDialog(picker,ex.getMessage() + " " + wrt.toString());// + painter.getClass().getName());
                }
            }
        });
    }
    
    public Painter getValue() {
        return painter;
    }
    
    public void setValue(Object object) {
        log("setting: " + object);
        painter = (Painter)object;
        super.setValue(object);
    }
    
    public void setAsText(String text) throws IllegalArgumentException {
//        u.p("setting as text: " + text);
        log("setting as text: " + text);
    }
    
    public String getAsText() {
        if(painter instanceof URLPainter) {
            return ((URLPainter)painter).getURL().toString();
        }
        if(painter != null) {
            return painter.getClass().getName();
        } else {
            return "null!!";
        }
    }
    
    public String getJavaInitializationString() {
        URLPainter painter = (URLPainter)getValue();
        return painter == null ? "null" : 
            "new org.jdesktop.swingx.painter.FilePainter(\""+
                painter.getURL().toString()+"\")";
    }

    public boolean supportsCustomEditor() {
        return true;
    }
    
    public Component getCustomEditor() {
        return picker;
    }
}
