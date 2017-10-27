package udel.rpng.sensors_driver.publishers.images3;

public class ImageParams {
    public enum ViewMode {
        RGBA,
        GRAY,
        CANNY;
    }

    public enum TransportType {
        NONE,
        PNG,
        JPEG;
    }

    public enum CompressionLevel {
        NONE(100),
        LOW(80),
        MEDIUM(50),
        HIGH(25),
        VERY_HIGH(10);

        private int level;

        private CompressionLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}