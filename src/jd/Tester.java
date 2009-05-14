package jd;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.ViewportLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;

import net.miginfocom.swing.MigLayout;

public class Tester extends JFrame {

    private static final long serialVersionUID = -2399077821905795553L;
    private JViewport vp;

    public Tester() {
        super();
        setLayout(new MigLayout("ins 5,wrap 2", "[]"));
JScrollPane sp;
        //       vp = new JViewport();
//        JScrollBar sb = new ScrollBar(JScrollBar.VERTICAL);
      //  vp.setView();
        this.add(sp=new JScrollPane(),"growx,pushx");
        sp.setViewport(vp=new JViewport(){
            public void setViewSize(Dimension newSize) {
                newSize.width=Tester.this.getSize().width;
                super.setViewSize(newSize);
            }
        });
        vp.setView(createPanel());
     
        sp.getViewport().setLayout(new ViewportLayout(){
         
            public Dimension preferredLayoutSize(Container parent) {
                
                Dimension ret = super.preferredLayoutSize(parent);
                ret.width=0;
                return ret;
                
            }
            
            
            
        });
//        this.add(sb, "pushy,growy");
        pack();
        setSize(new Dimension(300, 300));
        setVisible(true);
    
    }

    private Component createPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 5,wrap 1", "[]"));
        JTextArea ta = new JTextArea();
        ta.setLineWrap(true);

        ta.setText("This is just a very long dummytext. it is very long, so the textarea tries to be bigger than the jframe\rn\r\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\n\n\r\n\r\n\r\n\r\nHere is some text, too");
        panel.add(ta, "growx,pushx");
        panel.add(new JButton("Click me or not"), "alignx right");
        return panel;
    }

    public static void main(String[] args) {
        new Tester();
    }

    class ScrollBar extends JScrollBar implements UIResource {
        /**
         * Set to true when the unit increment has been explicitly set. If this
         * is false the viewport's view is obtained and if it is an instance of
         * <code>Scrollable</code> the unit increment from it is used.
         */
        private boolean unitIncrementSet;
        /**
         * Set to true when the block increment has been explicitly set. If this
         * is false the viewport's view is obtained and if it is an instance of
         * <code>Scrollable</code> the block increment from it is used.
         */
        private boolean blockIncrementSet;

        /**
         * Creates a scrollbar with the specified orientation. The options are:
         * <ul>
         * <li><code>ScrollPaneConstants.VERTICAL</code>
         * <li><code>ScrollPaneConstants.HORIZONTAL</code>
         * </ul>
         * 
         * @param orientation
         *            an integer specifying one of the legal orientation values
         *            shown above
         * @since 1.4
         */
        public ScrollBar(int orientation) {
            super(orientation);
            this.putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE);
        }

        /**
         * Messages super to set the value, and resets the
         * <code>unitIncrementSet</code> instance variable to true.
         * 
         * @param unitIncrement
         *            the new unit increment value, in pixels
         */
        public void setUnitIncrement(int unitIncrement) {
            unitIncrementSet = true;
            this.putClientProperty("JScrollBar.fastWheelScrolling", null);
            super.setUnitIncrement(unitIncrement);
        }

        /**
         * Computes the unit increment for scrolling if the viewport's view is a
         * <code>Scrollable</code> object. Otherwise return
         * <code>super.getUnitIncrement</code>.
         * 
         * @param direction
         *            less than zero to scroll up/left, greater than zero for
         *            down/right
         * @return an integer, in pixels, containing the unit increment
         * @see Scrollable#getScrollableUnitIncrement
         */
        public int getUnitIncrement(int direction) {
            JViewport vp = getViewport();
            if (!unitIncrementSet && (vp != null) && (vp.getView() instanceof Scrollable)) {
                Scrollable view = (Scrollable) (vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableUnitIncrement(vr, getOrientation(), direction);
            } else {
                return super.getUnitIncrement(direction);
            }
        }

        /**
         * Messages super to set the value, and resets the
         * <code>blockIncrementSet</code> instance variable to true.
         * 
         * @param blockIncrement
         *            the new block increment value, in pixels
         */
        public void setBlockIncrement(int blockIncrement) {
            blockIncrementSet = true;
            this.putClientProperty("JScrollBar.fastWheelScrolling", null);
            super.setBlockIncrement(blockIncrement);
        }

        /**
         * Computes the block increment for scrolling if the viewport's view is
         * a <code>Scrollable</code> object. Otherwise the
         * <code>blockIncrement</code> equals the viewport's width or height. If
         * there's no viewport return <code>super.getBlockIncrement</code>.
         * 
         * @param direction
         *            less than zero to scroll up/left, greater than zero for
         *            down/right
         * @return an integer, in pixels, containing the block increment
         * @see Scrollable#getScrollableBlockIncrement
         */
        public int getBlockIncrement(int direction) {
            JViewport vp = getViewport();
            if (blockIncrementSet || vp == null) {
                return super.getBlockIncrement(direction);
            } else if (vp.getView() instanceof Scrollable) {
                Scrollable view = (Scrollable) (vp.getView());
                Rectangle vr = vp.getViewRect();
                return view.getScrollableBlockIncrement(vr, getOrientation(), direction);
            } else if (getOrientation() == VERTICAL) {
                return vp.getExtentSize().height;
            } else {
                return vp.getExtentSize().width;
            }
        }

    }

    public JViewport getViewport() {
        // TODO Auto-generated method stub
        return vp;
    }
}
