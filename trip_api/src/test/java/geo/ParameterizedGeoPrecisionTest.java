package geo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.locationtech.spatial4j.distance.DistanceUtils;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class ParameterizedGeoPrecisionTest {

    private static final double startLong = -33.0;
    private static final double startLat = -151.0;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {startLat + (4.8d / 1000 / DistanceUtils.DEG_TO_KM), startLong - (4.7d / 1000 / DistanceUtils.DEG_TO_KM), 9},
                {startLat + (5d / 1000 / DistanceUtils.DEG_TO_KM), startLong - (5d / 1000 / DistanceUtils.DEG_TO_KM), 8},
                {startLat + (152.4d / 1000 / DistanceUtils.DEG_TO_KM), startLong - (152d / 1000 / DistanceUtils.DEG_TO_KM), 7},
                {startLat + (609.0d / 1000 / DistanceUtils.DEG_TO_KM), startLong - (1.1d / DistanceUtils.DEG_TO_KM), 6},
                {startLat + (4.8d / DistanceUtils.DEG_TO_KM), startLong - (4.8d / DistanceUtils.DEG_TO_KM), 5},
                {startLat + (19d / DistanceUtils.DEG_TO_KM), startLong - (38d / DistanceUtils.DEG_TO_KM), 4},
                {startLat + (155d / DistanceUtils.DEG_TO_KM), startLong - (155d / DistanceUtils.DEG_TO_KM), 3},
                {startLat + (624d / DistanceUtils.DEG_TO_KM), startLong - (1252d / DistanceUtils.DEG_TO_KM), 2},
                {startLat + (650d / DistanceUtils.DEG_TO_KM), startLong - (1270d / DistanceUtils.DEG_TO_KM), 1}

        });
    }

    private double lat;
    private double lng;
    private int expected;

    public ParameterizedGeoPrecisionTest(double latitude, double longitude, int expec) {
        lat = latitude;
        lng = longitude;
        expected = expec;
    }

    @Test
    public void test() {
        int precision = GeoHashHelper.getSmallestEncompassingPrecision(lat, lng, startLat, startLong);
        assertThat(precision, is(expected));
    }

}
