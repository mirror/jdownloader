/*
 * $Id: JXColorSelectionButton.java,v 1.3 2007/12/31 14:26:10 rah003 Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdesktop.swingx.color.*;

/**
 * A button which allows the user to select a single color. The button has a platform
 * specific look. Ex: on Mac OS X it will mimic an NSColorWell. When the user
 * clicks the button it will open a color chooser set to the current background
 * color of the button. The new selected color will be stored in the background
 * property and can be retrieved using the getBackground() method. As the user is
 * choosing colors within the color chooser the background property will be updated.
 * By listening to this property developers can make other parts of their program
 * update.
 *
 * @author joshua.marinacci@sun.com
 */
public class JXColorSelectionButton extends JButton {
    
    private JDialog dialog = null;
    private JColorChooser chooser = null;
    
    /**
     * Creates a new instance of JXColorSelectionButton
     */
    public JXColorSelectionButton() {
        this(Color.red);
    }
    public JXColorSelectionButton(Color col) {
        setBackground(col);
        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if(dialog == null) {
                    dialog = JColorChooser.createDialog(
                            JXColorSelectionButton.this,
                            "Choose a color",
                            true, 
                            getChooser(),
                            new ActionListener() {
                                public void actionPerformed(ActionEvent actionEvent) {
                                    //System.out.println("okay");
                                }
                            },
                            new ActionListener() {
                                public void actionPerformed(ActionEvent actionEvent) {
                                    //System.out.println("cancel");
                                }
                            }
                            );
                    dialog.getContentPane().add(getChooser());
                    getChooser().getSelectionModel().addChangeListener(new ColorChangeListener(JXColorSelectionButton.this));
                }
                dialog.setVisible(true);
                Color color = getChooser().getColor();
                
                if (color != null) {
                    setBackground(color);
                }
                
            }
        });
        this.setContentAreaFilled(false);
        this.setOpaque(false);
        
        try {
            colorwell = ImageIO.read(this.getClass().getResourceAsStream("/org/jdesktop/swingx/color/colorwell.png"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        this.addPropertyChangeListener("background",new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                getChooser().setColor(getBackground());
            }
        });
    }
    
    private BufferedImage colorwell;
    
    private class ColorChangeListener implements ChangeListener {
        public JXColorSelectionButton button;
        public ColorChangeListener(JXColorSelectionButton button) {
            this.button = button;
        }
        public void stateChanged(ChangeEvent changeEvent) {
            button.setBackground(button.getChooser().getColor());
        }
    }

    protected void paintComponent(Graphics g) {
        Insets ins = new Insets(5,5,5,5);        
        if(colorwell != null) {
            ColorUtil.tileStretchPaint(g, this, colorwell, ins);
        }
        
        // 0, 23, 255  = 235o, 100%, 100%
        // 31, 0, 204 =  249o, 100%,  80%
	g.setColor(ColorUtil.removeAlpha(getBackground()));
        g.fillRect(ins.left, ins.top, 
                    getWidth()  - ins.left - ins.right, 
                    getHeight() - ins.top - ins.bottom);
        g.setColor(ColorUtil.setBrightness(getBackground(),0.85f));
        g.drawRect(ins.left, ins.top,
                getWidth() - ins.left - ins.right - 1,
                getHeight() - ins.top - ins.bottom - 1);
        g.drawRect(ins.left + 1, ins.top + 1,
                getWidth() - ins.left - ins.right - 3,
                getHeight() - ins.top - ins.bottom - 3);
    }


    public static void main(String[] args) {
        JFrame frame = new JFrame("Color Button Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.add(new JXColorSelectionButton());
        panel.add(new JLabel("ColorSelectionButton test"));
        
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    /** Get the chooser that is used by this JXColorSelectionButton. This
     * chooser instance is shared between all invocations of the chooser, but is unique to
     * this instance of JXColorSelectionButton.
     */
    public JColorChooser getChooser() {
        if(chooser == null) {
            chooser = new JColorChooser();
        }
        return chooser;
    }
    
}
