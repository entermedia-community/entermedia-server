package org.entermediadb.modules.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.OpenEditException;
import org.openedit.WebPageRequest;
import org.openedit.config.Configuration;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetailsArchive;
import org.openedit.data.SearcherManager;
import org.openedit.modules.BaseModule;
import org.openedit.page.PageAction;
import org.openedit.page.PageRequestKeys;
import org.openedit.profile.UserProfile;
import org.openedit.util.URLUtilities;

public class ValidationModule extends BaseModule
{

	protected Validator fieldValidator;
	protected SearcherManager fieldSearcherManager;

	public void validateField(WebPageRequest inReq) throws Exception
	{
		PropertyDetail detail = (PropertyDetail) inReq.getPageValue("detail");

		if (detail == null)
		{
			String catalogid = inReq.getRequestParameter("catalogid");
			String searchType = inReq.getRequestParameter("searchtype");
			String view = inReq.getRequestParameter("view");
			String field = inReq.getRequestParameter("field");

			PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(catalogid);
			detail = archive.getDataProperty(searchType, view, field, inReq.getUser());
		}

		if (detail != null)
		{
			String value = inReq.getRequestParameter("value");
			Map errors = new HashMap();
			Map successes = new HashMap();
			Map infos = new HashMap();
			getValidator().validateDetail(errors, successes, infos, value, detail);
			inReq.putPageValue("errors", errors);
			inReq.putPageValue("successes", successes);
			inReq.putPageValue("infos", infos);
		}
		else
		{
			validateFields(inReq);
		}
	}


	
	
	public void validateFields(WebPageRequest inReq) throws Exception
	{
		String[] fields = inReq.getRequestParameters("field");
		if( fields == null )
		{
			return;
		}
		PageAction inAction = inReq.getCurrentAction();
		if (inAction != null && inAction.getConfig() != null)
		{
			String error_url = inReq.findValue("errorURL");
			if (error_url == null)
			{
				String referring = inReq.getReferringPage();
				URLUtilities utils = (URLUtilities) inReq.getPageValue(PageRequestKeys.URL_UTILITIES);
				String root = utils.buildAppRoot();
				error_url = referring.substring(root.length());
			}
			boolean redirect = Boolean.parseBoolean(inReq.findValue("redirect"));
			String catalogid = inReq.getRequestParameter("catalogid");
			if (catalogid == null)
			{
				catalogid = inReq.findPathValue("catalogid");
			}
			if (catalogid == null)
			{
				throw new OpenEditException("Cannot validate without a catalogid");
			}
			String[] views = inReq.getRequestParameters("view");
			if(views == null)
			{
				views = new String[] {inReq.findValue("view")};
			}

			String datatype = inReq.findPathValue("searchtype"); // product
			if (datatype == null)
			{
				datatype = inReq.findValue("type");
			}

			Map errors = new HashMap();
			Map successes = new HashMap();
			Map infos = new HashMap();
			
			Map<String, PropertyDetail> details = new HashMap<String, PropertyDetail>();
			PropertyDetailsArchive archive = getSearcherManager().getPropertyDetailsArchive(catalogid);
			
			for( String view: views )
			{
				List properties = archive.getDataProperties(datatype, view, (UserProfile)null);;
				if(properties == null)
				{
					continue;
				}
				for( Object o: properties )
				{
					PropertyDetail detail = (PropertyDetail) o;
					details.put(detail.getId(), detail);
				}
			}
			
			for (int i = 0; i < fields.length; i++)
			{
				String field = fields[i];
				String value = inReq.getRequestParameter(field + ".value");

				PropertyDetail detail = details.get(field);
				if (detail != null && "matched".equals(detail.getViewType()))
				{
					String val2 = inReq.getRequestParameter(field + "match.value");
					if (value != null)
					{
						if (!value.equals(val2))
						{
							errors.put(field, "error-matched");
						}

					}
				}

				getValidator().validateDetail(errors, successes, infos, value, detail);
			}
			
			
			
			if (errors.size() > 0)
			{

				inReq.setHasForwarded(true);
				inReq.setCancelActions(true);
				inReq.putPageValue("errors", errors);

				if (error_url != null && redirect)
				{
					inReq.forward(error_url);
				}
			}
			inReq.putPageValue("successes", successes);
			inReq.putPageValue("infos", infos);
		}

	}
	
	
	

	public void validate(WebPageRequest inReq) throws Exception
	{
		PageAction inAction = inReq.getCurrentAction();
		Map errors = new HashMap();

		if (inAction != null && inAction.getConfig() != null)
		{
			Configuration c = inAction.getConfig();
			Configuration error = c.getChild("error-url");
			String error_url = null;
			if (error != null)
			{
				error_url = c.getChild("error-url").getAttribute("name");
			}
			if (error_url == null)
			{
				error_url = inReq.findValue("error-url");
			}
			if (error_url == null)
			{
				error_url = inReq.getPageProperty("validationerror");
			}
			List elements = c.getChildren("field");

			for (Iterator iter = elements.iterator(); iter.hasNext();)
			{
				Configuration element = (Configuration) iter.next();
				String field = element.getAttribute("name");
				String fieldValue = inReq.getRequestParameter(field);
				List rules = element.getChildren("rule");
				for (Iterator iterator = rules.iterator(); iterator.hasNext();)
				{
					Configuration rule = (Configuration) iterator.next();
					String message = rule.getValue();
					String type = rule.getAttribute("name");
					if (!validate(fieldValue, type))
					{
						errors.put(field, message);
						break;
					}
				}
			}

			if (errors.size() > 0)
			{
				inReq.setHasForwarded(true);
				inReq.putPageValue("errors", errors);
				if (error_url != null)
				{
					inReq.forward(error_url);
				}
				inReq.setCancelActions(true);
			}
		}
	}

	public SearcherManager getSearcherManager()
	{
		return fieldSearcherManager;
	}

	public void setSearcherManager(SearcherManager inSearcherManager)
	{
		fieldSearcherManager = inSearcherManager;
	}

	public Validator getValidator()
	{
		return fieldValidator;
	}

	public void setValidator(Validator inValidator)
	{
		fieldValidator = inValidator;
	}

	protected boolean validate(String value, String type)
	{
		if ("email".equals(type))
		{
			if (value != null && value.indexOf("@") > -1 && value.indexOf(".") > -1)
			{
				return true;
			}
		}
		if ("required".equals(type))
		{
			if (value != null && !value.equals(""))
			{
				return true;
			}
		}
		if ("integer".equals(type))
		{
			if (value != null)
			{
				try
				{
					Integer.parseInt(value);
					return true;
				}
				catch (NumberFormatException ex)
				{
					// invalid number
				}
			}
		}
		if ("creditcard".equals(type)) // This is not really possible without
		// exparation date
		{
			if (value != null && value.length() > 10)
			{
				// TODO: Add in the checksum?
				return true;
			}
		}

		if (type != null && type.matches("minlength:\\d+"))
		{
			try
			{
				int minlength = Integer.parseInt(type.split(":")[1]);
				return (value != null && value.length() >= minlength);
			}
			catch (NumberFormatException e)
			{
				return false;
			}
		}

		if (type != null && type.matches("maxlength:\\d+"))
		{
			try
			{
				int maxlength = Integer.parseInt(type.split(":")[1]);
				return (value == null || value.length() <= maxlength);
			}
			catch (NumberFormatException e)
			{
				return false;
			}
		}

		return false;
	}

}
