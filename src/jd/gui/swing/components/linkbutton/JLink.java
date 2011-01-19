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

package jd.gui.swing.components.linkbutton;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.TableCellRenderer;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.swing.components.JDUnderlinedText;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.nutils.Executer;
import jd.nutils.nativeintegration.LocalBrowser;

import org.appwork.utils.AwReg;
import org.appwork.utils.event.DefaultEvent;
import org.appwork.utils.event.Eventsender;

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
            exec.setLogger(JDLogger.getLogger());
            String params = cfg.getStringProperty(JDGuiConstants.PARAM_CUSTOM_BROWSER_PARAM).replace("%url", url + "");
            exec.addParameters(AwReg.getLines(params));
            exec.start();
            exec.setWaitTimeout(1);
            exec.waitTimeout();
            if (exec.getException() != null) throw exec.getException();
        } else {
            String browser = SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER).getStringProperty(JDGuiConstants.PARAM_BROWSER, null);
            LocalBrowser.openURL(browser, url);
        }
    }

    private URL url;
    private JDUnderlinedText mouseListener;
    private transient Eventsender<ActionListener, DefaultEvent> broadcaster;

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

    public JLink(String text, String urlstr) {
        super(text);
        URL url = null;
        try {
            url = new URL(urlstr);
        } catch (Exception e) {
            // e.printStackTrace();
            this.setEnabled(false);
        }
        init(text, url);
    }

    public JLink(String s, URL url) {
        this(s, null, url);
    }

    public JLink(URL url) {
        this(null, null, url);
    }

    public JLink(String text, Icon icon, URL url) {
        super(text);

        this.setIcon(icon);

        init(text, url);
    }

    private void initBroadcaster() {
        this.broadcaster = new Eventsender<ActionListener, DefaultEvent>() {            
            
            protected void fireEvent(final ActionListener listener, final DefaultEvent event) {
                listener.actionPerformed(new ActionEvent(JLink.this, JLink.CLICKED, getText()));
            }

        };
    }

    public Eventsender<ActionListener, DefaultEvent> getBroadcaster() {
        if (broadcaster == null) initBroadcaster();
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

        addMouseListener(mouseListener = new JDUnderlinedText(this));
        addMouseListener(new JDMouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                try {
                    if (getUrl() != null) JLink.openURL(getUrl());
                    getBroadcaster().fireEvent(null);
                } catch (Exception e1) {
                    JDLogger.exception(e1);
                }
            }

        });

    }

    public void removeMouseListener() {
        this.removeMouseListener(mouseListener);
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
        if (url != null) this.setToolTipText(url.toExternalForm());
    }

    public static HyperlinkListener getHyperlinkListener() {
        return new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        JLink.openURL(e.getURL());
                    } catch (Exception e1) {
                        JDLogger.exception(e1);
                    }
                }
            }
        };
    }

}