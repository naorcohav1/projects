import java.util.Arrays;
import java.util.HashSet;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClientBuilder;
import com.amazonaws.services.elasticmapreduce.model.*;

public class App {

        public static AWSCredentialsProvider credentialsProvider;
        public static AmazonS3 S3;
        public static AmazonEC2 ec2;
        public static AmazonElasticMapReduce emr;

        public static int numberOfInstances = 3;

        public static HashSet<String> englishStopWords = new HashSet<>(Arrays.asList("a", "about", "above", "across",
                        "after", "afterwards", "again", "against", "all", "almost", "alone",
                        "along", "already", "also", "although", "always", "am", "among", "amongst", "amoungst",
                        "amount", "an",
                        "and", "another", "any", "anyhow", "anyone", "anything", "anyway", "anywhere", "are", "around",
                        "as", "at",
                        "back", "be", "became", "because", "become", "becomes", "becoming", "been", "before",
                        "beforehand", "behind",
                        "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom", "but",
                        "by", "call",
                        "can", "cannot", "cant", "co", "computer", "con", "could", "couldnt", "cry", "de", "describe",
                        "detail", "do",
                        "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven", "else", "elsewhere",
                        "empty",
                        "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except",
                        "few", "fifteen",
                        "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found",
                        "four", "from",
                        "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence",
                        "her", "here",
                        "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how",
                        "however",
                        "hundred", "i", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its",
                        "itself", "keep", "last",
                        "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might",
                        "mill", "mine",
                        "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely",
                        "neither", "never",
                        "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now",
                        "nowhere", "of",
                        "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise",
                        "our", "ours", "ourselves",
                        "out", "over", "own", "part", "per", "perhaps", "please", "put", "rather", "re", "same", "see",
                        "seem", "seemed",
                        "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere",
                        "six", "sixty", "so",
                        "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still",
                        "such", "system", "take", "ten",
                        "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter",
                        "thereby", "therefore",
                        "therein", "thereupon", "these", "they", "thick", "thin", "third", "this", "those", "though",
                        "three", "through",
                        "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve",
                        "twenty", "two", "un", "under",
                        "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever",
                        "when", "whence", "whenever",
                        "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether",
                        "which", "while", "whither",
                        "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would",
                        "yet", "you", "your",
                        "yours", "yourself", "yourselves"));

        public final static HashSet<String> hebrewStopWords = new HashSet<>(Arrays.asList("של", "רב", "פי", "עם",
                        "עליו", "עליהם", "על", "עד", "מן", "מכל", "מי", "מהם", "מה", "מ", "למה", "לכל", "לי", "לו",
                        "להיות", "לה", "לא", "כן", "כמה", "כלי", "כל", "כי", "יש", "ימים", "יותר",
                        "יד", "י", "זה", "ז", "ועל", "ומי", "ולא", "וכן", "וכל", "והיא", "והוא", "ואם", "ו", "הרבה",
                        "הנה", "היו",
                        "היה", "היא", "הזה", "הוא", "דבר", "ד", "ג", "בני", "בכל", "בו", "בה", "בא", "את", "אשר", "אם",
                        "אלה", "אל",
                        "אך", "איש", "אין", "אחת", "אחר", "אחד", "אז", "אותו", "־", "^", "?", ";", ":", "1", ".", "-",
                        "*", "\"", "״", "׳",
                        "!", "שלשה", "בעל", "פני", ")", "גדול", "שם", "עלי", "עולם", "מקום", "לעולם", "לנו", "להם",
                        "ישראל", "יודע",
                        "זאת", "השמים", "הזאת", "הדברים", "הדבר", "הבית", "האמת", "דברי", "במקום", "בהם", "אמרו",
                        "אינם", "אחרי",
                        "אותם", "אדם", "(", "חלק", "שני", "שכל", "שאר", "ש", "ר", "פעמים", "נעשה", "ן", "ממנו", "מלא",
                        "מזה", "ם",
                        "לפי", "ל", "כמו", "כבר", "כ", "זו", "ומה", "ולכל", "ובין", "ואין", "הן", "היתה", "הא", "ה",
                        "בל", "בין",
                        "בזה", "ב", "אף", "אי", "אותה", "או", "אבל", "א"));

