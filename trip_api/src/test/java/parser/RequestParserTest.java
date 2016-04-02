package parser;

import model.BoundingGeoRect;
import model.TimeRange;
import org.junit.Test;
import org.mockito.Mockito;
import org.vertexium.type.GeoPoint;
import spark.Request;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.when;


public class RequestParserTest {

    private final Request mockRequest = Mockito.mock(Request.class);


    @Test(expected = InvalidQueryParamsException.class)
    public void shouldThrowExceptionWhenFromTimingParamsInvalid() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("from")).thenReturn("");
        when(mockRequest.queryParams("to")).thenReturn("123456789");

        RequestParser.getTimeRangeForParams(mockRequest);
    }

    @Test(expected = InvalidQueryParamsException.class)
    public void shouldThrowExceptionWhenToTimingParamsInvalid() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("from")).thenReturn("123456789");
        when(mockRequest.queryParams("to")).thenReturn("ab");

        RequestParser.getTimeRangeForParams(mockRequest);
    }

    @Test(expected = InvalidQueryParamsException.class)
    public void shouldThrowExceptionWhenTimeRangeParamsNotNumeric() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("from")).thenReturn("abcdef");
        when(mockRequest.queryParams("to")).thenReturn("ghijkl");

        RequestParser.getGeoRectForParams(mockRequest);
    }

    @Test
    public void shouldReturnTimeRangeUpUntilNowWhenToParameterNotSupplied() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("from")).thenReturn("124567789");
        when(mockRequest.queryParams("to")).thenReturn(null);

        TimeRange timeRange = RequestParser.getTimeRangeForParams(mockRequest);
        assertThat(timeRange.getFromTime(), is(124567789L));
        assertNotNull(timeRange.getUntilTime());
    }

    @Test
    public void shouldReturnTimeRangeObjectWhenParamsValid() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("from")).thenReturn("124567789");
        when(mockRequest.queryParams("to")).thenReturn("234567899");

        TimeRange timeRange = RequestParser.getTimeRangeForParams(mockRequest);
        assertThat(timeRange.getFromTime(), is(124567789L));
        assertThat(timeRange.getUntilTime(), is(234567899L));
    }

    @Test(expected = InvalidQueryParamsException.class)
    public void shouldThrowExceptionWhenNWGeoRectParamsInvalid() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("nw")).thenReturn("1.00-3.99");
        when(mockRequest.queryParams("se")).thenReturn("-33,122.0");

        RequestParser.getGeoRectForParams(mockRequest);
    }

    @Test(expected = InvalidQueryParamsException.class)
    public void shouldThrowExceptionWhenSEGeoRectParamsInvalid() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("nw")).thenReturn("-33.0,122.0");
        when(mockRequest.queryParams("se")).thenReturn("abc");

        RequestParser.getGeoRectForParams(mockRequest);
    }

    @Test(expected = InvalidQueryParamsException.class)
    public void shouldThrowExceptionWhenGeoRectParamsNotNumeric() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("nw")).thenReturn("abc,def");
        when(mockRequest.queryParams("se")).thenReturn("ghi,jkl");

        RequestParser.getGeoRectForParams(mockRequest);
    }

    @Test
    public void shouldReturnValidGeoRectWhenParamsValid() throws InvalidQueryParamsException {
        when(mockRequest.queryParams("nw")).thenReturn("-33,122.0");
        when(mockRequest.queryParams("se")).thenReturn("-30, 118");

        BoundingGeoRect rect = RequestParser.getGeoRectForParams(mockRequest);

        assertThat(rect.getNorthWest(), is(new GeoPoint(-33, 122.0)));
        assertThat(rect.getSouthEast(), is(new GeoPoint(-30, 118)));
    }

}
