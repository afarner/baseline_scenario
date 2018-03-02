package ch.ethz.matsim.baseline_scenario.transit.connection;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public interface TransitConnection {
	Departure getDeparture();

	TransitRouteStop getAccessStop();

	TransitRouteStop getEgressStop();

	double getInVehicleTime();

	double getWaitingTime();
}
