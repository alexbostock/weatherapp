package InteractionDesign.Group3;

import org.json.JSONException;
import org.json.JSONObject;

public class WeatherData {
	
	public static enum ConditionCode {
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
		
		private ConditionCode (int code) {
			this.id = code;
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
	
	private final ConditionCode mCode;
	private final String mDescription;
	
	private final double mTemp;
	private final double mPressure;
	private final double mHumidity;
	
	private final double mWindSpeed;
	
	public WeatherData(JSONObject json) throws JSONException {
		JSONObject jsonWeather = json.getJSONArray("weather").getJSONObject(0);
		mCode = ConditionCode.valueof(jsonWeather.getInt("id"));
		mDescription = jsonWeather.getString("description");
		
		JSONObject jsonMain = json.getJSONObject("main");
		mTemp = jsonMain.getDouble("temp");
		mPressure = jsonMain.getDouble("pressure");
		mHumidity = jsonMain.getDouble("humidity");
		
		JSONObject jsonWind = json.getJSONObject("wind");
		mWindSpeed = jsonWind.getDouble("speed");
	}
	
	public ConditionCode getConditionCode() {
		return mCode;
	}
	
	public String getDescription() {
		return mDescription;
	}
	
	public double getTemperature() {
		return mTemp;
	}
	
	public double getPressure() {
		return mPressure;
	}
	
	public double getHumidity() {
		return mHumidity;
	}
	
	public double getWindSpeed() {
		return mWindSpeed;
	}
	
}
