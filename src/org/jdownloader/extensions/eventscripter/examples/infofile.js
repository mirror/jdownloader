/*
    Event Script: writeInfoFile
 
                  Creates an Info-File into the DL-Folder with detailed information
                  
    Version: 0.10
 
 Requirements:
    Trigger "Package Finished"

 Tested:
     JD2 on Windows7-32bit Platform and jre8 (see path-creation code) 
 */
//---------- Global declarations -----------------
var bWriteFile = false; // pessimistic approach: no file-writing at start, if
                        // situation ok, then set it "true"
var sInfoFilePath = ""; // the target file, if already available, then
                        // APPENDING, else creation
var myPackage = package;
var aParts = myPackage.getDownloadLinks();
var aArchives = package.getArchives();
// runtime vars
var sText = ""; // temporarily used!

// ---------- ANALYZING SITUATION ----------
if (myPackage.isFinished() == true) {
    // no further analysis due to Trigger "Package Finished" :-)
    bWriteFile = true;
}

if (bWriteFile == true) {
    // ---------- INFO.FILE naming -----------
    sInfoFilePath = myPackage.getDownloadFolder() + "/" + myPackage.getName() + ".info"; // <packageFolder>/<packageName>.info
    if (sInfoFilePath.length > 255) {
        // path to long! -> shorten!!
        sInfoFilePath = myPackage.getDownloadFolder() + "/" + "jd.info"; // <packageFolder>/jd.info
    }

    // ---------- Package Infos -----------
    sText = ""
    sText += "**********************************" + "\r\n"
    sText += "*          P A C K A G E         *" + "\r\n"
    sText += "**********************************" + "\r\n"
    sText += "Package.Name              : " + myPackage.getName() + "\r\n" + "Package.DownloadFolder    : " + myPackage.getDownloadFolder() + "\r\n" + "Package.Total             : " + myPackage.getBytesTotal() + "\r\n" + "Package.Loaded            : " + myPackage.getBytesLoaded() + "\r\n" + "Package.Finished          : " + myPackage.isFinished() + "\r\n";
    if (myPackage.getComment() != undefined) {
        sText += "Package.Comment           : " + myPackage.getComment() + "\r\n";
    }
    writeFile(sInfoFilePath, sText, true); /* Write [into] a text file */

    // ---------- Part Infos -----------
    sText = "\r\n"
    sText += "**********************************" + "\r\n"
    sText += "*            P A R T S           *" + "\r\n"
    sText += "**********************************" + "\r\n"
    writeFile(sInfoFilePath, sText, true); /* Write [into] a text file */
    for (var i = 0; i < aParts.length; i++) {
        sText = "Part.Name                 : " + aParts[i].getName() + "\r\n" + "Part.Status               : " + aParts[i].getStatus() + "\r\n" + "Part.Enabled              : " + aParts[i].isEnabled() + "\r\n" + "Part.Finished             : " + aParts[i].isFinished() + "\r\n" + "Part.Skipped              : " + aParts[i].isSkipped() + "\r\n" + "Part.ExtractionStatus     : " + aParts[i].getExtractionStatus() + "\r\n" + "Part.Total                : " + aParts[i].getBytesTotal() + "\r\n"
                + "Part.Loaded               : " + aParts[i].getBytesLoaded() + "\r\n";
        if (aParts[i].getUrl() != undefined) {
            sText += "Part.URL                  : " + aParts[i].getUrl() + "\r\n";
        }
        if (aParts[i].getComment() != undefined) {
            sText += "Part.Comment              : " + aParts[i].getComment() + "\r\n";
        }
        var myPart = aParts[i].getArchive();
        if (myPart != undefined) {
            if (myPart.getUsedPassword() != undefined) {
                sText += "Part.UsedPassword         : " + myPart.getUsedPassword() + "\r\n";
            }
        }
        sText += "--------------------------:-" + "\r\n"
        writeFile(sInfoFilePath, sText, true);
    }
    writeFile(sInfoFilePath, sText, true); /* Write [into] a text file */

    // ---------- Archive Infos -----------
    sText = "\r\n"
    sText += "**********************************" + "\r\n"
    sText += "*        A R C H I V E S         *" + "\r\n"
    sText += "**********************************" + "\r\n"
    writeFile(sInfoFilePath, sText, true);
    for (var i = 0; i < aArchives.length; i++) {
        sText = "Archive.Name                 : " + aArchives[i].getName() + "\r\n" + "Archive.ArchiveTyp           : " + aArchives[i].getArchiveType() + "\r\n";

        if (aArchives[i].getInfo() != undefined) {
            sText += "Package.Archive.Info         : ";
            sText += JSON.stringify(aArchives[i].getInfo(), null, 2) + "\r\n";
        }
        sText += "--------------------------:-" + "\r\n"
        writeFile(sInfoFilePath, sText, true);
    }

}