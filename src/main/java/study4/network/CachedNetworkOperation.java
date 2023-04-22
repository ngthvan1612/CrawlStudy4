package study4.network;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.net.URL;

public class CachedNetworkOperation {
    private final static Logger LOG = LoggerFactory.getLogger(CachedNetworkOperation.class);
    private final static String COOKIE = "a9qhfpla6iexlf1viq1wjuea3xpkfz90";
    private final static Jedis jedisInstance = new Jedis();
    private final static JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    static {
        jedisPoolConfig.setMaxTotal(2);
    }
    private final static JedisPool jedisPool = new JedisPool(jedisPoolConfig);

    public CachedNetworkOperation() {

    }

    public byte[] downloadStream(String url) throws IOException {
        final byte[] encodedUrl = url.getBytes();
        final Jedis jedisInstance = jedisPool.getResource();

        if (jedisInstance.get(encodedUrl) != null) {
            LOG.debug(url + " da co trong redis -> return");
            byte[] result = jedisInstance.get(encodedUrl);
            jedisPool.returnResource(jedisInstance);
            return result;
        }

        LOG.debug(url + " chua co trong redis -> download");
        BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
        final byte[] buffer = in.readAllBytes();
        in.close();

        jedisInstance.set(encodedUrl, buffer);

        jedisPool.returnResource(jedisInstance);
        return buffer;
    }

    public void writeCacheByteArrayToDisk(String url, String outputFileName) throws IOException {
        final byte[] encodedUrl = url.getBytes();
        final Jedis jedisInstance = jedisPool.getResource();

        if (jedisInstance.get(encodedUrl) == null) {
            jedisPool.returnResource(jedisInstance);
            throw new RuntimeException("Not found " + url + " in cache!");
        }

        FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);
        fileOutputStream.write(jedisInstance.get(encodedUrl));
        fileOutputStream.close();

        jedisPool.returnResource(jedisInstance);
    }

    public String GET(String url) throws IOException {
        final Jedis jedisInstance = jedisPool.getResource();

        if (jedisInstance.get(url) != null) {
            LOG.debug(url + " da co trong redis -> return");
            final String result = jedisInstance.get(url);
            jedisPool.returnResource(jedisInstance);
            return result;
        }

        LOG.debug(url + " chua co trong redis -> luu");

        Document document = Jsoup
                .connect(url)
                .cookie("sessionid", COOKIE)
                .maxBodySize(Integer.MAX_VALUE)
                .get();

        jedisInstance.set(url, document.toString());
        jedisPool.returnResource(jedisInstance);
        return document.toString();
    }

    private final static CachedNetworkOperation instance = new CachedNetworkOperation();
    public static CachedNetworkOperation getInstance() {
        return instance;
    }
}
