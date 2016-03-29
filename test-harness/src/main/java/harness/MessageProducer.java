package harness;


import redis.clients.jedis.Jedis;
import java.util.Random;


public class MessageProducer implements Runnable {

    private static final int MAX_REQUESTS = 100;

    private static final String BEGIN_UPDATE_MSG = "{\"event\":\"%s\", \"tripId\":%s, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":%d}";
    private static final String END_MSG = "{\"event\":\"%s\", \"tripId\":%s, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":%d, \"fare\":%.2f}";



    private final Jedis jedis;
    private final Random random = new Random();
    private final String channelName;

    public MessageProducer(Jedis jedis, String channelName) {
        this.jedis = jedis;
        this.channelName = channelName;
    }

    @Override
    public void run() {
        long count = 0;
        long startTime = System.currentTimeMillis();
        while (true) {
            while (System.currentTimeMillis() - startTime < 1000) {
                if (count <= MAX_REQUESTS) {
                    jedis.publish(channelName, getMessage());
                    try {
                        Thread.sleep(random.nextInt(11));
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    count++;
                }
            }
            count = 0;
            startTime = System.currentTimeMillis();
        }
    }

    /**
     * Randomly generates a message. Attempts to send more
     * update messages than other messages. No significant statistical or probability
     * analysis here, just something to exercise the subscriber and get messages into the
     * data stores.
     *
     * @return
     */
    private String getMessage() {
        int rnd = random.nextInt(100);
        if (rnd == 0) {
            return String.format(BEGIN_UPDATE_MSG, "begin", random.nextInt(Integer.MAX_VALUE), System.currentTimeMillis());
        } else if (rnd == 99) {
            return String.format(END_MSG, "end", random.nextInt(Integer.MAX_VALUE), System.currentTimeMillis(),
                    random.nextInt(50) * random.nextFloat());
        } else {
            return String.format(BEGIN_UPDATE_MSG, "update", random.nextInt(Integer.MAX_VALUE), System.currentTimeMillis());
        }
    }

}

