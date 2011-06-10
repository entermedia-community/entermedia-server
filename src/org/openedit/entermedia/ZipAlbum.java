package org.openedit.entermedia;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.albums.Album;
import org.openedit.entermedia.albums.AlbumItem;
import org.openedit.entermedia.creator.ConvertInstructions;

import com.openedit.OpenEditException;
import com.openedit.WebPageRequest;
import com.openedit.page.Page;
import com.openedit.users.User;
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;

public class ZipAlbum
{
	private static final Log log = LogFactory.getLog(ZipAlbum.class);

	protected User fieldUser;
	protected EnterMedia fieldMatt;

	public EnterMedia getMatt()
	{
		return fieldMatt;
	}

	public void setMatt(EnterMedia matt)
	{
		fieldMatt = matt;
	}

	public User getUser()
	{
		return fieldUser;
	}

	public void setUser(User user)
	{
		fieldUser = user;
	}

	protected void writeFileToZip(ZipOutputStream inZipOutputStream, OutputFiller inOutputFiller, File inFile)
	{
		ZipEntry entry = new ZipEntry(inFile.getName());
		entry.setSize(inFile.length());
		entry.setTime(inFile.lastModified());
		try
		{
			FileInputStream fis = new FileInputStream(inFile);
			inZipOutputStream.putNextEntry(entry);
			try
			{
				inOutputFiller.fill(fis, inZipOutputStream);
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

	protected String buildMissingDocumentsText(List inMissingDocumentItems)
	{
		StringBuffer missingAssetsStr = new StringBuffer("Original documents for the following could not be retrieved:\n\n");
		for (Iterator iter = inMissingDocumentItems.iterator(); iter.hasNext();)
		{
			AlbumItem item = (AlbumItem) iter.next();
			missingAssetsStr.append("    - ");
			missingAssetsStr.append(item.getAsset().getName());
			missingAssetsStr.append(" (");
			Page path = getMediaArchive(item.getAsset().getCatalogId()).getOriginalDocument(item.getAsset());
			if (path == null)
			{
				missingAssetsStr.append("no file name specified");
			}
			else
			{
				missingAssetsStr.append(path.getContentItem().getAbsolutePath());
			}
			missingAssetsStr.append(")\n");
		}
		return missingAssetsStr.toString();
	}

	public MediaArchive getMediaArchive(String inCatalogId)
	{
		return getMatt().getMediaArchive(inCatalogId);
	}

	public void zipAlbumItems(Album inAlbum, OutputStream inStream, WebPageRequest inReq) throws OpenEditException
	{
		List missingDocumentItems = new ArrayList();
		List documents = new ArrayList();
		ZipOutputStream zos = new ZipOutputStream(inStream);

		zos.setLevel(1); // for speed since these are jpegs
		try
		{
			OutputFiller filler = new OutputFiller();
			for (Object object : inAlbum.getAlbumItems(inReq))
			{
				AlbumItem item  = (AlbumItem)object;
				ConvertInstructions inStructions = new ConvertInstructions();
				inStructions.setAssetSourcePath(item.getAsset().getSourcePath());
				inStructions.setPageNumber(item.getAsset().getProperty("pagenumber"));
				inStructions.setWatermark(item.isWatermark());

				String inWidth = item.getWidth();
				if (inWidth != null)
				{
					inStructions.setMaxScaledSize(new Dimension(Integer.parseInt(item.getWidth()), 10000));
				}
				MediaArchive mediaArchive = getMediaArchive(item.getAsset().getCatalogId());
				Page documentFile = mediaArchive.getOriginalDocument(item.getAsset());
				// String type =
				// PathUtilities.extractPageType(documentFile.getName());
				if (item.isWatermark() || inWidth != null)
				{
					inStructions.setOutputExtension("jpg");
				}

				if (documentFile == null || !documentFile.exists())
				{
					if (documentFile != null)
					{
						log.info("Image missing:" + documentFile.getContentItem().getAbsolutePath());
					}
					missingDocumentItems.add(item);
				}
				else
				{
					try
					{
						File source = null;
						File temp = null;

						if (mediaArchive.canConvert(item.getAsset(), inStructions.getOutputExtension(), getUser()))
						{
							source = new File(mediaArchive.getCreatorManager().createOutput(inStructions).getContentItem().getAbsolutePath());
							String extension = "";
							if (inStructions.getOutputExtension() != null)
							{
								extension = "." + inStructions.getOutputExtension();
							}
							temp = new File(source.getParentFile(), item.getAsset().getSaveAsName() + extension);
						}
						else
						{
							source = new File(documentFile.getContentItem().getAbsolutePath());
							temp = new File(source.getParentFile(), item.getAsset().getSaveAsName());
						}

						if (!source.equals(temp))
						{
							new FileUtils().copyFiles(source, temp);
							writeFileToZip(zos, filler, temp);
							new FileUtils().deleteAll(temp);
						}
						else
						{
							writeFileToZip(zos, filler, source);
						}

						documents.add(item);
					}
					catch (Exception ex)
					{
						missingDocumentItems.add(item);
						log.error(ex);
						continue;
					}
				}
			}

			if (!missingDocumentItems.isEmpty())
			{
				String missingAssets = buildMissingDocumentsText(missingDocumentItems);

				writeStringToZip(zos, missingAssets, "missing.txt");
				getMatt().getEmailErrorHandler().sendNotification("Missing File Report", missingAssets);
				for (Iterator iterator = missingDocumentItems.iterator(); iterator.hasNext();)
				{
					AlbumItem item = (AlbumItem) iterator.next();
					getMediaArchive(item.getAsset().getCatalogId()).logDownload(item.getAsset().getSourcePath(), "missing", getUser());
				}

			}
			for (Iterator iterator = documents.iterator(); iterator.hasNext();)
			{
				AlbumItem item = (AlbumItem) iterator.next();
				getMediaArchive(item.getAsset().getCatalogId()).logDownload(item.getAsset().getSourcePath(), "success", getUser());
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

}
