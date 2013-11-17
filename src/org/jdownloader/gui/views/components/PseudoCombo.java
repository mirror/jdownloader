package org.jdownloader.gui.views.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class PseudoCombo<Type> extends JButton {

    protected Type  selectedItem = null;

    private Type[]  values;
    private boolean popDown      = false;

    public PseudoCombo(Type[] values) {
        super();
        this.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                onPopup();
            }
        });

        this.setHorizontalAlignment(SwingConstants.LEFT);
        setValues(values);

    }

    public void setValues(Type[] values) {
        this.values = values;
        Insets m = getMargin();
        if (m == null) m = new Insets(0, 0, 0, 0);
        m.right = getPopIcon(true).getIconWidth() + 5;
        setMargin(m);
        int width = 0;
        for (Type v : values) {
            setText(getLabel(v, true));
            setIcon(getIcon(v, true));
            width = Math.max(width, getPreferredSize().width);
        }
        setPreferredSize(new Dimension(width, 24));
    }

    protected Icon getPopIcon(boolean closed) {
        if (closed) {
            if (isPopDown()) {
                return NewTheme.I().getIcon(IconKey.ICON_POPDOWNBUTTON, -1);
            } else {
                return NewTheme.I().getIcon(IconKey.ICON_POPUPBUTTON, -1);
            }
        } else {
            if (isPopDown()) {
                return NewTheme.I().getIcon(IconKey.ICON_POPUPBUTTON, -1);
            } else {
                return NewTheme.I().getIcon(IconKey.ICON_POPDOWNBUTTON, -1);

            }
        }

    }

    public boolean isPopDown() {
        return popDown;
    }

    public void setPopDown(boolean popDown) {
        this.popDown = popDown;
    }

    protected Icon getIcon(Type v, boolean closed) {
        return null;
    }

    protected String getLabel(Type v, boolean closed) {
        return v + "";
    }

    private long    lastHide = 0;

    private boolean closed   = true;

    protected void onPopup() {
        long timeSinceLastHide = System.currentTimeMillis() - lastHide;
        if (timeSinceLastHide < 250) {
            //
            return;

        }

        JPopupMenu popup = new JPopupMenu() {

            @Override
            public void setVisible(boolean b) {
                if (!b) lastHide = System.currentTimeMillis();
                super.setVisible(b);
                closed = true;
                PseudoCombo.this.repaint();
            }

        };

        for (final Type sc : values) {
            if (sc == selectedItem && isHideSelf()) continue;
            popup.add(new AppAction() {
                private Type value;
                {
                    value = sc;
                    setName(getLabel(sc, false));
                    setSmallIcon(getIcon(sc, false));
                }

                public void actionPerformed(ActionEvent e) {
                    setSelectedItem(value);

                }
            });
        }
        int[] insets = LAFOptions.getInstance().getPopupBorderInsets();

        Dimension pref = popup.getPreferredSize();
        // pref.width = positionComp.getWidth() + ((Component)
        // e.getSource()).getWidth() + insets[1] + insets[3];
        popup.setPreferredSize(new Dimension((int) Math.max(getWidth() + insets[1] + insets[3], pref.getWidth()), (int) pref.getHeight()));
        // PseudoCombo.this.repaint();
        if (isPopDown()) {
            popup.show(this, -insets[1], getHeight() + insets[0]);

        } else {
            popup.show(this, -insets[1], -popup.getPreferredSize().height + insets[2]);

        }
        closed = false;
    }

    protected boolean isHideSelf() {
        return true;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Icon icon = getPopIcon(closed);
        if (!isEnabled()) {
            icon = ImageProvider.getDisabledIcon(icon);
        }
        icon.paintIcon(this, g, getWidth() - icon.getIconWidth() - 5, (getHeight() - icon.getIconHeight()) / 2);

    }

    public void setSelectedItem(Type value) {

        if (selectedItem != value) {
            selectedItem = value;
            onChanged(value);
        }

        setText(getLabel(value, true));
        setIcon(getIcon(value, true));
        setToolTipText(getLabel(value, false));

    }

    public void onChanged(Type newValue) {
    }

    public Type getSelectedItem() {
        return selectedItem;
    }

}
