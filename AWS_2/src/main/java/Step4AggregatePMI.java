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
 * Step4AggregatePMI
 */
public class Step4AggregatePMI {

    public static class Step4AggregatePMIMapper extends Mapper<LongWritable, Text, Text, Text> {

        // input - <w1 w2 decade, npmi >
        // output - < decade, <w1 w2 pmi> >

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String[] split = value.toString().split("\t");
            String newKey = split[0];
            String npmi = split[1];

            String[] keySplit = newKey.split(" ");
            String w1 = keySplit[0];
            String w2 = keySplit[1];
            String decade = keySplit[2];
            context.write(new Text(decade), new Text(w1 + " " + w2 + " " + npmi));

        }
    }

    public static class Step4AggregatePMIReducer extends Reducer<Text, Text, Text, Text> {

        // input - < decade, <w1 w2 pmi> >
        // output - < w1 w2 decade, <npmi relPmi >>
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            double sumPmiPerDecade = 0;
            List<String> bigramsWithPmi = new ArrayList<>();

            // First pass: Calculate the sum of PMI for the decade and store each bigram with its pmi
            for (Text value : values) {
                String[] parts = value.toString().split(" ");
                if (parts.length == 3) { // Ensure the value contains <w1 w2 pmi>
                    double pmi = Double.parseDouble(parts[2]);
                    sumPmiPerDecade += pmi;
                    bigramsWithPmi.add(value.toString());
                }
            }

            // Second pass: Calculate relPmi for each bigram and emit the result
            for (String bigramWithPmi : bigramsWithPmi) {
                String[] parts = bigramWithPmi.split(" ");
                String w1 = parts[0];
                String w2 = parts[1];
                double pmi = Double.parseDouble(parts[2]);
                double relPmi = sumPmiPerDecade > 0 ? pmi / sumPmiPerDecade : 0; // Prevent division by zero

                // Construct the output key as <w1 w2 decade>
                Text outputKey = new Text(w1 + " " + w2 + " " + key.toString());

                // Construct the output value as <npmi, relPmi>
                Text outputValue = new Text(pmi + " " + relPmi);

                context.write(outputKey, outputValue);
            }

        }
    }

    public static class Step4PartitionerClass extends Partitioner<Text, Text> {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }

    }

    public static void main(String[] args) throws Exception {

        System.out.println("[DEBUG] STEP 4 started!");
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "AggregatePMI");
        job.setJarByClass(Step4AggregatePMI.class);
        job.setMapperClass(Step4AggregatePMIMapper.class);
        job.setReducerClass(Step4AggregatePMIReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(Step4PartitionerClass.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setInputFormatClass(TextInputFormat.class);
        FileInputFormat.addInputPath(job, new Path("s3://n-gram-analysis/output_step_3_"));
        FileOutputFormat.setOutputPath(job, new Path("s3://n-gram-analysis/output_step_4_"));

        if (job.waitForCompletion(true)) {
            System.out.println("Step 4 finished");
        } else {
            System.out.println("Step 4 failed");
        }
    }
}