resource "aws_sqs_queue" "document_processing" {
    name = "document-processing"
    visibility_timeout_seconds = 4500
    redrive_policy = jsonencode({
        deadLetterTargetArn = aws_sqs_queue.document_redrive.arn
        maxReceiveCount = 1000
    })
}
resource "aws_sqs_queue" "document_redrive" {
    name = "document-redrive"
    visibility_timeout_seconds = 4500
}

