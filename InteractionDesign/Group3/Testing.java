package InteractionDesign.Group3;

import java.io.IOException;
import java.time.LocalTime;

public class Testing {
	public static void main(String[] args) throws APIException, CacheException, IOException {
		WeatherCache cache = WeatherCache.getCache();

		// cache.setLocation("Cambridge, GB");

		// cache.refresh();

		LocalTime t1 = LocalTime.now();
		LocalTime t2 = t1.plusHours(4);

		System.out.println("Summary");
		System.out.println(cache.getSummary());
		System.out.println("Today");
		System.out.println(cache.getToday());
		System.out.println("This week");
		System.out.println(cache.getThisWeek());
		System.out.println("Items");
		System.out.println(cache.getItems(t1, t2));
		System.out.println("Warnings");
		System.out.println(cache.getWarnings());
	}
}
