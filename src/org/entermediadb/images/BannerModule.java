package org.entermediadb.images;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.openedit.WebPageRequest;
import org.openedit.modules.BaseModule;

public class BannerModule extends BaseModule {

    Random fieldRandom;

	public Random getRandom()
	{
		if (fieldRandom == null)
		{
			fieldRandom = new Random();
		}
		return fieldRandom;
	}

	public void setRandom(Random inRandom)
	{
		fieldRandom = inRandom;
	}

	public void randomNumber(WebPageRequest inReq ) throws Exception
	{
	       String toplimit = inReq.getCurrentAction().getChildValue("toplimit");
	       if ( toplimit == null)
	       {
	           toplimit = "3";
	       }	       
	       int topmost = Integer.parseInt(toplimit);
	       int num = getRandom().nextInt(topmost);
	             //num is number from 0 to 3
	       num++;  //add one to it

	       inReq.putPageValue( "bannernumber",String.valueOf( num ) );
	}
	
	/**
	 * Return a list of paths to html banners.
	 * Default size of list = 1
	 * @param inReq
	 * @throws Exception
	 */
	public void randomBanners(WebPageRequest inReq) throws Exception
	{
		if( inReq.getPageProperty("bannerspath")==null)
			return;
		List paths = getPageManager().getChildrenPaths(inReq.getPageProperty("bannerspath"));
		for (int i=paths.size()-1; i>=0; i--)
		{
			String path = paths.get(i).toString();
			if( !getPageManager().getPage(path).isHtml() )
			{
				paths.remove(i);
			}
		}
		if (paths.size()<1)
		{
			return;
		}
		List banners = new ArrayList();
		String number = inReq.getPageProperty("numbanners");
		int numBanners = 1;
		if (number!=null)
		{
			numBanners = Integer.parseInt(number);
		}
		for (int i=0; i<numBanners; i++)
		{
			if( paths.size()>=1 )
			{
				int num = getRandom().nextInt(paths.size());
				banners.add(paths.get(num));
				paths.remove(num);
			} else {
				break;
			}
		}
		//int num = getRandom().nextInt(paths.size());
		//inReq.putPageValue("banner", paths.get(num));
		inReq.putPageValue("banners", banners);
	}
	/**
	 * Return a list of paths to html banners.
	 * Default size of list = 1
	 * @param inReq
	 * @throws Exception
	 */
	public void randomImage(WebPageRequest inReq) throws Exception
	{
		String bannerspath = inReq.findValue("bannerspath");
		if( bannerspath == null)
			return;
		
		List paths = getPageManager().getChildrenPaths(bannerspath);
		for (int i=paths.size()-1; i>=0; i--)
		{
			String path = paths.get(i).toString();
			if( !getPageManager().getPage(path).isImage() )
			{
				paths.remove(i);
			}
		}
		if (paths.size()<1)
		{
			return;
		}
		List banners = new ArrayList();
		String number = inReq.getPageProperty("numbanners");
		int numBanners = 1;
		if (number!=null)
		{
			numBanners = Integer.parseInt(number);
		}
		for (int i=0; i<numBanners; i++)
		{
			if( paths.size()>=1 )
			{
				int num = getRandom().nextInt(paths.size());
				banners.add(paths.get(num));
				paths.remove(num);
			} else {
				break;
			}
		}
		//int num = getRandom().nextInt(paths.size());
		//inReq.putPageValue("banner", paths.get(num));
		inReq.putPageValue("banners", banners);
	}
}
