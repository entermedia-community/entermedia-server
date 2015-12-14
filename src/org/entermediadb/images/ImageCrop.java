/*
 * Created on Jun 6, 2006
 */
package org.entermediadb.images;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.openedit.page.Page;
import org.openedit.page.manage.PageManager;
import org.openedit.repository.InputStreamItem;
import org.openedit.users.User;
import org.openedit.util.FileUtils;
import org.openedit.util.PathUtilities;

public class ImageCrop
{
	protected Rectangle fieldRange;
	protected PageManager fieldPageManager;
	protected float fieldScaleX;
	protected float fieldScaleY;
	
	public Rectangle getRange()
	{
		return fieldRange;
	}

	public void setRange(Rectangle inRange)
	{
		fieldRange = inRange;
	} 
	public void setRange( int inX, int inY, int inWidth, int inHeight)
	{
		setRange(new Rectangle(inX, inY, inWidth, inHeight));
	}
	public void crop( String inPath, User inUser, String inMessage ) throws Exception
	{
		Page input = getPageManager().getPage(inPath);
		if ( input.exists() )
		{
			InputStream in = null;
			OutputStream out = null;
			File tmp = File.createTempFile("crop", "image");
			try
			{
				in = input.getContentItem().getInputStream();
				BufferedImage origImage = ImageIO.read(in);
				BufferedImage done = crop(origImage);
				String type = PathUtilities.extractPageType(inPath);
				out = new FileOutputStream( tmp );
				ImageIO.write(done, type,  out);
			}
			finally
			{
				FileUtils.safeClose(in);
				FileUtils.safeClose(out);
			}
			InputStreamItem item = new InputStreamItem();
			item.setAuthor(inUser.getUserName());
			item.setMessage(inMessage);
			item.setPath(inPath);
			item.setInputStream(new FileInputStream(tmp));
			item.setMakeVersion(true);
			input.setContentItem(item);
			getPageManager().putPage(input);
			tmp.delete();
		}
	}
	public BufferedImage crop(BufferedImage inImage)
	{	
		//BufferedImage origImage = ImageIO.read( inInImageFile );
		int scaledX = (int)getRange().getX();
		int scaledY = (int)getRange().getY();
		int scaledWidth = (int)getRange().getWidth(); 
		int scaledHeight = (int)getRange().getHeight();
		
		if(getScaleX() > 0)
		{
			scaledX = (int)(scaledX * getScaleX());
			scaledWidth = (int)(scaledWidth * getScaleX());
		}
		if(getScaleY() > 0)
		{
			scaledY = (int)(scaledY * getScaleY());
			scaledHeight = (int)(scaledHeight * getScaleY());
		}
		
		int x = Math.max(0, scaledX);
		int y = Math.max(0, scaledY);
		
		int w = Math.min(scaledWidth, inImage.getWidth() - x );
		int h = Math.min(scaledHeight, inImage.getHeight() - y);

		x = Math.min(inImage.getWidth(), x);
		y = Math.min(inImage.getHeight(), y );

		BufferedImage crop =  inImage.getSubimage(x,y,w, h);
		return crop;
	}

	public PageManager getPageManager()
	{
		return fieldPageManager;
	}

	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}

	public void setRange(String inX, String inY, String inWidth, String inHeight)
	{
		setRange(Integer.parseInt(inX),Integer.parseInt(inY),Integer.parseInt(inWidth),Integer.parseInt(inHeight) );
	}

	public void resize(String inEditPath, User inUser, String inMessage) throws Exception
	{
		Page input = getPageManager().getPage(inEditPath);
		if ( input.exists() )
		{
			InputStream in = null;
			OutputStream out = null;
			File tmp = File.createTempFile("resize", "image");
			try
			{
				in = input.getContentItem().getInputStream();
				BufferedImage origImage = ImageIO.read(in);

				BufferedImage scaledImage = new BufferedImage( getRange().width,
					getRange().height, BufferedImage.TYPE_INT_RGB );
				Graphics2D scaledGraphics = scaledImage.createGraphics();
				scaledGraphics.setRenderingHint( RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				scaledGraphics.drawImage( origImage, 0, 0, getRange().width,
					getRange().height, null );

				String type = PathUtilities.extractPageType(inEditPath);
				out = new FileOutputStream( tmp );
				ImageIO.write(scaledImage, type,  out);
			}
			finally
			{
				FileUtils.safeClose(in);
				FileUtils.safeClose(out);
			}
			InputStreamItem item = new InputStreamItem();
			item.setAuthor(inUser.getUserName());
			item.setMessage(inMessage);
			item.setPath(inEditPath);
			item.setInputStream(new FileInputStream(tmp));
			item.setMakeVersion(true);
			input.setContentItem(item);
			getPageManager().putPage(input);
			tmp.delete();
		}
		
	}

	public float getScaleX()
	{
		return fieldScaleX;
	}

	public void setScaleX(float inScaleX)
	{
		fieldScaleX = inScaleX;
	}
	
	public void setScaleX(String inScaleX)
	{
		fieldScaleX = Float.parseFloat(inScaleX);
	}

	public Float getScaleY()
	{
		return fieldScaleY;
	}

	public void setScaleY(float inScaleY)
	{
		fieldScaleY = inScaleY;
	}
	
	public void setScaleY(String inScaleY)
	{
		fieldScaleY = Float.parseFloat(inScaleY);
	}
	
	public int getScaledWidth()
	{
		float x = getScaleX();
		if(x > 0)
		{
			return (int)(getRange().width * x);
		}
		else
		{
			return (int)getRange().width;
		}
	}
	
	public int getScaledHeight()
	{
		float y = getScaleY();
		if(y > 0)
		{
			return (int)(getRange().height * y);
		}
		else
		{
			return (int)getRange().height;
		}
	}
}
