//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
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

import jd.utils.JDTheme;

public class HTMLTooltip extends JWindow implements MouseListener, HyperlinkListener {

    private static final long serialVersionUID = 1L;

    public static HTMLTooltip show(String htmlText, Point loc) {
        HTMLTooltip ret = new HTMLTooltip();

        HashMap<String, String> props;

        ret.setStyleEntry("h1", props = new HashMap<String, String>());
        // props.put("font-family","Geneva, Arial, Helvetica, sans-serif");
        props.put("font-size", "10px");
        props.put("font-weight", "bold");
        props.put("text-align", "left");
        props.put("vertical-align", "top");
        props.put("display", "block");
        props.put("margin", "0px");
        props.put("padding", "0px");

        ret.setStyleEntry("p", props = new HashMap<String, String>());
        // props.put("font-family","Geneva, Arial, Helvetica, sans-serif");
        props.put("font-size", "9px");
        props.put("margin", "1px");
        props.put("padding", "0px");

        ret.setStyleEntry("div", props = new HashMap<String, String>());
        props.put("width", "100%");
        props.put("padding", "2px");
        props.put("background-color", "#" + JDTheme.V("gui.color.htmlTooltip_background", "94baff"));

        ret.setText(htmlText);

        ret.setVisible(true);

        ret.pack();
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        if (loc.x + ret.getWidth() > size.width) {
            loc.x = size.width - ret.getWidth();
        } else if (loc.x - ret.getWidth() < 0) {
            loc.x = 0;
        }
        if (loc.y + ret.getHeight() > size.height) {
            loc.y = size.height - ret.getHeight();
        } else if (loc.y - ret.getHeight() < 0) {
            loc.y = 0;
        }
        ret.setLocation(loc);
        return ret;
    }

    private Color BORDER_COLOR;
    private JTextPane htmlArea;
    private HashMap<String, HashMap<String, String>> styles = null;

    public HTMLTooltip() {
        setAlwaysOnTop(true);
        BORDER_COLOR = JDTheme.C("gui.color.htmlTooltip_border", "000000");
        htmlArea = new JTextPane();
        htmlArea.addMouseListener(this);
        htmlArea.setEditable(false);
        htmlArea.addHyperlinkListener(this);
        htmlArea.setContentType("text/html");
        htmlArea.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        getContentPane().add(htmlArea, BorderLayout.CENTER);
        pack();
    }

    public void destroy() {
        if (!isVisible()) { return; }
        setVisible(false);
        dispose();

    }

    public String getCSSString() {
        if (styles == null) { return ""; }
        StringBuilder sb = new StringBuilder();
        sb.append("<style>");
        sb.append("\r\n");

        Entry<String, HashMap<String, String>> next;
        Entry<String, String> prop;
        for (Iterator<Entry<String, HashMap<String, String>>> it = styles.entrySet().iterator(); it.hasNext();) {
            next = it.next();
            sb.append(next.getKey() + "{");
            sb.append("\r\n");
            for (Iterator<Entry<String, String>> it2 = next.getValue().entrySet().iterator(); it2.hasNext();) {
                prop = it2.next();
                sb.append(prop.getKey() + ":" + prop.getValue() + ";");
                sb.append("\r\n");
            }
            sb.append("}");
            sb.append("\r\n");

        }
        sb.append("</style>");

        return sb.toString();
    }

    public HashMap<String, HashMap<String, String>> getStyles() {
        return styles;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                JLinkButton.openURL(e.getURL());

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        destroy();
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void setStyleEntry(String key, HashMap<String, String> value) {
        if (styles == null) {
            styles = new HashMap<String, HashMap<String, String>>();
        }
        styles.put(key, value);
    }

    public void setText(String text) {
        String t = getCSSString() + text;

        htmlArea.setText(t);

        htmlArea.invalidate();
        pack();
    }

}