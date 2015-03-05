package conversions.creators;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.creator.BaseImageCreator;
import org.openedit.entermedia.creator.ConvertInstructions;
import org.openedit.entermedia.creator.ConvertResult;

import com.openedit.page.Page;
import com.openedit.util.ExecResult;



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
		path.append(inStructions.getAssetSourcePath()).append("/originalcopy");
		if (inStructions.getOutputExtension() != null)
		{
			path.append(".").append(inStructions.getOutputExtension());
		}
		//this is a preprocessor so don't want to populate outputpath (?)
//		inStructions.setOutputPath(path.toString());
//		return inStructions.getOutputPath();
		return path.toString();
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
			String outputpath = generatedpage.getContentItem().getAbsolutePath();
			new File(outputpath).getParentFile().mkdirs();
			inArchive.getPageManager().copyPage(original,generatedpage);
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
			String generatedpath = populateOutputPath(inArchive,inStructions);
			Page generatedpage = inArchive.getPageManager().getPage(generatedpath);
			String outputpath = generatedpage.getContentItem().getAbsolutePath();
			new File(outputpath).getParentFile().mkdirs();
			embedICCProfile(original,generatedpage);
			if (requiresICCProfileEmbed(generatedpage)){
				log.warn("preprocessing step: asset ["+inAsset.getId()+"] still does not have ICCProfile, unable to embed");
			} else {
				log.info("preprocessing step: asset ["+inAsset.getId()+"] has embedded profile, executed successfully");
				//at this point, need to replace original with embedded version
				//this is so that all future conversions are working from embedded version
				inArchive.getPageManager().copyPage(generatedpage,original);
				log.info("preprocessing step: asset ["+inAsset.getId()+"] copied generated embedded file to original location");
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
			String iccprofile = (tokens!=null && tokens.length >= 6 ? tokens[5] : null);
			if (colormode!=null && colormode.equalsIgnoreCase("cmyk")){
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
			String iccprofile = (tokens!=null && tokens.length >= 6 ? tokens[5] : null);
			if (colormode!=null && colormode.equalsIgnoreCase("cmyk")){
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
}
