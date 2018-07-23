package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.plugins.LinkInfo;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ArchiveExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.AudioExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.DocumentExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.HashExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ImageExtensions;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;

public class LinkInfoSandbox {
    private final LinkInfo linkInfo;

    public LinkInfoSandbox(LinkInfo linkInfo) {
        this.linkInfo = linkInfo;
    }

    public int getPartNum() {
        return linkInfo.getPartNum();
    }

    public String name() {
        return linkInfo.getExtension().name();
    }

    public String getDesc() {
        return linkInfo.getExtension().getDesc();
    }

    public String getGroup() {
        final ExtensionsFilterInterface extension = linkInfo.getExtension();
        if (extension.isSameExtensionGroup(HashExtensions.MD5)) {
            return "HashExtensions";
        } else if (extension.isSameExtensionGroup(DocumentExtensions.CSV)) {
            return "DocumentExtensions";
        } else if (extension.isSameExtensionGroup(AudioExtensions.AA)) {
            return "AudioExtensions";
        } else if (extension.isSameExtensionGroup(VideoExtensions.ASF)) {
            return "VideoExtensions";
        } else if (extension.isSameExtensionGroup(ArchiveExtensions.ACE)) {
            return "ArchiveExtensions";
        } else if (extension.isSameExtensionGroup(ImageExtensions.BMP)) {
            return "ImageExtensions";
        } else {
            return "Others";
        }
    }
}
