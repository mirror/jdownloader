package jd.gui.swing.jdgui.menu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

import javax.swing.JMenu;
import javax.swing.JToggleButton;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthConstants;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthStyle;

import jd.gui.swing.laf.LAFOptions;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.actions.AppAction;
import org.jdownloader.logging.LogController;

public class MenuJToggleButton extends JToggleButton {

    private LogSource logger;

    public MenuJToggleButton(AppAction action) {
        super(action);
        logger = LogController.getInstance().getLogger(MenuJToggleButton.class.getName());
    }

    private SynthContext context;
    private Object       painter;
    private Method       paintMenuBackground;
    private SynthContext mouseOverContext;
    private SynthContext mouseOutContext;
    {
        try {
            ;
            Class<?> cls = LAFOptions.getInstance().getMenuBackgroundPainterClass();
            painter = cls.newInstance();
            paintMenuBackground = cls.getMethod("paintMenuBackground", new Class[] { SynthContext.class, Graphics.class, int.class, int.class, int.class, int.class });
            mouseOverContext = context = new SynthContext(new JMenu() {
                {
                    putClientProperty("Synthetica.MOUSE_OVER", true);
                }

                public boolean isTopLevelMenu() {
                    return true;
                }

            }, Region.BUTTON, new SynthStyle() {

                @Override
                protected Font getFontForState(SynthContext context) {
                    return null;
                }

                @Override
                protected Color getColorForState(SynthContext context, ColorType type) {
                    return null;
                }
            }, SynthConstants.MOUSE_OVER);

            mouseOutContext = context = new SynthContext(new JMenu() {

                public boolean isTopLevelMenu() {
                    return true;
                }

            }, Region.BUTTON, new SynthStyle() {

                @Override
                protected Font getFontForState(SynthContext context) {
                    return null;
                }

                @Override
                protected Color getColorForState(SynthContext context, ColorType type) {
                    return null;
                }
            }, 0);

        } catch (Exception e) {
            logger.log(e);
        }
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(final MouseEvent e) {
                context = mouseOverContext;

            }

            @Override
            public void mouseExited(final MouseEvent e) {
                context = mouseOutContext;

            }
        });
    }

    protected void paintComponent(Graphics g) {

        if (paintMenuBackground != null) {
            try {
                paintMenuBackground.invoke(painter, new Object[] { context, g, 0, 0, getWidth(), getHeight() });
            } catch (Exception e) {
                logger.log(e);
            }
        }

        super.paintComponent(g);
    }

}
