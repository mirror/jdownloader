package jd.utils;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.parser.Regex;

public class CheckJava {
    public boolean check() {
        String runtimeName = System.getProperty("java.runtime.name").toLowerCase();
        String runtimeVersion = System.getProperty("java.runtime.version").toLowerCase();
        
        if(new Regex(runtimeVersion,"1\\.5").count() < 0 || new Regex(runtimeVersion,"1\\.6").count() < 0 || new Regex(runtimeName,"IcedTea").count() > 0) {
            String html = String.format(JDLocale.L("gui.javacheck.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><div style='width:534px;height;200px'><h2>You useses a wrong Java version. Please use a original Sun Java. Start jDownloader anyway?<table width='100%%'><tr><th colspan='2'>Your Java Version:</th></tr><tr><th>Runtime Name</th><td>%s</td></tr><tr><th>Runtime Version</th><td>%s</td></tr></table></div>"), runtimeName, runtimeVersion);
            HTMLDialog tda = new HTMLDialog(null, JDLocale.L("gui.javacheck.title", "Wrong Java Version"), html);
            return tda.success;
        }
        
        return true;
    }
    public class HTMLDialog extends JDialog implements ActionListener, HyperlinkListener  {
        private static final long serialVersionUID = -7741748123426268439L;
        protected Insets    insets = new Insets(0, 0, 0, 0);
        protected Logger    logger = JDUtilities.getLogger();
        private JButton     btnOk;
        private JButton     btnCancel;
        private JTextPane   htmlArea;
        private JScrollPane scrollPane;
        private boolean success=false;
        private HTMLDialog(JFrame frame, String title, String html) {
            super(frame);
            setLayout(new BorderLayout());
            this.setName(title);
            btnCancel = new JButton(JDLocale.L("gui.btn_cancel","Cancel"));
            btnCancel.addActionListener(this);
            btnOk = new JButton(JDLocale.L("gui.btn_ok","OK"));
            btnOk.addActionListener(this);
            setTitle(title);
            htmlArea = new JTextPane();
            scrollPane = new JScrollPane(htmlArea);
            htmlArea.setEditable(false);
            htmlArea.setContentType("text/html"); 
            htmlArea.setText(html);
            htmlArea.requestFocusInWindow();
            htmlArea.addHyperlinkListener(this);
           
            this.add(scrollPane,BorderLayout.CENTER);
            JPanel p= new JPanel();
            p.add(btnOk);
            p.add(btnCancel);
            this.add(p,BorderLayout.SOUTH);
            
            pack();
            getRootPane().setDefaultButton(btnOk);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            this.setVisible(true);
            LocationListener list = new LocationListener();
            this.addComponentListener(list);
             this.addWindowListener(list);
            this.setVisible(false);
            setModal(true);
            this.setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == btnOk) {
                this.success=true;
                dispose();
            }else{
                dispose();
            }
        }
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                JLinkButton.openURL( e.getURL());
        }
    }
}
