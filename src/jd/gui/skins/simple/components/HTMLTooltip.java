package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;





public class HTMLTooltip extends JWindow implements MouseListener,HyperlinkListener{
   
    private HashMap<String,HashMap<String,String>> styles=null;
    private JTextPane htmlArea;
    private Color BORDER_COLOR;
    public HTMLTooltip(){
        this.setAlwaysOnTop(true);
       BORDER_COLOR=JDTheme.C("gui.color.htmlTooltip_border","000000");
        this.htmlArea = new JTextPane();
        htmlArea.addMouseListener(this);
        htmlArea.setEditable(false);
        htmlArea.addHyperlinkListener(this);
        htmlArea.setContentType("text/html");
        htmlArea.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        getContentPane().add(htmlArea, BorderLayout.CENTER);
      pack();
    }
    
    
   public void setStyleEntry(String key, HashMap<String,String> value){
       if(styles==null){
           styles= new HashMap<String,HashMap<String,String>>();
       }
       styles.put(key, value);
   }
   public HashMap<String,HashMap<String,String>> getStyles(){
       return styles;
   }
   public String getCSSString(){
       if(styles==null)return "";
       StringBuffer sb= new StringBuffer();
       sb.append("<style>");
       sb.append("\r\n");
     
       Entry<String, HashMap<String, String>> next;
       Entry<String, String> prop;
       for(Iterator<Entry<String, HashMap<String, String>>> it = styles.entrySet().iterator();it.hasNext();){
            next = it.next();
            sb.append(next.getKey()+"{");
            sb.append("\r\n");
            for(Iterator<Entry<String, String>> it2 = next.getValue().entrySet().iterator();it2.hasNext();){
                 prop = it2.next();
                 sb.append(prop.getKey()+":"+prop.getValue()+";");
                 sb.append("\r\n");
            }
            sb.append("}");
            sb.append("\r\n");
            
       }
       sb.append("</style>");
      
       return sb.toString();
   }
    public void setText(String text){
        String t=getCSSString()+text;
        htmlArea.setText(t); 
     
        htmlArea.invalidate();
        this.pack();
    }
    public static HTMLTooltip show(String htmlText,Point loc){
        HTMLTooltip ret=new HTMLTooltip();
      
        HashMap<String,String> props;
       
        
        
      
        ret.setStyleEntry("h1", props=new  HashMap<String,String>());
        //props.put("font-family","Geneva, Arial, Helvetica, sans-serif");
        props.put("font-size","10px");
        props.put("font-weight","bold");
        props.put("text-align","left");
        props.put("vertical-align","top");
        props.put("display","block");
        props.put("margin","0px");
        props.put("padding","0px");   
        
        
        ret.setStyleEntry("p", props=new  HashMap<String,String>());
       // props.put("font-family","Geneva, Arial, Helvetica, sans-serif");
        props.put("font-size","9px");  
        props.put("margin","1px");
        props.put("padding","0px");  
        
        ret.setStyleEntry("div", props=new  HashMap<String,String>());
        props.put("width","100%");
        props.put("padding","2px");
        props.put("background-color","#"+JDTheme.V("gui.color.htmlTooltip_background","94baff"));  

        ret.setText(htmlText);
       
        ret.setVisible(true);
        
        ret.pack();
        ret.setLocation(loc);
        return ret;
    }
    public void setLocation(Point p){
        p.x-=this.getWidth()/2;
        p.y-=this.getHeight()+3;
        super.setLocation(p);
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