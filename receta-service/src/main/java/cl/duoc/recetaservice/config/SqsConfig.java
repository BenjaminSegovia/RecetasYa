package cl.duoc.recetaservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
public class SqsConfig {
    
    @Value("${aws.sqs.endpoint}")
    private String endpoint;

    @Value("${aws.sqs.region}")
    private String region;

    /**
     * Apunta al SQS de LocalStack mientras se desarrolla en local.
     * Las credenciales son de prueba: LocalStack no las valida contra AWS real,
     * solo exige que existan. Al pasar a AWS real, se elimina el endpointOverride
     * y se usan credenciales reales (o un rol IAM si corre en ECS/Lambda).
     */
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }
}
