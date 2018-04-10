package org.entermediadb.sitemonitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.entermediadb.asset.MediaArchive;
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
		if (error != null)
		{
			stat.setValue(error);
		}
		else
		{
			if (!inName.contains("Cpu"))
			{
				Long tmp = (Long)inValue / SiteMonitorModule.MEGABYTE;
				stat.setValue((Object)tmp);
			}
			else
			{
				stat.setValue(inValue);
			}
		}
		return stat;
	}

	private Stat getTotalAssetsCount(MediaArchive archive) {
		Stat stat = new Stat();

		stat.setName("totalassets");
		Collection assets = archive.getAssetSearcher().query().all().search();
		stat.setValue(assets.size());
		return stat;
	}

	private Stat getClusterStatusHealth(MediaArchive archive) {
		Stat stat = new Stat();

		stat.setName("clusterhealth");
		String health = archive.getNodeManager().getClusterHealth();
		
		if (health != null)
		{
			stat.setValue(health);
		}
		else
		{
			stat.setValue("can't retrieve cluster health status");
		}
		return stat;
	}
	
	public List<Stat> getStats(MediaArchive archive)
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

			stats.add(getTotalAssetsCount(archive));
			stats.add(getClusterStatusHealth(archive));
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
