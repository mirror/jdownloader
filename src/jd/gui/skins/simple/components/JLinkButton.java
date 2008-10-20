//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalButtonUI;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

class BasicLinkButtonUI extends MetalButtonUI {
    private static final BasicLinkButtonUI ui = new BasicLinkButtonUI();

    public static ComponentUI createUI(JComponent jcomponent) {
        return ui;
    }

    public BasicLinkButtonUI() {
    }

    @Override
    protected void paintText(Graphics g, JComponent com, Rectangle rect, String s) {
        JLinkButton bn = (JLinkButton) com;
        ButtonModel bnModel = bn.getModel();
        // Color color = bn.getForeground();
        // Object obj = null;
        if (bnModel.isEnabled()) {
            if (bnModel.isPressed()) {
                bn.setForeground(bn.getActiveLinkColor());
            } else if (bn.isLinkVisited()) {
                bn.setForeground(bn.getVisitedLinkColor());
            } else {
                bn.setForeground(bn.getLinkColor());
            }
        } else {
            if (bn.getDisabledLinkColor() != null) {
                bn.setForeground(bn.getDisabledLinkColor());
            }
        }
        super.paintText(g, com, rect, s);
        int behaviour = bn.getLinkBehavior();
        boolean drawLine = false;
        if (behaviour == JLinkButton.HOVER_UNDERLINE) {
            if (bnModel.isRollover()) {
                drawLine = true;
            }
        } else if (behaviour == JLinkButton.ALWAYS_UNDERLINE || behaviour == JLinkButton.SYSTEM_DEFAULT) {
            drawLine = true;
        }
        if (!drawLine) { return; }
        FontMetrics fm = g.getFontMetrics();
        int x = rect.x + getTextShiftOffset();
        int y = rect.y + fm.getAscent() + fm.getDescent() + getTextShiftOffset() - 1;
        if (bnModel.isEnabled()) {
            g.setColor(bn.getForeground());
            g.drawLine(x, y, x + rect.width - 1, y);
        } else {
            g.setColor(bn.getBackground().brighter());
            g.drawLine(x, y, x + rect.width - 1, y);
        }
    }
}

class JLinkButtonEditor implements TableCellEditor, ActionListener {

    private boolean stop = false;

    public void actionPerformed(ActionEvent e) {
        stop = true;
    }

    public void addCellEditorListener(CellEditorListener l) {
    }

    public void cancelCellEditing() {
    }

    public Object getCellEditorValue() {
        return null;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        stop = false;
        JLinkButton btn = (JLinkButton) value;
        btn.addActionListener(this);
        return btn;
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public void removeCellEditorListener(CellEditorListener l) {
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    public boolean stopCellEditing() {
        return stop;
    }

}

class JLinkButtonRenderer implements TableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return (JLinkButton) value;
    }
}

public class JLinkButton extends JButton {

    private static final long serialVersionUID = 1L;

    public static final int ALWAYS_UNDERLINE = 0;

    public static final int HOVER_UNDERLINE = 1;

    public static final int NEVER_UNDERLINE = 2;

    public static final int SYSTEM_DEFAULT = 3;

    public static JLinkButtonEditor getJLinkButtonEditor() {
        return new JLinkButtonEditor();
    }

    public static JLinkButtonRenderer getJLinkButtonRenderer() {
        return new JLinkButtonRenderer();
    }

    public static void openURL(String url) throws MalformedURLException {
        JLinkButton.openURL(new URL(url));
    }

