/*
 * $Id: JXTaskPane.java,v 1.10 2007/11/15 03:49:40 kschaefe Exp $
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
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jdesktop.swingx.plaf.TaskPaneAddon;
import org.jdesktop.swingx.plaf.LookAndFeelAddons;
import org.jdesktop.swingx.plaf.TaskPaneUI;

/**
 * <code>JXTaskPane</code> is a container for tasks and other
 * arbitrary components.
 * 
 * <p>
 * Several <code>JXTaskPane</code>s are usually grouped together within a
 * {@link org.jdesktop.swingx.JXTaskPaneContainer}. However it is not mandatory
 * to use a JXTaskPaneContainer as the parent for JXTaskPane. The JXTaskPane can
 * be added to any other container. See
 * {@link org.jdesktop.swingx.JXTaskPaneContainer} to understand the benefits of
 * using it as the parent container.
 * 
 * <p>
 * <code>JXTaskPane</code> provides control to expand and
 * collapse the content area in order to show or hide the task list. It can have an
 * <code>icon</code>, a <code>title</code> and can be marked as
 * <code>special</code>. Marking a <code>JXTaskPane</code> as
 * <code>special</code> ({@link #setSpecial(boolean)} is only a hint for
 * the pluggable UI which will usually paint it differently (by example by
 * using another color for the border of the pane).
 * 
 * <p> 
 * When the JXTaskPane is expanded or collapsed, it will be
 * animated with a fade effect. The animated can be disabled on a per
 * component basis through {@link #setAnimated(boolean)}.
 * 
 * To disable the animation for all newly created <code>JXTaskPane</code>,
 * use the UIManager property:
 * <code>UIManager.put("TaskPane.animate", Boolean.FALSE);</code>.
 * 
 * <p>
 * Example:
 * <pre>
 * <code>
 * JXFrame frame = new JXFrame();
 * 
 * // a container to put all JXTaskPane together
 * JXTaskPaneContainer taskPaneContainer = new JXTaskPaneContainer();
 * 
 * // create a first taskPane with common actions
 * JXTaskPane actionPane = new JXTaskPane();
 * actionPane.setTitle("Files and Folders");
 * actionPane.setSpecial(true);
 * 
 * // actions can be added, an hyperlink will be created
 * Action renameSelectedFile = createRenameFileAction();
 * actionPane.add(renameSelectedFile);
 * actionPane.add(createDeleteFileAction());
 * 
 * // add this taskPane to the taskPaneContainer
 * taskPaneContainer.add(actionPane);
 * 
 * // create another taskPane, it will show details of the selected file
 * JXTaskPane details = new JXTaskPane();
 * details.setTitle("Details");
 *  
 * // add standard components to the details taskPane
 * JLabel searchLabel = new JLabel("Search:");
 * JTextField searchField = new JTextField("");
 * details.add(searchLabel);
 * details.add(searchField);
 * 
 * taskPaneContainer.add(details);
 * 
 * // put the action list on the left 
 * frame.add(taskPaneContainer, BorderLayout.EAST);
 * 
 * // and a file browser in the middle
 * frame.add(fileBrowser, BorderLayout.CENTER);
 * 
 * frame.pack().
 * frame.setVisible(true);
 * </code>
 * </pre>
 * 
 * @see org.jdesktop.swingx.JXTaskPaneContainer
 * @see org.jdesktop.swingx.JXCollapsiblePane
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
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
 *          name="JXTaskPane"
 *          shortDescription="JXTaskPane is a container for tasks and other arbitrary components."
 *          stopClass="java.awt.Component"
 * 
 * @javabean.icons
 *          mono16="JXTaskPane16-mono.gif"
 *          color16="JXTaskPane16.gif"
 *          mono32="JXTaskPane32-mono.gif"
 *          color32="JXTaskPane32.gif"
 */
