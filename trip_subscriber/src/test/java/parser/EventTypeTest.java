package parser;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class EventTypeTest {

    @Test
    public void shouldConvertToEnumFromString() {
        assertThat(EventType.fromString("begin"), is(EventType.BEGIN));
        assertThat(EventType.fromString("update"), is(EventType.UPDATE));
        assertThat(EventType.fromString("end"), is(EventType.END));
    }

}
