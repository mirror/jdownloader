package org.jdownloader.gui.settings;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.swing.components.ExtCheckBox;
import org.appwork.utils.ColorUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.Header;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractConfigPanel extends SwitchPanel {
    public static class HighlightLabel extends JLabel {
        {
            LAFOptions.getInstance().applyConfigLabelEnabledTextColor(this);
        }

        public HighlightLabel(String text) {
            super(text);
        }

        @Override
        public void setEnabled(boolean paramBoolean) {
            if (paramBoolean) {
                if (!LAFOptions.getInstance().applyConfigLabelEnabledTextColor(this)) {
                    super.setEnabled(true);
                } else {
                    super.setEnabled(true);
                }
            } else {
                if (!LAFOptions.getInstance().applyConfigLabelDisabledTextColor(this)) {
                    super.setEnabled(false);
                } else {
                    super.setEnabled(true);
                }
            }
        }

        public void setHighlight(boolean b) {
            repaint();
        }
    }

    private static final String       PAIR_CONDITION   = "PAIR_CONDITION";
    private static final long         serialVersionUID = -8483438886830392777L;
    protected java.util.List<Pair<?>> pairs;
    private HighlightLabel            lastLabel;
    protected Pair                    over;
    private MouseMotionListener       ml;

    public AbstractConfigPanel() {
        this(15);
    }

    private String getFieldNameOf(Object input) {
        if (input instanceof Component) {
            if (((Component) input).getName() != null) {
                return ((Component) input).getName();
            }
        }
        for (Field f : getClass().getDeclaredFields()) {
            try {
                if (f.getType() == input.getClass()) {
                    f.setAccessible(true);
                    if (f.get(this) == input) {
                        return f.getName();
                    }
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    public String getPanelID() {
        return getClass().getSimpleName();
    }

    public AbstractConfigPanel(int insets) {
        super(new MigLayout("ins " + insets + ", wrap 2", "[grow][grow,fill]", "[]"));
        pairs = new ArrayList<Pair<?>>();
        setOpaque(false);
        // mouse listener to paint the highlighting
        addMouseMotionListener(ml = new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean found = false;
                Point point = e.getPoint();
                point = SwingUtilities.convertPoint(e.getComponent(), point, AbstractConfigPanel.this);
                for (Pair p : pairs) {
                    if (!(p.getComponent() instanceof Component)) {
                        continue;
                    }
                    if (point.y >= p.getYMin() - 5 && point.y < p.getYMax()) {
                        found = true;
                        if (over != p) {
                            Pair last = over;
                            over = p;
                            {
                                int yMin = p.getYMin();
                                if (last != null) {
                                    yMin = Math.min(yMin, last.getYMin());
                                }
                                int yMax = p.getYMax();
                                if (last != null) {
                                    yMax = Math.max(yMax, last.getYMax());
                                }
                                repaint(0, yMin - 10, getWidth(), yMax - yMin + 20);
                            }
                        }
                        break;
                    }
                }
                if (!found) {
                    if (over != null) {
                        Pair last = over;
                        over = null;
                        repaint(0, last.getYMin(), getWidth(), last.getHeight());
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Pair p = over;
        if (p != null && !p.getLabel().getText().equals("<html></html>")) {
            Graphics2D g2 = (Graphics2D) ((Graphics2D) g).create(0, p.getYMin(), getWidth(), p.getHeight());
            g2.setClip(null);
            g2.setColor(ColorUtils.getAlphaInstance(getForeground(), 15));
            g2.fillRect(p.getLabel().getX() - 5, 0, getWidth(), p.getHeight());
        }
        super.paintComponent(g);
    }

    public JLabel addDescription(String description) {
        JLabel txt = addDescriptionPlain(description);
        add(new JSeparator(), "gapleft " + getLeftGap() + ",spanx,growx,pushx,gapbottom 5" + getRightGap());
        return txt;
    }

    public JLabel addDescriptionPlain(String description) {
        if (!description.toLowerCase().startsWith("<html>")) {
            description = "<html>" + description.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "</html>";
        }
        JLabel txt = new JLabel();
        SwingUtils.setOpaque(txt, false);
        // txt.setEnabled(false);
        LAFOptions.getInstance().applyConfigDescriptionTextColor(txt);
        txt.setText(description);
        add(txt, "gaptop 0,spanx,growx,pushx,gapleft " + getLeftGap() + ",gapbottom 5,wmin 10" + getRightGap());
        return txt;
    }

    public void addTopHeader(String name, Icon icon) {
        add(new Header(name, icon), "spanx,growx,pushx" + getRightGap());
    }

    protected void showRestartRequiredMessage() {
        try {
            Dialog.getInstance().showConfirmDialog(0, _JDT.T.dialog_optional_showRestartRequiredMessage_title(), _JDT.T.dialog_optional_showRestartRequiredMessage_msg(), null, _JDT.T.basics_yes(), _JDT.T.basics_no());
            RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
        } catch (DialogClosedException e) {
        } catch (DialogCanceledException e) {
        }
    }

    @Deprecated
    protected Header addHeader(String name, String iconKey) {
        return this.addHeader(name, NewTheme.I().getIcon(iconKey, 32));
    }

    public abstract Icon getIcon();

    public abstract String getTitle();

    public <T extends SettingsComponent> Pair<T> addPair(String name, BooleanKeyHandler enabled, T comp) {
        String lblConstraints = "gapleft " + getLeftGap() + ",aligny " + (comp.isMultiline() ? "top" : "center");
        return addPair(name, lblConstraints, enabled, comp);
    }

    public <T extends SettingsComponent> Pair<T> addPair(String name, String lblConstraints, BooleanKeyHandler enabled, T comp) {
        final HighlightLabel lbl = createLabel(name);
        ExtCheckBox cb = null;
        String con = "pushx,growy";
        lastLabel = lbl;
        if (lbl != null) {
            lbl.addMouseMotionListener(ml);
        }
        if (enabled != null) {
            add(lbl, lblConstraints + ",split 2,growx");
            cb = new ExtCheckBox(enabled, lbl, (JComponent) comp);
            SwingUtils.setOpaque(cb, false);
            add(cb, "width " + cb.getPreferredSize().width + "!, aligny " + (comp.isMultiline() ? "top" : "center"));
            cb.setToolTipText(_GUI.T.AbstractConfigPanel_addPair_enabled());
            cb.addMouseMotionListener(ml);
        } else {
            add(lbl, lblConstraints);
        }
        if (comp instanceof Component) {
            ((Component) comp).addMouseMotionListener(ml);
        }
        if (comp.getConstraints() != null) {
            con += "," + comp.getConstraints();
        }
        con += getRightGap();
        add((JComponent) comp, con);
        final Pair<T> p = new Pair<T>(lbl, comp, cb);
        pairs.add(p);
        return p;
    }

    @Override
    protected void onShow() {
        updateContents();
        for (Pair p : pairs) {
            p.update();
        }
    }

    public abstract void save();

    public abstract void updateContents();

    @Override
    protected void onHide() {
        save();
    }

    public HighlightLabel createLabel(String text) {
        if (text != null && !text.toLowerCase().startsWith("<html>")) {
            text = "<html>" + text.replace("\r\n", "<br>").replace("\r", "<br>").replace("\n", "<br>") + "</html>";
        }
        HighlightLabel lbl = new HighlightLabel(text);
        return lbl;
    }

    public Component add(Component comp) {
        if (comp instanceof SettingsComponent) {
            String con = "gapleft" + getLeftGap() + ",spanx,growx,pushx";
            if (((SettingsComponent) comp).getConstraints() != null) {
                con += "," + ((SettingsComponent) comp).getConstraints();
            }
            super.add(comp, con);
            return comp;
        } else if (comp instanceof JScrollPane) {
            super.add(comp, "gapleft" + getLeftGap() + ",spanx,growx,pushx,height 60:n:n,pushy,growy");
            return comp;
        } else {
            super.add(comp, "growx, pushx,spanx");
            return comp;
        }
    }

    public String getRightGap() {
        return "";
    }

    public String getLeftGap() {
        return LAFOptions.getInstance().getCfg().getConfigPanelLeftIndent() + "";
    }

    protected Header addHeader(String name, Icon icon) {
        Header header;
        if (getComponentCount() == 0) {
            // first header
            add(header = new Header(name, icon), "spanx,growx,pushx");
        } else {
            add(header = new Header(name, icon), "spanx,newline,growx,pushx");
        }
        return header;
    }
}