    public static void openURL(URL url) {

        if (url != null) {
            String Browser = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty(SimpleGUI.PARAM_BROWSER, null);
            if (Browser == null) {
                BrowserLauncher launcher;
                List<?> ar = null;
                try {
                    launcher = new BrowserLauncher();
                    ar = launcher.getBrowserList();

                } catch (BrowserLaunchingInitializingException e1) {

                    e1.printStackTrace();
                } catch (UnsupportedOperatingSystemException e1) {

                    e1.printStackTrace();
                }

                Object[] BrowserArray = (Object[]) JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getProperty(SimpleGUI.PARAM_BROWSER_VARS, null);

                if (BrowserArray == null) {
                    if (ar.size() < 2) {
                        BrowserArray = new Object[] { "JavaBrowser" };
                    } else {
                        BrowserArray = new Object[ar.size() + 1];
                        for (int i = 0; i < BrowserArray.length - 1; i++) {
                            BrowserArray[i] = ar.get(i);
                        }
                        BrowserArray[BrowserArray.length - 1] = "JavaBrowser";
                    }
                    JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).setProperty(SimpleGUI.PARAM_BROWSER_VARS, BrowserArray);
                    JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).setProperty(SimpleGUI.PARAM_BROWSER, BrowserArray[0]);
                    Browser = (String) BrowserArray[0];
                    JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).save();
                }
            }
            if (Browser.equals("JavaBrowser")) {
                DnDWebBrowser browser = new DnDWebBrowser(((SimpleGUI) JDUtilities.getGUI()).getFrame());
                browser.goTo(url);
                browser.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                browser.setSize(800, 600);
                browser.setVisible(true);
            } else {
                try {
                    BrowserLauncher launcher = new BrowserLauncher();
                    launcher.openURLinBrowser(Browser, url.toString());
                } catch (BrowserLaunchingInitializingException e1) {

                    e1.printStackTrace();
                } catch (UnsupportedOperatingSystemException e1) {

                    e1.printStackTrace();
                }

            }

        }
    }

    private URL buttonURL;

    private Color colorPressed;

    private Action defaultAction;

    private Color disabledLinkColor;

    private boolean isLinkVisited;

    private int linkBehavior;

    private Color linkColor;

    private Color visitedLinkColor;

    public JLinkButton() {
        this(null, null, null);
    }

    public JLinkButton(Action action) {
        this();
        setAction(action);
    }

    public JLinkButton(Icon icon) {
        this(null, icon, null);
    }

    public JLinkButton(Icon icon, URL url) {
        this(null, icon, url);
    }

    public JLinkButton(String s) {
        this(s, null, null);
    }

    public JLinkButton(final String text, Icon icon, URL url) {
        super(text, icon);
        linkBehavior = SYSTEM_DEFAULT;
        linkColor = Color.blue;
        colorPressed = Color.red;
        visitedLinkColor = new Color(128, 0, 128);
        if (url == null && text != null) {
            if (text.matches("https?://.*")) {
                try {
                    url = new URL(text);
                } catch (MalformedURLException e1) {

                    e1.printStackTrace();
                }
            } else if (text.matches("www\\..*?\\..*")) {
                try {
                    url = new URL("http://" + text);
                } catch (MalformedURLException e1) {

                    e1.printStackTrace();
                }
            }
        }
        if (text == null && url != null) {
            setText(url.toExternalForm());
        }
        setLinkURL(url);
        setCursor(Cursor.getPredefinedCursor(12));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JLinkButton.openURL(getLinkURL());
            }
        });

        addActionListener(defaultAction);
    }

    public JLinkButton(String text, String urlstr) {
        super(text, null);
        URL url = null;
        try {
            url = new URL("http://www.malformed.com");
        } catch (MalformedURLException e3) {
        }
        try {
            url = new URL(urlstr);
        } catch (MalformedURLException e2) {
        }

        linkBehavior = SYSTEM_DEFAULT;
        linkColor = Color.blue;
        colorPressed = Color.red;
        visitedLinkColor = new Color(128, 0, 128);
        if (url == null && text != null) {
            if (text.matches("https?://.*")) {
                try {
                    url = new URL(text);
                } catch (MalformedURLException e1) {

                    e1.printStackTrace();
                }
            } else if (text.matches("www\\..*?\\..*")) {
                try {
                    url = new URL("http://" + text);
                } catch (MalformedURLException e1) {

                    e1.printStackTrace();
                }
            }
        }
        if (text == null && url != null) {
            setText(url.toExternalForm());
        }
        setLinkURL(url);
        setCursor(Cursor.getPredefinedCursor(12));
        setBorderPainted(false);
        setContentAreaFilled(false);
        setRolloverEnabled(true);

        addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JLinkButton.openURL(getLinkURL());
            }
        });

        addActionListener(defaultAction);

    }

    public JLinkButton(String s, URL url) {
        this(s, null, url);
    }

    public JLinkButton(URL url) {
        this(null, null, url);
    }

    private void checkLinkBehaviour(int beha) {
        if (beha != ALWAYS_UNDERLINE && beha != HOVER_UNDERLINE && beha != NEVER_UNDERLINE && beha != SYSTEM_DEFAULT) {
            throw new IllegalArgumentException("Not a legal LinkBehavior");
        } else {
            return;
        }
    }

    public Color getActiveLinkColor() {
        return colorPressed;
    }

    public Action getDefaultAction() {
        return defaultAction;
    }

    public Color getDisabledLinkColor() {
        return disabledLinkColor;
    }

    public int getLinkBehavior() {
        return linkBehavior;
    }

    public Color getLinkColor() {
        return linkColor;
    }

    public URL getLinkURL() {
        return buttonURL;
    }

    /*
     * private void setDefault() { UIManager.getDefaults().put("LinkButtonUI",
     * "BasicLinkButtonUI"); }
     */
    @Override
    public String getUIClassID() {
        return "LinkButtonUI";
    }

    public Color getVisitedLinkColor() {
        return visitedLinkColor;
    }
    public static HyperlinkListener getHyperlinkListener()
    {
        return new HyperlinkListener(){

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openURL(e.getURL());
                }
                
            }};
    }
    public boolean isLinkVisited() {
        return isLinkVisited;
    }

    @Override
    protected String paramString() {
        String str;
        if (linkBehavior == ALWAYS_UNDERLINE) {
            str = "ALWAYS_UNDERLINE";
        } else if (linkBehavior == HOVER_UNDERLINE) {
            str = "HOVER_UNDERLINE";
        } else if (linkBehavior == NEVER_UNDERLINE) {
            str = "NEVER_UNDERLINE";
        } else {
            str = "SYSTEM_DEFAULT";
        }
        String colorStr = linkColor == null ? "" : linkColor.toString();
        String colorPressStr = colorPressed == null ? "" : colorPressed.toString();
        String disabledLinkColorStr = disabledLinkColor == null ? "" : disabledLinkColor.toString();
        String visitedLinkColorStr = visitedLinkColor == null ? "" : visitedLinkColor.toString();
        String buttonURLStr = buttonURL == null ? "" : buttonURL.toString();
        String isLinkVisitedStr = isLinkVisited ? "true" : "false";
        return super.paramString() + ",linkBehavior=" + str + ",linkURL=" + buttonURLStr + ",linkColor=" + colorStr + ",activeLinkColor=" + colorPressStr + ",disabledLinkColor=" + disabledLinkColorStr + ",visitedLinkColor=" + visitedLinkColorStr + ",linkvisitedString=" + isLinkVisitedStr;
    }

    public void setActiveLinkColor(Color colorNew) {
        Color colorOld = colorPressed;
        colorPressed = colorNew;
        firePropertyChange("activeLinkColor", colorOld, colorNew);
        repaint();
    }

    public void setDefaultAction(Action actionNew) {
        Action actionOld = defaultAction;
        defaultAction = actionNew;
        firePropertyChange("defaultAction", actionOld, actionNew);
    }

    public void setDisabledLinkColor(Color color) {
        Color colorOld = disabledLinkColor;
        disabledLinkColor = color;
        firePropertyChange("disabledLinkColor", colorOld, color);
        if (!isEnabled()) {
            repaint();
        }
    }

    public void setLinkBehavior(int bnew) {
        checkLinkBehaviour(bnew);
        int old = linkBehavior;
        linkBehavior = bnew;
        firePropertyChange("linkBehavior", old, bnew);
        repaint();
    }

    public void setLinkColor(Color color) {
        Color colorOld = linkColor;
        linkColor = color;
        firePropertyChange("linkColor", colorOld, color);
        repaint();
    }

    public void setLinkURL(URL url) {
        URL urlOld = buttonURL;
        buttonURL = url;
        setupToolTipText();
        firePropertyChange("linkURL", urlOld, url);
        revalidate();
        repaint();
    }

    public void setLinkVisited(boolean flagNew) {
        boolean flagOld = isLinkVisited;
        isLinkVisited = flagNew;
        firePropertyChange("linkVisited", flagOld, flagNew);
        repaint();
    }

    protected void setupToolTipText() {
        String tip = null;
        if (buttonURL != null) {
            tip = buttonURL.toExternalForm();
        }
        setToolTipText(tip);
    }

    public void setVisitedLinkColor(Color colorNew) {
        Color colorOld = visitedLinkColor;
        visitedLinkColor = colorNew;
        firePropertyChange("visitedLinkColor", colorOld, colorNew);
        repaint();
    }

    @Override
    public void updateUI() {
        setUI(BasicLinkButtonUI.createUI(this));
    }
}