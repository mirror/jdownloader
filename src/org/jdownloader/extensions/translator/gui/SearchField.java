package org.jdownloader.extensions.translator.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.border.Border;

import jd.controlling.IOEQ;
import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.logging.Log;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.LinktablesSearchCategory;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class SearchField extends ExtTextField implements MouseMotionListener, MouseListener {
    /**
     * 
     */

    private static final int SIZE           = 20;
    private Image            img;
    private DelayedRunnable  delayedFilter;

    protected List<Pattern>  filterPatterns = null;
    private JLabel           label;
    private int              labelWidth;
    private Color            bgColor;
    private LinktablesSearchCategory[] searchCategories;
    private Image            popIcon;
    private int              iconGap        = 38;
    private Border           orgBorder;
    private TranslateTable   table2Filter;

    public SearchField(TranslateTable table) {
        super();

        this.table2Filter = table;
        img = NewTheme.I().getImage("search", SIZE);
        LAFOptions lafo = LookAndFeelController.getInstance().getLAFOptions();
        bgColor = new Color(lafo.getPanelHeaderColor());
        setHelpText(_GUI._.SearchField_SearchField_helptext());
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
        if (label != null) {

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

            g2.setColor(bgColor);

            g2.fillRect(1, 1, labelWidth + 5 + iconGap + 8 - 1, getHeight() - 1);
            g2.setColor(getBackground().darker());
            g2.drawLine(labelWidth + 5 + iconGap + 8, 1, labelWidth + iconGap + 5 + 8, getHeight() - 1);
            g2.setComposite(comp);

            g2.drawImage(selectedCategory.getIcon().getImage(), iconGap - 24, 3, null);

            g2.translate(iconGap + 1, 0);
            label.getUI().paint(g2, label);
            g2.drawImage(popIcon, labelWidth + 3, (getHeight() - popIcon.getHeight(null)) / 2, null);
            // label.paintComponents(g2);
            g2.translate(-iconGap - 1, 0);
        } else {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.setColor(bgColor);
            g2.fillRect(0, 0, 26, getHeight());
            g2.setColor(getBackground().darker());
            g2.drawLine(26, 1, 26, getHeight() - 1);
            g2.setComposite(comp);
            g2.drawImage(img, 3, 3, 3 + SIZE, 3 + SIZE, 0, 0, SIZE, SIZE, null);
        }

        // g2.dispose();

    }

    private synchronized void updateFilter() {

        String filterRegex = this.getText();
        boolean enabled = filterRegex.length() > 0;

        if (enabled) {

            ArrayList<Pattern> list = new ArrayList<Pattern>();
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

                table2Filter.updaterFilter(this);

            } catch (final Throwable e) {
                Log.exception(e);
            }
        } else {
            filterPatterns = null;
            table2Filter.updaterFilter(this);
        }
        table2Filter.updaterFilter(this);

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
        if (label != null && e.getX() < labelWidth + 5 + iconGap + 8) {
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
        if (label != null && e.getX() < labelWidth + 5 + iconGap + 8) {
            onCategoryPopup();
        }
    }

    private void onCategoryPopup() {

        JPopupMenu popup = new JPopupMenu();

        for (final LinktablesSearchCategory sc : searchCategories) {
            if (sc == selectedCategory) continue;
            popup.add(new AppAction() {
                private LinktablesSearchCategory category;
                {
                    category = sc;
                    setName(sc.getLabel());
                    setSmallIcon(sc.getIcon());
                }

                public void actionPerformed(ActionEvent e) {
                    setSelectedCategory(category);
                    focusLost(null);
                }
            });
        }
        int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(new Dimension(labelWidth + 5 + iconGap + 8 + insets[1] + insets[1] + insets[3], (int) pref.getHeight()));

        popup.show(this, -insets[1], -popup.getPreferredSize().height + insets[2]);
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

    protected LinktablesSearchCategory selectedCategory = LinktablesSearchCategory.FILENAME;

    public LinktablesSearchCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(LinktablesSearchCategory selectedCategory) {
        if (selectedCategory == null) {
            Log.exception(Level.WARNING, new NullPointerException("selectedCategory null"));
        }
        if (this.selectedCategory != selectedCategory) onChanged();
        this.selectedCategory = selectedCategory;
        if (label != null) {
            label.setText(selectedCategory.getLabel());
            setHelpText(selectedCategory.getHelpText());
        }
    }

    public void setCategories(LinktablesSearchCategory[] searchCategories) {
        this.searchCategories = searchCategories;
        label = new JLabel() {
            public boolean isShowing() {

                return true;
            }

            public boolean isVisible() {
                return true;
            }
        };

        for (LinktablesSearchCategory sc : searchCategories) {
            label.setText(sc.getLabel());
            labelWidth = Math.max(label.getPreferredSize().width, labelWidth);
        }

        label.setText(selectedCategory.getLabel());

        label.setSize(labelWidth, 24);
        // label.setEnabled(false);
        setBorder(BorderFactory.createCompoundBorder(orgBorder, BorderFactory.createEmptyBorder(0, labelWidth + 14 + iconGap, 0, 0)));

    }

    public boolean highlightFilter() {
        return true;
    }

    public void setTable(TranslateTable table) {
        this.table2Filter = table;
        table2Filter.updaterFilter(this);
    }
}