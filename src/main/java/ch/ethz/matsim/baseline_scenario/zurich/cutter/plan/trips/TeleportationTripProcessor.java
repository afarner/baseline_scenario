package ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPoint;
import ch.ethz.matsim.baseline_scenario.zurich.cutter.plan.points.TeleportationCrossingPointFinder;
import ch.ethz.matsim.baseline_scenario.zurich.extent.ScenarioExtent;

public class TeleportationTripProcessor implements TripProcessor {
	final private TeleportationCrossingPointFinder crossingPointFinder;
	final private ScenarioExtent extent;

	public TeleportationTripProcessor(TeleportationCrossingPointFinder crossingPointFinder, ScenarioExtent extent) {
		this.crossingPointFinder = crossingPointFinder;
		this.extent = extent;
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		Leg leg = (Leg) trip.get(0);

		return process(firstActivity.getCoord(), secondActivity.getCoord(), leg.getTravelTime(), leg.getDepartureTime(),
				leg.getMode(),
				!extent.isInside(firstActivity.getCoord()) && !extent.isInside(secondActivity.getCoord()));
	}

	public List<PlanElement> process(Coord firstCoord, Coord secondCoord, double travelTime, double departureTime,
			String mode, boolean allOutside) {
		List<TeleportationCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(firstCoord,
				secondCoord, travelTime, departureTime);

		if (crossingPoints.size() == 0) {
			return Arrays.asList(PopulationUtils.createLeg(allOutside ? "outside" : mode));
		} else {
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils.createLeg(crossingPoints.get(0).isOutgoing ? mode : "outside"));

			for (TeleportationCrossingPoint point : crossingPoints) {
				Activity activity = PopulationUtils.createActivityFromCoord("outside", point.coord);
				activity.setEndTime(point.time);
				result.add(activity);
				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : mode));
			}

			return result;
		}
	}
}
