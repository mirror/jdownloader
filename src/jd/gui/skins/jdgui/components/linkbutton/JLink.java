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

package jd.gui.skins.jdgui.components.linkbutton;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.event.JDBroadcaster;
import jd.event.JDEvent;
import jd.gui.skins.jdgui.JDGuiConstants;
import jd.gui.skins.simple.JDMouseAdapter;
import jd.nutils.Executer;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;

class JLinkButtonRenderer implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        return (Component) value;
    }
}

public class JLink extends JLabel {

    private static final long serialVersionUID = 1L;
    public static final int CLICKED = 0;

    public static JLinkButtonEditor getJLinkButtonEditor() {
        return new JLinkButtonEditor();
    }

    public static JLinkButtonRenderer getJLinkButtonRenderer() {
        return new JLinkButtonRenderer();
    }

    public static void openURL(String url) throws Exception {
        if (url == null) return;
        JLink.openURL(new URL(url));
    }

    public static void openURL(URL url) throws Exception {
        if (url == null) return;
        SubConfiguration cfg = SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER);
        if (cfg.getBooleanProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER_USE, false)) {
            Executer exec = new Executer(cfg.getStringProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER));
            String params = cfg.getStringProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER_PARAM).replace("%url", url + "");
            exec.addParameters(Regex.getLines(params));
            exec.start();
            exec.setWaitTimeout(1);
            exec.waitTimeout();
            if (exec.getException() != null) { throw exec.getException(); }
        } else {
            String browser = SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER).getStringProperty(JDGuiConstants.PARAM_BROWSER, null);
            LocalBrowser.openURL(browser, url);
        }
    }

    private URL url;
    private JDBroadcaster<ActionListener, JDEvent> broadcaster;

    public JLink() {
        this(null, null, null);
    }

    public JLink(Icon icon) {
        this(null, icon, null);
    }

    public JLink(Icon icon, URL url) {
        this(null, icon, url);
    }

    public JLink(String s) {
        this(s, null, null);
    }

    public JLink(final String text, Icon icon, URL url) {
        super(text);
   
        this.setIcon(icon);

        init(text, url);

     
    }

    private void initBroadcaster() {
        this.broadcaster = new JDBroadcaster<ActionListener, JDEvent>() {

            @Override
            protected void fireEvent(ActionListener listener, JDEvent event) {
                listener.actionPerformed(new ActionEvent(JLink.this, JLink.CLICKED, getText()));

            }

        };
    }

    public JDBroadcaster<ActionListener, JDEvent> getBroadcaster() {
        if(broadcaster==null)     initBroadcaster();
        return broadcaster;
    }

    private void init(String text, URL url) {
        if (url == null && text != null) {
            if (text.matches("https?://.*")) {
                try {
                    url = new URL(text);
                } catch (MalformedURLException e1) {

                    JDLogger.exception(e1);
                }
            } else if (text.matches("www\\..*?\\..*")) {
                try {
                    url = new URL("http://" + text);
                } catch (MalformedURLException e1) {

                    JDLogger.exception(e1);
                }
            }
        }
        if (text == null && url != null) {
            setText(url.toExternalForm());
        }
        setUrl(url);
        this.setBackground(null);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new JDMouseAdapter() {

            private Font originalFont;

            @SuppressWarnings("unchecked")
            @Override
            public void mouseEntered(MouseEvent evt) {
                originalFont = getFont();
                if (isEnabled()) {
                    Map attributes = originalFont.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);

                    setFont(originalFont.deriveFont(attributes));
                }
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                setFont(originalFont);
            }

            public void mouseClicked(MouseEvent e) {
                try {
                    if (getUrl() != null) JLink.openURL(getUrl());
                    broadcaster.fireEvent(null);
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                }
            }

        });

    }

    public JLink(String text, String urlstr) {
        super(text);
        URL url = null;
        try {
            url = new URL(urlstr);
        } catch (Exception e) {
//            e.printStackTrace();
            this.setEnabled(false);
        }
        ;
        init(text, url);
    }

    public JLink(String s, URL url) {
        this(s, null, url);
    }

    public JLink(URL url) {
        this(null, null, url);
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
        if(url!=null){
        this.setToolTipText(url.toExternalForm());
        }
    }

}