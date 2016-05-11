/**
 * 
 */
package org.aksw.limes.core.measures.measure.pointsets.link;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.aksw.limes.core.datastrutures.Point;
import org.aksw.limes.core.measures.mapper.atomic.hausdorff.Polygon;
import org.aksw.limes.core.measures.measure.pointsets.PointsetsMeasure;
import org.aksw.limes.core.util.Pair;

/**
 * @author sherif class to generate the link pairs between 2 polygons
 */
public class LinkFinder {
	protected List<Pair<Point>> linkPairsList;
	protected Polygon small, large;

	/**
	 * Constructor 
	 * 
	 * @param X Polygon
	 * @param Y Polygon
	 * @author sherif
	 */
	public LinkFinder(Polygon X, Polygon Y) {
		linkPairsList = new ArrayList<Pair<Point>>();
		if (X.points.size() < Y.points.size()) {
			small = X;
			large = Y;
		} else {
			small = Y;
			large = X;
		}
	}

	/**
	 * @return list of link pairs
	 */
	public List<Pair<Point>> getlinkPairsList() {
		if (linkPairsList.isEmpty()) {
			// compute the fair capacity for each of the small polygon points
			int fairCapacity = (int) Math.ceil((double) large.points.size() / (double) small.points.size());
			for (Point s : small.points) {
				int fairCount = 0;
				// get sorted set of all near by points
				TreeMap<Double, Point> nearestPoints = getSortedNearestPoints(s, large);
				// add fairCapacity times of the nearby point to the
				// linkPairsList
				for (Entry<Double, Point> e : nearestPoints.entrySet()) {
					Point l = e.getValue();
					linkPairsList.add(new Pair<Point>(l, s));
					fairCount++;
					// if the fair capacity reached the go to the next point
					if (fairCount == fairCapacity)
						break;
				}
			}
		}
		return linkPairsList;
	}

	/**
	 * @param x Point
	 * @param Y Point
	 * @return Sorted nearest points
	 */
	TreeMap<Double, Point> getSortedNearestPoints(Point x, Polygon Y) {
		TreeMap<Double, Point> result = new TreeMap<Double, Point>();
		for (Point y : Y.points) {
			result.put(PointsetsMeasure.pointToPointDistance(x, y), y);
		}
		return result;
	}

	/**
	 * @param mappingSize
	 * @return
	 */
	public double getRuntimeApproximation(double mappingSize) {
		throw new UnsupportedOperationException("Not supported yet.");
	}


}