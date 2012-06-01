package jd.http.ext;

import java.awt.event.MouseEvent;

import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.domimpl.HTMLAbstractUIElement;
import org.lobobrowser.html.domimpl.HTMLButtonElementImpl;
import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
import org.lobobrowser.html.domimpl.HTMLSpanElementImpl;
import org.lobobrowser.html.domimpl.ModelNode;
import org.lobobrowser.html.js.Event;
import org.lobobrowser.html.js.Executor;
import org.mozilla.javascript.Function;

public class InputController {

    public InputController() {

    }

    // public void enterPressed(ModelNode node) {
    // if (node instanceof HTMLInputElementImpl) {
    // HTMLInputElementImpl hie = (HTMLInputElementImpl) node;
    // if (hie.isSubmittableWithEnterKey()) {
    // hie.submitForm(null);
    //
    // }
    // }
    //
    // }

    public void mouseClick(ModelNode node, MouseEvent event, int x, int y) {

        if (node instanceof HTMLAbstractUIElement) {
            HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
            Function f = uiElement.getOnclick();
            if (f != null) {
                Event jsEvent = new Event("click", uiElement, event, x, y);
                if (!Executor.executeFunction(uiElement, f, jsEvent)) { return; }
            }
            HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
            if (rcontext != null) {
                if (!rcontext.onMouseClick(uiElement, event)) { return; }
            }
        }
        if (node instanceof HTMLLinkElementImpl) {
            ((HTMLLinkElementImpl) node).navigate();
            return;
        } else if (node instanceof HTMLButtonElementImpl) {
            HTMLButtonElementImpl button = (HTMLButtonElementImpl) node;
            String rawType = button.getAttribute("type");
            String type;
            if (rawType == null) {
                type = "submit";
            } else {
                type = rawType.trim().toLowerCase();
            }
            if ("submit".equals(type)) {
                FormInput[] formInputs;
                String name = button.getName();
                if (name == null) {
                    formInputs = null;
                } else {
                    formInputs = new FormInput[] { new FormInput(name, button.getValue()) };
                }
                button.submitForm(formInputs);
            } else if ("reset".equals(type)) {
                button.resetForm();
            } else {
                // NOP for "button"!
            }
            return;
        }

        // not consumed? delegate
        ModelNode parent = node.getParentModelNode();
        if (parent != null) {
            mouseClick(parent, event, x, y);
        }
    }

    // public void contextMenu(ModelNode node, MouseEvent event, int x, int y) {
    //        
    // if (node instanceof HTMLAbstractUIElement) {
    // HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
    // Function f = uiElement.getOncontextmenu();
    // if (f != null) {
    // Event jsEvent = new Event("contextmenu", uiElement, event, x, y);
    // if (!Executor.executeFunction(uiElement, f, jsEvent)) { return; }
    // }
    // HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
    // if (rcontext != null) {
    // // Needs to be done after Javascript, so the script
    // // is able to prevent it.
    // if (!rcontext.onContextMenu(uiElement, event)) { return; }
    // }
    // }
    // ModelNode parent = node.getParentModelNode();
    // if (parent != null) {
    // contextMenu(parent, event, x, y);};
    // }

    public void mouseOver(ModelNode node, MouseEvent event, int x, int y, ModelNode limit) {
        while (node != null) {
            if (node == limit) {
                break;
            }
            if (node instanceof HTMLAbstractUIElement) {
                HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
                uiElement.setMouseOver(true);
                Function f = uiElement.getOnmouseover();
                if (f != null) {
                    Event jsEvent = new Event("mouseover", uiElement, event, x, y);
                    Executor.executeFunction(uiElement, f, jsEvent);
                }
                HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
                if (rcontext != null) {
                    rcontext.onMouseOver(uiElement, event);
                }
            }
            node = node.getParentModelNode();
        }
    }

    public void mouseOut(ModelNode node, MouseEvent event, int x, int y, ModelNode limit) {
        while (node != null) {
            if (node == limit) {
                break;
            }
            if (node instanceof HTMLAbstractUIElement) {
                HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
                uiElement.setMouseOver(false);
                Function f = uiElement.getOnmouseout();
                if (f != null) {
                    Event jsEvent = new Event("mouseout", uiElement, event, x, y);
                    Executor.executeFunction(uiElement, f, jsEvent);
                }
                HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
                if (rcontext != null) {
                    rcontext.onMouseOut(uiElement, event);
                }
            }
            node = node.getParentModelNode();
        }
    }

    public void click(HTMLSpanElementImpl button) {

    }

