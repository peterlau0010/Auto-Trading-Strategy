package com.auto.trading.strategy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ta4j.core.AnalysisCriterion;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Order;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.AverageProfitCriterion;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.BuyAndHoldCriterion;
import org.ta4j.core.analysis.criteria.LinearTransactionCostCriterion;
import org.ta4j.core.analysis.criteria.MaximumDrawdownCriterion;
import org.ta4j.core.analysis.criteria.NumberOfBarsCriterion;
import org.ta4j.core.analysis.criteria.NumberOfTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.TrailingStopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

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

		client.onCandlestickEvent("btcusdt", CandlestickInterval.ONE_MINUTE, response -> {
//			if (response.getBarFinal()) {
			if (true) {
				series.addBar(ZonedDateTime.now(), response.getOpen(), response.getHigh(), response.getLow(),
						response.getClose(), response.getVolume());
				LAST_TICK_CLOSE_PRICE = series.getBar(series.getEndIndex()).getClosePrice().doubleValue();
				logger.debug("LAST_TICK_CLOSE_PRICE: " + LAST_TICK_CLOSE_PRICE);
//				logger.debug("LAST_TICK_CLOSE_PRICE: " + response.getClose());
				if (series.getBarCount() < INITIAL_BAR) {
					logger.debug("Initialing...");
				}

				if (series.getBarCount() == INITIAL_BAR) {
					strategy = buildStrategy(series);
				}

				if (series.getBarCount() > INITIAL_BAR) {
					liveTrade();
				}

			}
		});

//		List<Candlestick> candlesticks = restClient.getCandlestickBars("BTCUSDT", CandlestickInterval.ONE_MINUTE,2000,1514736000000l,1546272000000l);
//		System.out.println(candlesticks.get(0));
//		System.out.println(candlesticks.get(candlesticks.size()-1));
//		candlesticks.forEach(candlestick -> {
//			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(candlestick.getCloseTime()),
//					ZoneId.systemDefault());
//			series.addBar(zdt, candlestick.getOpen(), candlestick.getHigh(), candlestick.getLow(),
//					candlestick.getClose(), candlestick.getVolume());
//		});

//		getSMA(candlesticks.size() - 1);
//		buyStrategy(candlesticks.size() - 1);

	}

	private static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
//		SMAIndicator sma = new SMAIndicator(closePrice, 12);

		RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 6);

		Rule buyingRule = new UnderIndicatorRule(rsiIndicator, 50d);
		Rule sellingRule = new TrailingStopLossRule(closePrice, PrecisionNum.valueOf(0.0000000001));
//		Rule sellingRule = new OverIndicatorRule(rsiIndicator, 50d);
		// Signals
		// Buy when SMA goes over close price
		// Sell when close price goes over SMA
		Strategy buySellSignals = new BaseStrategy(buyingRule, sellingRule);
		return buySellSignals;
	}

	public void liveTrade() {
		try {
			ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

			int endIndex = series.getEndIndex();
//			TradingRecord tradingRecord = new BaseTradingRecord();
			if (strategy.shouldEnter(endIndex)) {
				logger.info("Strategy Enter");
				boolean entered = tradingRecord.enter(endIndex, DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE),
						DoubleNum.valueOf(10));
				if (entered) {
					Order entry = tradingRecord.getLastEntry();
					System.out.println("Entered on " + entry.getIndex() + " (price=" + DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE)
							+ ", amount=" + entry.getAmount().doubleValue() + ")");
				}
			}
			
			if (strategy.shouldExit(endIndex)) {
				logger.info("Strategy Exit");

				boolean exited = tradingRecord.exit(endIndex, DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE),
						DoubleNum.valueOf(10));

				if (exited) {
					Order exit = tradingRecord.getLastExit();
					System.out.println("Exited on " + exit.getIndex() + " (price=" + DoubleNum.valueOf(LAST_TICK_CLOSE_PRICE)
							+ ", amount=" + exit.getAmount().doubleValue() + ")");
				}
			}

			for (Trade trade : tradingRecord.getTrades()) {
				double buy = trade.getEntry().getPrice().doubleValue();
				double sell = trade.getExit().getPrice().doubleValue();
				System.out.println(trade + " | " + (sell - buy));
			}

		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}

	}

	public void buyStrategy(int count) {
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

		// Get Indicator Values
		RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 6);
		double rsiValue = rsiIndicator.getValue(count - 1).doubleValue();

		ADXIndicator adxIndicator = new ADXIndicator(series, 14);
		double adx = adxIndicator.getValue(count - 1).doubleValue();

		logger.info("RSI: " + rsiValue);
		logger.info("ADX: " + adx);

		Rule buyingRule = new UnderIndicatorRule(rsiIndicator, 30d);
		Rule sellingRule = new TrailingStopLossRule(closePrice, PrecisionNum.valueOf(0.05));
//		Rule sellingRule = new CrossedUpIndicatorRule(rsiIndicator, 60d);

		Strategy strategy = new BaseStrategy(buyingRule, sellingRule);

		TimeSeriesManager manager = new TimeSeriesManager(series);
		TradingRecord tradingRecord = manager.run(strategy);
		System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());

		double sumBuy = 0;
		double sumSell = 0;
		for (Trade trade : tradingRecord.getTrades()) {
			double buy = trade.getEntry().getPrice().doubleValue();
			double sell = trade.getExit().getPrice().doubleValue();
			System.out.println(trade + " | " + (sell - buy));
			sumBuy += buy;
			sumSell += sell;
		}

		System.out.println("Total Buy: " + sumBuy);
		System.out.println("Total Sell: " + sumSell);

		CashFlow cashFlow = new CashFlow(series, tradingRecord);
		// Total profit
		TotalProfitCriterion totalProfit = new TotalProfitCriterion();
		System.out.println("Total profit: " + totalProfit.calculate(series, tradingRecord));
		// Number of ticks
		System.out.println("Number of Bars: " + new NumberOfBarsCriterion().calculate(series, tradingRecord));
		// Average profit (per tick)
		System.out
				.println("Average profit (per tick): " + new AverageProfitCriterion().calculate(series, tradingRecord));
		// Number of trades
		System.out.println("Number of trades: " + new NumberOfTradesCriterion().calculate(series, tradingRecord));
		// Profitable trades ratio
		System.out.println(
				"Profitable trades ratio: " + new AverageProfitableTradesCriterion().calculate(series, tradingRecord));
		// Maximum drawdown
		System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));
		// Reward-risk ratio
		System.out.println("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(series, tradingRecord));
		// Total transaction cost
		System.out.println("Total transaction cost (from $1000): "
				+ new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));
		// Buy-and-hold
		System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));
		// Total profit vs buy-and-hold
		System.out.println("Custom strategy profit vs buy-and-hold strategy profit: "
				+ new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));

		if (rsiValue <= 20) {
			// execute buy
			logger.info("Execute Buy");
		}
	}
}
