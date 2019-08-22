package com.auto.trading.strategy;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

/**
 * Hello world! PSAQZ9C345M3GAJJ
 */
public class App {
	BinanceApiWebSocketClient client;

	TimeSeries series;

	public static void main(String[] args) {
		new App();
	}

	public void init() {
		client = BinanceApiClientFactory.newInstance().newWebSocketClient();
		series = new BaseTimeSeries.SeriesBuilder().withName("btcusdt").build();
	}

	public App() {
		init();

		client.onCandlestickEvent("btcusdt", CandlestickInterval.ONE_MINUTE, response -> {
			if (response.getBarFinal()) {
				series.addBar(ZonedDateTime.now(), response.getOpen(), response.getHigh(), response.getLow(),
						response.getClose(), response.getVolume());
				getSMA(series.getBarCount());
			}
		});
//		getSMA(1);
//		();

	}

	public void getSMA(int count) {

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		System.out.println("Latest close price : " + closePrice);

		SMAIndicator shortSma = new SMAIndicator(closePrice, 7);
		System.out.println("7-ticks-SMA value at the " + count + ": " + shortSma.getValue(count - 1).doubleValue());

		SMAIndicator longSma = new SMAIndicator(closePrice, 25);
		System.out.println("25-ticks-SMA value at the " + count + ": " + longSma.getValue(count - 1).doubleValue());

	}
}
