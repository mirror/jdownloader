/*
 * $Id: JXCollapsiblePane.java,v 1.23 2008/02/29 02:21:13 kschaefe Exp $
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

/**
 * <code>JXCollapsiblePane</code> provides a component which can collapse or
 * expand its content area with animation and fade in/fade out effects.
 * It also acts as a standard container for other Swing components.
 *
 * <p>
 * In this example, the <code>JXCollapsiblePane</code> is used to build
 * a Search pane which can be shown and hidden on demand.
 *
 * <pre>
 * <code>
 * JXCollapsiblePane cp = new JXCollapsiblePane();
 *
 * // JXCollapsiblePane can be used like any other container
 * cp.setLayout(new BorderLayout());
 *
 * // the Controls panel with a textfield to filter the tree
 * JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
 * controls.add(new JLabel("Search:"));
 * controls.add(new JTextField(10));
 * controls.add(new JButton("Refresh"));
 * controls.setBorder(new TitledBorder("Filters"));
 * cp.add("Center", controls);
 *
 * JXFrame frame = new JXFrame();
 * frame.setLayout(new BorderLayout());
 *
 * // Put the "Controls" first
 * frame.add("North", cp);
 *
 * // Then the tree - we assume the Controls would somehow filter the tree
 * JScrollPane scroll = new JScrollPane(new JTree());
 * frame.add("Center", scroll);
 *
 * // Show/hide the "Controls"
 * JButton toggle = new JButton(cp.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
 * toggle.setText("Show/Hide Search Panel");
 * frame.add("South", toggle);
 *
 * frame.pack();
 * frame.setVisible(true);
 * </code>
 * </pre>
 *
 * <p>
 * The <code>JXCollapsiblePane</code> has a default toggle action registered
 * under the name {@link #TOGGLE_ACTION}. Bind this action to a button and
 * pressing the button will automatically toggle the pane between expanded
 * and collapsed states. Additionally, you can define the icons to use through
 * the {@link #EXPAND_ICON} and {@link #COLLAPSE_ICON} properties on the action.
 * Example
 * <pre>
 * <code>
 * // get the built-in toggle action
 * Action toggleAction = collapsible.getActionMap().
 *   get(JXCollapsiblePane.TOGGLE_ACTION);
 *
 * // use the collapse/expand icons from the JTree UI
 * toggleAction.putValue(JXCollapsiblePane.COLLAPSE_ICON,
 *                       UIManager.getIcon("Tree.expandedIcon"));
 * toggleAction.putValue(JXCollapsiblePane.EXPAND_ICON,
 *                       UIManager.getIcon("Tree.collapsedIcon"));
 * </code>
 * </pre>
 *
 * <p>
 * Note: <code>JXCollapsiblePane</code> requires its parent container to have a
 * {@link java.awt.LayoutManager} using {@link #getPreferredSize()} when
 * calculating its layout (example {@link org.jdesktop.swingx.VerticalLayout},
 * {@link java.awt.BorderLayout}).
 *
 * @javabean.attribute
 *          name="isContainer"
 *          value="Boolean.TRUE"
 *          rtexpr="true"
 *
 * @javabean.attribute
 *          name="containerDelegate"
 *          value="getContentPane"
 *
 * @javabean.class
 *          name="JXCollapsiblePane"
 *          shortDescription="A pane which hides its content with an animation."
 *          stopClass="java.awt.Component"
 *
 * @author rbair (from the JDNC project)
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
 */
public class JXCollapsiblePane extends JXPanel {
    /**
     * The orientation defines in which direction the collapsible pane will
     * expand.
     */
    public enum Orientation {
        /**
         * The horizontal orientation makes the collapsible pane
         * expand horizontally
         */
        HORIZONTAL,
        /**
         * The horizontal orientation makes the collapsible pane
         * expand vertically
         */
        VERTICAL
    }

    /**
     * Used when generating PropertyChangeEvents for the "animationState"
     * property. The PropertyChangeEvent will takes the following different values
     * for {@link PropertyChangeEvent#getNewValue()}:
     * <ul>
     * <li><code>reinit</code> every time the animation starts
     * <li><code>expanded</code> when the animation ends and the pane is expanded
     * <li><code>collapsed</code> when the animation ends and the pane is collapsed
     * </ul>
     */
    public final static String ANIMATION_STATE_KEY = "animationState";

