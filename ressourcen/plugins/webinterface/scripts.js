var alreadyCleaned = false;
var autoPageReloadActive = true;
var IE = (document.all) ? true : false;


function pageLoaded() {
	if(document.cookie) {
		var cookieArray = document.cookie.split(";");

		for(i in cookieArray){
			var cookieName = cookieArray[i].split("=")[0].replace(/ /g,"");
			var cookieValue = cookieArray[i].split("=")[1].replace(/ /g,"");

			/* show the area of the page that has been shown on the last visit */
			if (cookieName=="scroll") {
				window.scrollTo(cookieValue.split(":")[0], cookieValue.split(":")[1]);
			}
		}
	}
}

function checkall(field,field2)
{
	/* field.length can be null when
	   there exist only one package
	   with only one file */
	formElementChanged();

	if (field != null) {
		if (field.length != null) {
			for (i = 0; i < field.length; i++) {
				field[i].checked = true ;
			}
		} else {
			field.checked = true;
		}
	}

	if (field2 != null) {
		if (field2.length != null) {
			for (i = 0; i < field2.length; i++) {
				field2[i].checked = true ;
			}
		} else {
			field2.checked = true;
		}
	}
}

function uncheckall(field,field2)
{
	/* field.length can be null when
	   there exist only one package
	   with only one file */
	formElementChanged();

	if (field != null) {
		if (field.length != null) {
			for (i = 0; i < field.length; i++) {
				field[i].checked = false ;
			}
		} else {
			field.checked = false;
		}
	}

	if (field2 != null) {
		if (field2.length != null) {
			for (i = 0; i < field2.length; i++) {
				field2[i].checked = false ;
			}
		} else {
			field2.checked = false;
		}
	}
}

function samecheckall(field,id)
{
	formElementChanged();

	/* field.length can be null when
	   there exist only one package
	   with only one file */
	if (field != null) {
		if (field.length != null) {
			for (i = 0; i < field.length; i++) {
				var name=field[i].value.split(" ");

				if (name[0]==id.value) {
					field[i].checked = id.checked;
				}
			};
		} else {
			field.checked = id.checked;
		}
	}

	areallchecked(field);
}

function areallsamechecked(id,field,field2)
{
	formElementChanged();

	/* field.length can be null when
	   there exist only one package
	   with only one file */
	var tempid=id.value.split(" ");
	var chkid=tempid[0];
	var allchecked=true;

	if (field != null) {
		if (field.length != null) {
			for (i = 0; i < field.length; i++){
				var name=field[i].value.split(" ");
				if (name[0]==chkid)
				{
					if (field[i].checked==false) allchecked=false;
				}
			}
		} else {
			if (field.checked==false) allchecked=false;
		}
	}

	if (field2 != null) {
		if (field2.length != null) {
			for (i = 0; i < field2.length; i++){
				if ( field2[i].value==chkid )
				{
					field2[i].checked=allchecked;
				}
			}
		} else {
			field2.checked=allchecked;
		}
	}

	areallchecked(field);
}

function areallchecked(field)
{
	if (field != null) {
		if (field.length != null && field.length > 0) {
			var allchecked = true;

			// is any field (single download) unchecked?
			for (i = 0; i < field.length; i++){
				if (field[i].checked == false) {
					allchecked = false;
				}
			}

			document.jd.checkallbox.checked = allchecked;

		} else {
			document.jd.checkallbox.checked = field.checked;
		}
	}
}

function adderSubmit(field)
{
	if (field.value=="add") {
		field.form.action="/index.tmpl";
	}

	submitForm('jdForm',field.form.action,'do','Submit')
}

function validateandsubmit(msg,jdForm,dest,val)
{
/* msg anzeigen und bei nein wird vom button der value gelöscht, bei ja zu dest submited*/
	conf=window.confirm(msg);
	if (conf == true)
	{
		submitForm(jdForm, dest, "do", val);
	}
}

/*
 * submit the form to the dest with the val as do-Action
 */
