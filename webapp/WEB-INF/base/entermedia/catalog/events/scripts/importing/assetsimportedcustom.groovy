package importing;

import assets.model.AssetTypeManager
import assets.model.EmailNotifier;

import com.openedit.page.manage.*;

public void setAssetTypes()
{
	AssetTypeManager manager = new AssetTypeManager();
	manager.context = context;
	manager.resetTypes();
}
public void sendEmail()
{
	EmailNotifier emailer = new EmailNotifier();
	emailer.context = context;
	emailer.emailOnImport();
}

setAssetTypes();
sendEmail();
