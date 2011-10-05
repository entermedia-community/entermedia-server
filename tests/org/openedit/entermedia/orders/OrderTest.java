package org.openedit.entermedia.orders;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Test;

public class OrderTest
{

	@Test
	public void testIsExpired()
	{
		Order order = new Order()
		{

			@Override
			protected String getStoredExpirationDate()
			{
				return new SimpleDateFormat("MM/dd/yyyy").format(Calendar.getInstance().getTime());
			}

		};
		assertNotNull("NULL order", order);
		assertTrue("not expired yet!", order.isExpired());

		Order notExpiredOrder = new Order()
		{
			@Override
			protected String getStoredExpirationDate()
			{
				Calendar c = Calendar.getInstance();
				c.add(Calendar.DAY_OF_YEAR, 1);
				return new SimpleDateFormat("MM/dd/yyyy").format(c.getTime());
			}
		};
		assertFalse("not expired yet!", notExpiredOrder.isExpired());
	}
}
