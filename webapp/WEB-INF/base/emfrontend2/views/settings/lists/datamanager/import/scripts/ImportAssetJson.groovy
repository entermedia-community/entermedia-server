import org.entermediadb.asset.MediaArchive



MediaArchive archive = context.getPageValue("mediaarchive");

archive.getModuleManager().getBean("JsonAssetModule").importAssetJson(context);
