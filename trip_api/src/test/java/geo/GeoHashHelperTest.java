package geo;

import model.BoundingGeoRect;
import org.junit.Test;
import org.vertexium.type.GeoHash;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class GeoHashHelperTest {


    @Test
    public void shouldFindExpectedGeoHashesForGeoRect() {
        BoundingGeoRect geoRect = new BoundingGeoRect(new GeoPoint(40.782181,-73.986743), new GeoPoint(40.725575, -73.971983));
        List<String> geoHashes = GeoHashHelper.calculateGeohashesWithinSearchArea(geoRect);

        assertTrue(geoHashes.contains("dr5rs"));
        assertTrue(geoHashes.contains("dr5ru"));
    }

    @Test
    public void shouldNotReturnAll32SmallerGeoHashesIfBoundingBoxIsAGeoHash() {
        GeoRect boundingBox = new GeoHash("r3gx8b").toGeoRect();

        assertThat(GeoHashHelper.removeOutOfBoundsGeoHashes("r3gx8b", boundingBox), is(Arrays.asList("r3gx8b")));
    }

    @Test
    public void shouldRemoveOutOfBoundsGeoHashesForLowPrecisionGeoHashes() {
        GeoRect boundingBox = new GeoRect(new GeoPoint(-33.519580, 150.748331), new GeoPoint(-33.847689, 151.280739));

        assertThat(GeoHashHelper.removeOutOfBoundsGeoHashes("r3g", boundingBox), is(Arrays.asList("r3gp", "r3gr", "r3gx")));
    }

    @Test
    public void shouldNotRemoveOutOfBoundsGeoHashesForHighPrecisionGeohashes() {
        GeoRect boundingBox = new GeoRect(new GeoPoint(-33.535963, 151.184950), new GeoPoint(-33.550248, 151.199569));

        assertThat(GeoHashHelper.removeOutOfBoundsGeoHashes("r3grkpd", boundingBox), is(Arrays.asList("r3grkpd")));
    }

}
