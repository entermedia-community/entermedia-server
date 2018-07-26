package org.entermediadb.asset;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.entermediadb.asset.convert.ConversionManager;
import org.entermediadb.asset.convert.ConvertInstructions;
import org.openedit.OpenEditException;
import org.openedit.page.Page;
import org.openedit.repository.ContentItem;
import org.openedit.users.User;
import org.openedit.util.FileUtils;
import org.openedit.util.OutputFiller;

public class ZipGroup
{
	private static final Log log = LogFactory.getLog(ZipGroup.class);

	protected User fieldUser;
	protected MediaArchive fieldMediaArchive;
	OutputFiller filler = new OutputFiller();

	public MediaArchive getMediaArchive()
	{
		return fieldMediaArchive;
	}

	public void setMediaArchive(MediaArchive inMediaArchive)
	{
		fieldMediaArchive = inMediaArchive;
	}

	protected void writeFileToZip(ZipOutputStream inZipOutputStream, String inName, File inFile)
	{
		ZipEntry entry = new ZipEntry(inName);
		entry.setSize(inFile.length());
		entry.setTime(inFile.lastModified());
		try
		{
			FileInputStream fis = new FileInputStream(inFile);
			inZipOutputStream.putNextEntry(entry);
			try
			{
				filler.fill(fis, inZipOutputStream);
			}
			finally
			{
				fis.close();
			}
			inZipOutputStream.closeEntry();
		}
		catch (Exception ex)
		{
			log.error(ex);
		}
	}

	protected void writeStringToZip(ZipOutputStream inZipOutputStream, String inText, String inFileName)
	{
		try
		{
			byte[] bytes = inText.getBytes("UTF-8");
			ZipEntry entry = new ZipEntry(inFileName);
			entry.setSize(bytes.length);
			inZipOutputStream.putNextEntry(entry);
			inZipOutputStream.write(bytes);
			inZipOutputStream.closeEntry();
		}
		catch (IOException ex)
		{
			log.error(ex);
		}
	}

	protected String buildMissingDocumentsText(List<Asset> inMissingDocumentItems)
	{
		StringBuffer missingAssetsStr = new StringBuffer("Original documents for the following could not be retrieved:\n\n");
		for (Asset asset: inMissingDocumentItems)
		{
			missingAssetsStr.append("    - ");
			missingAssetsStr.append(asset.getName());
			missingAssetsStr.append(" (");
			ContentItem path = getMediaArchive().getOriginalContent(asset);
			if (path == null)
			{
				missingAssetsStr.append("no file name specified");
			}
			else
			{
				missingAssetsStr.append(path.getAbsolutePath());
			}
			missingAssetsStr.append(")\n");
		}
		return missingAssetsStr.toString();
	}

	public void zipItems(Map<Asset, ConvertInstructions> inAssets, OutputStream inStream) throws OpenEditException
	{
		List<Asset> missing = new ArrayList<Asset>();
		List<Asset> okAssets = new ArrayList<Asset>();
		ZipOutputStream zos = new ZipOutputStream(inStream);
		zos.setLevel(1); // for speed since these are jpegs

		try
		{
			for (Asset asset: inAssets.keySet())
			{
				ContentItem documentFile = getMediaArchive().getOriginalContent(asset);
				if (documentFile == null || !documentFile.exists())
				{
					if (documentFile != null)
					{
						log.info("Image missing:" + documentFile.getAbsolutePath());
					}
					missing.add(asset);
				}
				else
				{
					try
					{
						File source = null;
						File temp = null;
						ConvertInstructions instructions = inAssets.get(asset);
						if (instructions != null)
						{
							ConversionManager manager = getMediaArchive().getTranscodeTools().getManagerByFileFormat(instructions.getOutputExtension());
							ContentItem converted = manager.createOutput(instructions).getOutput();
							source = new File(converted.getAbsolutePath());
							String extension = "";
							if (instructions.getOutputExtension() != null)
							{
								extension = "." + instructions.getOutputExtension();
							}
							temp = new File(source.getParentFile(), asset.getSaveAsName() + extension);
						}
						else
						{
							source = new File(documentFile.getAbsolutePath());
							temp = new File(source.getParentFile(), asset.getSaveAsName());
						}

						if (!source.equals(temp)) //What is this for?
						{
							new FileUtils().copyFiles(source, temp);
							writeFileToZip(zos, temp.getName(), temp);
							new FileUtils().deleteAll(temp);
						}
						else
						{
							writeFileToZip(zos, source.getName(), source);
						}
						okAssets.add(asset);
					}
					catch (Exception ex)
					{
						missing.add(asset);
						log.error("Error downloading",ex);
						continue;
					}
				}
			}

			if (!missing.isEmpty())
			{
				String missingAssets = buildMissingDocumentsText(missing);

				writeStringToZip(zos, missingAssets, "missing.txt");
//				EmailErrorHandler handler = getEnterMedia().getEmailErrorHandler();
//				if( handler != null )
//				{
//					handler.sendNotification("Missing File Report", missingAssets);
//				}
				for (Asset asset: missing)
				{
					getMediaArchive().logDownload(asset.getSourcePath(), "missing", getUser());
				}

			}
			for (Asset asset: okAssets)
			{
				getMediaArchive().logDownload(asset.getSourcePath(), "success", getUser());
			}

		}
		finally
		{
			try
			{
				FileUtils.safeClose(zos); // This will fail if there was any

			}
			catch (Exception ex2)
			{
				// nothing
			}
		}

	}


	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User user)
	{
		fieldUser = user;
	}

	public void zipAttachments(Asset inAsset, OutputStream inStream) throws OpenEditException
	{
		ZipOutputStream zos = new ZipOutputStream(inStream);
		zos.setLevel(1); // for speed since these are jpegs

		try
		{
			
			ContentItem item = getMediaArchive().getOriginalContent(inAsset);
			File input = new File( item.getAbsolutePath() );
			if (!input.exists())
			{
				writeStringToZip(zos, "Attachments missing "  + item.getAbsolutePath() , "missing.txt");
			}
			else
			{
				writeFolder(zos, input, input);
			}
		}
		finally
		{
			try
			{
				FileUtils.safeClose(zos); // This will fail if there was any

			}
			catch (Exception ex2)
			{
				// nothing
			}
		}

	}

	protected void writeFolder(ZipOutputStream inZos, File inRoot, File inFile)
	{
		if( inFile.isDirectory() )
		{
			//get the children
			File[] children = inFile.listFiles();
			if( children != null )
			{
				for (int i = 0; i < children.length; i++)
				{
					writeFolder(inZos, inRoot, children[i]);					
				}
			}
		}
		else
		{
			String fileName = inFile.getAbsolutePath().substring(inRoot.getAbsolutePath().length());
			fileName = fileName.replace("\\", "/");
			writeFileToZip(inZos,fileName, inFile);
		}
	}
	
}
