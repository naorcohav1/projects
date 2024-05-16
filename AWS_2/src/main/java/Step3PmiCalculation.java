import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
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

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Step3PmiCalculation
 */
public class Step3PmiCalculation {

    public static class Step3PmiCalculationMapper extends Mapper<LongWritable, Text, Text, Text> {

        // input - <w1 w2 decade, occur>, <w1 w2 decade, wi c(wi)>
        // output - forward input
        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String[] split = value.toString().split("\t");
            String newKey = split[0];
            String newValue = split[1];
            context.write(new Text(newKey), new Text(newValue));

        }
    }

    public static class Step3PmiCalculationReducer extends Reducer<Text, Text, Text, Text> {

        // HashMap to hold data [decade -> count]
        public HashMap<String, Long> decadeCounts = new HashMap<>();

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            // Create Hashmap fron counters.txt file
            super.setup(context);

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1)
                    .build();

            S3Object s3object = s3Client
                    .getObject(new GetObjectRequest("n-gram-analysis", "counters/decadeCountsData.txt"));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(s3object.getObjectContent()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Each line is expected to be in the format "decade\tcount\t"
                    String[] parts = line.split("\t");
                    if (parts.length >= 2) {
                        String decade = parts[0];
                        Long count = Long.parseLong(parts[1]);
                        decadeCounts.put(decade, count);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read from S3", e);
            }

        }

        // input - <w1 w2 decade, occur>, <w1 w2 decade, wi c(wi)>
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            double cw1 = 0;
            double cw2 = 0;
            double cw1w2 = 0;
        
            // Parse the values to extract counts
            for (Text value : values) {
                String[] parts = value.toString().split(" ");
                if (parts.length == 2) { // Expecting "wi count"
                    if ("w1".equals(parts[0])) {
                        cw1 = Double.parseDouble(parts[1]);
                    } else if ("w2".equals(parts[0])) {
                        cw2 = Double.parseDouble(parts[1]);
                    }
                } else {
                    // Assuming single part is cw1w2
                    cw1w2 = Double.parseDouble(value.toString());
                }
            }
        
            // Ensure counts are not zero to avoid division by zero in PMI calculation
            if (cw1w2 > 0 && cw1 > 0 && cw2 > 0) {
                String[] keySplit = key.toString().split(" ");
                String decade = keySplit[2];
                Long n = decadeCounts.getOrDefault(decade, 0L); // Use getOrDefault to handle missing decades
                
                if (n > 0) { // Ensure a valid N
                    String npmi = calculateNPmi(cw1w2, cw1, cw2, n);
                    if (!npmi.equals("NaN")) {
                        context.write(key, new Text(npmi)); // output - <w1 w2 decade, npmi>
                    }
                }
            }
        }
        
        public String calculateNPmi(double cw1w2, double cw1, double cw2, double n) {
            double pW1W2 = cw1w2 / n;
            double pmi = Math.log(pW1W2) + Math.log(n) - Math.log(cw1 / n) - Math.log(cw2 / n);
            double npmi = pmi / (-Math.log(pW1W2)); // Normalize PMI
            return String.valueOf(npmi);
        }
    }

    public static class Step3PartitionerClass extends Partitioner<Text, Text> {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("[DEBUG] STEP 3 started!");
        System.out.println(args.length > 0 ? args[0] : "no args");
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "PmiCalculation");
        job.setJarByClass(Step3PmiCalculation.class);
        job.setMapperClass(Step3PmiCalculationMapper.class);
        job.setReducerClass(Step3PmiCalculationReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(Step3PartitionerClass.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setInputFormatClass(TextInputFormat.class);
        FileInputFormat.addInputPath(job, new Path("s3://n-gram-analysis/output_step_2_"));
        FileOutputFormat.setOutputPath(job, new Path("s3://n-gram-analysis/output_step_3_"));

        if (job.waitForCompletion(true)) {
            System.out.println("Step 3 finished");
        } else {
            System.out.println("Step 3 failed");
        }
    }
}