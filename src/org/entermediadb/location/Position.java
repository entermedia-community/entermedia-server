package org.entermediadb.location;

//Copyright 2003 Princeton Board of Trustees.
//All rights reserved.

/**
 * The <code>Position</code> class represents coordinates given in
 * latitude/longitude pairs.
 */
public class Position {
	private Double latitude;
	private Double longitude;
	private Double accuracy;

	private final double RADIUS_EARTH = 6400000;

	/**
	 * Constructs a new <code>Position</code> with the position indicated by
	 * the arguments. The arguments can be <code>null</code> if the values are
	 * not known.
	 * 
	 * @param latitude
	 *            a <code>Double</code>
	 * @param longitude
	 *            a <code>Double</code>
	 */
	public Position(Double latitude, Double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Constructs a new <code>Position</code> that represents the same
	 * position as the argument; in other words, the newly created position is a
	 * copy of the argument position.
	 * 
	 * @param p
	 *            a <code>Position</code>
	 */
	public Position(Position p) {
		latitude = p.latitude;
		longitude = p.longitude;
	}

	/**
	 * Returns the latitude.
	 * 
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Returns the longitude.
	 * 
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Sets the latitude.
	 * 
	 * @param latitude
	 *            a <code>Double</code>
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Sets the longitude.
	 * 
	 * @param longitude
	 *            a <code>Double</code>
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Finds the distance in meters between two positions on earth.
	 * 
	 * @param position
	 *            a <code>Position</code>
	 * 
	 * @return the distance between two positions
	 */
	public double distanceTo(Position position) {
		double x_A = RADIUS_EARTH
				* Math.cos(Math.toRadians(latitude.doubleValue()))
				* Math.cos(Math.toRadians(longitude.doubleValue()));
		double y_A = RADIUS_EARTH
				* Math.cos(Math.toRadians(latitude.doubleValue()))
				* Math.sin(Math.toRadians(longitude.doubleValue()));
		double z_A = RADIUS_EARTH
				* Math.sin(Math.toRadians(latitude.doubleValue()));

		double x_B = RADIUS_EARTH
				* Math
						.cos(Math.toRadians(position.getLatitude()
								))
				* Math.cos(Math
						.toRadians(position.getLongitude()));
		double y_B = RADIUS_EARTH
				* Math
						.cos(Math.toRadians(position.getLatitude()
								))
				* Math.sin(Math
						.toRadians(position.getLongitude()));
		double z_B = RADIUS_EARTH
				* Math
						.sin(Math.toRadians(position.getLatitude()
								));

		double distance = Math.sqrt((x_A - x_B) * (x_A - x_B) + (y_A - y_B)
				* (y_A - y_B) + (z_A - z_B) * (z_A - z_B));

		return distance;
	}

	/**
	 * Finds the square of the distance between two positions, treating them as
	 * points on a flat plane.
	 * 
	 * @param position
	 *            a <code>Position</code>
	 * 
	 * @return the distance between two positions treated as points on a flat
	 *         plane
	 */
	public double coordinateDistanceTo(Position position) {
		double x_1 = latitude.doubleValue();
		double y_1 = longitude.doubleValue();
		double x_2 = position.getLatitude();
		double y_2 = position.getLongitude();
		double x_diff = x_1 - x_2;
		double y_diff = y_1 - y_2;
		if (y_diff > 180)
			y_diff = 360 - y_diff;

		return (x_diff) * (x_diff) + (y_diff) * (y_diff);
	}

	/**
	 * Returns true if both latitude and longitude are not null and false
	 * otherwise.
	 * 
	 * @return whether the position is defined
	 */
	public boolean isDefined() {
		if (latitude == null || longitude == null)
			return false;
		return true;
	}

	/**
	 * Returns a string representation of this <code>Position</code>. This is
	 * for debugging purposes only.
	 * 
	 * @return a string representation of this <code>Position</code>
	 */
	public String toString() {
		return "(" + latitude + ", " + longitude + ")";
	}

	public Double getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(Double accuracy) {
		this.accuracy = accuracy;
	}
}
