data "aws_iam_policy_document" "document_processing_lambda_logging" {
  statement {
    effect = "Allow"

    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = ["arn:aws:logs:*:*:*"]
  }
}

data "aws_iam_policy_document" "document_processing_lambda_s3_access" {
  statement {
    effect = "Allow"

    actions = [
      "s3:*"
    ]

    resources = ["*"]
  }
}

data "aws_iam_policy_document" "document_processing_lambda_ses_access" {
  statement {
    effect = "Allow"

    actions = [
      "ses:*"
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "document_processing_lambda_logging" {
  name        = "document_processing_lambda_logging"
  description = "Allows Processing Lambda functions to write logs to CloudWatch Logs."
  policy      = data.aws_iam_policy_document.document_processing_lambda_logging.json
  path        = "/"
}

resource "aws_iam_policy" "document_processing_lambda_s3_access" {
  name = "document_processing_lambda_s3_access"
  description = "Allows Processing Lambda functions to use S3"
  policy = data.aws_iam_policy_document.document_processing_lambda_s3_access.json
  path = "/"
}

resource "aws_iam_policy" "document_processing_lambda_ses_access" {
  name = "document_processing_lambda_ses_access"
  description = "Allows Processing Lambda functions to use SES"
  policy = data.aws_iam_policy_document.document_processing_lambda_ses_access.json
  path = "/"
}

resource "aws_iam_role" "document_processing_lambda_role" {
  name               = "document_processing_lambda_role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "document_processing_lambda_role_policy_attachment" {
  role       = aws_iam_role.document_processing_lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole"
}

resource "aws_iam_role_policy_attachment" "document_processing_lambda_role_logging_attachment" {
  role       = aws_iam_role.document_processing_lambda_role.name
  policy_arn = aws_iam_policy.document_processing_lambda_logging.arn
}

resource "aws_iam_role_policy_attachment" "document_processing_lambda_role_s3_attachment" {
  role = aws_iam_role.document_processing_lambda_role.name
  policy_arn = aws_iam_policy.document_processing_lambda_s3_access.arn
}

resource "aws_iam_role_policy_attachment" "document_processing_lambda_role_ses_attachment" {
  role = aws_iam_role.document_processing_lambda_role.name
  policy_arn = aws_iam_policy.document_processing_lambda_ses_access.arn
}
