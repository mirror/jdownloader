//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.skins.simple;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.skins.simple.tasks.TaskPanel;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.acryl.AcrylTabbedPaneUI;

public class TaskPane extends JTabbedPane {

    private static final long serialVersionUID = 2484591650508276173L;
    private TaskPanel currentTab = null;

    // private ArrayList<TaskPanel> panels;

    public TaskPane() {
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (currentTab != null) {
                    currentTab.onHide();
                    currentTab.setActiveTab(false);
                }
                ((TaskPanel) getSelectedComponent()).broadcastEvent(new ActionEvent(((TaskPanel) getSelectedComponent()), TaskPanel.ACTION_CLICK, "Click"));
                currentTab = ((TaskPanel) getSelectedComponent());
                currentTab.onDisplay();
                currentTab.setActiveTab(true);
            }

        });
        // com.jtattoo.plaf.BaseTableHeaderUI
        // com.jtattoo.plaf.acryl.AcrylInternalFrameUI
        if (getUI() instanceof AcrylTabbedPaneUI) {

            setUI(new AcrylTabbedPaneUI() {
                

                protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
                    // super.paintContentBorder(arg0, arg1, arg2, arg3, arg4,
                    // arg5, arg6)
                    int sepHeight = tabAreaInsets.bottom;
                    if (sepHeight > 0) {
                        switch (tabPlacement) {
                        case LEFT: {
                            int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                            if (sepHeight > 1) {
                                Color colors[] = getContentBorderColors(tabPlacement);
                                for (int i = 0; i < colors.length; i++) {
                                    g.setColor(colors[i]);
                                    g.drawLine(x + tabAreaWidth - sepHeight + i + 1, y, x + tabAreaWidth - sepHeight + i + 1, y + h);
                                }
                            } else {
                                g.setColor(getContentBorderColors(tabPlacement)[0]);
                                g.drawLine(x + tabAreaWidth, y, x + tabAreaWidth, h);
                            }
                            g.setColor(AbstractLookAndFeel.getControlDarkShadow());
                            g.drawLine(x + tabAreaWidth - 1, y, x + tabAreaWidth - 1, y + h - 1);
                            break;
                        }
                        case RIGHT: {
                            int tabAreaWidth = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                            if (sepHeight > 1) {
                                Color colors[] = getContentBorderColors(tabPlacement);
                                for (int i = 0; i < colors.length; i++) {
                                    g.setColor(colors[i]);
                                    g.drawLine(x + w - tabAreaWidth + i, y, x + w - tabAreaWidth + i, y + h);
                                }
                            } else {
                                g.setColor(getContentBorderColors(tabPlacement)[0]);
                                g.drawLine(x + w - tabAreaWidth, y, x + w - tabAreaWidth, h);
                            }
                            g.setColor(AbstractLookAndFeel.getControlDarkShadow());
                            g.drawLine(x, y, x, y + h - 1);
                            break;
                        }
                        case TOP: {
                            int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                            if (sepHeight > 1) {
                                Color colors[] = getContentBorderColors(tabPlacement);
                                for (int i = 0; i < colors.length; i++) {
                                    g.setColor(colors[i]);
                                    g.drawLine(x, y + tabAreaHeight - sepHeight + i + 1, x + w, y + tabAreaHeight - sepHeight + i + 1);
                                }
                            } else {
                                g.setColor(getContentBorderColors(tabPlacement)[0]);
                                g.drawLine(x, y + tabAreaHeight, w, y + tabAreaHeight);
                            }
                            g.setColor(AbstractLookAndFeel.getControlDarkShadow());
                            g.drawLine(x + w - 1, y + tabAreaHeight - 1, x + w - 1, h);
                            break;
                        }
                        case BOTTOM: {
                            int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                            if (sepHeight > 1) {
                                Color colors[] = getContentBorderColors(tabPlacement);
                                for (int i = 0; i < colors.length; i++) {
                                    g.setColor(colors[i]);
                                    // g.drawLine(x, y + h - tabAreaHeight + i -
                                    // 1, x + w, y + h - tabAreaHeight + i - 1);
                                    g.drawLine(x, y + h - tabAreaHeight + i, x + w, y + h - tabAreaHeight + i);
                                }
                            } else {
                                g.setColor(getContentBorderColors(tabPlacement)[0]);
                                // g.drawLine(x, y + h - tabAreaHeight - 1, w, y
                                // + h - tabAreaHeight - 1);
                                g.drawLine(x, y + h - tabAreaHeight, w, y + h - tabAreaHeight);
                            }
                            g.setColor(AbstractLookAndFeel.getControlDarkShadow());
                            g.drawLine(x + w - 1, y, x + w - 1, h - tabAreaHeight);
                            break;
                        }
                        }

                    }

                }
            });

        }
        this.setFocusable(false);
        // panels = new ArrayList<TaskPanel>();
        // this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.setTabPlacement(JTabbedPane.TOP);
        // this.setBackgroundPainter(null);
        // this.setPreferredSize(new Dimension(180, 10));
        // this.setLayout(new MigLayout("ins 5, wrap 1", "[fill,grow]",
        // "[]2[]2[]2[]2[]2[]2[]2[]2[fill,grow]"));
    }

    public void add(TaskPanel panel) {
        super.addTab(null, panel.getIcon(), panel, panel.getTaskName());
    }

    public void add(int index, TaskPanel panel) {
        super.insertTab(null, panel.getIcon(), panel, panel.getToolTipText(), index);
        // panel.setMaximumSize(new Dimension(193, Integer.MAX_VALUE));
        // if (!panels.contains(panel)) {
        // panel.addActionListener(this);
        // switcher(null);
        // }
    }

    public void remove(TaskPanel panel) {

        super.removeTabAt(this.indexOfComponent(panel));

        // if (panels.contains(panel)) {
        // panel.removeActionListener(this);
        // // int index = panels.indexOf(panel);
        // panels.remove(panel);
        // // switcher(panels.get(index));
        // }
    }

    // public void actionPerformed(ActionEvent e) {
    // if (e.getSource() instanceof TaskPanel && e.getID() ==
    // TaskPanel.ACTION_TOGGLE) {
    // TaskPanel source = ((TaskPanel) e.getSource());
    //
    // if (source != lastSource) {
    // lastSource = source;
    // switcher(null);
    // }
    // }
    // }

    public void switcher(TaskPanel src) {

        // int index = this.indexOfComponent(src);
        this.setSelectedComponent(src);
        if (src != null) {

            src.broadcastEvent(new ActionEvent(src, TaskPanel.ACTION_CLICK, "Click"));
           SimpleGUI.CURRENTGUI.hideSideBar(false);
        }
        // new GuiRunnable<Object>() {
        //
        // @Override
        // public Object runSave() {
        // for (TaskPanel p : panels) {
        // if (p != lastSource) {
        // p.setSpecial(false);
        // p.setDeligateCollapsed(true);
        // } else {
        // p.setSpecial(true);
        // p.setDeligateCollapsed(false);
        // }
        // }
        // return null;
        // }
        //
        // }.start();

    }

}
