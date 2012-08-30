package org.jdownloader.extensions.streaming.mediaarchive;

import static org.fourthline.cling.model.XMLUtil.appendNewElement;
import static org.fourthline.cling.model.XMLUtil.appendNewElementIfNotNull;

import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DescMeta;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.item.Item;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ExtDIDLParser extends DIDLParser {
    protected void generateItem(Item item, Document descriptor, Element parent) {

        if (item.getTitle() == null) { throw new RuntimeException("Missing 'dc:title' element for item: " + item.getId()); }
        if (item.getClazz() == null) { throw new RuntimeException("Missing 'upnp:class' element for item: " + item.getId()); }

        Element itemElement = appendNewElement(descriptor, parent, "item");

        if (item.getId() == null) throw new NullPointerException("Missing id on item: " + item);
        itemElement.setAttribute("id", item.getId());

        if (item.getParentID() == null) throw new NullPointerException("Missing parent id on item: " + item);
        itemElement.setAttribute("parentID", item.getParentID());

        if (item.getRefID() != null) itemElement.setAttribute("refID", item.getRefID());
        itemElement.setAttribute("restricted", Boolean.toString(item.isRestricted()));

        appendNewElementIfNotNull(descriptor, itemElement, "dc:title", item.getTitle(), DIDLObject.Property.DC.NAMESPACE.URI);

        appendNewElementIfNotNull(descriptor, itemElement, "dc:creator", item.getCreator(), DIDLObject.Property.DC.NAMESPACE.URI);

        appendNewElementIfNotNull(descriptor, itemElement, "upnp:writeStatus", item.getWriteStatus(), DIDLObject.Property.UPNP.NAMESPACE.URI);

        appendClass(descriptor, itemElement, item.getClazz(), "upnp:class", false);

        appendProperties(descriptor, itemElement, item, "upnp", DIDLObject.Property.UPNP.NAMESPACE.class, DIDLObject.Property.UPNP.NAMESPACE.URI);
        appendProperties(descriptor, itemElement, item, "dc", DIDLObject.Property.DC.NAMESPACE.class, DIDLObject.Property.DC.NAMESPACE.URI);

        for (Res resource : item.getResources()) {
            if (resource == null) continue;
            generateResource(resource, descriptor, itemElement);
        }

        for (DescMeta descMeta : item.getDescMetadata()) {
            if (descMeta == null) continue;
            generateDescMetadata(descMeta, descriptor, itemElement);
        }
        appendProperties(descriptor, itemElement, item, "sec", SEC.NAMESPACE.class, SEC.NAMESPACE.URI);

    }
}