    /**
     * JXCollapsible has a built-in toggle action which can be bound to buttons.
     * Accesses the action through
     * <code>collapsiblePane.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION)</code>.
     */
    public final static String TOGGLE_ACTION = "toggle";

    /**
     * The icon used by the "toggle" action when the JXCollapsiblePane is
     * expanded, i.e the icon which indicates the pane can be collapsed.
     */
    public final static String COLLAPSE_ICON = "collapseIcon";

    /**
     * The icon used by the "toggle" action when the JXCollapsiblePane is
     * collapsed, i.e the icon which indicates the pane can be expanded.
     */
    public final static String EXPAND_ICON = "expandIcon";

    /**
     * Indicates whether the component is collapsed or expanded
     */
    private boolean collapsed = false;

    /**
     * Defines the orientation of the component.
     */
    private Orientation orientation = Orientation.VERTICAL;

    /**
     * Timer used for doing the transparency animation (fade-in)
     */
    private Timer animateTimer;
    private AnimationListener animator;
    private int currentDimension = -1;
    private WrapperContainer wrapper;
    private boolean useAnimation = true;
    private AnimationParams animationParams;

    /**
     * Constructs a new JXCollapsiblePane with a {@link JPanel} as content pane
     * and a vertical {@link VerticalLayout} with a gap of 2 pixels as layout
     * manager and a vertical orientation.
     */
    public JXCollapsiblePane() {
        this(Orientation.VERTICAL, new BorderLayout(0, 0));
    }

    /**
     * Constructs a new JXCollapsiblePane with a {@link JPanel} as content pane
     * and the specified orientation.
     */
    public JXCollapsiblePane(Orientation orientation) {
        this(orientation, new BorderLayout(0, 0));
    }

    /**
     * Constructs a new JXCollapsiblePane with a {@link JPanel} as content pane
     * and the given LayoutManager and a vertical orientation
     */
    public JXCollapsiblePane(LayoutManager layout) {
        this(Orientation.VERTICAL, layout);
    }

    /**
     * Constructs a new JXCollapsiblePane with a {@link JPanel} as content pane
     * and the given LayoutManager and orientation. A vertical orientation enables
     * a vertical {@link VerticalLayout} with a gap of 2 pixels as layout
     * manager. A horizontal orientation enables a horizontal
     * {@link HorizontalLayout} with a gap of 2 pixels as layout manager
     */
    public JXCollapsiblePane(Orientation orientation, LayoutManager layout) {
        super.setLayout(layout);

        this.orientation = orientation;

        JXPanel panel = new JXPanel();
        if (orientation == Orientation.VERTICAL) {
            panel.setLayout(new VerticalLayout(2));
        } else {
            panel.setLayout(new HorizontalLayout(2));
        }
        setContentPane(panel);

        animator = new AnimationListener();
        setAnimationParams(new AnimationParams(30, 8, 0.01f, 1.0f));

        // add an action to automatically toggle the state of the pane
        getActionMap().put(TOGGLE_ACTION, new ToggleAction());
    }

    /**
     * Toggles the JXCollapsiblePane state and updates its icon based on the
     * JXCollapsiblePane "collapsed" status.
     */
    private class ToggleAction extends AbstractAction implements
                                                      PropertyChangeListener {
        public ToggleAction() {
            super(TOGGLE_ACTION);
            // the action must track the collapsed status of the pane to update its
            // icon
            JXCollapsiblePane.this.addPropertyChangeListener("collapsed", this);
        }

        @Override
        public void putValue(String key, Object newValue) {
            super.putValue(key, newValue);
            if (EXPAND_ICON.equals(key) || COLLAPSE_ICON.equals(key)) {
                updateIcon();
            }
        }

        public void actionPerformed(ActionEvent e) {
            setCollapsed(!isCollapsed());
        }

        public void propertyChange(PropertyChangeEvent evt) {
            updateIcon();
        }

        void updateIcon() {
            if (isCollapsed()) {
                putValue(SMALL_ICON, getValue(EXPAND_ICON));
            } else {
                putValue(SMALL_ICON, getValue(COLLAPSE_ICON));
            }
        }
    }

