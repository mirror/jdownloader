package org.jdownloader.tests;

/**
 *
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * @author Thomas.Darimont
 */
public class CustomScrollingListExample extends JFrame {

    public CustomScrollingListExample() {
        super("CustomScrollingListExample");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setSize(400, 300);

        final JPopupMenu popupMenu = new JPopupMenu("PopupMenu");
        JList list = new JList(new Object[] { "AAAA", "BBBB", "CCCCCCCC", "DDDD", "EEEE", "FFFF", "GGGG", "0000", "1111", "2222", "3333", "4444" });
        popupMenu.add(new CustomList(list));

        setVisible(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                popupMenu.show(CustomScrollingListExample.this, e.getX(), e.getY());
            }
        });

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new CustomScrollingListExample();
    }

    static class ScrollButton extends JButton {

        static enum ScrollButtonType {
            UP,
            DOWN
        };

        Polygon          polygon;
        ScrollButtonType type;

        public ScrollButton(int width, int height, ScrollButtonType type) {
            this.type = type;
            setBorder(BorderFactory.createEmptyBorder());
            this.setPreferredSize(new Dimension(width, height));
            switch (type) {
            case DOWN:
                polygon = new Polygon(new int[] { 0, width / 2, width }, new int[] { 0, height, 0 }, 3);
                break;
            case UP:
                polygon = new Polygon(new int[] { 0, width / 2, width }, new int[] { height, 0, height }, 3);
                break;
            }
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fillPolygon(polygon);
        }
    }

    static class CustomList extends JComponent {
        JList       list;
        JScrollPane scrollPane;
        JButton     btnScrollUp;
        JButton     btnScrollDown;

        public CustomList(final JList list) {
            this.list = list;
            scrollPane = new JScrollPane(list);
            Dimension scrollPaneSize = new Dimension(list.getPreferredSize().width + 10, list.getFontMetrics(list.getFont()).getHeight() * list.getVisibleRowCount());
            scrollPane.setMaximumSize(scrollPaneSize);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            ActionListener actionListener = new ActionListener() {
                int viewIndex;

                public void actionPerformed(ActionEvent e) {
                    if (btnScrollUp == e.getSource()) {
                        System.out.println("up: " + viewIndex);
                        if (viewIndex > 0) {
                            viewIndex--;
                        }
                        Point point = list.getUI().indexToLocation(list, viewIndex);
                        scrollPane.getViewport().setViewPosition(point);

                    } else if (btnScrollDown == e.getSource()) {
                        System.out.println("down: " + viewIndex);
                        int listSize = list.getModel().getSize();
                        int visibleRowCount = list.getVisibleRowCount();
                        if (viewIndex <= listSize - visibleRowCount) {
                            viewIndex++;
                        }

                        Point point = list.getUI().indexToLocation(list, viewIndex);
                        scrollPane.getViewport().setViewPosition(point);
                    }
                }
            };

            btnScrollUp = new ScrollButton(list.getPreferredSize().width, 10, CustomScrollingListExample.ScrollButton.ScrollButtonType.UP);
            btnScrollDown = new ScrollButton(list.getPreferredSize().width, 10, CustomScrollingListExample.ScrollButton.ScrollButtonType.DOWN);

            btnScrollUp.addActionListener(actionListener);
            btnScrollDown.addActionListener(actionListener);

            setLayout(new BorderLayout());

            add(btnScrollUp, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
            add(btnScrollDown, BorderLayout.SOUTH);

            setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, btnScrollUp.getPreferredSize().height + scrollPane.getMaximumSize().height + btnScrollDown.getPreferredSize().height));
        }
    }
}
