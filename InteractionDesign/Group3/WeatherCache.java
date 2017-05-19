package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cache of the most recently loaded weather data.
 * This acts as an interface between the frontend and the API.
 * Any data fetched are no more than 1 hour old.
 * This is a singleton. When instantiated, the cache is loaded from disk if a
 * cache file already exists.
 */
public class WeatherCache {
	private static WeatherCache theObj;

	private APIClient mGordon;
	private SearchSuggester mSearchSug;

	private Map<WeatherData.ConditionCode, Icon> mIconMap;

	private final String mCacheFile;
	private final String mCityListFile;

	private LocalDateTime mLastUpdated;
	private String mLocation;

	private Record mSummary;
	private List<List<Record>> mThisWeek;
	private List<Warning> mWarnings;

	private LocalDateTime mSunrise;
	private LocalDateTime mSunset;

	/**
	 * Returns the singleton instance of WeatherData.
	 * After this call, the cache will have up to date weather data, unless an exception is thrown.
	 *
	 * @return	the singleton
	 * @throws	APIException	if fetching weather data from the API fails
	 * @throws	CacheException	if the cache file is invalid
	 */
	public static WeatherCache getCache() throws APIException, CacheException {
		if (theObj == null)
			theObj = new WeatherCache();

		return theObj;
	}

	private WeatherCache() throws APIException, CacheException {
		long t1 = System.nanoTime();

		// Default values

		mCacheFile = "data/weatherCache.csv";
		mCityListFile = "data/cityList.txt";

		mLocation = "Cambridge, GB";

		makeIconMap();

		mGordon = new APIClient();

		loadFromDisk();

		if (! isFresh())
			refresh();

		long t2 = System.nanoTime();

		try {
			mSearchSug = new SearchSuggester(mCityListFile);

		} catch (IOException e) {
			// Note that this state should never occur in usage
			// It means the install is invalid

			System.out.println("Fatal error");
			System.out.println("City list file not present");
			System.out.println("File must be present at data/cityList.text");
			System.exit(1);
		}

		long t3 = System.nanoTime();

		System.out.println("SearchSuggester instantiated in " + (t3 - t2) / 1000000 + "ms");
		System.out.println("WeatherCache instantiated in " + (t3 - t1) / 1000000 + "ms (including the above)");
	}

	/**
	 * Gets a list of recommended items based on two time stamps and the daily
	 * forecast.
	 *
	 * @param	start	the first time stamp
	 * @param	fin		the second time stamp
	 * @return	a list of Items
	 * @throws	APIException	if fetching weather data from the API fails
	 * @throws	CacheException	if the cache file is invalid
	 */
	public List<Item> getItems(LocalTime start, LocalTime fin) throws APIException, CacheException {
		if (! isFresh())
			refresh();

		if (mThisWeek == null)
			throw new CacheException("Recommending items failed");

		boolean rain = false;
		boolean cold = false;
		boolean sunny = false;
		boolean heavyRain = false;
		boolean snow = false;

		for (Record r : getToday()) {
			LocalTime t = r.getTimeStamp().toLocalTime();

			if (t.compareTo(start) > 0 || t.compareTo(fin.plusHours(1)) < 0) {
				Icon i = r.getIcon();

				heavyRain = heavyRain || (i == Icon.HEAVY_RAIN || i == Icon.HAIL || i == Icon.THUNDERSTORM);

				snow = snow || (i == Icon.HEAVY_SNOW || i == Icon.LIGHT_SNOW || i == Icon.SNOWFLAKE);

				rain = rain || (i == Icon.LIGHT_RAIN);

				sunny = sunny || (i == Icon.SUN);

				cold = cold || (r.getTemp() < 10);
			}
		}

		rain = rain || heavyRain || snow;

		boolean dark = (start.compareTo(mSunrise.toLocalTime()) < 0
						|| fin.plusHours(1).compareTo(mSunset.toLocalTime()) > 0);

		List<Item> result = new ArrayList<>();

		if (dark)
			result.add(Item.LIGHTS);

		if (rain || cold)
			result.add(Item.COAT);

		if (cold)
			result.add(Item.GLOVES);

		if (rain)
			result.add(Item.SEAT_COVER);

		if (sunny)
			result.add(Item.SUNGLASSES);

		if (heavyRain || snow)
			result.add(Item.BAG_COVER);

		result.add(Item.HELMET);

		return result;
	}

