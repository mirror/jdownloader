package org.jdownloader.gui.views.downloads;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;


import org.appwork.utils.logging.Log;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.laf.jddefault.LAFOptions;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PseudoCombo extends JButton {

    protected View selectedItem = View.ALL;
    private Image  icon;
    private View[] views;

    public PseudoCombo(View[] views) {
        super();
        this.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onPopup();
            }
        });
        this.setToolTipText(_GUI._.PseudoCombo_PseudoCombo_tt_());
        this.views = views;
        this.setHorizontalAlignment(SwingConstants.LEFT);

        int width = 0;
        for (View v : views) {
            setText(v.getLabel());
            setIcon(v.getIcon());
            width = Math.max(width, getPreferredSize().width);
        }
        setPreferredSize(new Dimension(width, 24));
        icon = NewTheme.I().getImage("popupButton", -1);
        setSelectedItem(View.ALL);
    }

    protected void onPopup() {

        JPopupMenu popup = new JPopupMenu();

        for (final View sc : views) {
            if (sc == selectedItem) continue;
            popup.add(new AppAction() {
                private View view;
                {
                    view = sc;
                    setName(sc.getLabel());
                    setSmallIcon(sc.getIcon());
                }

                public void actionPerformed(ActionEvent e) {
                    setSelectedItem(view);

                }
            });
        }
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(new Dimension(getWidth() + insets[1] + insets[3], (int) pref.getHeight()));

        popup.show(this, -insets[1], -popup.getPreferredSize().height + insets[2]);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(icon, getWidth() - 10, (getHeight() - icon.getHeight(null)) / 2, null);

    }

    public void setSelectedItem(View value) {
        if (value == null) {
            Log.exception(Level.WARNING, new NullPointerException("view null"));
        }
        if (selectedItem != value) {
            onChanged(value);
        }
        if (value != null) {
            selectedItem = value;
            setIcon(value.getIcon());
            setText(value.getLabel());
        }
    }

    public void onChanged(View newValue) {
    }

    public View getSelectedItem() {
        return selectedItem;
    }

}
