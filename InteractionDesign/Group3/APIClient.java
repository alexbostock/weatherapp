package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONException;
import org.json.JSONObject;

public class APIClient {

	private final String APPID = "&APPID=dcf195ce911b00f98dcd3f9f077cb234";
	private final String baseURL = "http://api.openweathermap.org/data/2.5/";
	private HttpClient httpClient;
	
	public APIClient() {
		this.httpClient = HttpClientBuilder.create().build();
	}
	
	public WeatherData currentWeatherAtCity (int cityID) throws IOException, JSONException {
		String subURL = "weather?id=" + cityID;
		JSONObject response = query(subURL);
		return new WeatherData(response);
	}
	
	public WeatherForecast forecastWeatherAtCity (int cityID) throws JSONException, IOException {
		String subURL = "forecast?id=" + cityID;
		JSONObject response = query(subURL);
		return new WeatherForecast(response);
	}
	
	private JSONObject query(String subURL) throws JSONException, IOException {
		String responseBody = null;
		HttpGet httpget = new HttpGet(baseURL + subURL + APPID);
		HttpResponse response = httpClient.execute(httpget);
		InputStream contentStream = null;
		try {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine == null) {
				throw new IOException("Cannot connect to OpenWeatherMap server");
			}
			int statusCode = statusLine.getStatusCode();
			if (statusCode < 200 && statusCode >= 300) {
				throw new IOException ("OpenWeatherMap server responded with status code " + statusCode + ": " + statusLine);
			}
			HttpEntity responseEntity = response.getEntity ();
			contentStream = responseEntity.getContent ();
			Reader isReader = new BufferedReader(new InputStreamReader(contentStream));
			int contentSize = (int) responseEntity.getContentLength();
			if (contentSize < 0)
				contentSize = 8*1024;
			char[] buffer = new char[contentSize];
			isReader.read(buffer);
			responseBody = new String(buffer);
		} finally {
			if (contentStream != null) contentStream.close();
		}
		return new JSONObject(responseBody);
	}
	
	public static void main(String[] args) throws IOException, JSONException {
		APIClient client = new APIClient();
		WeatherForecast weather = client.forecastWeatherAtCity(2172797);
		System.out.println(weather.getForecast(0).getHumidity());
	}
}
