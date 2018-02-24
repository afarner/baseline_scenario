package ch.ethz.matsim.baseline_scenario.zurich.cutter.schedule;

import java.util.List;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public interface StopSequenceCrossingPointFinder {
	List<StopSequenceCrossingPoint> findCrossingPoints(List<TransitRouteStop> stopSequence);
}