    // public void doubleClick(ModelNode node, MouseEvent event, int x, int y) {
    //       
    // if (node instanceof HTMLAbstractUIElement) {
    // HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
    // Function f = uiElement.getOndblclick();
    // if (f != null) {
    // Event jsEvent = new Event("dblclick", uiElement, event, x, y);
    // if (!Executor.executeFunction(uiElement, f, jsEvent)) { return; }
    // }
    // HtmlRendererContext rcontext = uiElement.getHtmlRendererContext();
    // if (rcontext != null) {
    // if (!rcontext.onDoubleClick(uiElement, event)) { return; }
    // }
    // }
    // ModelNode parent = node.getParentModelNode();
    // if (parent == null) { return; }
    // doubleClick(parent, event, x, y);
    // }

    // public void mouseDisarmed(ModelNode node, MouseEvent event) {
    // if (node instanceof HTMLLinkElementImpl) {
    // ((HTMLLinkElementImpl) node).getCurrentStyle().setOverlayColor(null);
    // return;
    // }
    // ModelNode parent = node.getParentModelNode();
    // if (parent == null) { return ; }
    // mouseDisarmed(parent, event);
    // }

    // public void mouseDown(ModelNode node, MouseEvent event, int x, int y) {
    // boolean pass = true;
    // if (node instanceof HTMLAbstractUIElement) {
    // HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
    // Function f = uiElement.getOnmousedown();
    // if (f != null) {
    // Event jsEvent = new Event("mousedown", uiElement, event, x, y);
    // pass = Executor.executeFunction(uiElement, f, jsEvent);
    // }
    // }
    // if (node instanceof HTMLLinkElementImpl) {
    // ((HTMLLinkElementImpl)
    // node).getCurrentStyle().setOverlayColor("#9090FF80");
    // return ;
    // }
    // if (!pass) { return ; }
    // ModelNode parent = node.getParentModelNode();
    // if (parent == null) { return; }
    // mouseDown(parent, event, x, y);
    // }

    // public void mouseUp(ModelNode node, MouseEvent event, int x, int y) {
    // boolean pass = true;
    // if (node instanceof HTMLAbstractUIElement) {
    // HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
    // Function f = uiElement.getOnmouseup();
    // if (f != null) {
    // Event jsEvent = new Event("mouseup", uiElement, event, x, y);
    // pass = Executor.executeFunction(uiElement, f, jsEvent);
    // }
    // }
    // if (node instanceof HTMLLinkElementImpl) {
    // ((HTMLLinkElementImpl) node).getCurrentStyle().setOverlayColor(null);
    // return;
    // }
    // if (!pass) { return; }
    // ModelNode parent = node.getParentModelNode();
    // if (parent == null) { return; }
    // mouseUp(parent, event, x, y);
    // }

    // public void press(ModelNode node, InputEvent event, int x, int y) {
    // if (node instanceof HTMLAbstractUIElement) {
    // HTMLAbstractUIElement uiElement = (HTMLAbstractUIElement) node;
    // Function f = uiElement.getOnclick();
    // if (f != null) {
    // Event jsEvent = new Event("click", uiElement, event, x, y);
    // if (!Executor.executeFunction(uiElement, f, jsEvent)) { return; }
    // }
    // }
    // if (node instanceof HTMLInputElementImpl) {
    // HTMLInputElementImpl hie = (HTMLInputElementImpl) node;
    // if (hie.isSubmitInput()) {
    // FormInput[] formInputs;
    // String name = hie.getName();
    // if (name == null) {
    // formInputs = null;
    // } else {
    // formInputs = new FormInput[] { new FormInput(name, hie.getValue()) };
    // }
    // hie.submitForm(formInputs);
    // } else if (hie.isImageInput()) {
    // String name = hie.getName();
    // String prefix = name == null ? "" : name + ".";
    // FormInput[] extraFormInputs = new FormInput[] { new FormInput(prefix +
    // "x", String.valueOf(x)), new FormInput(prefix + "y", String.valueOf(y))
    // };
    // hie.submitForm(extraFormInputs);
    // } else if (hie.isResetInput()) {
    // hie.resetForm();
    // }
    // }
    //      
    // }

    // public void change(ModelNode node) {
    // if (node instanceof HTMLSelectElementImpl) {
    // HTMLSelectElementImpl uiElement = (HTMLSelectElementImpl) node;
    // Function f = uiElement.getOnchange();
    // if (f != null) {
    // Event jsEvent = new Event("change", uiElement);
    // if (!Executor.executeFunction(uiElement, f, jsEvent)) { return ; }
    // }
    // }
    //    
    // }
}
