package org.jdownloader.gui.views.components.packagetable;

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

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.filter.LinkgrabberFilterRuleWrapper;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.SearchCatInterface;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.updatev2.gui.LAFOptions;

public class SearchField<SearchCat extends SearchCatInterface, PackageType extends AbstractPackageNode<ChildType, PackageType>, ChildType extends AbstractPackageChildrenNode<PackageType>> extends ExtTextField implements PackageControllerTableModelFilter<PackageType, ChildType>, MouseMotionListener, MouseListener {
    /**
     * 
     */
    private static final long                              serialVersionUID = -8079363840549073686L;
    private static final int                               SIZE             = 20;
    private Image                                          img;
    private DelayedRunnable                                delayedFilter;
    private PackageControllerTable<PackageType, ChildType> table2Filter;
    protected List<Pattern>                                filterPatterns   = null;
    private JLabel                                         label;
    private int                                            labelWidth;
    private Color                                          bgColor;
    private SearchCat[]                                    searchCategories;
    private Image                                          popIcon;
    private int                                            iconGap          = 38;
    private Border                                         orgBorder;
    private Image                                          close;

    private int                                            closeXPos        = -1;
    private boolean                                        mouseoverClose   = false;
    private boolean                                        closeEnabled     = false;

    public SearchField(final PackageControllerTable<PackageType, ChildType> table2Filter, SearchCat defCategory) {
        super();
        this.table2Filter = table2Filter;
        img = NewTheme.I().getImage("search", SIZE);
        close = NewTheme.I().getImage("close", -1);

        LAFOptions lafo = LAFOptions.getInstance();
        bgColor = (lafo.getColorForPanelHeaderBackground());
        setHelpText(_GUI._.SearchField_SearchField_helptext());
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        popIcon = NewTheme.I().getImage("popupButton", -1);
        delayedFilter = new DelayedRunnable(150l, 2000l) {

            @Override
            public String getID() {
                return "SearchField" + table2Filter.getModel().getModelID();
            }

            @Override
            public void delayedrun() {
                System.out.println("update filter");
                updateFilter();
            }

        };
        orgBorder = getBorder();
        setBorder(BorderFactory.createCompoundBorder(orgBorder, BorderFactory.createEmptyBorder(0, 28, 0, 18)));
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
        closeEnabled = StringUtils.isNotEmpty(getText());
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
        if (closeEnabled) {

            closeXPos = getWidth() - close.getWidth(null) - (getHeight() - close.getHeight(null)) / 2;
            g2.drawImage(close, closeXPos, (getHeight() - close.getHeight(null)) / 2, close.getWidth(null), close.getHeight(null), null);

        }
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

                table2Filter.getModel().addFilter(this);

            } catch (final Throwable e) {
                Log.exception(e);
            }
        } else {
            table2Filter.getModel().removeFilter(this);
        }
        table2Filter.getModel().recreateModel(true);

    }

    public boolean isFiltered(PackageType e) {
        return false;
    }

    public boolean isFiltered(ChildType v) {
        return false;
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
            mouseoverClose = false;
        } else if (closeXPos > 0 && e.getX() > closeXPos) {
            mouseoverClose = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setCaretColor(getBackground());
            focusLost(null);
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            setCaretColor(null);
            focusGained(null);
            mouseoverClose = false;
        }
    }

    public void focusGained(final FocusEvent arg0) {
        if (arg0 != null && arg0.getOppositeComponent() instanceof JRootPane) return;
        super.focusGained(arg0);
    }

    public void mouseClicked(MouseEvent e) {
        if (mouseoverClose && closeEnabled) {
            onResetPerformed();
        } else if (label != null && e.getX() < labelWidth + 5 + iconGap + 8) {
            onCategoryPopup();
        }
    }

    private void onResetPerformed() {
        setText(null);
        onChanged();
    }

    private void onCategoryPopup() {

        JPopupMenu popup = new JPopupMenu();

        for (final SearchCat sc : searchCategories) {
            if (sc == selectedCategory) continue;
            popup.add(new AppAction() {
                private SearchCat category;
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
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

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
        mouseoverClose = false;
    }

    protected SearchCat selectedCategory = null;

    public SearchCat getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(SearchCat selectedCategory) {
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

    public void setCategories(SearchCat[] searchCategories) {
        this.searchCategories = searchCategories;
        label = new JLabel() {
            public boolean isShowing() {

                return true;
            }

            public boolean isVisible() {
                return true;
            }
        };

        for (SearchCat sc : searchCategories) {
            label.setText(sc.getLabel());
            labelWidth = Math.max(label.getPreferredSize().width, labelWidth);
        }

        label.setText(selectedCategory.getLabel());

        label.setSize(labelWidth, 24);
        // label.setEnabled(false);
        setBorder(BorderFactory.createCompoundBorder(orgBorder, BorderFactory.createEmptyBorder(0, labelWidth + 14 + iconGap, 0, 18)));

    }

    public boolean highlightFilter() {
        return true;
    }

    @Override
    public void reset() {
    }
}
