package org.entermediadb.users;

import java.util.Collection;
import java.util.Collections;

import org.entermediadb.asset.Category;
import org.entermediadb.find.EntityManager;
import org.openedit.Data;
import org.openedit.users.Permissions;

public class EntityPermissions extends Permissions
{
	protected EntityManager fieldEntityManager;

	public EntityManager getEntityManager()
	{
		return fieldEntityManager;
	}

	public void setEntityManager(EntityManager inEntityManager)
	{
		fieldEntityManager = inEntityManager;
	}
	
//	@Override
//	protected boolean isEditorFor(Data inModule, Data inEntity)
//	{
//		Category entiytycategory = getEntityManager().loadDefaultFolder(inModule, inEntity, null);
//		if( entiytycategory == null )
//		{
//			return false;
//		}
//		Collection<String> editorusers = entiytycategory.collectValues("editorusers");
//		if( editorusers.contains( getUserProfile().getId() ) )
//		{
//			return true;
//		}
//		
//		Collection<String> editorroles = entiytycategory.collectValues("editorroles");
//		if( editorroles.contains( getUserProfile().getSettingsGroupIndexId() ) )
//		{
//			return true;
//		}
//
//		Collection<String> editorgroups = entiytycategory.collectValues("editorgroups");
//		if( editorgroups.contains( getUserProfile().getUser().isInGroup(editorgroups) ) )
//		{
//			return true;
//		}
//		
//		return false;
//		
//	}
}
