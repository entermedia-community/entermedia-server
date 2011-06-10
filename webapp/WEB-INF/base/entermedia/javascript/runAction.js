function runAction( inAction, aForm)
{
	if (aForm == null)
	{
		aForm = "wizard";
	}
	var theForm = document.forms[aForm];
	theForm.elements["oe-action"].value = inAction;
	theForm.submit();
}