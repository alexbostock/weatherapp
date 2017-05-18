package InteractionDesign.Group3;

import java.io.IOException;
import java.time.LocalTime;

/**
 * A class used for testing purposes only
 */
public class Testing {
	public static void main(String[] args) throws APIException, CacheException, IOException {
		long time1 = System.nanoTime();

		WeatherCache cache = WeatherCache.getCache();

		// Trigger a refresh (API call)
		cache.setLocation("Cambridge, GB");

		LocalTime t1 = LocalTime.now();
		LocalTime t2 = t1.plusHours(4);

		System.out.println(t1 + " " + t2);

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

		System.out.println("");

		System.out.println("Search suggestions");
		System.out.println("Cam");
		System.out.println(cache.getSearchSuggestions("Cam"));
		System.out.println("");
		System.out.println("Camb");
		System.out.println(cache.getSearchSuggestions("Camb"));
		System.out.println("");
		System.out.println("Cambr");
		System.out.println(cache.getSearchSuggestions("Cambr"));
		System.out.println("");

		long time2 = System.nanoTime();

		System.out.println("Test completed in " + (time2 - time1) / 1000000 + "ms");
	}
}
