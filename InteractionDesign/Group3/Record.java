package InteractionDesign.Group3;

import java.time.LocalDateTime;

/**
 * Represents the weather forecast at a particular time.
 * Stores the temperature, weather icon, and a time stamp.
 * I think this will need expanding so we have enough data to recommend items.
 */
public class Record implements Comparable<Record> {
	private final Icon mIcon;
	private final int mTemp;				// In Celcius
	private final LocalDateTime mTimeStamp;	// Watch out for time zone issues

	private String mLabel;

	/**
	 * Instantiates Record with the parameters given
	 *
	 * @param	i	the weather icon to display
	 * @param	t	the temperature, in degrees Celcius
	 * @param	ts	the time stamp of this record
	 */
	public Record(Icon i, int t, LocalDateTime ts) {
		mIcon = i;
		mTemp = t;
		mTimeStamp = ts;

		mLabel = "";
	}

	/**
	 * Instantiates Record from a string.
	 * This is used in loading the cache file. It is the opposite of the
	 * toString method.
	 *
	 * @param	record	the string to parse
	 */
	public Record(String record) {
		String[] vals = record.split(",");

		mLabel = vals[0];
		mIcon = Icon.valueOf(vals[1]);
		mTemp = Integer.parseInt(vals[2]);
		mTimeStamp = LocalDateTime.parse(vals[3]);	// Should be ISO date eg. "2017-05-15T18:00:00"
	}

	/**
	 * Gets the weather icon.
	 *
	 * @return	the icon
	 */
	public Icon getIcon() {
		return mIcon;
	}

	/**
	 * Gets the label of this record. eg. "Today", "Tomrrow", "Wednesday" etc.
	 *
	 * @return	the label
	 */
	public String getLabel() {
		return mLabel;
	}

	/**
	 * Gets the temperature, in degrees Celcius.
	 *
	 * @return	the temperature
	 */
	public int getTemp() {
		return mTemp;
	}

	/**
	 * Gets the time stamp. This includes date and time.
	 *
	 * @return	the time stamp
	 */
	public LocalDateTime getTimeStamp() {
		return mTimeStamp;
	}

	/**
	 * Updates the record's label to the given string.
	 * The label should have a value such "Today", "Tommorow", or "Tuesday"
	 *
	 * @param	l	the new label
	 */
	public void setLabel(String l) {
		mLabel = l;
	}

	public String toString() {
		return mLabel + "," + mIcon.toString() + "," + mTemp + "," + mTimeStamp.toString();
	}

	@Override
	public int compareTo(Record o) {
		return this.mTimeStamp.compareTo(o.mTimeStamp);
	}
}
