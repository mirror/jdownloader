package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.Link.JLinkButton;





public class HTMLTooltip extends JWindow implements MouseListener,HyperlinkListener{
    private String style="<style>h1{    font-family:Geneva, Arial, Helvetica, sans-serif; font-size:10px;font-weight:bold;text-align: left;vertical-align: top;display: block; margin: 0px;padding: 0px;}p{font-family:Geneva, Arial, Helvetica, sans-serif;font-size:9px; padding: 0px; margin: 1px;}div{width:300px;background-color:#8976F8;border: 1px solid #000000;}</style>";
    private JTextPane htmlArea;
    public HTMLTooltip(){
        this.setAlwaysOnTop(true);
       
        this.htmlArea = new JTextPane();
        htmlArea.addMouseListener(this);
        htmlArea.setEditable(false);
        htmlArea.addHyperlinkListener(this);
        htmlArea.setContentType("text/html");
        
        getContentPane().add(htmlArea, BorderLayout.CENTER);
      pack();
    }
    public String getStyle() {
        return style;
    }
    public void setStyle(String style) {
        this.style = style;
    }
    public void setText(String text){
        String t=style+text;
        htmlArea.setText(t); 
     
        htmlArea.invalidate();
        this.pack();
    }
    public static HTMLTooltip show(String htmlText,Point loc){
        HTMLTooltip ret=new HTMLTooltip();
        ret.setText(htmlText);
        ret.setLocation(loc);
        ret.setVisible(true);
        
        
        ret.pack();
        
        return ret;
    }
    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    public void mouseExited(MouseEvent e) {
        destroy();
        
    }
    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }
    public void destroy() {
        if(!this.isVisible())return;
        this.setVisible(false);
        this.dispose();
        
    }
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            
            JLinkButton.openURL( e.getURL());
            
          }
        
    }

}