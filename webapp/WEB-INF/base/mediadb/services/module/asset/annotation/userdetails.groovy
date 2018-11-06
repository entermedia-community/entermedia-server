if( user.get("defaultcolor") == null )
{
	String[] availableColors = ["#00FFFF",
		"#00FF00","#EE82EE","#000080","#000000","#FFFF00","#B8860B","#B8860B","#723421","#523421","#323421",
		"#123421", "#fff120", "#abf000", "#ff4300"];

	char first = user.getScreenName().toLowerCase().charAt(0);
	if( Character.isLetter(first) )
	{
		int index = first - 97;
		index = index /2;
		String color = availableColors[index];
		user.setProperty("defaultcolor",color);
	}
	else
	{
		user.setProperty("defaultcolor","#00FF00");
	}
}
