package InteractionDesign.Group3;

import com.google.gson.JsonObject;

public class WeatherForecast extends WeatherData {
	
	//the date-time of the forecast in 'YYYY-MM-DD 00:00:00' format
	String mDate;

	//gathers data from JSON document to fill fields
	public WeatherForecast(JsonObject json) {
		super(json);
		mDate = json.get("dt_txt").getAsString();
	}
	
	//returns mDate
	public String getDate() {
		return mDate;
	}
	
}
