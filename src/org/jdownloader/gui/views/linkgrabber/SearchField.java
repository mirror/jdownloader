package org.jdownloader.gui.views.linkgrabber;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;

import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.gui.swing.laf.LAFOptions;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.logging.Log;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.SearchCategory;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTableModelFilter;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;

public class SearchField<PackageType extends AbstractPackageNode<ChildType, PackageType>, ChildType extends AbstractPackageChildrenNode<PackageType>> extends ExtTextField implements PackageControllerTableModelFilter<PackageType, ChildType>, MouseMotionListener, MouseListener {
    /**
     * 
     */
    private static final long                              serialVersionUID = -8079363840549073686L;
    private static final int                               SIZE             = 20;
    private Image                                          img;
    private DelayedRunnable                                delayedFilter;
    private PackageControllerTable<PackageType, ChildType> table2Filter;
    private Pattern                                        filterPattern    = null;
    private JLabel                                         label;
    private int                                            labelWidth;
    private Color                                          bgColor;
    private SearchCategory[]                               searchCategories;
    private Image                                          popIcon;

    public SearchField(PackageControllerTable<PackageType, ChildType> table2Filter) {
        super();
        this.table2Filter = table2Filter;
        img = NewTheme.I().getImage("search", SIZE);

        setHelpText(_GUI._.SearchField_SearchField_helptext());
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        popIcon = NewTheme.I().getImage("exttable/sortAsc", -1);
        delayedFilter = new DelayedRunnable(IOEQ.TIMINGQUEUE, 150l, 2000l) {

            @Override
            public void delayedrun() {
                updateFilter();
            }

        };

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

            g2.fillRect(0, 0, labelWidth + 5 + 24 + 5, getHeight());
            g2.setColor(getBackground().darker());
            g2.drawLine(labelWidth + 5 + 24 + 5, 1, labelWidth + 24 + 5 + 5, getHeight() - 1);
            g2.setComposite(comp);
        }
        g2.drawImage(img, 3, 3, 3 + SIZE, 3 + SIZE, 0, 0, SIZE, SIZE, null);
        if (label != null) {
            g2.translate(24, 0);

            label.getUI().paint(g2, label);
            g2.drawImage(popIcon, labelWidth + 3, (getHeight() - popIcon.getHeight(null)) / 2, null);
            // label.paintComponents(g2);
            g2.translate(-24, 0);
        }

    }

    private synchronized void updateFilter() {
        String filterRegex = this.getText();
        boolean enabled = filterRegex.length() > 0;
        if (enabled) {
            try {
                filterPattern = LinkgrabberFilterRuleWrapper.createPattern(filterRegex, (JsonConfig.create(GeneralSettings.class).isFilterRegex()));
                table2Filter.getPackageControllerTableModel().addFilter(this);
            } catch (final Throwable e) {
                Log.exception(e);
            }
        } else {
            table2Filter.getPackageControllerTableModel().removeFilter(this);
        }
        table2Filter.getPackageControllerTableModel().recreateModel(true);
    }

    public boolean isFiltered(PackageType e) {
        return false;
    }

    public boolean isFiltered(ChildType v) {
        return !filterPattern.matcher(v.getName()).find();
    }

    public void reset() {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        updateCursor(e);
    }

    private void updateCursor(MouseEvent e) {
        if (label != null && e.getX() < labelWidth + 5 + 24) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            setCaretColor(getBackground());
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            setCaretColor(null);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (label != null && e.getX() < labelWidth + 5 + 24) {
            onCategoryPopup();
        }
    }

    private void onCategoryPopup() {

        JPopupMenu popup = new JPopupMenu();

        for (final SearchCategory sc : searchCategories) {
            if (sc == selectedCategory) continue;
            popup.add(new AppAction() {
                private SearchCategory category;
                {
                    category = sc;
                    setName(sc.getLabel());
                    setSmallIcon(sc.getIcon());
                }

                public void actionPerformed(ActionEvent e) {
                    setSelectedCategory(category);
                    repaint();
                }
            });
        }
        int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(pref);

        popup.show(this, -insets[1] - 1 + 24 - 10, -popup.getPreferredSize().height + insets[2]);
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

    private SearchCategory selectedCategory = SearchCategory.FILENAME;

    public SearchCategory getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(SearchCategory selectedCategory) {
        if (selectedCategory == null) {
            Log.exception(Level.WARNING, new NullPointerException("selectedCategory null"));
        }
        this.selectedCategory = selectedCategory;
        if (label != null) {
            label.setText(selectedCategory.getLabel());
            repaint();
        }
    }

    public void setCategories(SearchCategory[] searchCategories) {
        this.searchCategories = searchCategories;
        label = new JLabel() {
            public boolean isShowing() {

                return true;
            }

            public boolean isVisible() {
                return true;
            }
        };
        LAFOptions lafo = LookAndFeelController.getInstance().getLAFOptions();
        bgColor = new Color(lafo.getPanelHeaderColor());

        for (SearchCategory sc : searchCategories) {
            label.setText(sc.getLabel());
            labelWidth = Math.max(label.getPreferredSize().width, labelWidth);
        }

        label.setText(selectedCategory.getLabel());

        label.setSize(labelWidth, 24);
        // label.setEnabled(false);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder(0, labelWidth + 35, 0, 0)));

    }
}
