package org.jdownloader.iconsetter;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.swing.components.IDIcon;
import org.appwork.swing.components.IconComponentInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;

public class IconSetMakerAdapter implements AWTEventListener {
    private ArrayList<Component> find(Container p, Point point) {

        ArrayList<Component> ret = new ArrayList<Component>();

        for (Component c : p.getComponents()) {
            if (c.getBounds().contains(point) && c.isVisible() && c.isShowing()) {
                ret.add(c);
                Point newPoint = SwingUtilities.convertPoint(p, new Point(point.x, point.y), c);
                if (c instanceof Container) {
                    ret.addAll(find((Container) c, newPoint));

                } else {

                }
            }
        }

        return ret;

    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            if (me.isAltDown() && me.isControlDown()) {
                me.consume();
                if (me.getID() == MouseEvent.MOUSE_ENTERED || me.getID() == MouseEvent.MOUSE_PRESSED) {
                    try {
                        Container p = ((Component) me.getSource()).getParent();

                        while (true) {
                            Container pp = p.getParent();
                            if (pp != null) {
                                p = pp;
                            } else {
                                break;
                            }
                        }
                        ;
                        Point orgPoint = new Point(me.getPoint().x, me.getPoint().y);
                        ArrayList<Component> ret = find(p, SwingUtilities.convertPoint((Component) me.getSource(), me.getPoint().x, me.getPoint().y, p));
                        Collections.reverse(ret);
                        HashSet<IDIcon> icons = new HashSet<IDIcon>();
                        for (Component c : ret) {

                            if (c instanceof JLabel) {
                                Icon ico = ((JLabel) c).getIcon();
                                if (ico != null && ico instanceof IDIcon) {
                                    icons.add((IDIcon) ico);

                                }
                            } else if (c instanceof AbstractButton) {
                                Icon ico = ((AbstractButton) c).getIcon();
                                if (ico != null && ico instanceof IDIcon) {
                                    icons.add((IDIcon) ico);

                                }

                                Action action = ((AbstractButton) c).getAction();
                                if (action != null) {
                                    ico = (Icon) action.getValue(Action.SMALL_ICON);
                                    if (ico != null && ico instanceof IDIcon) {
                                        icons.add((IDIcon) ico);

                                    }

                                    ico = (Icon) action.getValue(Action.LARGE_ICON_KEY);
                                    if (ico != null && ico instanceof IDIcon) {
                                        icons.add((IDIcon) ico);

                                    }
                                }
                            } else if (c instanceof IconComponentInterface) {
                                Point point = SwingUtilities.convertPoint((Component) me.getSource(), orgPoint.x, orgPoint.y, c);
                                List<Icon> ico = ((IconComponentInterface) c).getIcons(point.x, point.y);
                                if (ico != null) {
                                    for (Icon i : ico) {
                                        if (i instanceof IDIcon) {
                                            icons.add((IDIcon) i);

                                        }
                                    }

                                }
                            }
                            System.out.println(c);
                        }
                        for (IDIcon icon : icons) {
                            System.out.println("Icon " + icon);
                            System.out.println(JSonStorage.serializeToJson(icon.getIdentifier()));
                        }

                    } catch (Throwable e) {
                        LoggerFactory.getDefaultLogger().log(e);

                    }
                }

            }
        }

        // if (event instanceof MouseEvent) {
        //
        // }
    }

}