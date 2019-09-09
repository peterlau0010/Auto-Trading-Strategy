package com.auto.trading.strategy;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

public class LoadHistoryRecord {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("API-KEY", "SECRET");
		BinanceApiRestClient restClient = factory.newRestClient();

		List<Candlestick> history = new ArrayList<Candlestick>();
		long start = 1514764799999l; // 2018/01/01
		long end = 1566691199999l; // 2019/08/25
//		start = 1546271999999l; //2019/08/01
		start = 1483199999999l; // 2017/01/01
		end = 1567267199999l;

		long timeFrame = 60000 * 60;// millisecond 1 min = 60000ms, 1hr = 1440000ms
		CandlestickInterval interval = CandlestickInterval.HOURLY;

		int limit = 720; // Get 720 record each time
		long eachLoopMs = timeFrame * limit;

		long count = ((end - start) % eachLoopMs) == 0 ? ((end - start) / eachLoopMs)
				: ((end - start) / eachLoopMs + 1);

		System.out.println("Count:" + count);
		System.out.println("Time Frame:" + timeFrame);
		System.out.println("Limit:" + limit);
		System.out.println("Each Loop Ms:" + (timeFrame * limit));
		SimpleDateFormat sdf = new SimpleDateFormat();
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

		for (int i = 0; i < count; i++) {
			List<Candlestick> candlesticks = restClient.getCandlestickBars("BTCUSDT", interval, limit, start,
					(start + eachLoopMs));

			history.addAll(candlesticks);

			start += eachLoopMs;
		}
//		List<Candlestick> candlesticks = restClient.getCandlestickBars("BTCUSDT", CandlestickInterval.DAILY);
//		history.addAll(candlesticks);
		FileWriter csvWriter = new FileWriter("history.csv");
		csvWriter.append("Time,Open,High,Low,Close,Volume,UTCTime\n");

		for (Candlestick candlestick : history) {
			csvWriter.append(candlestick.getOpenTime() + ",");
			csvWriter.append(candlestick.getOpen() + ",");
			csvWriter.append(candlestick.getHigh() + ",");
			csvWriter.append(candlestick.getLow() + ",");
			csvWriter.append(candlestick.getClose() + ",");
			csvWriter.append(candlestick.getVolume() + ",");
			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candlestick.getOpenTime()),
					ZoneId.systemDefault());
			csvWriter.append(zdt + "");
			csvWriter.append("\n");
		}

		csvWriter.flush();
		csvWriter.close();

		System.out.println(history.get(0));
		System.out.println(history.get(history.size() - 1));
		System.out.println(history.size());
		System.out.println("Start: " + sdf.format(new Date(history.get(0).getCloseTime())));
		System.out.println("End: " + sdf.format(new Date(history.get(history.size() - 1).getCloseTime())));
	}

}
