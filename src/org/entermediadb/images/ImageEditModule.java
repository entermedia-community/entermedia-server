/*
 * Created on Jun 6, 2006
 */
package org.entermediadb.images;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.modules.edit.BaseEditorModule;
import org.openedit.page.Page;

public class ImageEditModule extends BaseEditorModule
{
	public ImageEditorSession getImageSession(WebPageRequest inReq) throws OpenEditException
	{
		ImageEditorSession session = new ImageEditorSession();
		session.setParentName(inReq.getRequestParameter("parentName") );
		session.setOriginalUrl(inReq.getRequestParameter("origURL"));

		String editPath = inReq.getRequestParameter("editPath");
		Page editPage = getPageManager().getPage(editPath);
		session.setEditPage(editPage);
		inReq.putPageValue("imageeditsession", session);
		return session;
	}
	public void resize(WebPageRequest inReq ) throws Exception
	{
		if( inReq.getUser() == null /*|| !inReq.getUser().hasPermission("oe.edit")*/)
		{
			throw new OpenEditException("No edit permissions");
		}
		String width = inReq.getRequestParameter("width");
		String height = inReq.getRequestParameter("height");
		ImageEditorSession session = getImageSession(inReq);
		String message = inReq.getRequestParameter("message");
		if( "reason for edit".equals(message ) )
		{
			message = "online resize";
		}
		ImageCrop crop = new ImageCrop();
		crop.setRange("0","0",width,height);
		crop.setPageManager(getPageManager());
		crop.resize(session.getEditPath(),inReq.getUser(),message);
	}
	public void crop(WebPageRequest inReq ) throws Exception
	{
		if( inReq.getUser() == null /*|| !inReq.getUser().hasPermission("oe.edit")*/)
		{
			throw new OpenEditException("No edit permissions");
		}
		String x = inReq.getRequestParameter("x1");
		String y = inReq.getRequestParameter("y1");
		String width = inReq.getRequestParameter("width");
		String height = inReq.getRequestParameter("height");
		ImageCrop crop = new ImageCrop();
		crop.setPageManager(getPageManager());
		crop.setRange(x,y,width,height);

		ImageEditorSession session = getImageSession(inReq);
		String message = inReq.getRequestParameter("message");
		if( "reason for edit".equals(message ) )
		{
			message = "online croping";
		}
		crop.crop(session.getEditPath(),inReq.getUser(),message);
	}
}
