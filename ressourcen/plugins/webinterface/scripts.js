var alreadyCleaned = false;

function checkall(field,field2)
{
	for (i = 0; i < field.length; i++)
	{
		field[i].checked = true ;
	};
	for (i = 0; i < field2.length; i++)
	{
		field2[i].checked = true ;
	};
}

function uncheckall(field,field2)
{
	for (i = 0; i < field.length; i++)
	{
		field[i].checked = false ;
	}
	for (i = 0; i < field2.length; i++)
	{
		field2[i].checked = false ;
	}
}

function samecheckall(field,id)
{
	for (i = 0; i < field.length; i++) {
		name=field[i].value.split("");

		if (name[0]==id.value) {
			field[i].checked = id.checked;
		}
	};
}

function areallsamechecked(id,field,field2)
{
	var tempid=id.value.split("");
	var chkid=tempid[0];
	var allchecked=true;

	for (i = 0; i < field.length; i++){
		name=field[i].value.split("");
		if (name[0]==chkid)
		{
			if (field[i].checked==false) allchecked=false;
		}
	}
	for (i = 0; i < field2.length; i++){
		if ( field2[i].value==chkid )
		{
			field2[i].checked=allchecked;
		}
	}
}

function check_adder(field)
{
	if (field.value=="add")
	{
		field.form.action="/index.tmpl";
	}
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
	setTimeout("window.location.href=window.location.href", interval);
}

/*popup code*/
var pop = null;

function popup(obj,w,h) {
	var url = (obj.getAttribute) ? obj.getAttribute('href') : obj.href;
	if (!url) return true;
	w = (w) ? w += 20 : 150;  // 150px*150px is the default size
	h = (h) ? h += 25 : 150;
	var args = 'width='+w+',height='+h+',resizable,scrollbars';
	popdown();
	pop = window.open(url,'',args);
	return (pop) ? false : true;
}

function popdown() {
	if (pop && !pop.closed) pop.close();
}

//window.onunload = popdown;
//window.onfocus = popdown;
