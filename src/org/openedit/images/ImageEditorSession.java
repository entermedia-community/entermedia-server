/*
 * Created on Jun 6, 2006
 */
package org.openedit.images;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.openedit.OpenEditRuntimeException;
import com.openedit.modules.edit.EditSession;
import com.openedit.util.FileUtils;

public class ImageEditorSession extends EditSession
{
	protected BufferedImage fieldEditImage;
	public BufferedImage getEditImage()
	{
		if( fieldEditImage == null)
		{
			InputStream in = null;
			try
			{
				in = getEditPage().getContentItem().getInputStream();
				fieldEditImage = ImageIO.read(in);
			}
			catch ( Exception ex)
			{
				throw new OpenEditRuntimeException(ex);
			}
			finally
			{
				FileUtils.safeClose(in);
			}
		}
		return fieldEditImage;
	}

}