function submitForm(jdForm, dest, fieldname, val) {
		var hfield = document.createElement("input");
		hfield.type = "hidden";
		hfield.name = fieldname;
		hfield.value = val;

		var formular = document.getElementById(jdForm);
		formular.insertBefore(hfield,formular.firstChild);

		formular.action=dest;
		formular.submit();
}

/*
 * Switch between Auto-Reconnect on/off
 */
function switchAutoReconnect(jdForm, dest, currentStatus) {
	var newChecked = (currentStatus=='checked')? false : true;
	document.getElementById('autoreconnect').checked = newChecked;
	submitForm(jdForm, dest, 'do', 'submit');
}

function clean(whattoclean)
{
/*delete the value of whattoclean
  if it was already cleaned nothin happens*/
	if (alreadyCleaned==false) {
		whattoclean.value="";
		alreadyCleaned = true;
	}
}

function forwardto(wohin) {
/*zu seite 'wohin' weiterleiten*/
	window.location = wohin ;
}

function startPageReload(interval) {
	if (interval != 0) {
		setTimeout("reloadPage()", interval*1000);
	}
}

function reloadPage() {
	if (autoPageReloadActive==true) {
		//save coordinates of visible area in a cookie
		var cookieExpire = new Date(new Date().getTime() + 365*24*60*60*1000).toGMTString();

		var diffY, diffX;
		if (IE) { diffY = document.body.scrollTop; diffX = document.body.scrollLeft; }
		else { diffY = window.pageYOffset; diffX = window.pageXOffset; }

		document.cookie = "scroll="+diffX+":"+diffY+";expires="+cookieExpire;

		//reload
		window.location.replace( window.location );
	}
}

function formElementChanged() {
//deactivateAutoPageReload
	autoPageReloadActive=false;

	var notify = document.getElementById('deactivatedAutoReload');

	if (notify != null) {
		notify.style.display = "block";
	}
}

/*popup code*/
var pop = null;

function popup(obj,w,h) {
	var url = (obj.getAttribute) ? obj.getAttribute('href') : obj.href;
	if (!url) return true;
	w = (w) ? w += 20 : 150;  // 150px*150px is the default size
	h = (h) ? h += 25 : 150;
	var args = 'width='+w+',height='+h+',resizable,scrollbars';
	//var args = 'width='+w+',height='+h+',resizable,scrollbars,location,menubar,status';
	popdown();
	pop = window.open(url,'',args);
	return (pop) ? false : true;
}

function popdown() {
	if (pop && !pop.closed) pop.close();
}

function resizeInfoWindow(tableid, currWidth) {
	var table = document.getElementById(tableid);

	var newheight = table.getElementsByTagName('tr').length * 20 + 75;
	var maxheight = screen.availHeight;
	newheight = (newheight > maxheight)? maxheight : newheight;
	window.resizeTo(currWidth, newheight);
}

//window.onunload = popdown;
//window.onfocus = popdown;

function allowChars(id, chars) {
	var obj = document.getElementById(id);

	if(obj.type == "text" || obj.type == "textarea") {

		obj.timer = "";
		obj.chars = chars;

		controllFunc = function() {
			//var self = this;
			var self = obj;
			controll = function() {
				//check each char
				for(var t='',x=0; x<self.value.length; ++x) {
					if(self.chars.indexOf(self.value.charAt(x))>-1) {
						t += self.value.charAt(x);
					}
				}
				self.value = t;
			};
// 			this.timer = setTimeout(controll,1);
		};

		clearFunc = function() {
			clearTimeout(this.timer);
		};

		// add EventListener
		if (obj.addEventListener) {
			obj.addEventListener ("keypress", controllFunc, false);
			obj.addEventListener ("keydown", controllFunc, false);
			obj.addEventListener ("keyup", clearFunc, false);

		} else if (obj.attachEvent) {
		// IE
			obj.attachEvent("onkeypress", controllFunc);
			obj.attachEvent("onkeydown", controllFunc);
			obj.attachEvent("onkeyup", clearFunc);
		}
	}
}