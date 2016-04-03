package parser;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


public class MessageParserTest {


    @Test(expected = InvalidMessageFormatException.class)
    public void shouldThrowAnExceptionIfMessageIsEmpty() throws Exception {
        final String msg = "";
        MessageParser.parseMessage(msg);
    }

    @Test(expected = InvalidMessageFormatException.class)
    public void shouldThrowAnExceptionIfMessageIsNull() throws Exception {
        MessageParser.parseMessage(null);
    }

    @Test
    public void shouldParseValidUpdateMessage() throws Exception {
        final String msg = "{\"event\":\"update\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":1392864673040}";
        Message parsedMsg = MessageParser.parseMessage(msg);

        assertThat(parsedMsg.getEvent(),  is(EventType.UPDATE));
        assertThat(parsedMsg.getTripId(), is("432"));
        assertThat(parsedMsg.getLat(),    is(37.79947));
        assertThat(parsedMsg.getLng(),    is(122.511635));
        assertThat(parsedMsg.getEpoch(),  is(1392864673040L));
        assertThat(parsedMsg.getFare(),   nullValue());
        assertThat(parsedMsg.getGeoHash().length(),   is(9));
    }

    @Test
    public void shouldParseValidStartMessage() throws Exception {
        final String msg = "{\"event\":\"begin\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":1392864673040}";
        Message parsedMsg = MessageParser.parseMessage(msg);

        assertThat(parsedMsg.getEvent(),  is(EventType.BEGIN));
        assertThat(parsedMsg.getFare(),   nullValue());
    }

    @Test
    public void shouldParseValidEndMessage() throws Exception {
        final String msg = "{\"event\":\"end\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"fare\": 43.55, \"epoch\":1392864673040}";
        Message parsedMsg = MessageParser.parseMessage(msg);

        assertThat(parsedMsg.getEvent(),  is(EventType.END));
        assertThat(parsedMsg.getFare(),   is(43.55F));
    }

    @Test(expected = InvalidMessageFormatException.class)
    public void shouldThrowExceptionIfEndMessageHasNoFare() throws Exception {
        final String msg = "{\"event\":\"end\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":1392864673040}";
        MessageParser.parseMessage(msg);
    }

    @Test(expected = InvalidMessageFormatException.class)
    public void shouldThrowExceptionIfUpdateOrBeginMessageHasFare() throws Exception {
        final String msg = "{\"event\":\"begin\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"fare\": 43.55, \"epoch\":1392864673040}";
        MessageParser.parseMessage(msg);
    }

}
