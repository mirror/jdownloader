/*
 * $Id: HyperlinkProvider.java,v 1.2 2008/01/22 16:26:06 kleopatra Exp $
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
package org.jdesktop.swingx.renderer;

import java.awt.Point;
import java.awt.event.ActionEvent;

import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.RolloverProducer;
import org.jdesktop.swingx.RolloverRenderer;
import org.jdesktop.swingx.action.LinkAction;

/**
 * Renderer for hyperlinks". <p>
 * 
 * The renderer is configured with a LinkAction<T>. 
 * It's mostly up to the developer to guarantee that the all
 * values which are passed into the getXXRendererComponent(...) are
 * compatible with T: she can provide a runtime class to check against.
 * If it isn't the renderer will configure the
 * action with a null target. <p>
 * 
 * It's recommended to not use the given Action anywhere else in code,
 * as it is updated on each getXXRendererComponent() call which might
 * lead to undesirable side-effects. <p>
 * 
 * Internally uses JXHyperlink as rendering component. <p>
 * 
 * PENDING: can go from ButtonProvider? <p>
 * 
 * PENDING: make renderer respect selected cell state. <p>
 * 
 * PENDING: TreeCellRenderer has several issues <p>
 * <ol>
 *   <li> no icons
 *   <li> usual background highlighter issues
 * </ol>  
 * 
 * @author Jeanette Winzenburg
 */
public class HyperlinkProvider
   extends ComponentProvider<JXHyperlink> implements
         RolloverRenderer {


    private LinkAction<Object> linkAction;
    protected Class<?> targetClass;

    /**
     * Instantiate a LinkRenderer with null LinkAction and null
     * targetClass.
     *
     */
    public HyperlinkProvider() {
        this(null, null);
    }

    /**
     * Instantiate a LinkRenderer with the LinkAction to use with
     * target values. 
     * 
     * @param linkAction the action that acts on values.
     */
    public HyperlinkProvider(LinkAction linkAction) {
        this(linkAction, null);
    }
    
    /**
     * Instantiate a LinkRenderer with a LinkAction to use with
     * target values and the type of values the action can cope with. <p>
     * 
     * It's up to developers to take care of matching types.
     * 
     * @param linkAction the action that acts on values.
     * @param targetClass the type of values the action can handle.
     */
    public HyperlinkProvider(LinkAction linkAction, Class targetClass) {
        super();
//        rendererComponent.addActionListener(createEditorActionListener());
        setLinkAction(linkAction, targetClass);
    }
    
    /**
     * Sets the class the action is supposed to handle. <p>
     * 
     * PENDING: make sense to set independently of LinkAction?
     * 
     * @param targetClass the type of values the action can handle.
     */
    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * Sets the LinkAction for handling the values. <p>
     * 
     * The action is assumed to be able to cope with any type, that is
     * this method is equivalent to setLinkAction(linkAction, null).
     * 
     * @param linkAction
     */
    public void setLinkAction(LinkAction linkAction) {
        setLinkAction(linkAction, null);
    }
    
    /**
     * Sets the LinkAction for handling the values and the 
     * class the action can handle. <p>
     * 
     * PENDING: in the general case this is not independent of the
     * targetClass. Need api to set them combined?
     * 
     * @param linkAction
     */
    public void setLinkAction(LinkAction linkAction, Class targetClass) {
        if (linkAction == null) {
            linkAction = createDefaultLinkAction();
        }
        setTargetClass(targetClass); 
        this.linkAction = linkAction;
        rendererComponent.setAction(linkAction);
        
    }
    /**
     * decides if the given target is acceptable for setTarget.
     * <p>
     *  
     *  target == null is acceptable for all types.
     *  targetClass == null is the same as Object.class
     *  
     * @param target the target to set.
     * @return true if setTarget can cope with the object, 
     *  false otherwise.
     * 
     */
    public  boolean isTargetable(Object target) {
        // we accept everything
        if (targetClass == null) return true;
        if (target == null) return true;
        return targetClass.isAssignableFrom(target.getClass());
    }



    /** 
     * default action - does nothing... except showing the target.
     * 
     * @return a default LinkAction for showing the target.
     */
    protected LinkAction createDefaultLinkAction() {
        return new LinkAction<Object>(null) {

            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                
            }
            
        };
    }

//----------------------- Implement RolloverRenderer
    
    public boolean isEnabled() {
        return true;
    }

    public void doClick() {
        rendererComponent.doClick();
    }
    
//------------------------ TableCellRenderer
    


    /**
     * {@inheritDoc}
     */
    @Override
    protected JXHyperlink createRendererComponent() {
        return new JXHyperlink() {

            @Override
            public void updateUI() {
                super.updateUI();
                setBorderPainted(true);
                setOpaque(true);
            }
            
        };
    }

    /**
     * {@inheritDoc} <p>
     * 
     * Overridden to set the hyperlink's rollover state. 
     */
    @Override
    protected void configureState(CellContext context) {
//        rendererComponent.setHorizontalAlignment(getHorizontalAlignment());
        if (context.getComponent() !=  null) {
            Point p = (Point) context.getComponent()
                    .getClientProperty(RolloverProducer.ROLLOVER_KEY);
            if (/*hasFocus || */(p != null && (p.x >= 0) && 
                    (p.x == context.getColumn()) && (p.y == context.getRow()))) {
                 rendererComponent.getModel().setRollover(true);
            } else {
                rendererComponent.getModel().setRollover(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Overridden to set the LinkAction's target to the context's value, if 
     * targetable.<p>
     * 
     * PENDING: Forces foreground color
     * to those defined by hyperlink - this should happen automatically? Hyperlink
     * bug?
     * 
     */
    @Override
    protected void format(CellContext context) {
        Object value = context.getValue();
        if (isTargetable(value)) {
            linkAction.setTarget(value);
        } else {
            linkAction.setTarget(null);
        }
        // hmm... the hyperlink should do this automatically..
        rendererComponent.setForeground(linkAction.isVisited() ? 
                rendererComponent.getClickedColor() : rendererComponent.getUnclickedColor());
        
    }


}
