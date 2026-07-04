package com.campus.lostfound.common.utils;

/**
 * GeoHash 工具类
 * 将经纬度编码为字符串，实现空间索引和距离估算
 *
 * 原理：将二维经纬度空间通过二分法交替编码为一维二进制串，
 * 再 Base32 编码为字符串。相邻 GeoHash 拥有公共前缀，
 * 适合数据库索引和前缀查询。
 */
public class GeohashUtil {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    private static final int[] BITS = {16, 8, 4, 2, 1};

    private GeohashUtil() {
    }

    /**
     * 编码经纬度为 GeoHash 字符串
     *
     * @param latitude  纬度 [-90, 90]
     * @param longitude 经度 [-180, 180]
     * @param precision 字符串长度（6位约 1.2km x 0.6km，7位约 153m x 153m，8位约 38m x 19m）
     * @return GeoHash 字符串
     */
    public static String encode(double latitude, double longitude, int precision) {
        if (precision <= 0) {
            throw new IllegalArgumentException("precision must be positive");
        }
        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        boolean isEven = true;
        int bit = 0;
        int ch = 0;

        StringBuilder geohash = new StringBuilder();

        while (geohash.length() < precision) {
            if (isEven) {
                double mid = (lonRange[0] + lonRange[1]) / 2;
                if (longitude >= mid) {
                    ch |= BITS[bit];
                    lonRange[0] = mid;
                } else {
                    lonRange[1] = mid;
                }
            } else {
                double mid = (latRange[0] + latRange[1]) / 2;
                if (latitude >= mid) {
                    ch |= BITS[bit];
                    latRange[0] = mid;
                } else {
                    latRange[1] = mid;
                }
            }
            isEven = !isEven;

            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }

    /**
     * 解码 GeoHash 字符串为经纬度（返回中心点）
     *
     * @param geohash GeoHash 字符串
     * @return double[]{latitude, longitude}
     */
    public static double[] decode(String geohash) {
        if (geohash == null || geohash.isEmpty()) {
            throw new IllegalArgumentException("geohash cannot be empty");
        }

        double[] latRange = {-90.0, 90.0};
        double[] lonRange = {-180.0, 180.0};

        boolean isEven = true;

        for (int i = 0; i < geohash.length(); i++) {
            char c = geohash.charAt(i);
            int cd = BASE32.indexOf(c);
            if (cd < 0) {
                throw new IllegalArgumentException("invalid geohash character: " + c);
            }

            for (int mask : BITS) {
                if (isEven) {
                    if ((cd & mask) != 0) {
                        lonRange[0] = (lonRange[0] + lonRange[1]) / 2;
                    } else {
                        lonRange[1] = (lonRange[0] + lonRange[1]) / 2;
                    }
                } else {
                    if ((cd & mask) != 0) {
                        latRange[0] = (latRange[0] + latRange[1]) / 2;
                    } else {
                        latRange[1] = (latRange[0] + latRange[1]) / 2;
                    }
                }
                isEven = !isEven;
            }
        }

        double latitude = (latRange[0] + latRange[1]) / 2;
        double longitude = (lonRange[0] + lonRange[1]) / 2;

        return new double[]{latitude, longitude};
    }

    /**
     * 使用 Haversine 公式计算两个经纬度之间的球面距离
     *
     * @param lat1 纬度1
     * @param lon1 经度1
     * @param lat2 纬度2
     * @param lon2 经度2
     * @return 距离（米）
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半径（米）

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 将距离转换为 0-1 的相似度分数
     * 使用指数衰减函数：距离越近分数越高
     *
     * @param distanceMeters 距离（米）
     * @return 相似度分数 [0, 1]
     */
    public static double distanceToScore(double distanceMeters) {
        if (distanceMeters <= 0) {
            return 1.0;
        }
        // 500米内满分，1000米半分，2000米以上趋近0
        return Math.exp(-distanceMeters / 1000.0);
    }

    /**
     * 获取指定 GeoHash 的 8 个相邻区域（用于范围查询）
     *
     * @param geohash GeoHash 字符串
     * @return 相邻 GeoHash 数组（顺序：N, NE, E, SE, S, SW, W, NW）
     */
    public static String[] getNeighbors(String geohash) {
        String[][] grid = getGrid(geohash);
        // grid[0][1]=N, grid[1][2]=E, grid[2][1]=S, grid[1][0]=W
        // grid[0][0]=NW, grid[0][2]=NE, grid[2][0]=SW, grid[2][2]=SE
        return new String[]{
                grid[0][1], grid[0][2], grid[1][2], grid[2][2],
                grid[2][1], grid[2][0], grid[1][0], grid[0][0]
        };
    }

    /**
     * 构建 3x3 的 GeoHash 网格（中心为输入 geohash）
     */
    private static String[][] getGrid(String geohash) {
        String[][] grid = new String[3][3];
        grid[1][1] = geohash;

        String north = getAdjacent(geohash, 'n');
        String south = getAdjacent(geohash, 's');
        grid[0][1] = north;
        grid[2][1] = south;

        grid[0][0] = getAdjacent(grid[0][1], 'w');
        grid[0][2] = getAdjacent(grid[0][1], 'e');
        grid[1][0] = getAdjacent(geohash, 'w');
        grid[1][2] = getAdjacent(geohash, 'e');
        grid[2][0] = getAdjacent(grid[2][1], 'w');
        grid[2][2] = getAdjacent(grid[2][1], 'e');

        return grid;
    }

    // ===== GeoHash 相邻区域计算 =====

    private static final String[][] NEIGHBORS = {
            {"p0r21436x8zb9dcf5h7kjnmqesgutwvy", "bc01fg45238967deuvhjyznpkmstqrwx"},
            {"bc01fg45238967deuvhjyznpkmstqrwx", "p0r21436x8zb9dcf5h7kjnmqesgutwvy"}
    };

    private static final String[][] BORDERS = {
            {"prxz", "bcfguvyz"},
            {"bcfguvyz", "prxz"}
    };

    /**
     * 获取指定方向上的相邻 GeoHash
     *
     * @param geohash  GeoHash 字符串
     * @param direction 方向 (n/s/e/w)
     * @return 相邻 GeoHash
     */
    private static String getAdjacent(String geohash, char direction) {
        if (geohash == null || geohash.isEmpty()) {
            return geohash;
        }

        geohash = geohash.toLowerCase();
        char lastChar = geohash.charAt(geohash.length() - 1);
        String parent = geohash.substring(0, geohash.length() - 1);

        int type = (geohash.length() % 2 == 1) ? 0 : 1;

        int dirIdx;
        switch (direction) {
            case 'n':
                dirIdx = 0;
                break;
            case 's':
                dirIdx = 0;
                break;
            case 'e':
                dirIdx = 1;
                break;
            case 'w':
                dirIdx = 1;
                break;
            default:
                return geohash;
        }

        // 检查是否在边界上
        String border = BORDERS[dirIdx][type];
        String neighborPattern = NEIGHBORS[dirIdx][type];

        if (border.indexOf(lastChar) != -1 && !parent.isEmpty()) {
            parent = getAdjacent(parent, direction);
        }

        int idx = neighborPattern.indexOf(lastChar);
        if (idx >= 0 && idx < BASE32.length()) {
            return parent + BASE32.charAt(idx);
        }

        return geohash;
    }
}
