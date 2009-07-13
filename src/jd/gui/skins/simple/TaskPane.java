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
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.View;

import jd.gui.skins.simple.tasks.TaskPanel;

import com.jgoodies.looks.Options;
import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.acryl.AcrylTabbedPaneUI;

public class TaskPane extends JTabbedPane {

    private static final long serialVersionUID = 2484591650508276173L;
    // private ArrayList<TaskPanel> panels;
    private TaskPanel lastSource;

    public TaskPane() {
        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {

                ((TaskPanel) getSelectedComponent()).broadcastEvent(new ActionEvent(((TaskPanel) getSelectedComponent()), TaskPanel.ACTION_CLICK, "Click"));
            }

        });
      
        if (getUI() instanceof AcrylTabbedPaneUI) {
            

            setUI(new AcrylTabbedPaneUI() {

//                protected void layoutLabel(int tabPlacement, FontMetrics metrics, int tabIndex, String title, Icon icon, Rectangle tabRect, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
//                    textRect.x = textRect.y = iconRect.x = iconRect.y = 0;
//                    View v = getTextViewForTab(tabIndex);
//                    if (v != null) {
//                        tabPane.putClientProperty("html", v);
//                    }
//
//                    SwingUtilities.layoutCompoundLabel((JComponent) tabPane,
//                            metrics, title, icon,
//                            SwingUtilities.CENTER,
//                            SwingUtilities.CENTER,
//                            SwingUtilities.CENTER,
//                            SwingUtilities.LEADING,
//                            tabRect,
//                            iconRect,
//                            textRect,
//                            textIconGap);
//
//                    tabPane.putClientProperty("html", null);
//
//                    int xNudge = getTabLabelShiftX(tabPlacement, tabIndex, isSelected);
//                    int yNudge = getTabLabelShiftY(tabPlacement, tabIndex, isSelected);
//                    iconRect.x += xNudge+10;
//                    iconRect.y += yNudge+1;
//                    textRect.x += xNudge+10;
//                    textRect.y += yNudge+1;
//                }
                
                protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
                
                    int sepHeight = tabAreaInsets.bottom;
                    if (sepHeight > 0) {
                        switch (tabPlacement) {
                          
                            case BOTTOM: {
                                int tabAreaHeight = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                                if (sepHeight > 1) {
                                    Color colors[] = getContentBorderColors(tabPlacement);
                                    for (int i = 0; i < colors.length; i++) {
                                        g.setColor(colors[i]);
                                        //g.drawLine(x, y + h - tabAreaHeight + i - 1, x + w, y + h - tabAreaHeight + i - 1);
                                        g.drawLine(x, y + h - tabAreaHeight + i, x + w, y + h - tabAreaHeight + i);
                                    }
                                } else {
                                    g.setColor(getContentBorderColors(tabPlacement)[0]);
                                    //g.drawLine(x, y + h - tabAreaHeight - 1, w, y + h - tabAreaHeight - 1);
                                    g.drawLine(x, y + h - tabAreaHeight, w, y + h - tabAreaHeight);
                                }
//                                g.setColor(AbstractLookAndFeel.getControlDarkShadow());
//                                g.drawRect(x, y, x + w - 1, h - tabAreaHeight);
                                break;
                            }
                        }
                    }
                
                }

            });

        }
      
        // panels = new ArrayList<TaskPanel>();
        // this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.setTabPlacement(JTabbedPane.BOTTOM);
        // this.setBackgroundPainter(null);
        // this.setPreferredSize(new Dimension(180, 10));
        // this.setLayout(new MigLayout("ins 5, wrap 1", "[fill,grow]",
        // "[]2[]2[]2[]2[]2[]2[]2[]2[fill,grow]"));
    }

    public void add(TaskPanel panel) {
        super.addTab(null, panel.getIcon(), panel,panel.getTaskName());
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
            lastSource = src;

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
