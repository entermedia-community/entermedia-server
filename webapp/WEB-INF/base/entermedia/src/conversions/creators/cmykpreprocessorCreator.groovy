package conversions.creators;

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.openedit.Data
import org.openedit.entermedia.Asset
import org.openedit.entermedia.MediaArchive
import org.openedit.entermedia.creator.BaseImageCreator
import org.openedit.entermedia.creator.ConvertInstructions
import org.openedit.entermedia.creator.ConvertResult
import org.openedit.util.DateStorageUtil;

import com.openedit.OpenEditException
import com.openedit.page.Page
import com.openedit.util.ExecResult
import com.openedit.util.FileUtils



public class cmykreprocessorCreator extends BaseImageCreator {
	private static final Log log = LogFactory.getLog(cmykreprocessorCreator.class);
	
	protected String fieldCMYKProfile = null;
	
	public String getCMYKProfile() {
		if (fieldCMYKProfile == null){
			Page profile = getPageManager().getPage("/system/components/conversions/USWebCoatedSWOP.icc");
			fieldCMYKProfile = profile.getContentItem().getAbsolutePath();
		}
		return fieldCMYKProfile;
	}

	public void setCMYKProfile(String inCMYKProfile) {
		fieldCMYKProfile = inCMYKProfile;
	}

	@Override
	public boolean canReadIn(MediaArchive inArchive, String inInputType) {
		return ("jpg".equals(inInputType));
	}
	
	@Override
	public String populateOutputPath(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		StringBuffer path = new StringBuffer();
		String prefix = inStructions.getProperty("pathprefix");
		if( prefix != null)
		{
			path.append(prefix);
		}
		else
		{
			path.append("/WEB-INF/data");
			path.append(inArchive.getCatalogHome());
			path.append("/generated/");
		}
		path.append(inStructions.getAssetSourcePath()).append("/cmykinfo.xconf");//make an xconf
//		if (inStructions.getOutputExtension() != null)
//		{
//			path.append(".").append(inStructions.getOutputExtension());
//		}
		inStructions.setOutputPath(path.toString());
		return inStructions.getOutputPath();
	}

	@Override
	public ConvertResult convert(MediaArchive inArchive, Asset inAsset,
			Page inOut, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		Page original = inArchive.getOriginalDocument(inAsset);
		String generatedpath = populateOutputPath(inArchive,inStructions);
		Page generatedpage = inArchive.getPageManager().getPage(generatedpath);
		String colorspace = inAsset.get("colorspace");
		if (colorspace!=null && colorspace.isEmpty()==false)
		{
			Data colorspacedata  = inArchive.getData("colorspace",colorspace);
			if (colorspacedata!=null && colorspacedata.getName().equalsIgnoreCase("cmyk"))
			{
				preprocessCMYKAsset(inArchive,inAsset,inStructions,result);
			}
		}
		if (!generatedpage.exists()){
//			String outputpath = generatedpage.getContentItem().getAbsolutePath();
//			new File(outputpath).getParentFile().mkdirs();
//			inArchive.getPageManager().copyPage(original,generatedpage);
			//do not copy original file - create an xconf with a timestamp
			saveOutput(inArchive,inStructions);
		}
		result.setOk(true);
		result.setComplete(true);
		return result;
	}
	
	protected void preprocessCMYKAsset(MediaArchive inArchive, Asset inAsset, ConvertInstructions inStructions, ConvertResult inResult)
	{
		Page original = inArchive.getOriginalDocument(inAsset);
		if (requiresICCProfileEmbed(original)){
			log.info("preprocessing step: asset ["+inAsset.getId()+"] has CMYK ColorMode and no ICCProfile, need to embed profile");
//			String generatedpath = populateOutputPath(inArchive,inStructions);
//			Page generatedpage = inArchive.getPageManager().getPage(generatedpath);
//			String outputpath = generatedpage.getContentItem().getAbsolutePath();
//			new File(outputpath).getParentFile().mkdirs();
//			embedICCProfile(original,generatedpage);
			embedICCProfile(original,original);//change the original
//			if (requiresICCProfileEmbed(generatedpage)){
			if (requiresICCProfileEmbed(original)){//check the original
				log.warn("preprocessing step: asset ["+inAsset.getId()+"] still does not have ICCProfile, unable to embed");
				saveOutput(inArchive,inStructions,"unable to embed ICCProfile");
			} else {
				log.info("preprocessing step: asset ["+inAsset.getId()+"] has embedded profile, executed successfully");
				//at this point, need to replace original with embedded version
				//this is so that all future conversions are working from embedded version
//				inArchive.getPageManager().copyPage(generatedpage,original);
//				log.info("preprocessing step: asset ["+inAsset.getId()+"] copied generated embedded file to original location");
				saveOutput(inArchive,inStructions,"embedded ICCProfile");
			}
		}
		else {
			log.info("preprocessing step: asset ["+inAsset.getId()+"] does not require an embedded ICCProfile");
		}
	}
	
