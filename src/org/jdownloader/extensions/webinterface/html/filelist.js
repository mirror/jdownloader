var packContainer, cookieArray, showArray, cookieName;

function initiate(useCookieName) {
	var cookieArrayTemp;
	showArray = new Array();
	cookieName = useCookieName;

	if(document.cookie) {
		cookieArray = document.cookie.split(";");
		cookieArrayTemp = new Array();

		for(i in cookieArray){
			cookieArrayTemp[cookieArray[i].split("=")[0].replace(/ /g,"")]=cookieArray[i].split("=")[1].replace(/ /g,"");
		}
	}

	//get show-value(true, false) for each package
	cookieArray=(document.cookie.indexOf(cookieName+"=")>=0)?cookieArrayTemp[cookieName].split(","):new Array();

	//contains all packages
	packContainer=document.getElementById("packageContainer");

	//var cookieCount=0;
	var currPackageNum = -1;

	// for all table-rows (packages and downloads)
	for(var i=0; i<packContainer.getElementsByTagName("tr").length; i++){

		var currRow = packContainer.getElementsByTagName("tr")[i];

		// row is package
		if (startsWith(currRow.className, "package")) {
			currPackageNum++;
			currRow.id = "package" + currPackageNum;

			var expander = document.createElement("span");
			expander.id = "expander" + currPackageNum;
			expander.className = "symbol";

			// plus- or minus-pic
			expander.style.backgroundImage = (cookieArray.length>currPackageNum)? ((cookieArray[currPackageNum]=="true")? "url(img/minus.gif)" : "url(img/plus.gif)") : "url(img/plus.gif)";

			expander.onclick= new Function("expanderClick("+currPackageNum+");");

			currRow.getElementsByTagName('td')[1].insertBefore(expander, currRow.getElementsByTagName('td')[1].firstChild);

			showArray[currPackageNum] = (cookieArray.length>currPackageNum)? ((cookieArray[currPackageNum]=="true")? "true" : "false" ) : "false" ;

		} else if (startsWith(currRow.className, "download")) {
		//row is a download
			var showVal = (testProperty('display', 'table-row'))? 'table-row' : 'block';
			var displayVal = (cookieArray.length>currPackageNum)? ((cookieArray[currPackageNum]=="true")? showVal : "none") : "none";
			currRow.style.display = displayVal;
		}

  }

}

function expanderClick(currPackageNum) {
	showhide(currPackageNum);
	saveListsVisibility();
}

function startsWith(string, text) {
	var pos = string.indexOf(text);
	if (pos == 0) {
		return true;
	} else {
		return false;
	}
}

function showhide(packageNum) {

	var package = document.getElementById("package" + packageNum);
	var expander = document.getElementById("expander" + packageNum);

	var showVal = (testProperty('display', 'table-row'))? 'table-row' : 'block';
	var displayVal = (showArray[packageNum]=="false")? showVal : "none";

	var nextRow = package.nextSibling;

	while (nextRow != null) {
		// accept only tr-nodes
		if (nextRow.tagName == null || nextRow.tagName.toLowerCase() != 'tr') {
			nextRow = nextRow.nextSibling;
			continue;
		}

		// reached next package?
		if (startsWith(nextRow.className, "package")) {
			break;
		}

		nextRow.style.display = displayVal;
		nextRow = nextRow.nextSibling;
	}

	expander.style.backgroundImage = (showArray[packageNum]=="false")? "url(img/minus.gif)" : "url(img/plus.gif)";
	showArray[packageNum] = (showArray[packageNum]=="false")? "true" : "false";

}



function saveListsVisibility() {
// save the visibility of the package-lists in a cookie

	cookieArray=new Array();
	var packNum = -1;

	for(var i=0; i<packContainer.getElementsByTagName("tr").length; i++){

		var currRow = packContainer.getElementsByTagName("tr")[i];

		if (startsWith(currRow.className, "package")) {
			packNum++;
			var newShow = (showArray[packNum]=="true");
			cookieArray[cookieArray.length] = newShow;
			showArray[packNum] = ""+newShow+"";
		}
	}

  document.cookie=cookieName+"="+cookieArray.join(",")+";expires="+new Date(new Date().getTime() + 365*24*60*60*1000).toGMTString();

}

/**
 * Tests, if the IE supports the property-Value
 */
function testProperty(prop, value) {
	var tempEl = document.createElement("div");

	if(typeof tempEl.style[prop] != 'undefined') {
		try {
			tempEl.style[prop] = value;
			return true;
		}
		catch(e) {
			return false;
		}
	} else {
		return false;
	}
}