import java.io.IOException;

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
 * Step5DisplaySortedPMI
 */
public class Step5DisplaySortedPMI {

    public static class Step5DisplaySortedPMIMapper extends Mapper<LongWritable, Text, ComparableKey, Text> {

        // input - < w1 w2 decade, <npmi relPmi >>
        // output - forward input with comparable key, sort phase happens after map and
        // before reduce
        // < comparableKey(w1 w2 decade npmi), <npmi relPmi> >

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

            String[] split = value.toString().split("\t");
            String newKey = split[0];
            String npmi_relPmi = split[1];
            String npmi = npmi_relPmi.split(" ")[0];

            String[] keySplit = newKey.split(" ");
            String w1 = keySplit[0];
            String w2 = keySplit[1];
            String decade = keySplit[2];
            context.write(new ComparableKey(w1, w2, decade, npmi), new Text(npmi_relPmi));

        }
    }

    public static class Step5DisplaySortedPMIReducer extends Reducer<ComparableKey, Text, Text, Text> {

        private double minPmi;
        private double relMinPmi;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            super.setup(context);

            Configuration conf = context.getConfiguration();
            minPmi = Double.parseDouble(conf.get("minPmi"));
            relMinPmi = Double.parseDouble(conf.get("relMinPmi"));
        }

        // input - < comparableKey(w1 w2 decade npmi), <npmi relPmi> >
        // output - < w1 w2 decade, npmi > is a collocation
        @Override
        public void reduce(ComparableKey key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            String bigram = key.getW1().toString() + " " + key.getW2().toString();
            String decade = key.getDecade().toString();
            double curr_npmi = key.getDoublenpmi();

            // Assuming each key will have a single value.
            Text value = values.iterator().next();
            String[] parts = value.toString().split(" ");
            double curr_relPmi = parts.length == 2 ? Double.parseDouble(parts[1]) : Double.MAX_VALUE;

            if (curr_npmi >= minPmi || curr_relPmi >= relMinPmi) {
                context.write(new Text(bigram + " " + decade), new Text(String.valueOf(curr_npmi)));
            }
        }
    }

    public static class Step5PartitionerClass extends Partitioner<Text, Text> {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("[DEBUG] STEP 5 started!");
        // if (args.length != 2)
        //     return;
        String minPmiStr = args[0];
        String relMinPmiStr = args[1];
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "DisplaySortedPMI");
        conf.setDouble("minPmi", Double.parseDouble(minPmiStr));
        conf.setDouble("relMinPmi", Double.parseDouble(relMinPmiStr));
        job.setJarByClass(Step5DisplaySortedPMI.class);
        job.setMapperClass(Step5DisplaySortedPMIMapper.class);
        job.setReducerClass(Step5DisplaySortedPMIReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(Step5PartitionerClass.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setInputFormatClass(TextInputFormat.class);
        FileInputFormat.addInputPath(job, new Path("s3://n-gram-analysis/output_step_4_"));
        FileOutputFormat.setOutputPath(job, new Path("s3://n-gram-analysis/output_step_5_"));

        if (job.waitForCompletion(true)) {
            System.out.println("Step 5 finished");
        } else {
            System.out.println("Step 5 failed");
        }
    }
}