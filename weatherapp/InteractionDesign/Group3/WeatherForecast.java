package InteractionDesign.Group3;

import com.google.gson.JsonObject;

/**
 * Represents the weather data from the API, at a particular time
 */
public class WeatherForecast extends WeatherData {
	
	// The date-time of the forecast in 'YYYY-MM-DD 00:00:00' format
	String mDate;

	/**
	 * Constructs a WeatherForecast object from a json object
	 *
	 * @param	json	the json data from an API call
	 */
	public WeatherForecast(JsonObject json) {
		super(json);
		mDate = json.get("dt_txt").getAsString();
	}
	
	/**
	 * Gets the time stamp, in a slightly different format from IEEE standard.
	 * The format is YYYY-MM-DD 00:00:00
	 *
	 * @return	timestamp
	 */
	public String getDate() {
		return mDate;
	}
	
}
