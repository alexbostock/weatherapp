package InteractionDesign.Group3;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherForecast {

	private List<WeatherData> mForecasts = new ArrayList<>();
	
	public WeatherForecast(JSONObject json) throws JSONException {
		JSONArray JSONlist = json.getJSONArray("list");
		for (int i = 0; i < JSONlist.length(); ++i) {
			mForecasts.add(new WeatherData(JSONlist.getJSONObject(i)));
		}
	}
	
	public WeatherData getForecast(int t) {
		if (t < 0 || t > mForecasts.size()-1) throw new IllegalArgumentException("There is no forecast for that day");
		return mForecasts.get(t);
	}
	
}
