package org.openedit.entermedia.orders;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Test;

public class OrderManagerTest
{

	@Test
	public void testOrderDateDefault()
	{
		OrderManager mgr = new OrderManager();
		Calendar oneMonth = null;
		oneMonth = mgr.getOrderDefaultDate();
		assertNotNull("NULL date", oneMonth);
		assertEquals(Calendar.getInstance().get(Calendar.MONTH) + 1, oneMonth.get(Calendar.MONTH));
	}

}
