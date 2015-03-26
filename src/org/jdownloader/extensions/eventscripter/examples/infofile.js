/*
    Event Script: writeInfoFile
 
                  Creates an Info-File into the DL-Folder with detailed information
                  
    Version: 0.21
 
 Requirements:
    Trigger "Package Finished"

 Tested:
     JD2 on Windows7-32bit Platform and jre8 (see path-creation code at the end of this script) 
 */


//---------- Global declarations -----------------
var bWriteFile = false; //pessimistic approach: no file-writing at start, if situation ok, then set it "true"
var sInfoFilePath = ""; //the target file, if already available, then APPENDING, else creation
var sText = ""; //will be filled with content (or not :-)
var sInfoFileType = ".info" //set path creation at the end of the script
var iContent = 0; //setting bit 1 if pwd is available, setting bit 2 if comments available //if not set, no info file
var myPackage = package;
var aParts = myPackage.getDownloadLinks();
var aArchives = package.getArchives();






//---------- ANALYZING SITUATION ----------
if (myPackage.isFinished() == true) {
    //no further analysis due to Trigger "Package Finished" :-)
    bWriteFile = true;
}




//---------- Building Info-Text -----------
if (bWriteFile == true) {
    sText += "**********************************" + "\r\n"
    sText += "*          P A C K A G E         *" + "\r\n"
    sText += "**********************************" + "\r\n"
    sText += "Package.Name              : " + myPackage.getName() + "\r\n" +
        "Package.DownloadFolder    : " + myPackage.getDownloadFolder() + "\r\n" +
        "Package.Total             : " + myPackage.getBytesTotal() + "\r\n" +
        "Package.Loaded            : " + myPackage.getBytesLoaded() + "\r\n" +
        "Package.Finished          : " + myPackage.isFinished() + "\r\n";
    if (myPackage.getComment() != undefined) {
        sText += "Package.Comment           : " + myPackage.getComment() + "\r\n";
        iContent |= 2;
    }

    sText += "\r\n"
    sText += "**********************************" + "\r\n"
    sText += "*            P A R T S           *" + "\r\n"
    sText += "**********************************" + "\r\n"
    for (var i = 0; i < aParts.length; i++) {
        sText += "Part.#                    : " + i + "\r\n" +
            "Part.Name                 : " + aParts[i].getName() + "\r\n" +
            "Part.Status               : " + aParts[i].getStatus() + "\r\n" +
            "Part.Enabled              : " + aParts[i].isEnabled() + "\r\n" +
            "Part.Finished             : " + aParts[i].isFinished() + "\r\n" +
            "Part.Skipped              : " + aParts[i].isSkipped() + "\r\n" +
            "Part.ExtractionStatus     : " + aParts[i].getExtractionStatus() + "\r\n" +
            "Part.Total                : " + aParts[i].getBytesTotal() + "\r\n" +
            "Part.Loaded               : " + aParts[i].getBytesLoaded() + "\r\n";
        if (aParts[i].getUrl() != undefined) {
            sText += "Part.URL                  : " + aParts[i].getUrl() + "\r\n";
        }
        if (aParts[i].getComment() != undefined) {
            sText += "Part.Comment              : " + aParts[i].getComment() + "\r\n";
            iContent |= 2;
        }
        var myArchive = aParts[i].getArchive();
        if (myArchive != undefined) {
            if (myArchive.getUsedPassword() != undefined) {
                sText += "Part.UsedPassword         : " + myArchive.getUsedPassword() + "\r\n";
                iContent |= 1;
            }
        }
        sText += "--------------------------:-" + "\r\n"
    }

    sText += "\r\n"
    sText += "**********************************" + "\r\n"
    sText += "*        A R C H I V E S         *" + "\r\n"
    sText += "**********************************" + "\r\n"
    for (var i = 0; i < aArchives.length; i++) {
        sText += "Archive.#                    : " + i + "\r\n" +
            "Archive.Name                 : " + aArchives[i].getName() + "\r\n" +
            "Archive.ArchiveTyp           : " + aArchives[i].getArchiveType() + "\r\n";

        if (aArchives[i].getInfo() != undefined) {
            sText += "Package.Archive.Info         : ";
            sText += JSON.stringify(aArchives[i].getInfo(), null, 2) + "\r\n";
        }
        sText += "--------------------------:-" + "\r\n"
    }
}




//---------- INFO.FILE naming and writing ----------- 
if (bWriteFile == true && iContent > 0) { //comment out or set "iContent >= 0" if info-file should be written always
    //- path creation
    sInfoFileType = "." + iContent + sInfoFileType //add content-type hint to filetype
    sInfoFilePath = myPackage.getDownloadFolder() + "/" + myPackage.getName() + sInfoFileType; //<packageFolder>/<packageName>.<iContent>.info
    if (sInfoFilePath.length > 255) { //path to long! -> shorten!!
        sInfoFilePath = myPackage.getDownloadFolder() + "/jd" + sInfoFileType; //<packageFolder>/jd.<iContent>.info
    }
    //- writing
    try {
        writeFile(sInfoFilePath, sText, true);
    } catch (e) {
        //no error handling implemented !    
    }
}