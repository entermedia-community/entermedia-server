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
	
	public String getCMYKProfile()
	{
		if (fieldCMYKProfile == null){
			Page profile = getPageManager().getPage("/system/components/conversions/USWebCoatedSWOP.icc");
			fieldCMYKProfile = profile.getContentItem().getAbsolutePath();
		}
		return fieldCMYKProfile;
	}

	public void setCMYKProfile(String inCMYKProfile)
	{
		fieldCMYKProfile = inCMYKProfile;
	}

	@Override
	public boolean canReadIn(MediaArchive inArchive, String inInputType) 
	{
		return ("jpg".equalsIgnoreCase(inInputType) || "jpeg".equalsIgnoreCase(inInputType) ||
			"tiff".equalsIgnoreCase(inInputType) || "tif".equalsIgnoreCase(inInputType) ||
			"gif".equalsIgnoreCase(inInputType)); //|| "eps".equalsIgnoreCase(inInputType));
	}
	
	protected boolean omitEmbed(MediaArchive inArchive, Page inOriginal){
		if (inOriginal.getName().toLowerCase().endsWith(".eps")){
			return true;
		}
		return false;
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
		if (hasCMYKColorModel(inArchive, inAsset)){
			if (omitEmbed(inArchive,original) == false){
				preprocessCMYKAsset(inArchive,inAsset,inStructions,result);
			}
			//if asset is CMKY, then make sure colormodel property is correct
			String colorspace = inAsset.get("colorspace");
			if (!colorspace || (colorspace!="4" && colorspace!="5")){
				inAsset.setProperty("colorspace","4");// CMYK is 4 or 5
				inArchive.saveAsset(inAsset, null);
			}
		}
		else {
			//check case where incoming image that does not have CMYK color model has an embedded CMYK color profile
			if (requiresICCProfileEmbed(original) == false){
				stripProfile(original);
				if(requiresICCProfileEmbed(original) == false){
					log.warn("could not remove CMYK profile from asset ${inAsset.getId()} that does not have CMYK color model, may result in conversion error");
				} else {
					log.info("removed CMYK profile from asset ${inAsset.getId()} that does not have a CMYK color model");
				}
			}
		}
		if (!generatedpage.exists()){
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
			log.info("preprocessing step: asset ["+inAsset.getId()+"] does not have ICCProfile, need to strip existing profile first");
			stripProfile(original);
		}
		log.info("preprocessing step: asset ["+inAsset.getId()+"] embedding profile");
		embedICCProfile(original,original);//change the original
		if (requiresICCProfileEmbed(original)){//check the original
			log.warn("preprocessing step: asset ["+inAsset.getId()+"] still does not have ICCProfile, unable to embed");
			saveOutput(inArchive,inStructions,"unable to embed ICCProfile");
		} else {
			log.info("preprocessing step: asset ["+inAsset.getId()+"] has embedded profile, executed successfully");
			saveOutput(inArchive,inStructions,"embedded ICCProfile");
		}
	}
	
	protected void stripProfile(Page inOriginal){
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		//convert input.jpg +profile "icc" out.jpg
		if (isOnWindows()){
			command.add("\"" +  inOriginal.getContentItem().getAbsolutePath()+ "\"");
			command.add("+profile");
			command.add("\"icc\"");
			command.add("\"" +  inOriginal.getContentItem().getAbsolutePath()+ "\"");
		} else {
			command.add(inOriginal.getContentItem().getAbsolutePath());
			command.add("+profile");
			command.add("\"icc\"");
			command.add(inOriginal.getContentItem().getAbsolutePath());
		}
		execute("convert",command,out,err);
	}
	
	protected boolean requiresICCProfileEmbed(Page inPage)
	{
		boolean requiresProfile = false;
		String colorinfo = getColorModeAndProfileInfo(inPage);
		if (colorinfo!=null){
			String [] tokens = colorinfo.split("\\s");
			String iccprofile = (tokens!=null && tokens.length >= 3 ? tokens[2] : null);
			if (iccprofile==null || !iccprofile.equalsIgnoreCase("cmyk")){
				requiresProfile = true;
			}
		}
		return requiresProfile;
	}
	
	protected boolean hasICCProfile(Page inPage)//check if it has cmyk profile
	{
		boolean hasprofile = false;
		String colorinfo = getColorModeAndProfileInfo(inPage);
		if (colorinfo!=null){
			String [] tokens = colorinfo.split("\\s");
			String iccprofile = (tokens!=null && tokens.length >= 3 ? tokens[2] : null);
			if (iccprofile!=null && iccprofile.equalsIgnoreCase("cmyk")){
				hasprofile = true;
			}
		}
		return hasprofile;
	}
	
	protected boolean hasCMYKColorModel(MediaArchive inArchive, Asset inAsset)
	{
		Page original = inArchive.getOriginalDocument(inAsset);
		String colormode = getColorMode(original);
		String [] tokens = colormode.split("\n");
		if (tokens){
			for(String token:tokens){
				if (token && token.trim().startsWith("Colorspace:")){//Colorspace: CMYK
					String [] words = token.trim().split("\\s");
					if (words.length >= 2 && words[1]){
						boolean isCMYK = (words[1].trim().toLowerCase() == "cmyk");
						return isCMYK;
					}
				}
			}
		}
		return false;
	}
	
	protected String getColorMode(Page inOriginal)
	{
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		//identify -verbose <file> | grep 'Colorspace'
		command.add("-verbose");
		if (isOnWindows()){
			command.add("\"" +  inOriginal.getContentItem().getAbsolutePath()+ "\"");
		} else {
			command.add(inOriginal.getContentItem().getAbsolutePath());
		}
		if (execute("identify",command,out,err)){
			return out.toString();
		}
		return err.toString();
	}
	
	protected String getColorModeAndProfileInfo(Page inOriginal)
	{
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		command.add("-a");
		command.add("-S");
		command.add("-G0");
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
	
	protected void embedICCProfile(Page inOriginal, Page inGenerated)
	{
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
	
	protected boolean execute(String command, List<String> args)
	{
		return execute(command,args,null,null);
	}
	
	protected boolean execute(String command, List<String> args, StringBuilder out, StringBuilder err)
	{
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
//			log.info("standard out stream: \n"+info);
			if (out!=null){
				out.append(info);
			}
		}
		return (result.getReturnValue() == 0);
	}
	
	protected void saveOutput(MediaArchive inArchive, ConvertInstructions inStructions)
	{
		saveOutput(inArchive,inStructions,"no change");
	}
	
	protected void saveOutput(MediaArchive inArchive, ConvertInstructions inStructions, String inDetails)
	{
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
	
	protected void writeXmlFile( Element inRoot, File inFile )
	{
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
