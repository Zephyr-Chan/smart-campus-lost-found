package com.campus.lostfound;

import com.campus.lostfound.common.utils.GeohashUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GeoHash 工具类单元测试
 */
public class GeohashUtilTest {

    @Test
    public void testEncode() {
        // 北京天安门: 39.9042, 116.4074
        String geohash = GeohashUtil.encode(39.9042, 116.4074, 7);
        assertNotNull(geohash, "GeoHash 不应为 null");
        assertEquals(7, geohash.length(), "GeoHash 长度应为 7");
        // 北京区域 GeoHash 前缀应为 "wx4g"
        assertTrue(geohash.startsWith("wx4"), "北京区域 GeoHash 应以 wx4 开头, 实际: " + geohash);
    }

    @Test
    public void testEncodeDifferentPrecision() {
        double lat = 39.9042;
        double lon = 116.4074;

        String hash6 = GeohashUtil.encode(lat, lon, 6);
        String hash7 = GeohashUtil.encode(lat, lon, 7);
        String hash8 = GeohashUtil.encode(lat, lon, 8);

        assertEquals(6, hash6.length(), "6位精度长度应为6");
        assertEquals(7, hash7.length(), "7位精度长度应为7");
        assertEquals(8, hash8.length(), "8位精度长度应为8");
        // 高精度是低精度的前缀
        assertTrue(hash7.startsWith(hash6), "7位应以6位为前缀");
        assertTrue(hash8.startsWith(hash7), "8位应以7位为前缀");
    }

    @Test
    public void testEncodeEdgeCases() {
        // 原点
        String origin = GeohashUtil.encode(0, 0, 6);
        assertNotNull(origin, "原点 GeoHash 不应为 null");
        assertEquals(6, origin.length());

        // 极端坐标
        String northPole = GeohashUtil.encode(90, 180, 6);
        assertNotNull(northPole, "北极点 GeoHash 不应为 null");

        String southPole = GeohashUtil.encode(-90, -180, 6);
        assertNotNull(southPole, "南极点 GeoHash 不应为 null");
    }

    @Test
    public void testEncodeInvalidPrecision() {
        assertThrows(IllegalArgumentException.class, () -> {
            GeohashUtil.encode(39.9, 116.4, 0);
        }, "精度为0时应抛出异常");
        assertThrows(IllegalArgumentException.class, () -> {
            GeohashUtil.encode(39.9, 116.4, -1);
        }, "精度为负数时应抛出异常");
    }

    @Test
    public void testDecode() {
        // 编码后解码，验证坐标接近
        double origLat = 39.9042;
        double origLon = 116.4074;
        String geohash = GeohashUtil.encode(origLat, origLon, 8);
        double[] decoded = GeohashUtil.decode(geohash);

        assertNotNull(decoded);
        assertEquals(2, decoded.length, "解码结果应包含纬度和经度");
        // 8位精度误差约 38m x 19m，经纬度误差应小于 0.001 度
        assertEquals(origLat, decoded[0], 0.001, "纬度解码误差应小于 0.001 度");
        assertEquals(origLon, decoded[1], 0.001, "经度解码误差应小于 0.001 度");
    }

    @Test
    public void testDecodeInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            GeohashUtil.decode(null);
        }, "null 输入应抛出异常");
        assertThrows(IllegalArgumentException.class, () -> {
            GeohashUtil.decode("");
        }, "空字符串应抛出异常");
    }

    @Test
    public void testHaversineDistanceSamePoint() {
        double distance = GeohashUtil.haversineDistance(39.9042, 116.4074, 39.9042, 116.4074);
        assertEquals(0, distance, 0.01, "同一点距离应为 0");
    }

    @Test
    public void testHaversineDistanceKnownCities() {
        // 北京 39.9042, 116.4074
        // 上海 31.2304, 121.4737
        // 直线距离约 1067km
        double distance = GeohashUtil.haversineDistance(39.9042, 116.4074, 31.2304, 121.4737);
        assertTrue(distance > 1_000_000, "北京到上海距离应大于 1000km, 实际: " + distance);
        assertTrue(distance < 1_200_000, "北京到上海距离应小于 1200km, 实际: " + distance);
    }

    @Test
    public void testDistanceToScore() {
        // 距离为 0，分数应为 1.0
        assertEquals(1.0, GeohashUtil.distanceToScore(0), 0.001, "距离 0 分数应为 1.0");

        // 距离越大，分数越小
        double score100 = GeohashUtil.distanceToScore(100);
        double score500 = GeohashUtil.distanceToScore(500);
        double score1000 = GeohashUtil.distanceToScore(1000);
        double score2000 = GeohashUtil.distanceToScore(2000);

        assertTrue(score100 > score500, "100m 分数应大于 500m");
        assertTrue(score500 > score1000, "500m 分数应大于 1000m");
        assertTrue(score1000 > score2000, "1000m 分数应大于 2000m");

        // 1000m 距离的分数应约等于 e^(-1) ≈ 0.368
        assertEquals(Math.exp(-1.0), score1000, 0.01, "1000m 分数应约等于 e^(-1)");
    }

    @Test
    public void testGetNeighbors() {
        String geohash = GeohashUtil.encode(39.9042, 116.4074, 7);
        String[] neighbors = GeohashUtil.getNeighbors(geohash);

        assertNotNull(neighbors, "相邻区域不应为 null");
        assertEquals(8, neighbors.length, "应有 8 个相邻区域");

        // 每个相邻区域都不应为 null 且长度相同
        for (String neighbor : neighbors) {
            assertNotNull(neighbor, "相邻区域不应为 null");
            assertEquals(geohash.length(), neighbor.length(), "相邻区域长度应相同");
            assertNotEquals(geohash, neighbor, "相邻区域不应等于原始区域");
        }
    }
}
