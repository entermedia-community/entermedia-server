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
		log.info("#### canReadIn");
		return true;
	}

	@Override
	public ConvertResult convert(MediaArchive inArchive, Asset inAsset,
			Page inOut, ConvertInstructions inStructions)
	{
		ConvertResult result = new ConvertResult();
		String colorspace = inAsset.get("colorspace");
		if (colorspace!=null && colorspace.isEmpty()==false)
		{
			Data colorspacedata  = inArchive.getData("colorspace",colorspace);
			if (colorspacedata!=null && colorspacedata.getName().equalsIgnoreCase("cmyk"))
			{
				preprocessCMYKAsset(inArchive,inAsset,result);
			}
		}
		result.setComplete(false);
		return result;
	}
	
	protected void preprocessCMYKAsset(MediaArchive inArchive, Asset inAsset, ConvertResult inResult)
	{
		Page original = inArchive.getOriginalDocument(inAsset);
		String colorinfo = getColorModeAndProfileInfo(original);
		if (colorinfo!=null){
			String [] tokens = colorinfo.split("\\s");
			//[XMP];ColorMode:;CMYK
			//[ICC_Profile];ColorSpaceData:;CMYK
			String colormode = (tokens!=null && tokens.length >= 3 ? tokens[2] : null);
			String iccprofile = (tokens!=null && tokens.length >= 6 ? tokens[5] : null);
			if (colormode!=null && colormode.equalsIgnoreCase("cmyk")){
				if (iccprofile!=null && iccprofile.equalsIgnoreCase("cmyk")){
					log.info("preprocessing step: asset ["+inAsset.getId()+"] has CMYK ColorMode and ICCProfile, do not need to embed profile");
				}
				else {
					log.info("#### preprocessing step: asset ["+inAsset.getId()+"] has CMYK ColorMode and no ICCProfile, need to embed profile");
					String out = getEmbedICCProfile(original);
					System.out.println("result: "+out);
					
				}
			} else {
				log.info("preprocessing step: asset ["+inAsset.getId()+"] does not have CMYK ColorMode, skipping");
			}
		}
		else {
			log.info("preprocessing step: asset ["+inAsset.getId()+"] unable to retrieve ColorMode and ICCProfile info, aborting");
		}
	}
	
	protected String getColorModeAndProfileInfo(Page inOriginal){
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		command.add("exiftool");
		command.add("-a");
		command.add("-S");
		command.add("-G0");
		command.add("-ColorMode");
		command.add("-ICC_Profile:ColorSpaceData");
		command.add(inOriginal.getContentItem().getAbsolutePath());
		if (execute(command,out,err)){
			return out.toString();
		}
		return err.toString();
	}
	
	protected String getEmbedICCProfile(Page inOriginal){
		//convert input.jpg -profile "USWebCoatedSWOP.icc" out.jpg
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		List<String> command = new ArrayList<String>();
		command.add("convert");
		command.add(inOriginal.getContentItem().getAbsolutePath());
		command.add("-profile");
		command.add(getCMYKProfile());
		command.add(inOriginal.getContentItem().getAbsolutePath());//should have a different output path?
		if (execute(command,out,err)){
			return out.toString();
		}
		return err.toString();
	}
	
	protected boolean execute(List<String> command){
		return execute(command,null,null);
	}
	
	protected boolean execute(List<String> command, StringBuilder out, StringBuilder err){
		ExecResult result = getExec().runExec(command, null, true, null);
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
