package com.pluralsight.orderfulfillment.abcfulfillmentcenter;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABCFulfillmentCenterAggregationStrategy implements AggregationStrategy {
	private static final Logger log = LoggerFactory.getLogger(ABCFulfillmentCenterAggregationStrategy.class);

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		if (oldExchange == null) {
			log.info("Old exchange aggregation");
			
			List<Object> exchangeList = new ArrayList<Object>();
			exchangeList.add(newExchange.getIn().getBody());
			newExchange.getIn().setBody(exchangeList);
			return newExchange;
		} else {
			log.info("New exchange aggregation");
			List<Object> exchangeList = oldExchange.getIn().getBody(List.class);
			exchangeList.add(newExchange.getIn().getBody());
			return oldExchange;
		}
	}
	
	
}