	/**
	 * Gets the time stamp when the API was last accessed.
	 *
	 * @return	time stamp of last refresh
	 */
	public LocalDateTime getLastUpdated() {
		return mLastUpdated;
	}

	/**
	 * Gets the current location setting.
	 * Location should be of the form [city name], [ISO 3166 country code]
	 *
	 * @return	location string
	 */
	public String getLocation() {
		return mLocation;
	}

	/**
	 * Recommends cities given the start of a city name.
	 * The input is case-insensitive.
	 * Returns null if the input is less than 3 characters.
	 * The value returned is of the form [city name], [ISO 3166 country code]
	 *
	 * @param	s	the beginning of a city name
	 * @return	a list of cities matching the request
	 */
	public List<String> getSearchSuggestions(String s) {
		return mSearchSug.getSuggestions(s);
	}

	/**
	 * Gets the headline summary of the current weather.
	 *
	 * @return	the weather right now
	 * @throws	APIException	if fetching weather data from the API fails
	 * @throws	CacheException	if the cache file is invalid
	 */
	public Record getSummary() throws APIException, CacheException {
		if (! isFresh())
			refresh();

		return mSummary;
	}

	/**
	 * Gets the 5 day forecast.
	 * Each element in the list is the forecast for one day.
	 * Each day is a list of record with forecast every 3 hours.
	 * Every record has a time stamp, and the lists are sorted chronologically.
	 *
	 * @return	the forecast for each day of the week
	 * @throws	APIException	if fetching weather data from the API fails
	 * @throws	CacheException	if the cache file is invalid
	 */
	public List<List<Record>> getThisWeek() throws APIException, CacheException {
		if (! isFresh())
			refresh();

		return mThisWeek;
	}

	/**
	 * Gets the forecast for today. Each record gives weather data for a
	 * particular time, as well as a time stamp.
	 * Records are 3 hours apart.
	 * The list is ordered chronolgically.
	 *
	 * @return	daily weather forecast
	 * @throws	APIException	if fetching weather data from the API fails
	 * @throws	CacheException	if the cache file is invalid
	 */
	public List<Record> getToday() throws APIException, CacheException {
		if (! isFresh())
			refresh();

		return mThisWeek.get(0);
	}

	/**
	 * Gets any weather warnings.
	 *
	 * @return	list of warnings
	 * @throws	APIException	if fetching weather data from the API fails
	 * @throws	CacheException	if the cache file is invalid
	 */
	public List<Warning> getWarnings() throws APIException, CacheException {
		if (! isFresh())
			refresh();

		return mWarnings;
	}

	private boolean isFresh() {
		if (mLastUpdated == null)
			return false;

		int comp = LocalDateTime.now().compareTo(mLastUpdated.plusHours(1));

		return comp < 0;
	}

	private void loadFromDisk() throws CacheException {
		File f = new File(mCacheFile);

		if (f.isFile()) {
			try (BufferedReader br = new BufferedReader(new FileReader(mCacheFile))) {

				// Load time stamp

				String line = br.readLine();
				mLastUpdated = LocalDateTime.parse(line);

				line = br.readLine();
				mLocation = line;

				// Load sunrise & sunset

				line = br.readLine();
				mSunrise = LocalDateTime.parse(line);

				line = br.readLine();
				mSunset = LocalDateTime.parse(line);

				br.readLine();	// Should be a blank line

				// Load weekly forecast

				mThisWeek = new ArrayList<>();

				List<Record> list = new ArrayList<>();

				while (! (line = br.readLine()).equals("")) {
					if (line.equals("___")) {
						mThisWeek.add(list);
						list = new ArrayList<>();

					} else {
						list.add(new Record(line));
					}
				}

				// Load daily summary

				line = br.readLine();
				mSummary = new Record(line);

				br.readLine();

				// Load weather warnings

				mWarnings = new ArrayList<>();

				while ((line = br.readLine()) != null) {
					if (! line.equals(""))
						mWarnings.add(Warning.valueOf(line));
				}

			} catch (DateTimeParseException e) {
				throw new CacheException("Invalid cache file");

			} catch (IOException e) {
				throw new CacheException("Failed to load cache file");
			}
			
		} else {
			System.out.println("No cache file present");
		}
	}

