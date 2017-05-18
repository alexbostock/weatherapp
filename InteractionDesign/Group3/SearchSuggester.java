package InteractionDesign.Group3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
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
	 * @throws IOException	if the file cannot be loaded
	 */
	public SearchSuggester(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));

		// 22635 is the number of cities
		List<String> strings = new ArrayList<>(22635);

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
				subMap.put(d, new ArrayList<>(50));
			}
		}

		for (String s : strings) {
			if (s.length() > 2) {
				try {
					Map<Character, List<String>> subMap = mSuggestionMap.get(Character.toLowerCase(s.charAt(0)));
					subMap.get(Character.toLowerCase(s.charAt(1))).add(s);
				} catch (NullPointerException e) {
					// Cities with non-ASCII characters will be quietly skipped
				}
			}
		}
	}

	/**
	 * Recommends cities given the start of a city name.
	 * The input is case-insensitive.
	 * Returns null if the input is less than 3 characters.
	 * The value returned is of the form [city name], [ISO 3166 country code]
	 *
	 * @param	start	the beginning of a city name
	 * @return	a list of cities matching the request
	 */
	public List<String> getSuggestions(String start) {
		if (start.length() < 3) {
			return null;
		}

		Map<Character, List<String>> subMap = mSuggestionMap.get(Character.toLowerCase(start.charAt(0)));
		List<String> poss = subMap.get(Character.toLowerCase(start.charAt(1)));

		List<String> result = new ArrayList<>();

		for (String s : poss){
			if (s.toLowerCase().startsWith(start.toLowerCase())) {
				result.add(s);
			}
		}

		return result;
	}
}
