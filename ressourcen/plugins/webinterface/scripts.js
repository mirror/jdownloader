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
};
for (i = 0; i < field2.length; i++)
{
	field2[i].checked = false ;	
};
}

function samecheckall(field,id)
{
for (i = 0; i < field.length; i++){
	name=field[i].value.split("");

	
	if (name[0]==id.value)
	{
	
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
	};
};
for (i = 0; i < field2.length; i++){
if ( field2[i].value==chkid )
{
	field2[i].checked=allchecked;
};
};
}

function validateandsubmit(msg,form)
{
conf=window.confirm(msg);
if (conf == false)
form.value="";  
}