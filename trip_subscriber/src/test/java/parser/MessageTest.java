package parser;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;


public class MessageTest {


    @Test
    public void shouldCreateMessageWithoutFare() {
        Message msg = new Message(EventType.BEGIN, "123", 33.0, -44.5, 123456789L);
        assertThat(msg.getFare(), nullValue());
    }

    @Test
    public void shouldCreateMessageWithFare() {
        Message msg = new Message(EventType.END, "123", 33.0, -44.5, 23.55f, 123456789L);
        assertThat(msg.getFare(), is(23.55f));
    }

    @Test
    public void shouldReturnGeohashWithPrecision9FromLatLong() {
        Message msg = new Message(EventType.END, "123", 33.0, -44.5, 23.55f, 123456789L);
        assertThat(msg.getGeoHash().length(), is(9));
    }

}
