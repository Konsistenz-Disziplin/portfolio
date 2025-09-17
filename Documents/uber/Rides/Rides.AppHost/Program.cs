var builder = DistributedApplication.CreateBuilder(args);
var kafka = builder.AddKafka("kafka")
                    .withKafkaUI(kafkaUI => kafkaUI.withHostPort(9100))
                    .withDataVolume(isReadOnly: false);
var apiservice = builder.AddProject<Projects.AspireApp_ApiService>("passenger-microservice").
                withReplicas(2).
                withReference(kafka);



builder.Build().Run();
