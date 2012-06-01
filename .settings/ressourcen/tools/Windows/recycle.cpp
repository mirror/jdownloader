#include <windows.h>
#include <shellapi.h>
#include <stdio.h>
#include <string.h>
#include <iostream>
#include <stdlib.h>
#include <ctype.h>

//Source for recycle.exe - doesn't have to be distributed with jd!

using namespace std;

int main (int argc, char *argv[])
{
        
if(argc!=2){
return 0;
};

strcat (argv[1],"\0");

SHFILEOPSTRUCTA fop;

// initialize all data required for the copy
fop.hwnd = NULL;
fop.wFunc = FO_DELETE;
fop.pFrom = argv[1]; //src is a char array should be valid but

fop.fFlags = FOF_SILENT | FOF_NOCONFIRMATION | FOF_NOERRORUI | FOF_ALLOWUNDO;

if(!SHFileOperation(&fop)) //performs copy only if path exist
{
    return 1;
}

return 0;

}
