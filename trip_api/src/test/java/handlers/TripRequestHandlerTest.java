package handlers;

import model.GeoTripData;
import model.TimeRange;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import service.TripDataService;
import service.TripDataServiceImpl;
import spark.Request;
import spark.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;


public class TripRequestHandlerTest {


    private TripDataService mockDataService = mock(TripDataService.class);
    private TripRequestHandler handler = new TripRequestHandler(mockDataService);
    private Request req = mock(Request.class);
    @Before
    public void setup() throws Exception {
        when(req.queryParams("from")).thenReturn("12345678");
        when(req.queryParams("to")).thenReturn("22345678");
        when(req.queryParams("nw")).thenReturn("-22.0,135");
        when(req.queryParams("se")).thenReturn("-23.0, 155");
        when(mockDataService.getStartFinishTripDataForGeoLocation(any())).thenReturn(new GeoTripData(200, 1789.55f));
    }


    @Test
    public void shouldCallGetNumberOfTripsInTimeRange() throws Exception {
        handler.getNumberOfTripsAtTime(req, mock(Response.class));
        verify(mockDataService, times(1)).getNumberOfTripsInTimeRange(any(TimeRange.class));
    }

    @Test
    public void shouldCallGetNumberOfTripsInGeo() {
        handler.getNumberOfTripsInGeo(req, mock(Response.class));
        verify(mockDataService).getNumberOfTripsInGeoLocation(anyList());
    }

    @Test
    public void shouldCallGetStartedOrCompletedInGeo() throws Exception {
        handler.getTripsStartedOrCompletedInGeo(req, mock(Response.class));
        verify(mockDataService, VerificationModeFactory.times(1)).getStartFinishTripDataForGeoLocation(Mockito.any());
    }

}
