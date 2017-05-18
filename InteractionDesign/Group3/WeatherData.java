package InteractionDesign.Group3;

import com.google.gson.JsonObject;

/**
 * Represents the data returned by an API request
 */
public class WeatherData {
	
	/**
	 * A brief summary of the weather, according to the API
	 */
	public static enum ConditionCode {
		//weather conditions and their corresponding OWM weather_id
		UNKNOWN                         (000),
		THUNDERSTORM_WITH_LIGHT_RAIN    (200),
		THUNDERSTORM_WITH_RAIN          (201),
		THUNDERSTORM_WITH_HEAVY_RAIN    (202),
		LIGHT_THUNDERSTORM              (210),
		THUNDERSTORM                    (211),
		HEAVY_THUNDERSTORM              (212),
		RAGGED_THUNDERSTORM             (221),
		THUNDERSTORM_WITH_LIGHT_DRIZZLE (230),
		THUNDERSTORM_WITH_DRIZZLE       (231),
		THUNDERSTORM_WITH_HEAVY_DRIZZLE (232),
		LIGHT_INTENSITY_DRIZZLE         (300),
		DRIZZLE                         (301),
		HEAVY_INTENSITY_DRIZZLE         (302),
		LIGHT_INTENSITY_DRIZZLE_RAIN    (310),
		DRIZZLE_RAIN                    (311),
		HEAVY_INTENSITY_DRIZZLE_RAIN    (312),
		SHOWER_DRIZZLE                  (321),
		LIGHT_RAIN                      (500),
		MODERATE_RAIN                   (501),
		HEAVY_INTENSITY_RAIN            (502),
		VERY_HEAVY_RAIN                 (503),
		EXTREME_RAIN                    (504),
		FREEZING_RAIN                   (511),
		LIGHT_INTENSITY_SHOWER_RAIN     (520),
		SHOWER_RAIN                     (521),
		HEAVY_INTENSITY_SHOWER_RAIN     (522),
		LIGHT_SNOW                      (600),
		SNOW                            (601),
		HEAVY_SNOW                      (602),
		SLEET                           (611),
		SHOWER_SNOW                     (621),
		MIST                            (701),
		SMOKE                           (711),
		HAZE                            (721),
		SAND_OR_DUST_WHIRLS             (731),
		FOG                             (741),
		SKY_IS_CLEAR                    (800),
		FEW_CLOUDS                      (801),
		SCATTERED_CLOUDS                (802),
		BROKEN_CLOUDS                   (803),
		OVERCAST_CLOUDS                 (804),
		TORNADO                         (900),
		TROPICAL_STORM                  (901),
		HURRICANE                       (902),
		COLD                            (903),
		HOT                             (904),
		WINDY                           (905),
		HAIL                            (906);

		private int id;
		
		private ConditionCode (int id) {
			this.id = id;
		}
		
		public static ConditionCode valueof (int id) {
			for (ConditionCode condition : ConditionCode.values()) {
				if (condition.id == id)
					return condition;
			}
			return ConditionCode.UNKNOWN;
		}

		public int getId () {
			return this.id;
		}
	}
	
	// Enum representing the overall condition of the weather
	private final ConditionCode mCode;
	// A description of the weather i.e. the text in the condition code
	private final String mDescription;
	
	// Temperature in celcius
	private final double mTemp;
	// Atmospheric pressure in hPa (hectopascals)
	private final double mPressure;
	// Humidity as a %
	private final double mHumidity;
	
	// Wind speed in m/s
	private final double mWindSpeed;
	
	// Sunrise time in unix UTC
	private final int mSunrise;
	// Sunset time in unic UTC
	private final int mSunset;
	
	/**
	 * Constructs a WeatherData object from a json object
	 *
	 * @param	json	the json data from an API call
	 */
	public WeatherData(JsonObject json) {
		JsonObject jsonWeather = json.getAsJsonArray("weather").get(0).getAsJsonObject();
		mCode = ConditionCode.valueof(jsonWeather.get("id").getAsInt());
		mDescription = jsonWeather.get("description").getAsString();
		
		JsonObject jsonMain = json.getAsJsonObject("main");
		mTemp = jsonMain.get("temp").getAsDouble() - 273.15;
		mPressure = jsonMain.get("pressure").getAsDouble();
		mHumidity = jsonMain.get("humidity").getAsDouble();
		
		JsonObject jsonWind = json.getAsJsonObject("wind");
		mWindSpeed = jsonWind.get("speed").getAsDouble();
		
		//WeatherForecast objects have their sunrise/sunset time set to -1
		//since this information is not contained in the forecast JSON documents
		JsonObject jsonSys = json.getAsJsonObject("sys");
		if (jsonSys.has("sunrise")) { 
			mSunrise = jsonSys.get("sunrise").getAsInt();
			mSunset = jsonSys.get("sunset").getAsInt();
		} else {
			mSunrise = -1;
			mSunset = -1;
		}
	}
	
	/**
	 * Gets the condition code from the API. eg. DRIZZLE, SNOW, MIST etc.
	 *
	 * @return	condition code
	 */
	public ConditionCode getConditionCode() {
		return mCode;
	}
	
	/**
	 * Gets a description of the weather.
	 * This is basically a string version of the condition code
	 *
	 * @return	weather description
	 */
	public String getDescription() {
		return mDescription;
	}
	
	/**
	 * Gets the temperature, in degrees Celcius.
	 *
	 * @return	temperature
	 */
	public double getTemperature() {
		return mTemp;
	}
	
	/**
	 * Gets the atmospheric pressure in hectoPascals
	 *
	 * @return	atmospheric pressure
	 */
	public double getPressure() {
		return mPressure;
	}
	
	/**
	 * Gets the humidity as a perentage
	 *
	 * @return	humidity
	 */
	public double getHumidity() {
		return mHumidity;
	}
	
	/**
	 * Gets the wind speed in metres/second
	 *
	 * @return	wind speed
	 */
	public double getWindSpeed() {
		return mWindSpeed;
	}
	
	/**
	 * Gets the sunrise time, as a unix time stamp
	 *
	 * @return	sunrise time
	 */
	public int getSunrise() {
		return mSunrise;
	}
	
	/**
	 * Gets the sunset time, as a unix time stamp
	 *
	 * @return	sunset time
	 */
	public int getSunset() {
		return mSunset;
	}
	
}