	protected boolean requiresICCProfileEmbed(Page inPage){
		boolean requiresProfile = false;
		String colorinfo = getColorModeAndProfileInfo(inPage);
		if (colorinfo!=null){
			String [] tokens = colorinfo.split("\\s");
			String colormode = (tokens!=null && tokens.length >= 3 ? tokens[2] : null);
			String colorspace = (tokens!=null && tokens.length >= 6 ? tokens[5] : null);
			String iccprofile = (tokens!=null && tokens.length >= 9 ? tokens[8] : null);
			if ( (colormode!=null && colormode.equalsIgnoreCase("cmyk")) ||
				(colorspace!=null && colorspace.equalsIgnoreCase("cmyk"))){
				if (iccprofile==null || !iccprofile.equalsIgnoreCase("cmyk")){
					requiresProfile = true;
				}
			}
		}
		return requiresProfile;
	}
	
	protected boolean hasICCProfile(Page inPage){
		boolean hasprofile = false;
		String colorinfo = getColorModeAndProfileInfo(inPage);
		if (colorinfo!=null){
			String [] tokens = colorinfo.split("\\s");
			String colormode = (tokens!=null && tokens.length >= 3 ? tokens[2] : null);
			String colorspace = (tokens!=null && tokens.length >= 6 ? tokens[5] : null);
			String iccprofile = (tokens!=null && tokens.length >= 9 ? tokens[8] : null);
			if ( (colormode!=null && colormode.equalsIgnoreCase("cmyk")) ||
				(colorspace!=null && colorspace.equalsIgnoreCase("cmyk"))){
				if (iccprofile!=null && iccprofile.equalsIgnoreCase("cmyk")){
					hasprofile = true;
				}
			}
		}
		return hasprofile;
	}
	
	protected String getColorModeAndProfileInfo(Page inOriginal){
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		command.add("-a");
		command.add("-S");
		command.add("-G0");
		command.add("-ColorMode");
		command.add("-ColorSpace");
		command.add("-ICC_Profile:ColorSpaceData");
		if (isOnWindows()){
			command.add("\"" +  inOriginal.getContentItem().getAbsolutePath()+ "\"");
		} else {
			command.add(inOriginal.getContentItem().getAbsolutePath());
		}
		if (execute("exiftool",command,out,err)){
			return out.toString();
		}
		return err.toString();
	}
	
	protected void embedICCProfile(Page inOriginal, Page inGenerated){
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		if (isOnWindows()){
			command.add("\"" + inOriginal.getContentItem().getAbsolutePath()+ "\"");
			command.add("-profile");
			command.add("\"" + getCMYKProfile()+ "\"");
			command.add("\"" + inGenerated.getContentItem().getAbsolutePath()+ "\"");
		} else {
			command.add(inOriginal.getContentItem().getAbsolutePath());
			command.add("-profile");
			command.add(getCMYKProfile());
			command.add(inGenerated.getContentItem().getAbsolutePath());
		}
		execute("convert",command);
	}
	
	protected boolean execute(String command, List<String> args){
		return execute(command,args,null,null);
	}
	
	protected boolean execute(String command, List<String> args, StringBuilder out, StringBuilder err){
		ExecResult result = getExec().runExec(command, args, true);
		if(!result.isRunOk()){
			String error = result.getStandardError();
			if (error != null) {
				log.info("error stream: \n"+error);
				if (err!=null){
					err.append(error);
				}
			}
		}
		String info = result.getStandardOut();
		if (info != null){
			log.info("standard out stream: \n"+info);
			if (out!=null){
				out.append(info);
			}
		}
		return (result.getReturnValue() == 0);
	}
	
	protected void saveOutput(MediaArchive inArchive, ConvertInstructions inStructions){
		saveOutput(inArchive,inStructions,"no change");
	}
	
	protected void saveOutput(MediaArchive inArchive, ConvertInstructions inStructions, String inDetails){
		String outputpath = populateOutputPath(inArchive,inStructions);
		Page output = inArchive.getPageManager().getPage(outputpath);
		File file = new File(output.getContentItem().getAbsolutePath());
		Element root = DocumentHelper.createDocument().addElement("properties");
		Element prop1 = root.addElement("property");
		prop1.addAttribute("name", "date");
		prop1.setText(DateStorageUtil.getStorageUtil().formatForStorage(new Date()));
		Element prop2 = root.addElement("property");
		prop2.addAttribute("name", "info");
		prop2.setText(inDetails);
		writeXmlFile(root,file);
	}
	
	protected void writeXmlFile( Element inRoot, File inFile ){
		File tempFile = new File( inFile.getParentFile(), inFile.getName() + ".temp" );
		inFile.getParentFile().mkdirs();
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream( tempFile );
			XMLWriter writer = new XMLWriter( fos, OutputFormat.createPrettyPrint() );
			writer.write( inRoot );
		}
		catch( IOException e )
		{
			throw new OpenEditException(e);
		}
		finally
		{
			FileUtils.safeClose( fos );
		}
		if (inFile.exists()){
			inFile.delete();
		}
		if ( !tempFile.renameTo(inFile) )
		{
			throw new OpenEditException( "Unable to rename " + tempFile + " to " + inFile );
		}
	}
}
