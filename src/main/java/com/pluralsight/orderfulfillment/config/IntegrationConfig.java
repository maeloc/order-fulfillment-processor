package com.pluralsight.orderfulfillment.config;

import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.jms.JmsConfiguration;
import org.apache.camel.component.sql.SqlComponent;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.pluralsight.orderfulfillment.generated.FulfillmentCenter;
import com.pluralsight.orderfulfillment.order.OrderStatus;

/**
 * Spring configuration for Apache Camel. Extends CamelConfiguration as this
 * class loads up the CamelContext along with the Spring Context.
 * 
 * @author Michael Hoffman, Pluralsight
 */
@Configuration
public class IntegrationConfig extends CamelConfiguration {

	@Inject
	private Environment environment;
	
	@Inject
	private DataSource dataSource;

	@Bean
	public ConnectionFactory jmsConnectionFactory() {
		return new ActiveMQConnectionFactory(environment.getProperty("activemq.broker.url"));
	}
	
	@Bean(initMethod="start", destroyMethod="stop")
	public PooledConnectionFactory pooledConnectionFactory() {
		PooledConnectionFactory factory = new PooledConnectionFactory();
		factory.setConnectionFactory(jmsConnectionFactory());
		factory.setMaxConnections(Integer.parseInt(environment.getProperty("pooledConnectionFactory.maxConnections")));
		return factory;
	}
	
	@Bean
	public JmsConfiguration jmsConfiguration() {
		JmsConfiguration jmsConfiguration = new JmsConfiguration();
		jmsConfiguration.setConnectionFactory(pooledConnectionFactory());
		return jmsConfiguration;
	}
	
	@Bean
	public ActiveMQComponent activeMq() {
		ActiveMQComponent activeMq = new ActiveMQComponent();
		activeMq.setConfiguration(jmsConfiguration());
		return activeMq;
	}
	
	/**
	 * SQL Component instance used for routing orders from the orders database
	 * and updating the orders database.
	 * 
	 * @return
	 */	
	@Bean
	public SqlComponent sql() {
		SqlComponent sqlComponent =  new SqlComponent();
		sqlComponent.setDataSource(dataSource);
		return sqlComponent;
	}
	
	/**
	 * Camel RouteBuilder for routing orders from the orders database. Routes any
	 * orders with status set to new, then updates the order status to be in
	 * process. The route sends the message exchange to a queue.
	 * 
	 * @return
	 */
	@Bean
	public RouteBuilder newWebSiteRouteBuilder() {
		return new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				from("sql:select id from orders.\"order\" where status = '"
						+ OrderStatus.NEW.getCode() + "'?"
						+ "consumer.onConsume=update orders.\"order\" set status = '"
						+ OrderStatus.PROCESSING.getCode() + "'"
						+ " where id = :#id")
				.beanRef("orderItemMessageTranslator", "transformToOrderItemMessage")
				.to("activemq:queue:ORDER_ITEM_PROCESSING");
			}
		};
	}
	
	/**
	 * Route builder to implement a Content-Based Router. Routes the message from
	 * the ORDER_ITEM_PROCESSING queue to the appropriate queue based on the
	 * fulfillment center element of the message. As the message from the
	 * ORDER_ITEM_PROCESSING queue is XML, a namespace is required. A Choice
	 * processor is used to realize the Content-Based Router. When the
	 * Fulfillment Center element is equal to the value of the ABC fulfillment
	 * center enumeration, the message will be routed to the ABC fulfillment
	 * center request queue. When the Fulfillment Center element is equal to the
	 * value of the Fulfillment Center 1 enumeration value, the message will be
	 * routed to the Fulfillment Center 1 request queue. If a message comes in
	 * with a Fulfillment Center element value that is unsupported, the message
	 * gets routed to an error queue. An XPath expression is used to lookup the
	 * fulfillment center value using the specified namespace.
	 * 
	 * Below is a snippet of the XML returned by the ORDER_ITEM_PROCESSING queue.
	 * 
	 * <Order xmlns="http://www.pluralsight.com/orderfulfillment/Order">
	 * <OrderType> <FulfillmentCenter>ABCFulfillmentCenter</FulfillmentCenter>
	 * 
	 * @return
	 */	
	@Bean
	public RouteBuilder fulfillmentCenterContentBasedRouter() {
		return new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				Namespaces namespace = new Namespaces("o", "http://www.pluralsight.com/orderfulfillment/Order");
				
				from("activemq:queue:ORDER_ITEM_PROCESSING")
				.choice()
					.when()
						.xpath("/o:Order/o:OrderType/o:FulfillmentCenter = '" + FulfillmentCenter.ABC_FULFILLMENT_CENTER.value()
								+ "'", namespace)
						.to("activemq:queue:ABC_FULFILLMENT_REQUEST")
					.when()
						.xpath("/o:Order/o:OrderType/o:FulfillmentCenter = '" + FulfillmentCenter.FULFILLMENT_CENTER_ONE.value()
								+ "'", namespace)
						.to("activemq:queue:FC1_FULFILLMENT_REQUEST")
					.otherwise()
						.to("activemq:queue:ERROR_FULFILLMENT_REQUEST");
			}
		};
	}
	
	
		
	
}