    /**
     * Sets the content pane of this JXCollapsiblePane. The {@code contentPanel}
     * <i>should</i> implement {@code Scrollable} and return {@code true} from
     * {@link Scrollable#getScrollableTracksViewportHeight()} and
     * {@link Scrollable#getScrollableTracksViewportWidth()}. If the content
     * pane fails to do so and a {@code JScrollPane} is added as a child, it is
     * likely that the scroll pane will never correctly size. While it is not
     * strictly necessary to implement {@code Scrollable} in this way, the
     * default content pane does so.
     * 
     * @param contentPanel
     *                the container delegate used to hold all of the contents
     *                for this collapsible pane
     * @throws IllegalArgumentException
     *                 if contentPanel is null
     */
    public void setContentPane(Container contentPanel) {
        if (contentPanel == null) {
            throw new IllegalArgumentException("Content pane can't be null");
        }

        if (wrapper != null) {
            //these next two lines are as they are because if I try to remove
            //the "wrapper" component directly, then super.remove(comp) ends up
            //calling remove(int), which is overridden in this class, leading to
            //improper behavior.
            assert super.getComponent(0) == wrapper;
            super.remove(0);
        }
        wrapper = new WrapperContainer(contentPanel);
        wrapper.collapsedState = isCollapsed();
        super.addImpl(wrapper, BorderLayout.CENTER, -1);
    }

    /**
     * @return the content pane
     */
    public Container getContentPane() {
        if (wrapper == null) {
            return null;
        }
        
        return (Container) wrapper.getView();
    }

    /**
     * Overriden to redirect call to the content pane.
     */
    @Override
    public void setLayout(LayoutManager mgr) {
        // wrapper can be null when setLayout is called by "super()" constructor
        if (wrapper != null) {
            getContentPane().setLayout(mgr);
        }
    }

