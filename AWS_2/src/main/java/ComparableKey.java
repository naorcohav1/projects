import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

public class ComparableKey implements WritableComparable<ComparableKey> {
    
    private Text w1;
    private Text w2;
    private Text decade;
    private Text npmi;

    public ComparableKey(String w1, String w2, String decade, String npmi) {
        this.w1 = new Text(w1);
        this.w2 = new Text(w2);
        this.decade = new Text(decade);
        this.npmi = new Text(npmi);
    }

    public ComparableKey() {
        this.w1 = new Text("");
        this.w2 = new Text("");
        this.decade = new Text("");
        this.npmi = new Text("");
    }

    public ComparableKey(Text w1, Text w2, Text decade, Text npmi) {
        this.w1 = w1;
        this.w2 = w2;
        this.decade = decade;
        this.npmi = npmi;
    }

    public Text toText() {
        return new Text(this.toString());
    }

    public Text getW1() {
        return w1;
    }

    public Text getW2() {
        return w2;
    }

    public Text getDecade() {
        return decade;
    }

    public Text getnpmi() {
        return npmi;
    }

    public Double getDoublenpmi() {
        return Double.parseDouble(npmi.toString());
    }

    @Override
    public int compareTo(ComparableKey other) {

        // First, compare decades (descending order)
        int decadeCompare = other.decade.compareTo(this.decade);
        if (decadeCompare != 0) {
            return decadeCompare;
        }

        // Next, compare npmi (descending order)
        try {
            Double thisnpmi = Double.parseDouble(this.npmi.toString());
            Double othernpmi = Double.parseDouble(other.npmi.toString());
            int npmiCompare = othernpmi.compareTo(thisnpmi);
            if (npmiCompare != 0) {
                return npmiCompare;
            }
        } catch (NumberFormatException e) {
            System.out.println("Number Format Exception: " + e);
        }

        // Then, compare w1 (ascending order)
        int w1Compare = this.w1.compareTo(other.w1);
        if (w1Compare != 0) {
            return w1Compare;
        }

        // Finally, compare w2 (ascending order)
        return this.w2.compareTo(other.w2);
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        w1.write(dataOutput);
        w2.write(dataOutput);
        decade.write(dataOutput);
        npmi.write(dataOutput);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        w1.readFields(dataInput);
        w2.readFields(dataInput);
        decade.readFields(dataInput);
        npmi.readFields(dataInput);
    }

    @Override
    public String toString() {
        return w1.toString() + " " + w2.toString() + " " + decade.toString() + " " + npmi.toString() + " ";
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
    
        ComparableKey that = (ComparableKey) o;
    
        return w1.equals(that.w1) &&
               w2.equals(that.w2) &&
               decade.equals(that.decade) &&
               npmi.equals(that.npmi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(w1, w2, decade, npmi);
    }
}