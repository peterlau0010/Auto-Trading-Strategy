package com.auto.trading.strategy;

import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Order;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.TrailingStopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import ch.qos.logback.core.net.server.Client;

/**
 * Hello world! PSAQZ9C345M3GAJJ
 */
public class App {
	BinanceApiWebSocketClient client;

	TimeSeries series;

	BinanceApiRestClient restClient;

	private static double LAST_TICK_CLOSE_PRICE;

	private static double INITIAL_BAR = 6;

	private static final Logger logger = LogManager.getLogger(App.class.getName());

	TradingRecord tradingRecord;

	Strategy strategy;

	public static void main(String[] args) {
		new App();
	}
	
	public void Aggregate () {
//		List<AggTrade> aggTrades = restClient.getAggTrades("BTCUSDT");
		List<Candlestick> candlesticks = restClient.getCandlestickBars("BTCUSDT", CandlestickInterval.ONE_MINUTE);
		candlesticks.forEach(trades ->System.out.println(trades));
		System.out.println(candlesticks.size());
	}

	public void init() {
		client = BinanceApiClientFactory.newInstance().newWebSocketClient();
		tradingRecord = new BaseTradingRecord();

		series = new BaseTimeSeries.SeriesBuilder().withName("btcusdt").build();
		series.setMaximumBarCount(20);

		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("API-KEY", "SECRET");
		restClient = factory.newRestClient();
	}

	public App() {
		init();
		
		Aggregate();
//		client.onCandlestickEvent("btcusdt", CandlestickInterval.ONE_MINUTE, response -> {
////			if (response.getBarFinal()) {
//			if (true) {
//				series.addBar(ZonedDateTime.now(), response.getOpen(), response.getHigh(), response.getLow(),
//						response.getClose(), response.getVolume());
//				LAST_TICK_CLOSE_PRICE = series.getBar(series.getEndIndex()).getClosePrice().doubleValue();
//				logger.debug("LAST_TICK_CLOSE_PRICE: " + LAST_TICK_CLOSE_PRICE);
////				logger.debug("LAST_TICK_CLOSE_PRICE: " + response.getClose());
//				if (series.getBarCount() < INITIAL_BAR) {
//					logger.debug("Initialing...");
//				}
//
//				if (series.getBarCount() == INITIAL_BAR) {
//					strategy = buildStrategy(series);
//				}
//
//				if (series.getBarCount() > INITIAL_BAR) {
//					liveTrade();
//				}
//
//			}
//		});

	}

	private static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

		RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 6);

		Rule buyingRule = new UnderIndicatorRule(rsiIndicator, 20d);
		Rule sellingRule = new TrailingStopLossRule(closePrice, PrecisionNum.valueOf(0.1));

		Strategy buySellSignals = new BaseStrategy(buyingRule, sellingRule);
		return buySellSignals;
	}

	public void liveTrade() {
		try {
//			ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

			int endIndex = series.getEndIndex();
//			TradingRecord tradingRecord = new BaseTradingRecord();
			if (strategy.shouldEnter(endIndex, tradingRecord)) {
				logger.info("Strategy Enter");
				boolean entered = tradingRecord.enter(endIndex, DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE),
						DoubleNum.valueOf(10));
				if (entered) {
					Order entry = tradingRecord.getLastEntry();
					System.out.println(
							"Entered on " + entry.getIndex() + " (price=" + DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE)
									+ ", amount=" + entry.getAmount().doubleValue() + ")");
				}
			}
			if (strategy.shouldExit(endIndex, tradingRecord)) {
				logger.info("Strategy Exit");

				boolean exited = tradingRecord.exit(endIndex, DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE),
						DoubleNum.valueOf(10));

				if (exited) {
					Order exit = tradingRecord.getLastExit();
					System.out.println(
							"Exited on " + exit.getIndex() + " (price=" + DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE)
									+ ", amount=" + exit.getAmount().doubleValue() + ")");
				}
			}

			double totalProfit = 0;
			for (Trade trade : tradingRecord.getTrades()) {
				double buy = trade.getEntry().getPrice().doubleValue();
				double sell = trade.getExit().getPrice().doubleValue();
				totalProfit += (sell - buy);
				System.out.println(trade + " | " + (sell - buy));
			}

			logger.info("Total Profit: " + totalProfit);
		} catch (Exception ex) {
			logger.error(ex.getMessage());
			logger.error(ex.toString());
		}

	}

}
