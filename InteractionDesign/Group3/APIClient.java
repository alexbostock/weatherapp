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

public class APIClient {

	//the part of the URL corresponding to the API key
	private final String APPID = "&APPID=dcf195ce911b00f98dcd3f9f077cb234";
	//the start of every URL call to the API
	private final String baseURL = "http://api.openweathermap.org/data/2.5/";
	
	//returns a WeatherData object for the weather at the city specified at this current point in time
	//location must be in the form city name and country code divided by comma, use ISO 3166 country codes
	public WeatherData currentWeatherAtCity (String location) throws IOException {
		String subURL = "weather?q=" + location;
		JsonObject response = query(subURL);
		return new WeatherData(response);
	}
	
	//returns a WeatherData object for the weather at the city specified at this current point in time
	//city is specified by cityID (recommended method)
	public WeatherData currentWeatherAtCity (int cityID) throws IOException {
		String subURL = "weather?id=" + cityID;
		JsonObject response = query(subURL);
		return new WeatherData(response);
	}
	
	//returns a list of WeatherForecast objects for every 3 hours over the next 5 days for the city specified
	//location must be in the form city name and country code divided by comma, use ISO 3166 country codes
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
	
	//returns a list of WeatherForecast objects for every 3 hours over the next 5 days for the city specified
	//objects are in ascending order according to the mDate field (see WeatherForecast class)
	//locations is specified by cityID (recommended method)
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
	
	//queries the API for weather data corresponding to the URL argument
	//returns a JsonObject that holds the weather data
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
