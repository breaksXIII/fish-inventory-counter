package com.fishinvcounter;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class FormatHoursTest
{
	@Test
	public void formatsHoursAndMinutes()
	{
		assertEquals("45m", FishInvCounterOverlay.formatHours(0.75));
		assertEquals("1h 40m", FishInvCounterOverlay.formatHours(1 + 40 / 60.0));
	}
}
