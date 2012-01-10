package org.openedit.entermedia.creator.old;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.BaseImageCreator;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.ConvertResult;

import com.openedit.OpenEditException;
import com.openedit.page.Page;
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;

public class IndesignImageCreatorOld extends BaseImageCreator
{
	private static final Log log = LogFactory.getLog(IndesignImageCreatorOld.class);

	public boolean canReadIn(MediaArchive inArchive, String inInput)
	{
		return inInput.endsWith("indd");
	}

	public ConvertResult convert(MediaArchive inArchive, Asset inAsset, Page inOut, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		//Check that someone did not pass in a starting image in the instructions
		Page input = inArchive.findOriginalMediaByType("image",inAsset);
		
		byte[] base64Encoded = getEncodedImageData(new File( input.getContentItem().getAbsolutePath()));
		if (base64Encoded == null)
		{
			result.setOk(false);
			log.error("No image created");
			return result;
		}
		String encodedString = null;
		try
		{
			encodedString = new String(base64Encoded, "UTF-8");
		}
		catch (UnsupportedEncodingException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		encodedString = encodedString.replace("&#xA;", "\n");
		//System.out.print(encodedString);

		if (base64Encoded != null)
		{
			byte[] encodedBytes = encodedString.getBytes();
			byte[] decoded = Base64.decodeBase64(encodedBytes);
			log.info("Decoded " + decoded.length + " bytes from " + input.getName());
			FileOutputStream outputStream = null;
			try
			{
				File out = new File(inOut.getContentItem().getAbsolutePath());
				out.getParentFile().mkdirs();
				outputStream = new FileOutputStream(out);
				new OutputFiller().fill(new ByteArrayInputStream(decoded), outputStream);
				result.setOk(true);

			}
			catch (IOException e)
			{
				result.setOk(false);
				e.printStackTrace();
			}
			finally
			{
				FileUtils.safeClose(outputStream);
			}

			return result;
		}

		result.setOk(false);
		return result;
	}

	public byte[] getEncodedImageData(File inFile)
	{
		//http://indesign.hilfdirselbst.ch/ausgabe-export/vorschau-extraktor.html
		byte[] data = findData(inFile, "<xmpGImg:image>", 2, "</xmpGImg:image>");
		if (data == null)
		{
			//Older INDD files used an alternative spelling!!!
			//"<xapGImg:image>"
			//xmpGImg
			data = findData(inFile, "<xapGImg:image>",1, "</xapGImg:image>");
		}
		log.info("Found " + data.length + " starts " + data[0] + " ends " + data[data.length - 1]);
		return data;
	}

	public byte[] findData(File inFile,  String inStart, int inCount, String inEnd)
	{
		BufferedInputStream reader = null;
		try
		{
			reader = new BufferedInputStream(new FileInputStream(inFile));
			boolean ok = false;
			for (int i = 0; i < inCount; i++)
			{
				ok = seekTill(reader, inStart);
			}
			if (ok)
			{
				ByteArrayOutputStream data = new ByteArrayOutputStream();
				if (readInto(reader, inEnd, data))
				{
					return data.toByteArray();
				}
			}
		}
		catch (Exception e)
		{
			throw new OpenEditException(e);
		}
		finally
		{
			FileUtils.safeClose(reader);
		}
		return null;
	}

	protected boolean seekTill(BufferedInputStream inInput, String inLookFor) throws IOException
	{
		return readInto(inInput, inLookFor, null);
	}

	protected boolean readInto(BufferedInputStream inInput, String inLookFor, OutputStream inOutput) throws IOException
	{
		byte[] target = inLookFor.getBytes("UTF-8");
		int limlen = target.length;
		int ch;

		top: for (ch = inInput.read(); ch != -1; ch = inInput.read())
		{
			if (ch == 0 && inOutput != null)
			{
				//For some reason CS3 puts chunks of null 0 bytes into the middle of the stream
				ch = pastNulls(inInput);
			}

			if (ch == target[0])
			{
				inInput.mark(limlen);
				for (int i = 1; i < limlen; i++)
				{
					int c = inInput.read();
					if (c != target[i])
					{
						inInput.reset();
						if (inOutput != null) //we are writing everything till the end
						{
							inOutput.write(ch);
						}
						continue top;
					}
				}
				return true;
			}
			else if (inOutput != null)
			{
				inOutput.write(ch);
			}
		}
		return false;
	}

	protected int pastNulls(BufferedInputStream inInput) throws IOException
	{
		int ch = inInput.read();
		if (ch != 0)
		{
			return ch;
		}
		while (ch == 0)
		{
			ch = inInput.read(); //zoom past a long list of zero
		}
		//Jump ahead two full byte since it leaves 4 more junk bits
		ch = inInput.read();
		ch = inInput.read();
		ch = inInput.read();
		//check the next byte
		return pastNulls(inInput);

	}

	public String createConvertPath(ConvertInstructions inStructions)
	{
		String path = inStructions.getAssetSourcePath() + "thumb.jpg";

		return path;
	}

	

}
