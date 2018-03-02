package ch.ethz.matsim.baseline_scenario.transit.routing;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDefaultEnrichedTransitRoute {
	@Test
	public void testRouteInformatonSerialization() throws IOException {
		DefaultEnrichedTransitRoute.RouteDescription description = new DefaultEnrichedTransitRoute.RouteDescription();

		description.transitLineId = Id.create("abc", TransitLine.class);
		description.transitRouteId = Id.create("def", TransitRoute.class);

		description.accessStopIndex = 20;
		description.egressStopindex = 40;

		description.inVehicleTime = 30.0;
		description.transferTime = 55.0;

		String serialized = new ObjectMapper().writeValueAsString(description);

		Assert.assertEquals(
				"{\"inVehicleTime\":30.0,\"transferTime\":55.0,\"accessStopIndex\":20,\"egressStopindex\":40,\"transitRouteId\":\"def\",\"transitLineId\":\"abc\"}",
				serialized);

		DefaultEnrichedTransitRoute.RouteDescription deserialized = new ObjectMapper().readValue(serialized,
				DefaultEnrichedTransitRoute.RouteDescription.class);

		Assert.assertEquals(20, deserialized.accessStopIndex);
		Assert.assertEquals(40, deserialized.egressStopindex);

		Assert.assertEquals(30.0, deserialized.inVehicleTime, 1e-3);
		Assert.assertEquals(55.0, deserialized.transferTime, 1e-3);

		Assert.assertEquals(description.transitLineId, deserialized.transitLineId);
		Assert.assertEquals(description.transitRouteId, deserialized.transitRouteId);
	}
}
