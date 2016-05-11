/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.limes.core.measures.measure.pointsets.hausdorff;

import org.aksw.limes.core.measures.mapper.atomic.hausdorff.Polygon;

/**
 *
 * @author ngonga
 */
public class SymmetricHausdorff extends NaiveHausdorff {

	/* (non-Javadoc)
	 * @see org.aksw.limes.core.measures.measure.pointsets.hausdorff.NaiveHausdorff#computeDistance(org.aksw.limes.core.measures.mapper.atomic.hausdorff.Polygon, org.aksw.limes.core.measures.mapper.atomic.hausdorff.Polygon, double)
	 */
	@Override
	public double computeDistance(Polygon X, Polygon Y, double threshold) {
		NaiveHausdorff nh = new NaiveHausdorff();
		return Math.max(nh.computeDistance(X, Y, threshold), nh.computeDistance(Y, X, threshold));
	}

	/* (non-Javadoc)
	 * @see org.aksw.limes.core.measures.measure.pointsets.hausdorff.NaiveHausdorff#getName()
	 */
	@Override
	public String getName() {
		return "symmetricHausdorff";
	}

	/* (non-Javadoc)
	 * @see org.aksw.limes.core.measures.measure.pointsets.hausdorff.NaiveHausdorff#getRuntimeApproximation(double)
	 */
	public double getRuntimeApproximation(double mappingSize) {
		return mappingSize / 1000d;
	}
}