public class JXTaskPane extends JPanel implements
  JXCollapsiblePane.CollapsiblePaneContainer {

  /**
   * JXTaskPane pluggable UI key <i>swingx/TaskPaneUI</i> 
   */
  public final static String uiClassID = "swingx/TaskPaneUI";
  
  // ensure at least the default ui is registered
  static {
    LookAndFeelAddons.contribute(new TaskPaneAddon());
  }

  /**
   * Used when generating PropertyChangeEvents for the "expanded" property
   */
  public static final String EXPANDED_CHANGED_KEY = "expanded";

  /**
   * Used when generating PropertyChangeEvents for the "scrollOnExpand" property
   */
  public static final String SCROLL_ON_EXPAND_CHANGED_KEY = "scrollOnExpand";

  /**
   * Used when generating PropertyChangeEvents for the "title" property
   */
  public static final String TITLE_CHANGED_KEY = "title";

  /**
   * Used when generating PropertyChangeEvents for the "icon" property
   */
  public static final String ICON_CHANGED_KEY = "icon";

  /**
   * Used when generating PropertyChangeEvents for the "special" property
   */
  public static final String SPECIAL_CHANGED_KEY = "special";

  /**
   * Used when generating PropertyChangeEvents for the "animated" property
   */
  public static final String ANIMATED_CHANGED_KEY = "animated";

  private String title;
  private Icon icon;
  private boolean special;
  private boolean expanded = true;
  private boolean scrollOnExpand;

  private JXCollapsiblePane collapsePane;
  
  /**
   * Creates a new empty <code>JXTaskPane</code>.
   */
  public JXTaskPane() {
    collapsePane = new JXCollapsiblePane();
    super.setLayout(new BorderLayout(0, 0));
    super.addImpl(collapsePane, BorderLayout.CENTER, -1);
    
    updateUI();
    setFocusable(true);
    setOpaque(false);

    // disable animation if specified in UIManager
    setAnimated(!Boolean.FALSE.equals(UIManager.get("TaskPane.animate")));
    
    // listen for animation events and forward them to registered listeners
    collapsePane.addPropertyChangeListener(
      JXCollapsiblePane.ANIMATION_STATE_KEY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          JXTaskPane.this.firePropertyChange(evt.getPropertyName(), evt
            .getOldValue(), evt.getNewValue());
        }
      });
  }

  /**
   * Returns the contentPane object for this JXTaskPane.
   * @return the contentPane property
   */
  public Container getContentPane() {
    return collapsePane.getContentPane();
  }
  
  /**
   * Notification from the <code>UIManager</code> that the L&F has changed.
   * Replaces the current UI object with the latest version from the <code>UIManager</code>.
   * 
   * @see javax.swing.JComponent#updateUI
   */
  @Override
  public void updateUI() {
    // collapsePane is null when updateUI() is called by the "super()"
    // constructor
    if (collapsePane == null) {
      return;
    }
    setUI((TaskPaneUI)LookAndFeelAddons.getUI(this, TaskPaneUI.class));
  }
  
  /**
   * Sets the L&F object that renders this component.
   * 
   * @param ui the <code>TaskPaneUI</code> L&F object
   * @see javax.swing.UIDefaults#getUI
   * 
   * @beaninfo bound: true hidden: true description: The UI object that
   * implements the taskpane group's LookAndFeel.
   */
  public void setUI(TaskPaneUI ui) {
    super.setUI(ui);
  }

  /**
   * Returns the name of the L&F class that renders this component.
   * 
   * @return the string {@link #uiClassID}
   * @see javax.swing.JComponent#getUIClassID
   * @see javax.swing.UIDefaults#getUI
   */
  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  /**
   * Returns the title currently displayed in the border of this pane.
   * 
   * @return the title currently displayed in the border of this pane
   */
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title to be displayed in the border of this pane.
   * 
   * @param title the title to be displayed in the border of this pane
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public void setTitle(String title) {
    String old = this.title;
    this.title = title;
    firePropertyChange(TITLE_CHANGED_KEY, old, title);
  }

  /**
   * Returns the icon currently displayed in the border of this pane.
   * 
   * @return the icon currently displayed in the border of this pane
   */
  public Icon getIcon() {
    return icon;
  }

  /**
   * Sets the icon to be displayed in the border of this pane. Some pluggable
   * UIs may impose size constraints for the icon. A size of 16x16 pixels is
   * the recommended icon size.
   * 
   * @param icon the icon to be displayed in the border of this pane
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public void setIcon(Icon icon) {
    Icon old = this.icon;
    this.icon = icon;
    firePropertyChange(ICON_CHANGED_KEY, old, icon);
  }

  /**
   * Returns true if this pane is "special".
   * 
   * @return true if this pane is "special"
   * @see #setSpecial(boolean)
   */
  public boolean isSpecial() {
    return special;
  }

  /**
   * Sets this pane to be "special" or not. Marking a <code>JXTaskPane</code>
   * as <code>special</code> is only a hint for the pluggable UI which will
   * usually paint it differently (by example by using another color for the
   * border of the pane).
   * 
   * <p>
   * Usually the first JXTaskPane in a JXTaskPaneContainer is marked as special
   * because it contains the default set of actions which can be executed given
   * the current context.
   * 
   * @param special
   *          true if this pane is "special", false otherwise
   * @javabean.property bound="true" preferred="true"
   */
  public void setSpecial(boolean special) {
    if (this.special != special) {
      this.special = special;
      firePropertyChange(SPECIAL_CHANGED_KEY, !special, special);
    }
  }

  /**
   * Should this group be scrolled to be visible on expand.
   * 
   * @param scrollOnExpand true to scroll this group to be
   * visible if this group is expanded.
   * 
   * @see #setExpanded(boolean)
   * 
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public void setScrollOnExpand(boolean scrollOnExpand) {
    if (this.scrollOnExpand != scrollOnExpand) {
      this.scrollOnExpand = scrollOnExpand;
      firePropertyChange(SCROLL_ON_EXPAND_CHANGED_KEY,
        !scrollOnExpand, scrollOnExpand);
    }
  }
  
  /**
   * Should this group scroll to be visible after
   * this group was expanded.
   * 
   * @return true if we should scroll false if nothing
   * should be done.
   */
  public boolean isScrollOnExpand() {
    return scrollOnExpand;
  }
  
  /**
   * Expands or collapses this group.
   * 
   * @param expanded true to expand the group, false to collapse it
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public void setExpanded(boolean expanded) {
    if (this.expanded != expanded) {
      this.expanded = expanded;
      collapsePane.setCollapsed(!expanded);
      firePropertyChange(EXPANDED_CHANGED_KEY, !expanded, expanded);
    }
  }

  /**
   * Returns true if this taskpane is expanded, false if it is collapsed.
   * 
   * @return true if this taskpane is expanded, false if it is collapsed.
   */
  public boolean isExpanded() {
    return expanded;
  }

  /**
   * Enables or disables animation during expand/collapse transition.
   * 
   * @param animated
   * @javabean.property
   *          bound="true"
   *          preferred="true"
   */
  public void setAnimated(boolean animated) {
    if (isAnimated() != animated) {
      collapsePane.setAnimated(animated);
      firePropertyChange(ANIMATED_CHANGED_KEY, !isAnimated(), isAnimated());
    }
  }
  
  /**
   * Returns true if this taskpane is animated during expand/collapse
   * transition.
   * 
   * @return true if this taskpane is animated during expand/collapse
   *         transition.
   */
  public boolean isAnimated() {
    return collapsePane.isAnimated();
  }
  
  /**
   * Adds an action to this <code>JXTaskPane</code>. Returns a
   * component built from the action. The returned component has been
   * added to the <code>JXTaskPane</code>.
   * 
   * @param action
   * @return a component built from the action
   */
  public Component add(Action action) {
    Component c = ((TaskPaneUI)ui).createAction(action);
    add(c);
    return c;
  }

  /**
   * @see JXCollapsiblePane.CollapsiblePaneContainer
   */
  public Container getValidatingContainer() {
    return getParent();
  }
  
  /**
   * Overriden to redirect call to the content pane.
   */
  @Override
  protected void addImpl(Component comp, Object constraints, int index) {
    getContentPane().add(comp, constraints, index);
    //Fixes SwingX #364; adding to internal component we need to revalidate ourself
    revalidate();
  }

  /**
   * Overriden to redirect call to the content pane.
   */
  @Override
  public void setLayout(LayoutManager mgr) {
    if (collapsePane != null) {
      getContentPane().setLayout(mgr);
    }
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
   * @see JComponent#paramString()
   */
  @Override
  protected String paramString() {
    return super.paramString()
      + ",title="
      + getTitle()
      + ",icon="
      + getIcon()
      + ",expanded="
      + String.valueOf(isExpanded())
      + ",special="
      + String.valueOf(isSpecial())
      + ",scrollOnExpand=" 
      + String.valueOf(isScrollOnExpand())
      + ",ui=" + getUI();
  }

}
