import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

/**
 * Step2BigramsAggregation
 */
public class Step2BigramsAggregation {

    public static class Step2BigramsAggregationMapper extends Mapper<LongWritable, Text, Text, Text> {

        // input - <w1 decade , <w1 w2 decade, occur> >,
        // <w1 w2 decade, occur>, <word decade, occur>
        // output - forward input
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String[] split = value.toString().split("\t");
            String newKey = split[0];
            String newValue = split[1];
            context.write(new Text(newKey), new Text(newValue));

        }
    }

    public static class Step2BigramsAggregationReducer extends Reducer<Text, Text, Text, Text> {

        
        // input - <w1 decade , <w1 w2 decade, occur> >, <w1 w2 decade, occur>, <word
        // decade,occur>
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            // Split the key to determine its type
            String[] keyParts = key.toString().split(" ");

            // Directly forward entries with keys indicating bigrams (w1 w2 decade)
            if (keyParts.length > 2) {
                for (Text value : values) {
                    context.write(key, value); // output - <w1 w2 decade, occur>
                }
            } else {
                // Process unigram keys (w1 decade)
                String word = keyParts[0];
                String occurrenceCount = null;
                List<Text> bigramValues = new ArrayList<>();

                for (Text value : values) {
                    String[] valueParts = value.toString().split(" ");

                    // Check if the value is a bigram occurrence or a single word occurrence
                    if (valueParts.length > 1) {
                        bigramValues.add(new Text(value));
                    } else {
                        occurrenceCount = value.toString();
                    }
                }

                // Verify that occurrenceCount has been set before using it
                if (occurrenceCount != null) {
                    for (Text bigramValue : bigramValues) {
                        String[] bigramParts = bigramValue.toString().split(" ");
                        String positionIndicator = word.equals(bigramParts[0]) ? "1" : "2";
                        String newValue = "w" + positionIndicator + " " + occurrenceCount;
                        context.write(bigramValue, new Text(newValue)); // output - <w1 w2 decade, wi c(wi)> | i->{1,2}
                    }
                } else {
                    System.err.println("Missing occurrence count for word: " + word);
                }
            }
        }
    }

    public static class Step2PartitionerClass extends Partitioner<Text, Text> {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("[DEBUG] STEP 2 started!");
        System.out.println(args.length > 0 ? args[0] : "no args");
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "BigramsAggregation");
        job.setJarByClass(Step2BigramsAggregation.class);
        job.setMapperClass(Step2BigramsAggregationMapper.class);
        job.setReducerClass(Step2BigramsAggregationReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(Step2PartitionerClass.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setInputFormatClass(TextInputFormat.class);
        FileInputFormat.addInputPath(job, new Path("s3://n-gram-analysis/output_step_1_"));
        FileOutputFormat.setOutputPath(job, new Path("s3://n-gram-analysis/output_step_2_"));

        if(job.waitForCompletion(true)) {
            System.out.println("Step 2 finished");
        }
        else{
            System.out.println("Step 2 failed");
        }
    }
}