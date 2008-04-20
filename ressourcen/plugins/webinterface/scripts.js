function checkAll(field,field2)
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

function uncheckAll(field,field2)
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

function samecheckAll(field,id)
{
for (i = 0; i < field.length; i++){
	name=field[i].value.split("");

	
	if (name[0]==id.value)
	{
	
	field[i].checked = id.checked;

	}
	};
}