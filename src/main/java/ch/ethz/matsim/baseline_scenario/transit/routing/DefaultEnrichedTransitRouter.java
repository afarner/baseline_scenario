package ch.ethz.matsim.baseline_scenario.transit.routing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnection;
import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnectionFinder;
import ch.ethz.matsim.baseline_scenario.transit.connection.TransitConnectionFinder.NoConnectionFoundException;

public class DefaultEnrichedTransitRouter implements EnrichedTransitRouter {
	final private TransitRouter delegate;
	final private TransitSchedule transitSchedule;
	final private TransitConnectionFinder connectionFinder;
	final private Network network;
	final private double beelineDistanceFactor;
	final private double additionalTransferTime;
	final private Collection<String> ptModes;

	@Deprecated
	public DefaultEnrichedTransitRouter(TransitRouter delegate, TransitSchedule transitSchedule,
			TransitConnectionFinder connectionFinder, Network network, double beelineDistanceFactor,
			double additionalTransferTime) {
		this(delegate, transitSchedule, connectionFinder, network, beelineDistanceFactor, additionalTransferTime,
				Collections.singleton("pt"));
	}

	public DefaultEnrichedTransitRouter(TransitRouter delegate, TransitSchedule transitSchedule,
			TransitConnectionFinder connectionFinder, Network network, double beelineDistanceFactor,
			double additionalTransferTime, Collection<String> ptModes) {
		this.delegate = delegate;
		this.transitSchedule = transitSchedule;
		this.connectionFinder = connectionFinder;
		this.network = network;
		this.beelineDistanceFactor = beelineDistanceFactor;
		this.additionalTransferTime = additionalTransferTime;
		this.ptModes = ptModes;
	}

	@Override
	public List<Leg> calculateRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		List<Leg> legs = delegate.calcRoute(fromFacility, toFacility, departureTime, person);
		double currentTime = departureTime;

		for (int i = 0; i < legs.size(); i++) {
			Leg currentLeg = legs.get(i);

			if (ptModes.contains(currentLeg.getMode())) {
				try {
					double departureAfterAdditionalTransfer = currentTime + additionalTransferTime;
					double totalTravelTimeAfterAdditionalTransfer = currentLeg.getTravelTime() - additionalTransferTime;

					ExperimentalTransitRoute originalRoute = (ExperimentalTransitRoute) currentLeg.getRoute();
					TransitRoute transitRoute = transitSchedule.getTransitLines().get(originalRoute.getLineId())
							.getRoutes().get(originalRoute.getRouteId());

					TransitConnection connection = connectionFinder.findConnection(departureAfterAdditionalTransfer,
							totalTravelTimeAfterAdditionalTransfer, originalRoute.getAccessStopId(),
							originalRoute.getEgressStopId(), transitRoute);

					double totalTime = additionalTransferTime + connection.getWaitingTime()
							+ connection.getInVehicleTime();

					if (Math.abs(totalTime - currentLeg.getTravelTime()) > 1e-3) {
						throw new IllegalStateException(String.format(
								"Calculation of travel time is not consistent! Leg travel time: %s, Additional transfer time: %s, Waiting time: %s, In-vehicle time: %s",
								currentLeg.getTravelTime(), additionalTransferTime, connection.getWaitingTime(),
								connection.getInVehicleTime()));
					}

					int accessStopIndex = transitRoute.getStops().indexOf(connection.getAccessStop());
					int egressStopIndex = transitRoute.getStops().indexOf(connection.getEgressStop());

					double distance = RouteUtils.calcDistance(originalRoute, transitSchedule, network);

					EnrichedTransitRoute enrichedRoute = new DefaultEnrichedTransitRoute(originalRoute.getStartLinkId(),
							originalRoute.getEndLinkId(), distance, connection.getInVehicleTime(),
							connection.getWaitingTime() + additionalTransferTime, accessStopIndex, egressStopIndex,
							originalRoute.getLineId(), originalRoute.getRouteId(), connection.getDeparture().getId());

					currentLeg.setRoute(enrichedRoute);
					currentLeg.setDepartureTime(currentTime);
				} catch (NoConnectionFoundException e) {
					throw new IllegalStateException("Cannot recover connection that the router produced.");
				}
			} else if (currentLeg.getMode().contains("walk") && !currentLeg.getMode().equals("walk")) {
				// We cannot update distances here yet, because not all access and egress stop
				// indices are known, see below

				currentLeg.setDepartureTime(currentTime);
			} else {
				throw new IllegalStateException("Can only enrich pt and *_walk legs");
			}

			currentTime += currentLeg.getTravelTime();
		}

		for (int i = 0; i < legs.size(); i++) {
			Leg currentLeg = legs.get(i);

			if (currentLeg.getMode().contains("walk")) {
				Coord originCoord = fromFacility.getCoord();
				Coord destinationCoord = toFacility.getCoord();

				if (i > 0) {
					EnrichedTransitRoute preceedingRoute = (EnrichedTransitRoute) legs.get(i - 1).getRoute();

					originCoord = transitSchedule.getTransitLines().get(preceedingRoute.getTransitLineId()).getRoutes()
							.get(preceedingRoute.getTransitRouteId()).getStops()
							.get(preceedingRoute.getEgressStopIndex()).getStopFacility().getCoord();
				}

				if (i < legs.size() - 2) {
					EnrichedTransitRoute followingRoute = (EnrichedTransitRoute) legs.get(i + 1).getRoute();

					destinationCoord = transitSchedule.getTransitLines().get(followingRoute.getTransitLineId())
							.getRoutes().get(followingRoute.getTransitRouteId()).getStops()
							.get(followingRoute.getAccessStopIndex()).getStopFacility().getCoord();
				}

				double beelineDistance = CoordUtils.calcEuclideanDistance(originCoord, destinationCoord);
				double distance = beelineDistance * beelineDistanceFactor;

				if (Double.isNaN(distance)) {
					throw new IllegalStateException("Distance is NaN");
				}

				currentLeg.getRoute().setDistance(distance);
			}
		}

		legs.get(0).getRoute().setStartLinkId(fromFacility.getLinkId());
		legs.get(legs.size() - 1).getRoute().setEndLinkId(toFacility.getLinkId());

		return legs;
	}
}
