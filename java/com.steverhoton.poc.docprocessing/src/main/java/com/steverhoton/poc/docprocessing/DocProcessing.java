package com.steverhoton.poc.docprocessing;

import jakarta.inject.Named;
import java.util.Map;
import java.util.HashMap;
import java.nio.file.Paths;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Properties;

@Named("docprocessing")
public class DocProcessing implements RequestHandler<SQSEvent, Void> {

    @Override
    public Void handleRequest(SQSEvent docEvent, Context context) {
        Map<String, SQSEvent.MessageAttribute> attributesMap = new HashMap<String, SQSEvent.MessageAttribute>();
        String bucketName = System.getenv("S3_BUCKET");
        for (SQSMessage message: docEvent.getRecords()){
            context.getLogger().log("Processing document " + message.getBody());
            context.getLogger().log("S3 File attributes: " + message.getMessageAttributes().toString());
            attributesMap = message.getMessageAttributes();
            context.getLogger().log("S3 file location: " + attributesMap.get("s3_file_key").getStringValue());
            final String fileKey = new String(attributesMap.get("s3_file_key").getStringValue());
            
            context.getLogger().log("Downloading from S3");
            S3TransferManager transferManager = S3TransferManager.create();
            DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                    .getObjectRequest(b -> b.bucket(bucketName).key(fileKey))
                    .destination(Paths.get("/tmp/foo.html"))
                    .build();

            FileDownload downloadFile = transferManager.downloadFile(downloadFileRequest);

            CompletedFileDownload downloadedResult = downloadFile.completionFuture().join();
            
            context.getLogger().log("File downloaded" + downloadedResult.response().contentLength());
            try {
                context.getLogger().log("Generating PDF");
                PdfRendererBuilder builder = new PdfRendererBuilder();
                File inputFile = new File("/tmp/foo.html");
                OutputStream os = new FileOutputStream("/tmp/foo.pdf");
                builder.useFastMode();
                builder.withFile(inputFile);
                builder.toStream(os);
                builder.run();
                context.getLogger().log("PDF generation complete");
            } catch (Exception e) {
                e.printStackTrace();
            }

            context.getLogger().log("Sending email");
            SesV2Client sesv2Client = SesV2Client.builder()
                .region(Region.US_EAST_1)
                .build();
            String sender = "documents@steverhoton.com";
            String recipient = "steve.rhoton@fullbay.com";
            String subject = "SO Doc";
            String body = message.getBody();
            String htmlBody = "<html><body><h1>SO Doc</h1><p>" + body + "</p></body></html>";

            try {
                byte[] pdfBytes = Files.readAllBytes(new File("/tmp/foo.pdf").toPath());
                Session session = Session.getDefaultInstance(new Properties());
                MimeMessage emailMessage = new MimeMessage(session);
                emailMessage.setSubject(subject);
                emailMessage.setFrom(new InternetAddress(sender));
                emailMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

                MimeMultipart messageBody = new MimeMultipart("alternative");
                MimeBodyPart wrap = new MimeBodyPart();
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(htmlBody, "text/html; charset=utf-8");
                messageBody.addBodyPart(htmlPart);
                wrap.setContent(messageBody);
                MimeMultipart msg = new MimeMultipart("mixed");
                emailMessage.setContent(msg);
                msg.addBodyPart(wrap);

                MimeBodyPart pdfPart = new MimeBodyPart();
                DataSource source = new ByteArrayDataSource(pdfBytes, "application/pdf");
                pdfPart.setDataHandler(new DataHandler(source));
                pdfPart.setFileName("foo.pdf");
                msg.addBodyPart(pdfPart);

                String reportName = "so.pdf";
                pdfPart.setFileName(reportName);
                pdfPart.setDisposition("attachment");

                context.getLogger().log("Sending email via SES");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                emailMessage.writeTo(outputStream);
                ByteBuffer buffer = ByteBuffer.wrap(outputStream.toByteArray());
                byte[] arr = new byte[buffer.remaining()];
                buffer.get(arr);

                SdkBytes data = SdkBytes.fromByteArray(arr);
                RawMessage rawMessage = RawMessage.builder().data(data).build();
                EmailContent emailContent = EmailContent.builder().raw(rawMessage).build();
                SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                    .content(emailContent)
                    .build();
               
                sesv2Client.sendEmail(sendEmailRequest);
                context.getLogger().log("Email sent");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        context.getLogger().log("Doc processing complete");
        return null;
    }
}