	private void makeIconMap() {
		mIconMap = new HashMap<>();

		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM_WITH_LIGHT_RAIN, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM_WITH_RAIN, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM_WITH_HEAVY_RAIN, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.LIGHT_THUNDERSTORM, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.HEAVY_THUNDERSTORM, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.RAGGED_THUNDERSTORM, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM_WITH_LIGHT_DRIZZLE, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM_WITH_DRIZZLE, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.THUNDERSTORM_WITH_HEAVY_DRIZZLE, Icon.THUNDERSTORM);
		mIconMap.put(WeatherData.ConditionCode.LIGHT_INTENSITY_DRIZZLE, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.DRIZZLE, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.HEAVY_INTENSITY_DRIZZLE, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.LIGHT_INTENSITY_DRIZZLE_RAIN, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.DRIZZLE_RAIN, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.HEAVY_INTENSITY_DRIZZLE_RAIN, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.SHOWER_DRIZZLE, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.LIGHT_RAIN, Icon.LIGHT_RAIN);
		mIconMap.put(WeatherData.ConditionCode.MODERATE_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.HEAVY_INTENSITY_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.VERY_HEAVY_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.EXTREME_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.FREEZING_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.LIGHT_INTENSITY_SHOWER_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.SHOWER_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.HEAVY_INTENSITY_SHOWER_RAIN, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.LIGHT_SNOW, Icon.LIGHT_SNOW);
		mIconMap.put(WeatherData.ConditionCode.SNOW, Icon.HEAVY_SNOW);
		mIconMap.put(WeatherData.ConditionCode.HEAVY_SNOW, Icon.HEAVY_SNOW);
		mIconMap.put(WeatherData.ConditionCode.SLEET, Icon.HAIL);
		mIconMap.put(WeatherData.ConditionCode.SHOWER_SNOW, Icon.HEAVY_SNOW);
		mIconMap.put(WeatherData.ConditionCode.MIST, Icon.MIST_DAY);
		mIconMap.put(WeatherData.ConditionCode.SMOKE, Icon.MIST_DAY);
		mIconMap.put(WeatherData.ConditionCode.HAZE, Icon.MIST_DAY);
		mIconMap.put(WeatherData.ConditionCode.SAND_OR_DUST_WHIRLS, Icon.MIST_DAY);
		mIconMap.put(WeatherData.ConditionCode.FOG, Icon.MIST_DAY);
		mIconMap.put(WeatherData.ConditionCode.SKY_IS_CLEAR, Icon.SUN);
		mIconMap.put(WeatherData.ConditionCode.FEW_CLOUDS, Icon.PARTLY_CLEAR_DAY);
		mIconMap.put(WeatherData.ConditionCode.SCATTERED_CLOUDS, Icon.PARTLY_CLEAR_DAY);
		mIconMap.put(WeatherData.ConditionCode.BROKEN_CLOUDS, Icon.LIGHT_CLOUDS);
		mIconMap.put(WeatherData.ConditionCode.OVERCAST_CLOUDS, Icon.HEAVY_CLOUDS);
		mIconMap.put(WeatherData.ConditionCode.TORNADO, Icon.WIND);
		mIconMap.put(WeatherData.ConditionCode.TROPICAL_STORM, Icon.HEAVY_RAIN);
		mIconMap.put(WeatherData.ConditionCode.HURRICANE, Icon.WIND);
		mIconMap.put(WeatherData.ConditionCode.COLD, Icon.SNOWFLAKE);
		mIconMap.put(WeatherData.ConditionCode.HOT, Icon.SUN);
		mIconMap.put(WeatherData.ConditionCode.WINDY, Icon.WIND);
		mIconMap.put(WeatherData.ConditionCode.HAIL, Icon.HAIL);
	}

	private Icon mapIcon(WeatherData.ConditionCode c, LocalDateTime t) {
		LocalTime time = t.toLocalTime();
		LocalTime rise = mSunrise.toLocalTime();
		LocalTime set = mSunset.toLocalTime();

		Icon i = mIconMap.get(c);

		if (time.compareTo(rise) < 0 || time.compareTo(set) > 0) {
			if (i == Icon.MIST_DAY)
				i = Icon.MIST_NIGHT;

			if (i == Icon.SUN)
				i = Icon.MOON;

			if (i == Icon.PARTLY_CLEAR_DAY)
				i = Icon.PARTLY_CLEAR_NIGHT;
		}

		return i;
	}

	private void refresh() throws APIException, CacheException {
		long time1 = System.nanoTime();

		// API call

		WeatherData data;
		List<WeatherForecast> forecasts;

		try {
			data = mGordon.currentWeatherAtCity(mLocation);
			forecasts = mGordon.forecastWeatherAtCity(mLocation);

		} catch (IOException e) {
			throw new APIException(e.getMessage());
		}

		// Sunrise and sunset times

		int sr = data.getSunrise();
		int ss = data.getSunset();

		mSunrise = LocalDateTime.ofInstant(Instant.ofEpochSecond(sr), ZoneId.systemDefault());
		mSunset = LocalDateTime.ofInstant(Instant.ofEpochSecond(ss), ZoneId.systemDefault());

		// Current summary

		LocalDateTime time = LocalDateTime.now();

		Icon i = mapIcon(data.getConditionCode(), time);
		int temp = (int) Math.round(data.getTemperature());

		mSummary = new Record(i, temp, time);
		mSummary.setLabel("Current");

		// Weekly forecast

		mThisWeek = new ArrayList<>();

		List<Record> current = new ArrayList<>();
		mThisWeek.add(current);

		int count = 1;

		for (WeatherForecast wf : forecasts) {
			String[] s = wf.getDate().split(" ");

			LocalDateTime t = LocalDateTime.parse(s[0] + "T" + s[1]);

			if (! t.toLocalDate().equals(time.toLocalDate())) {
				current = new ArrayList<>();
				mThisWeek.add(current);

				count++;
			}

			time = t;

			i = mapIcon(wf.getConditionCode(), time);
			temp = (int) Math.round(wf.getTemperature());

			Record r = new Record(i, temp, time);

			switch (count) {
				case 1: r.setLabel("Today");
				break;

				case 2: r.setLabel("Tomorrow");
				break;

				default: r.setLabel(time.getDayOfWeek().toString());
			}

			current.add(r);
		}

		// Save time stamp
		mLastUpdated = LocalDateTime.now();

		// Weather warnings

		mWarnings = new ArrayList<>();

		boolean ice = false;
		boolean vis = false;
		boolean storm = false;

		for (Record r : getToday()) {
			ice = ice || (r.getTemp() < 5);

			i = r.getIcon();

			vis = vis || (i == Icon.MIST || i == Icon.MIST_DAY || i == Icon.MIST_NIGHT);

			storm = storm || (i == Icon.THUNDERSTORM);
		}

		if (ice)
			mWarnings.add(Warning.ICY);

		if (vis)
			mWarnings.add(Warning.POOR_VISIBILITY);

		if (storm)
			mWarnings.add(Warning.STORMY);

		saveToDisk();

		long time2 = System.nanoTime();

		System.out.println("Data refreshed in " + (time2 - time1) / 1000000 + "ms (including saving to disk)");
	}

	private void saveToDisk() throws CacheException {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(mCacheFile))) {

			// Save time stamp

			bw.write(mLastUpdated.toString());
			bw.newLine();

			// Save location

			bw.write(mLocation);
			bw.newLine();

			// Sunrise & sunset

			bw.write(mSunrise.toString());
			bw.newLine();

			bw.write(mSunset.toString());
			bw.newLine();

			// Blank line

			bw.newLine();

			// Save weekly forecast

			for (List<Record> entry : mThisWeek) {
				for (Record r : entry) {
					bw.write(r.toString());
					bw.newLine();
				}

				bw.write("___");
				bw.newLine();
			}

			bw.newLine();

			// Save current summary

			bw.write(mSummary.toString());
			bw.newLine();

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
	 * Updates the location and fetches weather data for that location.
	 *
	 * @param	l				location string, in the form "[city name], [ISO 3166 country code]"
	 * @throws	APIException	if downloading weather data fails
	 * @throws	CacheException	if saving to disk fails
	 */
	public void setLocation(String l) throws APIException, CacheException {
		mLocation = l;

		refresh();
	}
}
