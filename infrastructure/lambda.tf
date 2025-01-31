resource "aws_cloudwatch_log_group" "document_processing_lambda" {
  name              = "/aws/lambda/document_processing_lambda"
  retention_in_days = 14
}

resource "aws_lambda_function" "document_processing_lambda" {
  function_name    = "document_processing_lambda"
  role             = aws_iam_role.document_processing_lambda_role.arn
  handler          = "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"
  runtime          = "java21"
  filename         = "../java/com.steverhoton.poc.docprocessing/target/function.zip"
  timeout          = 900
  memory_size      = 1024
  publish          = true
  ephemeral_storage {
    size = 1024
  }
  logging_config {
    log_group = aws_cloudwatch_log_group.document_processing_lambda.name
    log_format = "Text"
  } 
  environment {
    variables = {
      "S3_BUCKET" = "sprhoto-doc-processing"
    }
  }
}

resource "aws_lambda_event_source_mapping" "document_processing_mapping" {
  event_source_arn = aws_sqs_queue.document_processing.arn
  function_name = aws_lambda_function.document_processing_lambda.arn
}
