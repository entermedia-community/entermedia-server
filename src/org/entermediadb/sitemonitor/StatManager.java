package org.entermediadb.sitemonitor;

import java.util.ArrayList;
import java.util.List;

import org.openedit.CatalogEnabled;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class StatManager implements CatalogEnabled
{
	protected String fieldCatalogId;

	private Stat buildStat(Stat stat, String inName, Object inValue, String error)
	{
		stat.setName(inName);
		if (inValue instanceof Long)
		{
			if (!inName.contains("Cpu"))
			{
				stat.setValue((Long) inValue / SiteMonitorModule.MEGABYTE);
			}
			else
			{
				stat.setValue((Long) inValue);
			}
		}
		if (inValue instanceof Double)
		{
			if (!inName.contains("Cpu"))
			{
				stat.setValue((Double) inValue / SiteMonitorModule.MEGABYTE);
			}
			else
			{
				stat.setValue((Double) inValue);
			}
		}
		if (error != null)
		{
			stat.setError(true);
			stat.setErrorMsg(error);
		}
		return stat;
	}

	public List<Stat> getStats()
	{
		List<Stat> stats = new ArrayList<Stat>();

		try
		{
			OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
			for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods())
			{
				method.setAccessible(true);
				if (method.getName().startsWith("get") && Modifier.isPublic(method.getModifiers()))
				{
					Stat stat = new Stat();
					String error = null;
					Object value = null;
					try
					{
						value = method.invoke(operatingSystemMXBean);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						error = e.toString();
					}
					stat = buildStat(stat, method.getName(), value, error);
					stats.add(stat);
				}
			}
			return stats;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return new ArrayList<Stat>();
	}

	public String getCatalogId()
	{
		return fieldCatalogId;
	}

	@Override
	public void setCatalogId(String inId)
	{
		this.fieldCatalogId = inId;
	}
}
