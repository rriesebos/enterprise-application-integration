package com.eai.scootermaintenanceapp.messaging;

import com.eai.scootermaintenanceapp.data.model.Region;
import com.eai.scootermaintenanceapp.data.model.Scooter;
import com.swiftmq.amqp.AMQPContext;
import com.swiftmq.amqp.v100.client.AMQPException;
import com.swiftmq.amqp.v100.client.Connection;
import com.swiftmq.amqp.v100.client.ExceptionListener;
import com.swiftmq.amqp.v100.client.Producer;
import com.swiftmq.amqp.v100.client.QoS;
import com.swiftmq.amqp.v100.client.Session;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import com.swiftmq.amqp.v100.types.AMQPString;

import java.util.Date;

public class ScooterProducer {
    private static final String LOG_TAG = ScooterProducer.class.getSimpleName();

    private final Region region;
    private final String hostName;
    private final Integer port;

    private boolean isProducing = false;

    private Connection connection;
    private Session session;
    private Producer producer;

    ScooterProducer(Region region, String hostName, Integer port) {
        this.region = region;
        this.hostName = hostName;
        this.port = port;
    }

    public void startProducing() {
        isProducing = true;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    AMQPContext ctx = new AMQPContext(AMQPContext.CLIENT);

                    // Authentication is disabled because required imports for the constructor below
                    //     new Connection(ctx, hostName, port, "default", "default");
                    // gave java.lang.ClassNotFoundException errors due to differences between
                    // the Java Virtual Machine (JVM) and the Dalvik Virtual Machine (DVM) that
                    // Android uses (javax is not included).
                    connection = new Connection(ctx, hostName, port, false);
                    connection.setContainerId("maintenanceAppProducer");
                    connection.setExceptionListener(new ExceptionListener() {
                        public void onException(Exception e) {
                            e.printStackTrace();
                        }
                    });

                    connection.connect();

                    session = connection.createSession(50, 50);
                    producer = session.createProducer(region.getId(), QoS.AT_LEAST_ONCE);

                    while (isProducing) {
                        try {
                            AMQPMessage message = new AMQPMessage();
                            message.setAmqpValue(new AmqpValue(new AMQPString(new Date().toString())));

                            producer.send(message);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    public void produce(Scooter scooter) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AMQPContext ctx = new AMQPContext(AMQPContext.CLIENT);

                    // Authentication is disabled because required imports for the constructor below
                    //     new Connection(ctx, hostName, port, "default", "default");
                    // gave java.lang.ClassNotFoundException errors due to differences between
                    // the Java Virtual Machine (JVM) and the Dalvik Virtual Machine (DVM) that
                    // Android uses (javax is not included).
                    if (connection == null) {
                        connection = new Connection(ctx, hostName, port, false);
                        connection.setContainerId("maintenanceAppProducer");
                        connection.setExceptionListener(new ExceptionListener() {
                            public void onException(Exception e) {
                                e.printStackTrace();
                            }
                        });

                        connection.connect();
                    }

                    if (session == null) {
                        session = connection.createSession(50, 50);
                    }

                    if (producer == null) {
                        producer = session.createProducer("TEST.FOO", QoS.AT_LEAST_ONCE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                AMQPMessage message = new AMQPMessage();
                String jsonString = MessagingMapper.scooterToJson(scooter);
                message.setAmqpValue(new AmqpValue(new AMQPString(jsonString)));

                try {
                    producer.send(message);
                } catch (AMQPException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    public void close() {
        try {
            if (producer != null) {
                producer.close();
            }

            if (session != null) {
                session.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
