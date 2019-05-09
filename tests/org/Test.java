package org;

public class Test {

	public static void main(String p[])
	{
		String path = "Collections/Pixel/Photos RC/INFORMATION/2019/2019-02-13 Toronto/PH RC 20190213 IMG_6254.jpg";
		if ((path == null) || path.equals("") || path.equals("/"))
		{
			
		}
		int indexLib = path.indexOf("Collections/");
		
		if (indexLib >= 0) {
			indexLib += "Collections/".length();
			int slashPos = path.indexOf('/', indexLib+1);
			if (slashPos >= 0)
			{
				System.out.println(path.substring(indexLib, slashPos)); //strip off the slash
			}
		}
		System.out.println("NOTHING");
	}
}