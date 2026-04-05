record Point(double y, double z) {
    public static Point[] createPoints(double y1, double z1, double y2, double z2) {
        return new Point[]{new Point(y1, z1), new Point(y2, z2)};
    }

    double distanceTo(Point other) {
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return Math.sqrt(dy * dy + dz * dz);
    }

    boolean equals(Point other) {
        return (this.y == other.y && this.z == other.z);
    }

    @Override
    public String toString() {
        return "Point(Y=" + y + ", Z=" + z + ")";
    }
}
