package edu.macalester.wpsemsim.sim.category;

/**
 *
 * @author shilad
 */
final class CategoryDistance implements Comparable<CategoryDistance> {
    private int catIndex;
    private String catString;
    private double distance;
    private byte direction; // +1 or -1

    public CategoryDistance(int catIndex, String catString, double distance, byte direction) {
        this.catIndex = catIndex;
        this.catString = catString;
        this.distance = distance;
        this.direction = direction;
    }

    public final int getCatIndex() {
        return catIndex;
    }

    public final byte getDirection() {
        return direction;
    }

    public final double getDistance() {
        return distance;
    }

    public final int compareTo(CategoryDistance t) {
        if (distance < t.distance)
            return -1;
        else if (distance > t.distance)
            return 1;
        else
            return catIndex * direction - t.catIndex * t.direction;
    }

    @Override
    public String toString() {
        return "CategoryDistance{" +
                "catIndex=" + catIndex +
                ", catString='" + catString + '\'' +
                ", distance=" + distance +
                ", direction=" + direction +
                '}';
    }
}
