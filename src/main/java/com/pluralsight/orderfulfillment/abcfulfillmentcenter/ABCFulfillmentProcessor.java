package com.pluralsight.orderfulfillment.abcfulfillmentcenter;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ABCFulfillmentProcessor {

	private static final Logger log = LoggerFactory.getLogger(ABCFulfillmentProcessor.class);
	
	public List<Map<String, Object>> processAggregate(List orders) throws Exception {
		log.info("Processing the aggregate");
		
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>(); 
				
		// 1 - Add the header first. LinkedHashMap para garantizar el orden!!
		Map<String, Object> header = new LinkedHashMap<String, Object>(3);
		header.put("orderNumber", "Order Number");
		header.put("firstName", "First Name");
		header.put("lastName", "Last Name");
		results.add(header);
		log.info("Header added: " + header.toString());
		
		// 2 - Add each order
		try {
			if (orders != null) {
				for(int i = 0; i < orders.size(); i++) {
					com.pluralsight.orderfulfillment.generated.Order order = unmashallOrder((String) orders.get(i));
					Map<String, Object> row = new LinkedHashMap<String, Object>(3);
					row.put("orderNumber", order.getOrderType().getOrderNumber());
					row.put("firstName", order.getOrderType().getFirstName());
					row.put("lastName", order.getOrderType().getLastName());
					results.add(row);
					log.info("Order added: " + row.toString());
				}
			}
		} catch (Exception e) {
			log.error("An error ocurred while trying to process messages for the ABC fulfillment center: "
					+ e.getMessage(), e);
			throw e;
		}
				
		return results;
		
	}

	private com.pluralsight.orderfulfillment.generated.Order unmashallOrder(String orderXml) throws JAXBException {
		// Unmarshall the Order from a XML string to the generated order class
		JAXBContext context = JAXBContext.newInstance(com.pluralsight.orderfulfillment.generated.Order.class);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		return (com.pluralsight.orderfulfillment.generated.Order) unmarshaller.unmarshal(new StringReader(orderXml));
	}
	
}
