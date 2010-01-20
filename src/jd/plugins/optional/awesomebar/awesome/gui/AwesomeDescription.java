package jd.plugins.optional.awesomebar.awesome.gui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;

import jd.utils.locale.JDL;

public class AwesomeDescription extends JLabel{
    private static final long serialVersionUID = -2089443319539371495L;
    private final AwesomeToolbarPanel awesomePanel;
    
    AwesomeDescription(AwesomeToolbarPanel awesomePanel){
        super(JDL.L("jd.plugins.optional.awesomebar.panel.description", "thisisatest"));
        this.awesomePanel = awesomePanel;
        this.setForeground(Color.gray);
        this.setFont(new Font("Arial", Font.PLAIN, 9));
        this.setSize(150, 9);
    }

    public AwesomeToolbarPanel getAwesomePanel() {
        return awesomePanel;
    }
    
}