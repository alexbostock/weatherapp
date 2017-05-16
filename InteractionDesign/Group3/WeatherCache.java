package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

// TODO - suggested locations (search suggestions)

/**
 * A cache of the most recently loaded weather data.
 * This acts as an interface between the frontend and the API.
 * The cache is saved to disk, so that is can be reloaded later.
 * This is a singleton. When instantiated, the cache is loaded from disk if a
 * cache file already exists.
 */
public class WeatherCache {
	private static WeatherCache theObj;

	private final String mCacheFile;
	private LocalDateTime mLastUpdated;
	private String mLocation;

	private Map<LocalDate, List<Record>> mThisWeek;
	private List<Record> mToday;
	private List<Warning> mWarnings;

	/**
	 * Returns the singleton instance of WeatherCache.
	 *
	 * @return					the singleton
	 * @throws	CacheException	if the cache file fails to load (which may be
	 *							because the cache file does not yet exist)
	 */
	public static WeatherCache getWeatherCacheObj() throws CacheException {
		if (theObj == null)
			theObj = new WeatherCache();

		return theObj;
	}

	private WeatherCache() throws CacheException {
		// Default values

		mCacheFile = "weatherCache.csv";
		mLocation = "Cambridge";

		loadFromDisk();
	}

	/**
	 * Gets a list of recommended items based on two time stamps and the daily
	 * forecast. Not yet implemented...
	 *
	 * @param	start	the first time stamp
	 * @param	fin		the second time stamp
	 * @return			a list of Items
	 */
	public List<Item> getItems(LocalTime start, LocalTime fin) {
		// Function of times and daily forecast - TODO

		return new ArrayList<>();
	}

	/**
	 * Gets the time stamp when the API was last accessed.
	 *
	 * @return			time stamp of last refresh
	 */
	public LocalDateTime getLastUpdated() {
		return mLastUpdated;
	}

	/**
	 * Gets the current location setting.
	 *
	 * @return			location string
	 */
	public String getLocation() {
		return mLocation;
	}

	/**
	 * Gets the weekly forecast. This is a map of date to forecast.
	 * The Map is ordered by date, and each list is ordered by time.
	 *
	 * @return			the forecast for each day of the week
	 */
	public Map<LocalDate, List<Record>> getThisWeek() {
		return mThisWeek;
	}

	/**
	 * Gets the forecast for today. Each record gives weather data for a
	 * particular time, as well as a time stamp.
	 * The list is ordered.
	 * Watch out for daily forecast not referring to today, if the cache is out
	 * of date (check time stamps).
	 *
	 * @return			daily weather forecast
	 */
	public List<Record> getToday() {
		return mToday;
	}

	/**
	 * Gets any weather warnings.
	 *
	 * @return			list of warnings
	 */
	public List<Warning> getWarnings() {
		return mWarnings;
	}

	private void loadFromDisk() throws CacheException {
		File f = new File(mCacheFile);

		if (! f.isFile())
			// Note that this case is
			throw new CacheException("No cache file present");

		try (BufferedReader br = new BufferedReader(new FileReader(mCacheFile))) {

			// Load time stamp

			String line = br.readLine();
			mLastUpdated = LocalDateTime.parse(line);

			line = br.readLine();
			mLocation = line;

			br.readLine();	// Should be a blank line

			// Load weekly forecast

			mThisWeek = new TreeMap<>();

			List<Record> list = new ArrayList<>();

			while (! (line = br.readLine()).equals("")) {
				if (line.startsWith("day")) {
					LocalDate date = LocalDate.parse(line.split(":")[1]);
					list = new ArrayList<>();
					mThisWeek.put(date, list);

				} else {
					list.add(new Record(line));
				}
			}

			// Load daily forecast (not necessarily today's !)

			mToday = new ArrayList<>();

			while (! (line = br.readLine()).equals("")) {
				mToday.add(new Record(line));
			}

			// Load weather warnings

			mWarnings = new ArrayList<>();

			while ((line = br.readLine()) != null) {
				mWarnings.add(Warning.valueOf(line));
			}

			// TODO - check whether the cached daily forecast saved is today's
				// (Low priority - should be fine for the tick)

		} catch (DateTimeParseException e) {
			throw new CacheException("Invalid cache file");

		} catch (IOException e) {
			throw new CacheException("Failed to load cache file");
		}
	}

	/**
	 * Accesses the API and updates the cache. The latest weather forecast is
	 * downloaded. If no exception is thrown, the latest data can be accessed
	 * through getters. The latest forecast is also saved to file, so that it
	 * can be loaded if the app is restarted.
	 *
	 * @throws	APIException	if downloading weather forecast fails
	 * @throws	CacheException	if saving to disk fails
	 */
	public void refresh() throws APIException, CacheException {
		// API call

		// mToday =
		// mThisWeek =
		// mWarnings =

		saveToDisk();

		// Save time stamp (if it succeeded)
		mLastUpdated = LocalDateTime.now();

		// Don't overwrite previous values before API call has succeeded
			// - old data > no data

		// Ensure that mToday and mThisWeek are sorted chronologically

		// Doesn't return any value
			// Exception thrown if it fails (hopefully with a message)
			// Data can be fetched with getters
	}

	private void saveToDisk() throws CacheException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(mCacheFile))) {

			// Save time stamp

			bw.write(mLastUpdated.toString());

			bw.newLine();

			// Save location

			bw.write(mLocation);

			bw.newLine();

			// Blank line

			bw.newLine();

			// Save weekly forecast

			for (Entry<LocalDate, List<Record>> entry : mThisWeek.entrySet()) {
				bw.write(entry.getKey().toString());
				bw.newLine();

				for (Record r : entry.getValue()) {
					bw.write(r.toString());
					bw.newLine();
				}
			}

			bw.newLine();

			// Save daily forecast

			for (Record r : mToday) {
				bw.write(r.toString());
				bw.newLine();
			}

			bw.newLine();

			// Save weather warnings

			bw.newLine();

			for (Warning w : mWarnings) {
				bw.write(w.toString());
				bw.newLine();
			}

		} catch (IOException e) {
			throw new CacheException("Failed to save to cache file");
		}	
	}

	/**
	 * Updates the location and saves to disk.
	 * Note that this is only a string, and not currently validated.
	 * We should look at validating it with the API.
	 *
	 * @param	l				location string
	 * @throws	CacheException	if saving to disk fails
	 */
	public void setLocation(String l) throws CacheException {
		mLocation = l;

		saveToDisk();
	}
}