    /**
     * Overriden to redirect call to the content pane.
     */
    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        getContentPane().add(comp, constraints, index);
    }

    /**
     * Overriden to redirect call to the content pane
     */
    @Override
    public void remove(Component comp) {
        getContentPane().remove(comp);
    }

    /**
     * Overriden to redirect call to the content pane.
     */
    @Override
    public void remove(int index) {
        getContentPane().remove(index);
    }

    /**
     * Overriden to redirect call to the content pane.
     */
    @Override
    public void removeAll() {
        getContentPane().removeAll();
    }

    /**
     * If true, enables the animation when pane is collapsed/expanded. If false,
     * animation is turned off.
     *
     * <p>
     * When animated, the <code>JXCollapsiblePane</code> will progressively
     * reduce (when collapsing) or enlarge (when expanding) the height of its
     * content area until it becomes 0 or until it reaches the preferred height of
     * the components it contains. The transparency of the content area will also
     * change during the animation.
     *
     * <p>
     * If not animated, the <code>JXCollapsiblePane</code> will simply hide
     * (collapsing) or show (expanding) its content area.
     *
     * @param animated
     * @javabean.property bound="true" preferred="true"
     */
    public void setAnimated(boolean animated) {
        if (animated != useAnimation) {
            useAnimation = animated;
            firePropertyChange("animated", !useAnimation, useAnimation);
        }
    }

    /**
     * @return true if the pane is animated, false otherwise
     * @see #setAnimated(boolean)
     */
    public boolean isAnimated() {
        return useAnimation;
    }

    /**
     * Changes the orientation of this collapsible pane. Doing so changes the
     * layout of the underlying content pane. If the chosen orientation is
     * vertical, a vertical layout with a gap of 2 pixels is chosen. Otherwise,
     * a horizontal layout with a gap of 2 pixels is chosen.
     *
     * @see #getOrientation()
     * @param orientation the new {@link Orientation} for this collapsible pane
     * @throws IllegalStateException when this method is called while a
     *                               collapsing/restore operation is running
     * @javabean.property
     *    bound="true"
     *    preferred="true"
     */
    public void setOrientation(Orientation orientation) {
        if (orientation != this.orientation) {
            if (animateTimer.isRunning()) {
                throw new IllegalStateException("Orientation cannot be changed " +
                    "during collapsing.");
            }

            this.orientation = orientation;

            if (orientation == Orientation.VERTICAL) {
                getContentPane().setLayout(new VerticalLayout(2));
            } else {
                getContentPane().setLayout(new HorizontalLayout(2));
            }
        }
    }
    
    /**
     * @see #setOrientation(Orientation)
     * @return the current {@link Orientation}
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * @return true if the pane is collapsed, false if expanded
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * Expands or collapses this <code>JXCollapsiblePane</code>.
     *
     * <p>
     * If the component is collapsed and <code>val</code> is false, then this
     * call expands the JXCollapsiblePane, such that the entire JXCollapsiblePane
     * will be visible. If {@link #isAnimated()} returns true, the expansion will
     * be accompanied by an animation.
     *
     * <p>
     * However, if the component is expanded and <code>val</code> is true, then
     * this call collapses the JXCollapsiblePane, such that the entire
     * JXCollapsiblePane will be invisible. If {@link #isAnimated()} returns true,
     * the collapse will be accompanied by an animation.
     *
     * @see #isAnimated()
     * @see #setAnimated(boolean)
     * @javabean.property
     *    bound="true"
     *    preferred="true"
     */
    public void setCollapsed(boolean val) {
        if (collapsed != val) {
            collapsed = val;
            if (isAnimated()) {
                if (collapsed) {
                    int dimension = orientation == Orientation.VERTICAL ?
                                    wrapper.getHeight() : wrapper.getWidth();
                    setAnimationParams(new AnimationParams(30,
                                                           Math.max(8, dimension / 10), 1.0f, 0.01f));
                    animator.reinit(dimension, 0);
                    animateTimer.start();
                } else {
                    int dimension = orientation == Orientation.VERTICAL ?
                                    wrapper.getHeight() : wrapper.getWidth();
                    int preferredDimension = orientation == Orientation.VERTICAL ?
                                             getContentPane().getPreferredSize().height :
                                             getContentPane().getPreferredSize().width;
                    int delta = Math.max(8, preferredDimension / 10);

                    setAnimationParams(new AnimationParams(30, delta, 0.01f, 1.0f));
                    animator.reinit(dimension, preferredDimension);
                    animateTimer.start();
                }
            } else {
                wrapper.collapsedState = collapsed;
                revalidate();
            }
            repaint();
            firePropertyChange("collapsed", !collapsed, collapsed);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Border getBorder() {
        if (getContentPane() instanceof JComponent) {
            return ((JComponent) getContentPane()).getBorder();
        }
        
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setBorder(Border border) {
        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setBorder(border);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    /**
     * The critical part of the animation of this <code>JXCollapsiblePane</code>
     * relies on the calculation of its preferred size. During the animation, its
     * preferred size (specially its height) will change, when expanding, from 0
     * to the preferred size of the content pane, and the reverse when collapsing.
     *
     * @return this component preferred size
     */
    @Override
    public Dimension getPreferredSize() {
        /*
         * The preferred size is calculated based on the current position of the
         * component in its animation sequence. If the Component is expanded, then
         * the preferred size will be the preferred size of the top component plus
         * the preferred size of the embedded content container. <p>However, if the
         * scroll up is in any state of animation, the height component of the
         * preferred size will be the current height of the component (as contained
         * in the currentDimension variable and when orientation is VERTICAL, otherwise
         * the same applies to the width)
         */
        Dimension dim = getContentPane().getPreferredSize();
        if (currentDimension != -1) {
                if (orientation == Orientation.VERTICAL) {
                    dim.height = currentDimension;
                } else {
                    dim.width = currentDimension;
                }
        } else if(wrapper.collapsedState) {
            if (orientation == Orientation.VERTICAL) {
                dim.height = 0;
            } else {
                dim.width = 0;
            }
        }
        return dim;
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        getContentPane().setPreferredSize(preferredSize);
    }

    /**
     * Sets the parameters controlling the animation
     *
     * @param params
     * @throws IllegalArgumentException
     *           if params is null
     */
    private void setAnimationParams(AnimationParams params) {
        if (params == null) { throw new IllegalArgumentException(
                "params can't be null"); }
        if (animateTimer != null) {
            animateTimer.stop();
        }
        animationParams = params;
        animateTimer = new Timer(animationParams.waitTime, animator);
        animateTimer.setInitialDelay(0);
    }

    /**
     * Tagging interface for containers in a JXCollapsiblePane hierarchy who needs
     * to be revalidated (invalidate/validate/repaint) when the pane is expanding
     * or collapsing. Usually validating only the parent of the JXCollapsiblePane
     * is enough but there might be cases where the parent parent must be
     * validated.
     */
    public static interface CollapsiblePaneContainer {
        Container getValidatingContainer();
    }

    /**
     * Parameters controlling the animations
     */
    private static class AnimationParams {
        final int waitTime;
        final int delta;
        final float alphaStart;
        final float alphaEnd;

        /**
         * @param waitTime
         *          the amount of time in milliseconds to wait between calls to the
         *          animation thread
         * @param delta
         *          the delta, in the direction as specified by the orientation,
         *          to inc/dec the size of the scroll up by
         * @param alphaStart
         *          the starting alpha transparency level
         * @param alphaEnd
         *          the ending alpha transparency level
         */
        public AnimationParams(int waitTime, int delta, float alphaStart,
                               float alphaEnd) {
            this.waitTime = waitTime;
            this.delta = delta;
            this.alphaStart = alphaStart;
            this.alphaEnd = alphaEnd;
        }
    }

    /**
     * This class actual provides the animation support for scrolling up/down this
     * component. This listener is called whenever the animateTimer fires off. It
     * fires off in response to scroll up/down requests. This listener is
     * responsible for modifying the size of the content container and causing it
     * to be repainted.
     *
     * @author Richard Bair
     */
    private final class AnimationListener implements ActionListener {
        /**
         * Mutex used to ensure that the startDimension/finalDimension are not changed
         * during a repaint operation.
         */
        private final Object ANIMATION_MUTEX = "Animation Synchronization Mutex";
        /**
         * This is the starting dimension when animating. If > finalDimension, then the
         * animation is going to be to scroll up the component. If it is less than
         * finalDimension, then the animation will scroll down the component.
         */
        private int startDimension = 0;
        /**
         * This is the final dimension that the content container is going to be when
         * scrolling is finished.
         */
        private int finalDimension = 0;
        /**
         * The current alpha setting used during "animation" (fade-in/fade-out)
         */
        @SuppressWarnings({"FieldCanBeLocal"})
        private float animateAlpha = 1.0f;

        public void actionPerformed(ActionEvent e) {
            /*
            * Pre-1) If startDimension == finalDimension, then we're done so stop the timer
            * 1) Calculate whether we're contracting or expanding. 2) Calculate the
            * delta (which is either positive or negative, depending on the results
            * of (1)) 3) Calculate the alpha value 4) Resize the ContentContainer 5)
            * Revalidate/Repaint the content container
            */
            synchronized (ANIMATION_MUTEX) {
                if (startDimension == finalDimension) {
                    animateTimer.stop();
                    animateAlpha = animationParams.alphaEnd;
                    // keep the content pane hidden when it is collapsed, other it may
                    // still receive focus.
                    if (finalDimension > 0) {
                        currentDimension = -1;
                        wrapper.collapsedState = false;
                        validate();
                        JXCollapsiblePane.this.firePropertyChange(ANIMATION_STATE_KEY, null,
                                                                  "expanded");
                        return;
                    } else {
                        wrapper.collapsedState = true;
                        JXCollapsiblePane.this.firePropertyChange(ANIMATION_STATE_KEY, null,
                                                                  "collapsed");
                    }
                }

                final boolean contracting = startDimension > finalDimension;
                final int delta = contracting?-1 * animationParams.delta
                                  :animationParams.delta;
                int newDimension;
                if (orientation == Orientation.VERTICAL) {
                    newDimension = wrapper.getHeight() + delta;
                } else {
                    newDimension = wrapper.getWidth() + delta;
                }
                if (contracting) {
                    if (newDimension < finalDimension) {
                        newDimension = finalDimension;
                    }
                } else {
                    if (newDimension > finalDimension) {
                        newDimension = finalDimension;
                    }
                }
                int dimension;
                if (orientation == Orientation.VERTICAL) {
                    dimension = wrapper.getView().getPreferredSize().height;
                } else {
                    dimension = wrapper.getView().getPreferredSize().width;
                }
                animateAlpha = (float)newDimension / (float)dimension;

                Rectangle bounds = wrapper.getBounds();

                if (orientation == Orientation.VERTICAL) {
                    int oldHeight = bounds.height;
                    bounds.height = newDimension;
                    wrapper.setBounds(bounds);
                    wrapper.setViewPosition(new Point(0, wrapper.getView().getPreferredSize().height - newDimension));
                    bounds = getBounds();
                    bounds.height = (bounds.height - oldHeight) + newDimension;
                    currentDimension = bounds.height;
                } else {
                    int oldWidth = bounds.width;
                    bounds.width = newDimension;
                    wrapper.setBounds(bounds);
                    wrapper.setViewPosition(new Point(wrapper.getView().getPreferredSize().width - newDimension, 0));
                    bounds = getBounds();
                    bounds.width = (bounds.width - oldWidth) + newDimension;
                    currentDimension = bounds.width;
                }

                setBounds(bounds);
                startDimension = newDimension;

                // it happens the animateAlpha goes over the alphaStart/alphaEnd range
                // this code ensures it stays in bounds. This behavior is seen when
                // component such as JTextComponents are used in the container.
                if (contracting) {
                    // alphaStart > animateAlpha > alphaEnd
                    if (animateAlpha < animationParams.alphaEnd) {
                        animateAlpha = animationParams.alphaEnd;
                    }
                    if (animateAlpha > animationParams.alphaStart) {
                        animateAlpha = animationParams.alphaStart;
                    }
                } else {
                    // alphaStart < animateAlpha < alphaEnd
                    if (animateAlpha > animationParams.alphaEnd) {
                        animateAlpha = animationParams.alphaEnd;
                    }
                    if (animateAlpha < animationParams.alphaStart) {
                        animateAlpha = animationParams.alphaStart;
                    }
                }
                wrapper.alpha = animateAlpha;

                validate();
            }
        }

        void validate() {
            Container parent = SwingUtilities.getAncestorOfClass(
                    CollapsiblePaneContainer.class, JXCollapsiblePane.this);
            if (parent != null) {
                parent = ((CollapsiblePaneContainer)parent).getValidatingContainer();
            } else {
                parent = getParent();
            }

            if (parent != null) {
                if (parent instanceof JComponent) {
                    ((JComponent)parent).revalidate();
                } else {
                    parent.invalidate();
                }
                parent.doLayout();
                parent.repaint();
            }
        }

        /**
         * Reinitializes the timer for scrolling up/down the component. This method
         * is properly synchronized, so you may make this call regardless of whether
         * the timer is currently executing or not.
         *
         * @param startDimension
         * @param stopDimension
         */
        public void reinit(int startDimension, int stopDimension) {
            synchronized (ANIMATION_MUTEX) {
                JXCollapsiblePane.this.firePropertyChange(ANIMATION_STATE_KEY, null,
                                                          "reinit");
                this.startDimension = startDimension;
                this.finalDimension = stopDimension;
                animateAlpha = animationParams.alphaStart;
                currentDimension = -1;
            }
        }
    }

    private final class WrapperContainer extends JViewport {
        float alpha;
        boolean collapsedState;

        public WrapperContainer(Container c) {
            alpha = 1.0f;
            collapsedState = false;
            setView(c);

            // we must ensure the container is opaque. It is not opaque it introduces
            // painting glitches specially on Linux with JDK 1.5 and GTK look and feel.
            // GTK look and feel calls setOpaque(false)
            if (c instanceof JComponent && !c.isOpaque()) {
                ((JComponent) c).setOpaque(true);
            }
        }
    }

// TEST CASE
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                JFrame f = new JFrame("Test Oriented Collapsible Pane");
//
//                f.add(new JLabel("Press Ctrl+F or Ctrl+G to collapse panes."),
//                      BorderLayout.NORTH);
//
//                JTree tree1 = new JTree();
//                tree1.setBorder(BorderFactory.createEtchedBorder());
//                f.add(tree1);
//
//                JXCollapsiblePane pane = new JXCollapsiblePane(Orientation.VERTICAL);
//                pane.setCollapsed(true);
//                JTree tree2 = new JTree();
//                tree2.setBorder(BorderFactory.createEtchedBorder());
//                pane.add(tree2);
//                f.add(pane, BorderLayout.SOUTH);
//
//                pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
//                        KeyStroke.getKeyStroke("ctrl F"),
//                        JXCollapsiblePane.TOGGLE_ACTION);
//                    
//                pane = new JXCollapsiblePane(Orientation.HORIZONTAL);
//                JTree tree3 = new JTree();
//                pane.add(tree3);
//                tree3.setBorder(BorderFactory.createEtchedBorder());
//                f.add(pane, BorderLayout.WEST);
//
//                pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
//                        KeyStroke.getKeyStroke("ctrl G"),
//                        JXCollapsiblePane.TOGGLE_ACTION);
//
//                f.setSize(640, 480);
//                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//                f.setVisible(true);
//        }
//        });
//    }
}
