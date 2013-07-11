package jd.gui.swing.jdgui.views.settings.panels.pluginsettings;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JRootPane;
import javax.swing.border.Border;

import jd.controlling.IOEQ;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.logging.Log;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class SearchField extends ExtTextField implements MouseMotionListener, MouseListener {
    /**
     * 
     */

    private static final int SIZE           = 22;
    private Image            img;
    private DelayedRunnable  delayedFilter;

    protected List<Pattern>  filterPatterns = null;

    private int              labelWidth;
    private Color            bgColor;

    private Image            popIcon;
    private int              iconGap        = 38;
    private Border           orgBorder;

    public SearchField() {
        super();

        img = NewTheme.I().getImage("search", SIZE);
        LAFOptions lafo = LAFOptions.getInstance();
        bgColor = (lafo.getColorForPanelHeaderBackground());
        setHelpText(_GUI._.pluginsettings_search_helptext());
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        popIcon = NewTheme.I().getImage("popupButton", -1);
        delayedFilter = new DelayedRunnable(IOEQ.TIMINGQUEUE, 150l, 2000l) {

            @Override
            public void delayedrun() {
                updateFilter();
            }

        };
        orgBorder = getBorder();
        setBorder(BorderFactory.createCompoundBorder(orgBorder, BorderFactory.createEmptyBorder(0, 28, 0, 0)));
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public Image getPopIcon() {
        return popIcon;
    }

    public void setPopIcon(Image popIcon) {
        this.popIcon = popIcon;
    }

    @Override
    public void onChanged() {
        delayedFilter.run();
    }

    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Composite comp = g2.getComposite();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(bgColor);
        g2.fillRect(0, 0, 26, getHeight());
        g2.setColor(getBackground().darker());
        g2.drawLine(26, 1, 26, getHeight() - 1);
        g2.setComposite(comp);
        g2.drawImage(img, 3, 3, 3 + SIZE, 3 + SIZE, 0, 0, SIZE, SIZE, null);

        // g2.dispose();

    }

    private synchronized void updateFilter() {

        String filterRegex = this.getText();
        boolean enabled = filterRegex.length() > 0;

        if (enabled) {

            java.util.List<Pattern> list = new ArrayList<Pattern>();
            try {
                if (JsonConfig.create(GeneralSettings.class).isFilterRegex()) {
                    list.add(LinkgrabberFilterRuleWrapper.createPattern(filterRegex, true));
                } else {
                    String[] filters = filterRegex.split("\\|");
                    for (String filter : filters) {
                        list.add(LinkgrabberFilterRuleWrapper.createPattern(filter, false));
                    }

                }
                filterPatterns = list;

                updaterFilter();

            } catch (final Throwable e) {
                Log.exception(e);
            }
        } else {
            filterPatterns = null;
            updaterFilter();
        }
        updaterFilter();

    }

    public List<Pattern> getFilterPatterns() {
        return filterPatterns;
    }

    protected void updaterFilter() {
    }

    public void reset() {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        updateCursor(e);
    }

    private void updateCursor(MouseEvent e) {
        if (!hasFocus()) return;
        if (e.getX() < labelWidth + 5 + iconGap + 8) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            setCaretColor(getBackground());
            focusLost(null);
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            setCaretColor(null);
            focusGained(null);
        }
    }

    public void focusGained(final FocusEvent arg0) {
        if (arg0 != null && arg0.getOppositeComponent() instanceof JRootPane) return;
        super.focusGained(arg0);
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        updateCursor(e);
    }

    public void mouseExited(MouseEvent e) {
    }

    public boolean highlightFilter() {
        return true;
    }

}