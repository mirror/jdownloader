package org.jdownloader.downloader.hds;


public class F4vBox {
    // Each box is identified with a 32-bit type. For most boxes, this 32-bit type doubles as a human-readable four- character ASCII code or
    // 4CC, such as 'moov' (0x6D6F6F76) and 'mdat' (0x6D646174).
    public static final int MDAT = 0x6D646174;
    public static final int MOOV = 0x6D6F6F76;

    // 2.11.1 Fragment Random Access box
    // Box type: 'afra'
    // Container: File
    // Mandatory: Yes for HTTP streaming support with F4V fragments, otherwise no.
    // Quantity: One per fragment for HTTP streaming support with F4V fragments, otherwise zero.
    // The Fragment Random Access (afra) box provides random access information to one or more fragments.
    // For HTTP streaming support with F4V fragments, the F4V file can contain one afra box for each fragment. The afra
    // box shall be located before the fragment's Media Data (mdat) and Movie Fragment (moof) boxes. The afra box can
    // be used to seek to the exact point in the F4V file that contains the closest random access sample to a given time.
    // The afra box is associated with a given fragment (here referred to as “the associated fragment”). The afra box also
    // provides random access to information in other fragments in the same segment or different segments.
    // The afra box contains arrays of entries. Each entry contains the location and the presentation time of a random
    // access sample. If a random access sample is not within the associated fragment, the entry also provides the
    // following information:
    // - Segment identifying information
    // - Fragment identifying information
    // - The byte offset from the beginning of the containing segment to the ‘afra’ box associated with this random
    // access point
    // - The byte offset from the associated ‘afra’ box to the sample
    // Note: Every random access sample in a fragment does not necessarily have an array entry.
    // The absence of the afra box does not mean that all the samples are sync samples. Random access information in the
    // 'trun', 'traf', and 'trex' are set appropriately regardless of the presence of this box.
    public static final int AFRA = 0x61667261;

}
