package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Gives search suggestions for city names.
 */
public class SearchSuggester {
	private Map<Character, Map<Character, List<String>>> mSuggestionMap;

	/**
	 * Constructs search suggestion engine for a list of cities.
	 *
	 * @param	file	text file containing a list of cities
	 * @throws	IOException	if the file cannot be loaded
	 */
	public SearchSuggester(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		List<String> strings = new LinkedList<>();

		String line;
		while ((line = br.readLine()) != null) {
			strings.add(line);
		}

		br.close();

		mSuggestionMap = new HashMap<>();

		for (char c = 'a'; c <= 'z'; c++) {
			Map<Character, List<String>> subMap = new HashMap<>();
			mSuggestionMap.put(c, subMap);

			for (char d = 'a'; d <= 'z'; d++) {
				subMap.put(d, new LinkedList<>());
			}
		}

		for (String s : strings) {
			if (s.length() > 2) {
				Map<Character, List<String>> subMap = mSuggestionMap.get(Character.toLowerCase(s.charAt(0)));

				// Cities with non-ASCII characters will be quietly skipped

				if (subMap == null) {
					continue;
				}

				List<String> list = subMap.get(Character.toLowerCase(s.charAt(1)));

				if (list != null) {
					list.add(s);
				}
			}
		}
	}

	/**
	 * Recommends cities given the start of a city name.
	 * The input is case-insensitive.
	 * Returns empty list if the string given is less than 3 characters
	 * The value returned is of the form [city name], [ISO 3166 country code]
	 *
	 * @param	start	the beginning of a city name
	 * @return	a list of cities matching the request
	 */
	public List<String> getSuggestions(String start) {
		List<String> result = new LinkedList<>();

		if (start.length() >= 3) {
			Map<Character, List<String>> subMap = mSuggestionMap.getOrDefault(Character.toLowerCase(start.charAt(0)), new HashMap<>());
			List<String> poss = subMap.getOrDefault(Character.toLowerCase(start.charAt(1)), new LinkedList<>());

			for (String s : poss){
				if (s.toLowerCase().startsWith(start.toLowerCase())) {
					result.add(s);
				}
			}
		}

		return result;
	}
}
