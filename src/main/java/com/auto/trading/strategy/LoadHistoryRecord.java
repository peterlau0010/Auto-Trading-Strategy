package com.auto.trading.strategy;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

public class LoadHistoryRecord {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("API-KEY", "SECRET");
		BinanceApiRestClient restClient = factory.newRestClient();

		List<Candlestick> history = new ArrayList<Candlestick>();
		long start = 1546300800000l; // 2019/01/01
		long end = 1561939200000l; // 2019/07/01
		int limit = 720;
		int count = (int) ((end - start) / 1000 / 60 / limit);
		System.out.println(count);
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//		System.out.println(new Timestamp(end));
		for (int i = 0; i < count; i++) {
			System.out.println(sdf.format(new Date(start)));
			List<Candlestick> candlesticks = restClient.getCandlestickBars("BTCUSDT", CandlestickInterval.ONE_MINUTE,
					limit, start, end);

			history.addAll(candlesticks);

			System.out.println(sdf.format(new Date(history.get(history.size() - 1).getCloseTime())));
			start += (limit * 1000 * 60);
		}
		System.out.println(history.get(0));
		System.out.println(history.get(history.size() - 1));
		System.out.println(history.size());
		System.out.println(sdf.format(new Date(history.get(history.size() - 1).getCloseTime())));
	}

}
