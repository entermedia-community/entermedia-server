package assets.model

import org.openedit.Data;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.MediaArchive;

class LibraryAddingAssetTypeManager extends AssetTypeManager
{
		public Asset checkLibrary(MediaArchive mediaarchive, Data hit)
		{
			if( hit.get("assettype") == "printreadyfinal" )
			{
				String libraries = hit.get("libraries");
				if( libraries == null || !libraries.contains("printreadyfinal") )
				{
					Asset real = mediaarchive.getAssetBySourcePath(hit.getSourcePath());
					return checkLibrary(mediaarchive,real);
				}
			}
			return null;
		}
		public Asset checkLibrary(MediaArchive mediaarchive, Asset real)
		{
			if( real.get("assettype") == "printreadyfinal" )
			{
				Collection values = real.getValues("libraries");
				if( values == null || !values.contains("printreadyfinal") )
				{
					real.addValue("libraries", "printreadyfinal");
				}
			}
			return real;
		}
		public String findCorrectAssetType(Data inAssetHit, String inSuggested)
		{
			String path = inAssetHit.getSourcePath().toLowerCase();
			if( path.contains("/links/") )
			{
				return "links";
			}
			else if( path.contains("/press ready pdf/") || path.endsWith("_pfinal.pdf") )
			{
				return "printreadyfinal";
			}
			return inSuggested;
		}
	
}
