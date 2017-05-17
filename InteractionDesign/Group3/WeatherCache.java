package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

	private APIClient mGordon;

	private final String mCacheFile;
	private LocalDateTime mLastUpdated;
	private String mLocation;

	private Record mSummary;
	private List<List<Record>> mThisWeek;
	private List<Warning> mWarnings;

	private LocalDateTime mSunrise;
	private LocalDateTime mSunset;

	private Map<WeatherData.ConditionCode, Icon> mIconMap;

	/**
	 * Returns the singleton instance of WeatherData.
	 *
	 * @return					the singleton
	 * @throws	CacheException	if the cache file fails to load (which may be
	 *							because the cache file does not yet exist)
	 */
	public static WeatherCache getCache() throws APIException, CacheException {
		if (theObj == null)
			theObj = new WeatherCache();

		return theObj;
	}

	private WeatherCache() throws APIException, CacheException {
		mGordon = new APIClient();

		makeIconMap();

		// Default values

		mCacheFile = "weatherCache.csv";
		mLocation = "Cambridge, GB";

		loadFromDisk();

		if (! isFresh())
			refresh();
	}

	/**
	 * Gets a list of recommended items based on two time stamps and the daily
	 * forecast. Not yet implemented...
	 *
	 * @param	start	the first time stamp
	 * @param	fin		the second time stamp
	 * @return			a list of Items
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

		boolean dark = (start.compareTo(mSunrise.toLocalTime()) < 0 || fin.plusHours(1).compareTo(mSunset.toLocalTime()) > 0);

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

	public Record getSummary() throws APIException, CacheException {
		if (! isFresh())
			refresh();

		return mSummary;
	}

	/**
	 * Gets the weekly forecast. This is a map of date to forecast.
	 * The Map is ordered by date, and each list is ordered by time.
	 *
	 * @return			the forecast for each day of the week
	 */
	public List<List<Record>> getThisWeek() throws APIException, CacheException {
		if (! isFresh())
			refresh();

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
	public List<Record> getToday() throws APIException, CacheException {
		if (! isFresh())
			refresh();

		return mThisWeek.get(0);
	}

	/**
	 * Gets any weather warnings.
	 *
	 * @return			list of warnings
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

		if (! f.isFile()) {
			System.out.println("No cache file present");
			return;
		}

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
					list = new ArrayList<>();
					mThisWeek.add(list);

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

		mSunrise = LocalDateTime.ofInstant(Instant.ofEpochMilli(sr), ZoneId.systemDefault());
		mSunset = LocalDateTime.ofInstant(Instant.ofEpochMilli(ss), ZoneId.systemDefault());

		// Current summary

		LocalDateTime time = LocalDateTime.now();

		Icon i = mapIcon(data.getConditionCode(), time);
		int temp = (int) data.getTemperature();

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
			temp = (int) data.getTemperature();

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
	 * Updates the location and saves to disk.
	 * Note that this is only a string, and not currently validated.
	 * We should look at validating it with the API.
	 *
	 * @param	l				location string
	 * @throws	CacheException	if saving to disk fails
	 */
	public void setLocation(String l) throws APIException, CacheException {
		mLocation = l;

		refresh();
	}
}
