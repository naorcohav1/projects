import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.apache.hadoop.mapreduce.Counter;

/**
 * Step1FrequencyCount
 */
public class Step1FrequencyCount {

    public static class Step1FreqCountMapper extends Mapper<LongWritable, Text, Text, Text> {

        private static Pattern HEBREW_PATTERN = Pattern.compile("[\\p{InHebrew}]+", Pattern.UNICODE_CASE);
        private String language;

        @Override
        public void setup(Context context) throws IOException, InterruptedException {
            language = context.getConfiguration().get("language", "heb");
        }

        // input - 2gram (line: w1?w2? decade occur)
        // output - <w1 w2 decade, occur>,
        // <word decade, occur>
        @Override
        public void map(LongWritable lineId, Text line, Context context) throws IOException, InterruptedException {

            String[] splitted = line.toString().split("\t");

            if (splitted.length < 3)
                return;

            String decade = convertYearToDecade(splitted[1]);
            String[] bigram = splitted[0].split(" ");
            Set<String> stopWords = language.equals("heb") ? App.hebrewStopWords : App.englishStopWords;
            boolean isHebrew = language.equals("heb");
            boolean validText = true;

            // Use Matcher for Hebrew case
            if (isHebrew) {
                Matcher m = HEBREW_PATTERN.matcher(splitted[0]);
                validText = m.matches();
            }

            if (validText) {

                if (bigram.length > 0 && !stopWords.contains(bigram[0])) {
                    Text keyW1 = new Text(bigram[0] + " " + decade);
                    context.write(keyW1, new Text(splitted[2])); // <word decade, occur>
                }

                if (bigram.length > 1 && !stopWords.contains(bigram[1])) {
                    Text keyW2 = new Text(bigram[1] + " " + decade);
                    context.write(keyW2, new Text(splitted[2])); // <word decade, occur>
                }

                if (bigram.length > 1 && !stopWords.contains(bigram[0]) && !stopWords.contains(bigram[1])) {
                    Text keyW1W2 = new Text(bigram[0] + " " + bigram[1] + " " + decade);
                    context.write(keyW1W2, new Text(splitted[2])); // <w1 w2 decade, occur>
                }
            }
        }

        public static String convertYearToDecade(String yearStr) {

            // Check if the input string is a valid year
            if (yearStr == null || yearStr.length() != 4 || !yearStr.matches("\\d{4}")) {
                return "Invalid year";
            }

            String decadeStr = yearStr.substring(0, 3) + "0";
            return decadeStr;
        }
    }

    public static class Step1FreqCountReducer extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            String[] keySplit = key.toString().split(" ");
            long sum = 0;
            for (Text value : values) {
                sum += Long.parseLong(value.toString());
            }
            if (keySplit.length < 3) {
                // in - <word decade, occur>
                context.write(key, new Text(String.valueOf(sum))); // output - <word decade,occur>
            } else {
                // in - <w1 w2 decade, occur>
                String w1 = keySplit[0];
                String w2 = keySplit[1];
                String decade = keySplit[2];
                context.getCounter("DecadesCounters", decade).increment(sum);
                context.write(new Text(w1 + " " + decade), key); // output - <w1 decade , <w1 w2 decade, occur> >
                context.write(new Text(w2 + " " + decade), key); // output - <w2 decade , <w1 w2 decade, occur> >
                context.write(key, new Text(String.valueOf(sum))); // output - <w1 w2 decade,occur>
            }
        }
    }

    public static class Step1CombinerClass extends Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            for (Text value : values) {
                sum += Long.parseLong(value.toString());
            }
            context.write(key, new Text(String.valueOf(sum))); // <key, aggregatedSum>
        }
    }

    public static class Step1PartitionerClass extends Partitioner<Text, Text> {
        @Override
        public int getPartition(Text key, Text value, int numPartitions) {
            return (key.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    public static void uploadToS3(String bucketName, String objectKey, String content) {

        AmazonS3 s3client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length());

        try (ByteArrayInputStream contentStream = new ByteArrayInputStream(content.getBytes())) {
            s3client.putObject(new PutObjectRequest(bucketName, objectKey, contentStream, metadata));
            System.out.println("Successfully uploaded counters data to S3");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error uploading counters data to S3");
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("[DEBUG] STEP 1 started!");
        System.out.println(args.length > 0 ? args[0] : "no args");
        Configuration conf = new Configuration();
        conf.set("language", "eng");
        Job job = Job.getInstance(conf, "FrequencyCount");
        job.setJarByClass(Step1FrequencyCount.class);
        job.setMapperClass(Step1FreqCountMapper.class);
        job.setReducerClass(Step1FreqCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setPartitionerClass(Step1PartitionerClass.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        // job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setCombinerClass(Step1CombinerClass.class);
        // TextInputFormat.addInputPath(job, new
        // Path("s3://datasets.elasticmapreduce/ngrams/books/20090715/heb-all/2gram/"));
        TextInputFormat.addInputPath(job, new Path("s3://n-gram-analysis/english_bigrams.txt"));

        // Generate a unique directory name with a timestamp
        // String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // String uniqueOutputDir = "output_step_1_" + timestamp;

        String uniqueOutputDir = "output_step_1_";

        FileOutputFormat.setOutputPath(job, new Path("s3://n-gram-analysis/" + uniqueOutputDir));

        if (job.waitForCompletion(true)) {
            CounterGroup group = job.getCounters().getGroup("DecadesCounters");
            StringBuilder counterData = new StringBuilder();

            for (Counter counter : group) {
                counterData.append(counter.getName()).append("\t").append(counter.getValue()).append("\n");
            }

            uploadToS3("n-gram-analysis", "counters/decadeCountsData.txt", counterData.toString());
            System.out.println("Step 1 finished");
        }else {
            System.out.println("Step 1 failed");
        }
    }
}