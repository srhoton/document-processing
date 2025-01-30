<?php
require 'vendor/autoload.php';
use Aws\S3\S3Client;
use Aws\Exception\AwsException;
use Aws\Sqs\SqsClient;

$returnedHtml = ' <html> <body> <h1>My First Heading</h1> <p>My first paragraph.</p> </body> </html> ';


$s3Client = new S3Client([
  'version' => '2006-03-01',
  'region'  => 'us-east-1',
]);

$sqsClient = new SqsClient([
    'region' => 'us-east-1',
    'version' => '2012-11-05'
]);

$bucket = getenv('S3_BUCKET')?: die("No s3 bucket specified");
$queue_url = getenv('SQS_QUEUE_URL')?: die("No queue url specified");

$s3_file_key = 'uploads/test.html';

try {
  $result = $s3Client->putObject([
    'Bucket' => $bucket,
    'Key'    => $s3_file_key,
    'Body'   => $returnedHtml,
    'ContentType' => 'text/html',
  ]);
} catch (AwsException $e) {
  echo $e->getMessage();
  echo "\n";
}

echo "Upload completed successfully. Adding to SQS now." ;

$sqs_params = [
  'DelaySeconds' => 10, 
  'MessageAttributes' => [
    's3_file_key' => [
      'DataType' => 'String',
      'StringValue' => "$s3_file_key"
    ]
  ],
  'MessageBody' => 'This is a test',
  'QueueUrl' => $queue_url
];

try {
  $result = $sqsClient->sendMessage($sqs_params);
} catch (AwsException $e) {
  echo $e->getMessage();
  echo "\n";
}

?>
