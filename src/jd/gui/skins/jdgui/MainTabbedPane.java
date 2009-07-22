package jd.gui.skins.jdgui;

import java.awt.Component;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.skins.jdgui.interfaces.View;

public class MainTabbedPane extends JTabbedPane {

    private static final long serialVersionUID = -1531827591735215594L;
    // private Boolean extraHighlight;
    protected View latestSelection;

    public void addTab(View downloadView) {
        super.addTab(downloadView.getTitle(), downloadView.getIcon(), downloadView, downloadView.getTooltip());
        this.setFocusable(false);
        // extraHighlight =
        // SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER).getBooleanProperty(JDGuiConstants.CFG_KEY_MAIN_TABBED_HIGHLIGHT,
        // false);
        // initUI();
    }

    public MainTabbedPane() {
this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT );
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                try {
                    View comp = (View) getSelectedComponent();
                    if (comp == latestSelection) return;
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    latestSelection = comp;
                    comp.setShown();
                    revalidate();
                } catch (Exception e2) {
                }

            }

        });
    }

    // /**
    // * inits the ui for special lafs
    // */
    // private void initUI() {
    // if (getUI() instanceof AcrylTabbedPaneUI) {
    //
    // setUI(new AcrylTabbedPaneUI() {
    // public void installDefaults() {
    // super.installDefaults();
    // contentBorderInsets = new Insets(0, 0, 0, 0);
    // int inset = extraHighlight ? 4 : 1;
    // tabInsets = new Insets(inset, 6, inset, 6);
    // }
    //
    // protected void paintContentBorder(Graphics g, int tabPlacement, int
    // selectedIndex, int x, int y, int w, int h) {
    // // super.paintContentBorder(arg0, arg1, arg2, arg3, arg4,
    // // arg5, arg6)
    // int sepHeight = tabAreaInsets.bottom;
    //
    // if (sepHeight > 0) {
    // switch (tabPlacement) {
    // case TOP: {
    // int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount,
    // maxTabHeight);
    // Color colors[] = getContentBorderColors(tabPlacement);
    // for (int i = 0; i < colors.length; i++) {
    // g.setColor(colors[i]);
    // g.drawLine(x, y + tabAreaHeight - sepHeight + i + 1, x + w, y +
    // tabAreaHeight - sepHeight + i + 1);
    // }
    //
    // break;
    // }
    //
    // }
    //
    // }
    //
    // }
    //
    // protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects,
    // int tabIndex, Rectangle iconRect, Rectangle textRect) {
    // Rectangle tabRect = rects[tabIndex];
    // int selectedIndex = tabPane.getSelectedIndex();
    // boolean isSelected = selectedIndex == tabIndex;
    //
    // if (extraHighlight && !isSelected) {
    // tabRect.y += 5;
    // tabRect.height -= 5;
    //
    // }
    // paintTabBackground(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
    // tabRect.width, tabRect.height, isSelected);
    // paintTabBorder(g, tabPlacement, tabIndex, tabRect.x, tabRect.y,
    // tabRect.width, tabRect.height, isSelected);
    // if (extraHighlight && isSelected) {
    // tabRect.y += 5;
    // tabRect.height -= 5;
    // }
    // try {
    // boolean doPaintContent = true;
    // if (JTattooUtilities.getJavaVersion() >= 1.6) {
    // doPaintContent = (tabPane.getTabComponentAt(tabIndex) == null);
    // }
    // if (doPaintContent) {
    // String title = tabPane.getTitleAt(tabIndex);
    // Font font = getTabFont(isSelected);
    // FontMetrics metrics = g.getFontMetrics(font);
    // Icon icon = getIconForTab(tabIndex);
    //
    // layoutLabel(tabPlacement, metrics, tabIndex, title, icon, tabRect,
    // iconRect, textRect, isSelected);
    // paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect,
    // isSelected);
    // paintIcon(g, tabPlacement, tabIndex, icon, iconRect, isSelected);
    // }
    // paintFocusIndicator(g, tabPlacement, rects, tabIndex, iconRect, textRect,
    // isSelected);
    // } catch (Exception ex) {
    // //
    // outStream.print("----------------------------------------------------\n");
    // // ex.printStackTrace(outStream);
    // }
    // if (extraHighlight) {
    // tabRect.y -= 5;
    // tabRect.height += 5;
    // }
    //
    // }
    //
    // });
    //
    // }
    // }

    /**
     * gets called form the main frame if it gets closed
     */
    public void onClose() {
        getSelectedView().setHidden();
    }

    /**
     * returns the currently selected View
     */

    public View getSelectedView() {
        return (View) super.getSelectedComponent();
    }

    public void setSelectedComponent(Component e) {
        super.setSelectedComponent(e);
    }

    /**
     * CHecks if there is already a tabbepanel of this type in this pane.
     * 
     * @param view
     * @return
     */
    public boolean contains(View view) {
        for (int i = 0; i < this.getTabCount(); i++) {
            if (this.getTabComponentAt(i).equals(view)) return true;
        }
        return false;
    }

}
