package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Provides an interface for fetching weather data and location search suggestions.
 * Weather data from the openweathermap API. (https://openweathermap.org/)
 * Search suggestions from a local file
 */
public class APIClient {

	// Array of locations for which we have data
	private String[] mLocations;
	// The part of the URL corresponding to the API key
	private final String APPID = "&APPID=dcf195ce911b00f98dcd3f9f077cb234";
	// The start of every URL call to the API
	private final String baseURL = "http://api.openweathermap.org/data/2.5/";
	// Location of json document with locations on disk
	private final String mPath;

	/**
	 * Instantiates the client, and loads location suggestions from disk.
	 *
	 * @param	file	location of the city list file
	 * @throws	FileNotFoundException
	 */
	public APIClient(String file) throws FileNotFoundException {
		mPath = file;

		JsonReader reader = new JsonReader(new FileReader(mPath));
		JsonParser parser = new JsonParser();
		JsonArray array = parser.parse(reader).getAsJsonArray();
		mLocations = new String[array.size()];
		for (int i = 0; i < mLocations.length; ++i) {
			JsonObject loc = array.get(i).getAsJsonObject();
			mLocations[i] = loc.get("name").getAsString() + ", " + loc.get("country").getAsString();
		}
	}

	/**
	 * Gets the current weather data at a particular city, from the city name.
	 *
	 * @param	location	the name of a city, and its ISO 3166 country code, separated by a comma eg. "London, GB"
	 * @return		details of the current weather in that location
	 * @throws	IOException	if the API request fails
	 */
	public WeatherData currentWeatherAtCity (String location) throws IOException {
		String subURL = "weather?q=" + location;
		JsonObject response = query(subURL);
		return new WeatherData(response);
	}

	/**
	 * Gets the current weather data at a particular city, from the city ID.
	 * City IDs are defined by openweather map. They are listed at http://openweathermap.org/help/city_list.txt
	 *
	 * @param	cityID	the city ID
	 * @return		details of the current weather in that location
	 * @throws	IOException	if the API request fails
	 */
	public WeatherData currentWeatherAtCity (int cityID) throws IOException {
		String subURL = "weather?id=" + cityID;
		JsonObject response = query(subURL);
		return new WeatherData(response);
	}
	
	/**
	 * Gets the 5 day weather forecast for a particular city, from the city name.
	 * City IDs are defined by openweather map. They are listed at http://openweathermap.org/help/city_list.txt
	 *
	 * @param	location	the name of a city, and its ISO 3166 country code, separated by a comma eg. "London, GB"
	 * @return		details of the weather forecast in that location
	 * @throws	IOException	if the API request fails
	 */
	public List<WeatherForecast> forecastWeatherAtCity (String location) throws IOException {
		String subURL = "forecast?q=" + location;
		JsonObject response = query(subURL);
		List<WeatherForecast> forecasts = new ArrayList<>();
		JsonArray JSONlist = response.getAsJsonArray("list");
		for (int i = 0; i < JSONlist.size(); ++i) {
			forecasts.add(new WeatherForecast(JSONlist.get(i).getAsJsonObject()));
		}
		return forecasts;
	}
	
	/**
	 * Gets the 5 day weather forecast for a particular city, from the city ID.
	 * City IDs are defined by openweather map. They are listed at http://openweathermap.org/help/city_list.txt
	 *
	 * @param	cityID	the city ID
	 * @return		details of the weather forecast in that location
	 * @throws	IOException	if the API request fails
	 */
	public List<WeatherForecast> forecastWeatherAtCity (int cityID) throws IOException {
		String subURL = "forecast?id=" + cityID;
		JsonObject response = query(subURL);
		List<WeatherForecast> forecasts = new ArrayList<>();
		JsonArray JSONlist = response.getAsJsonArray("list");
		for (int i = 0; i < JSONlist.size(); ++i) {
			forecasts.add(new WeatherForecast(JSONlist.get(i).getAsJsonObject()));
		}
		return forecasts;
	}
	
	// Queries the API for weather data corresponding to the URL argument
	// Returns a JsonObject that holds the weather data
	private JsonObject query(String subURL) throws IOException {
	    BufferedReader reader = null;
	    try {
	    	JsonParser parser = new JsonParser();
	        URL url = new URL(baseURL + subURL + APPID);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        return parser.parse(reader).getAsJsonObject();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
}
