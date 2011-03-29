package org.jdownloader.extensions.jdfeedme;

import java.text.SimpleDateFormat;
import java.util.Date;

import jd.controlling.JDLogger;
import jd.utils.JDUtilities;

import org.jdownloader.extensions.jdfeedme.posts.JDFeedMePost;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RssParser {

    private JDFeedMeFeed feed;

    private enum ContentMode {
        MODE_UNKNOWN, MODE_RSS, MODE_ATOM
    }

    private ContentMode contentMode;
    private NodeList    items;
    private int         currentItem;

    public RssParser(JDFeedMeFeed feed) {
        this.feed = feed;
        this.contentMode = ContentMode.MODE_UNKNOWN;
        this.items = null;
        this.currentItem = 0;
    }

    public void parseContent(String content) throws Exception {
        // first fix potential errors in the xml string
        content = fixXMLErrors(content);

        // parse the xml
        Document doc = JDUtilities.parseXmlString(content, false);
        if (doc == null) {
            JDLogger.getLogger().severe("JDFeedMe error: malformed xml for feed: " + feed.getAddress());
            throw new Exception("Malformed XML");
        }

        // check which type of feed we have

        // check for RSS mode
        items = doc.getElementsByTagName("item");
        if (items.getLength() > 0) {
            contentMode = ContentMode.MODE_RSS;
            return;
        }

        // check for Atom mode
        items = doc.getElementsByTagName("entry");
        if (items.getLength() > 0) {
            contentMode = ContentMode.MODE_ATOM;
            return;
        }

        // unknown mode
        contentMode = ContentMode.MODE_UNKNOWN;
    }

    public String fixXMLErrors(String content) {
        // potential bug in some blog engines is to add stuff after the root tag
        // end
        String closing_tag = "</rss>";
        int closing_tag_index = content.lastIndexOf(closing_tag);
        if (closing_tag_index == -1) {
            closing_tag = "</feed>";
            closing_tag_index = content.lastIndexOf(closing_tag);
        }
        if (closing_tag_index != -1) {
            content = content.substring(0, closing_tag_index + closing_tag.length());
        }

        return content;
    }

    /**
     * @return the next available post in the feed, null if there are no more
     */
    public JDFeedMePost getPost() {
        // make sure we have another item
        if (contentMode == ContentMode.MODE_UNKNOWN) return null;
        if (items == null) return null;
        if (currentItem >= items.getLength()) return null;

        // get the item
        Node item = items.item(currentItem);
        currentItem++;
        JDFeedMePost post = new JDFeedMePost();

        // go over the fields of this item
        NodeList fields = item.getChildNodes();
        for (int j = 0; j < fields.getLength(); j++) {
            Node field = fields.item(j);

            // handle field according to mode
            if (contentMode == ContentMode.MODE_RSS) handleFieldModeRss(field, post);
            if (contentMode == ContentMode.MODE_ATOM) handleFieldModeAtom(field, post);
        }

        return post;
    }

    private void handleFieldModeRss(Node field, JDFeedMePost post) {
        String field_name = field.getNodeName();

        // standard rss fields

        if (field_name.equalsIgnoreCase("title")) {
            post.setTitle(field.getTextContent().trim());
            return;
        }

        if (field_name.equalsIgnoreCase("pubDate")) {
            post.setTimestamp(field.getTextContent().trim());
            return;
        }

        if (field_name.equalsIgnoreCase("link")) {
            post.setLink(field.getTextContent().trim());
            return;
        }

        if (field_name.equalsIgnoreCase("description")) {
            post.setDescription(field.getTextContent().trim());
            return;
        }

        // custom rss fields (check jdfeedme.com for more info)
        if (field_name.equalsIgnoreCase("file")) {
            post.addFile(field.getTextContent().trim());
            return;
        }
    }

    private void handleFieldModeAtom(Node field, JDFeedMePost post) {
        String field_name = field.getNodeName();

        // standard atom fields
        if (field_name.equalsIgnoreCase("title")) {
            post.setTitle(field.getTextContent().trim());
            return;
        }

        if (field_name.equalsIgnoreCase("published")) {
            Date date = parseAtomTimestamp(field.getTextContent().trim());
            if (date == null) return;
            post.setTimestamp(new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z").format(date));
            return;
        }

        if (field_name.equalsIgnoreCase("updated")) {
            if (post.hasValidTimestamp()) return;
            Date date = parseAtomTimestamp(field.getTextContent().trim());
            if (date == null) return;
            post.setTimestamp(new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z").format(date));
            return;
        }

        if (field_name.equalsIgnoreCase("link")) {
            String rel = JDUtilities.getAttribute(field, "rel").trim();
            String href = JDUtilities.getAttribute(field, "href").trim();
            if ((rel == null) || (href == null)) return;
            if (rel.equalsIgnoreCase("alternate")) post.setLink(href);
            return;
        }

        if (field_name.equalsIgnoreCase("content")) {
            post.setDescription(field.getTextContent().trim());
            return;
        }
    }

    // 2010-10-28T11:04:00.000-07:00
    // 2003-12-13T18:30:02Z
    private Date parseAtomTimestamp(String atom_timestamp) {
        Date date = new Date();

        try {
            if (atom_timestamp.endsWith("Z")) {
                try {
                    SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    date = s.parse(atom_timestamp);
                } catch (java.text.ParseException pe) {
                    SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
                    s.setLenient(true);
                    date = s.parse(atom_timestamp);
                }
                return date;
            }

            String firstpart = atom_timestamp.substring(0, atom_timestamp.lastIndexOf('-'));
            String secondpart = atom_timestamp.substring(atom_timestamp.lastIndexOf('-'));

            secondpart = secondpart.substring(0, secondpart.indexOf(':')) + secondpart.substring(secondpart.indexOf(':') + 1);
            atom_timestamp = firstpart + secondpart;
            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
            try {
                date = s.parse(atom_timestamp);
            } catch (java.text.ParseException pe) {
                s = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
                s.setLenient(true);
                date = s.parse(atom_timestamp);
            }
            return date;
        } catch (Exception e) {
            return null;
        }
    }

}