        public static void main(String[] args) {

        //         if (args.length >= 3 && "ExtractCollations".equals(args[0])) {
        //                 String minPmi = args[1];
        //                 String relMinPmi = args[2];

        //                 // Now you can use minPmi and relMinPmi for setting up your EMR job
        //                 setupAndRunEmrJob(minPmi, relMinPmi);
        //         } else {
        //                 System.out.println("Usage: java -jar ass2.jar ExtractCollations <minPmi> <relMinPmi>");
        //         }
        // }

        // public static void setupAndRunEmrJob(String minPmi, String relMinPmi) {

                credentialsProvider = new ProfileCredentialsProvider();

                System.out.println("[INFO] Connecting to aws");
                ec2 = AmazonEC2ClientBuilder.standard()
                                .withCredentials(credentialsProvider)
                                .withRegion("us-east-1")
                                .build();
                S3 = AmazonS3ClientBuilder.standard()
                                .withCredentials(credentialsProvider)
                                .withRegion("us-east-1")
                                .build();
                emr = AmazonElasticMapReduceClientBuilder.standard()
                                .withCredentials(credentialsProvider)
                                .withRegion("us-east-1")
                                .build();

                System.out.println("list cluster");
                System.out.println(emr.listClusters());
                
                // --------------Step 1--------------

                HadoopJarStepConfig step1 = new HadoopJarStepConfig()
                                .withJar("s3://n-gram-analysis/jars/Step1FrequencyCount.jar")
                                .withMainClass("Step1FrequencyCount");

                StepConfig stepConfig1 = new StepConfig()
                                .withName("Step1")
                                .withHadoopJarStep(step1)
                                .withActionOnFailure("TERMINATE_JOB_FLOW");

                // --------------Step 2--------------
                HadoopJarStepConfig step2 = new HadoopJarStepConfig()
                                .withJar("s3://n-gram-analysis/jars/Step2BigramsAggregation.jar")
                                .withMainClass("Step2BigramsAggregation");

                StepConfig stepConfig2 = new StepConfig()
                                .withName("Step2")
                                .withHadoopJarStep(step2)
                                .withActionOnFailure("TERMINATE_JOB_FLOW");

                // --------------Step 3--------------
                HadoopJarStepConfig step3 = new HadoopJarStepConfig()
                                .withJar("s3://n-gram-analysis/jars/Step3PmiCalculation.jar")
                                .withMainClass("Step3PmiCalculation");

                StepConfig stepConfig3 = new StepConfig()
                                .withName("Step3")
                                .withHadoopJarStep(step3)
                                .withActionOnFailure("TERMINATE_JOB_FLOW");

                // --------------Step 4--------------
                HadoopJarStepConfig step4 = new HadoopJarStepConfig()
                                .withJar("s3://n-gram-analysis/jars/Step4AggregatePMI.jar")
                                .withMainClass("Step4AggregatePMI");

                StepConfig stepConfig4 = new StepConfig()
                                .withName("Step4")
                                .withHadoopJarStep(step4)
                                .withActionOnFailure("TERMINATE_JOB_FLOW");

                // --------------Step 5--------------
                HadoopJarStepConfig step5 = new HadoopJarStepConfig()
                                .withJar("s3://n-gram-analysis/jars/Step5DisplaySortedPMI.jar")
                                .withMainClass("Step5DisplaySortedPMI")
                                .withArgs("0.2", "0.5");

                StepConfig stepConfig5 = new StepConfig()
                                .withName("Step5")
                                .withHadoopJarStep(step5)
                                .withActionOnFailure("TERMINATE_JOB_FLOW");

                // Job flow
                JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                                .withInstanceCount(numberOfInstances)
                                .withMasterInstanceType(InstanceType.M4Large.toString())
                                .withSlaveInstanceType(InstanceType.M4Large.toString())
                                .withHadoopVersion("2.9.2")
                                .withEc2KeyName("vockey")
                                .withKeepJobFlowAliveWhenNoSteps(false)
                                .withPlacement(new PlacementType("us-east-1a"));

                System.out.println("Set steps");
                RunJobFlowRequest runFlowRequest = new RunJobFlowRequest()
                                .withName("Map reduce project")
                                .withInstances(instances)
                                .withSteps(stepConfig1, stepConfig2, stepConfig3, stepConfig4, stepConfig5)
                                .withLogUri("s3://n-gram-analysis/logs/")
                                .withServiceRole("EMR_DefaultRole")
                                .withJobFlowRole("EMR_EC2_DefaultRole")
                                .withReleaseLabel("emr-5.11.0");

                RunJobFlowResult runJobFlowResult = emr.runJobFlow(runFlowRequest);
                String jobFlowId = runJobFlowResult.getJobFlowId();
                System.out.println("Ran job flow with id: " + jobFlowId);
        }

}
