import org.entermediadb.asset.AssetUtilities;
import org.entermediadb.asset.Category;
import org.entermediadb.asset.MediaArchive;

AssetUtilities utils = mediaarchive.getAssetImporter().getAssetUtilities();
		Category root =mediaarchive.getCategoryArchive().getRootCategory();
		String folder = "/myexportfolder";
		utils.exportCategoryTree(mediaarchive,root, folder);