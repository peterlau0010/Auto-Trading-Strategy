package com.auto.trading.strategy;

import java.time.ZonedDateTime;

import org.ta4j.core.*;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.TrailingStopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import ta4jexamples.loaders.CsvTradesLoader;

public class AppTest {

	/** Close price of the last bar */
	private static Num LAST_BAR_CLOSE_PRICE;

	/**
	 * Builds a moving time series (i.e. keeping only the maxBarCount last bars)
	 * 
	 * @param maxBarCount the number of bars to keep in the time series (at maximum)
	 * @return a moving time series
	 */
	private static TimeSeries initMovingTimeSeries(int maxBarCount) {
		TimeSeries series = CsvTradesLoader.loadBitstampSeries();
		System.out.print("Initial bar count: " + series.getBarCount());
		// Limitating the number of bars to maxBarCount
		series.setMaximumBarCount(maxBarCount);
		LAST_BAR_CLOSE_PRICE = series.getBar(series.getEndIndex()).getClosePrice();
		System.out.println(" (limited to " + maxBarCount + "), close price = " + LAST_BAR_CLOSE_PRICE);
		return series;
	}

	/**
	 * @param series a time series
	 * @return a dummy strategy
	 */
	
	
	private static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}

		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		SMAIndicator sma = new SMAIndicator(closePrice, 12);

		// Signals
		// Buy when SMA goes over close price
		// Sell when close price goes over SMA
		Strategy buySellSignals = new BaseStrategy(new OverIndicatorRule(sma, closePrice),
//                new UnderIndicatorRule(sma, closePrice)
				new TrailingStopLossRule(closePrice, PrecisionNum.valueOf(0.01)));
		return buySellSignals;
	}

	/**
	 * Generates a random decimal number between min and max.
	 * 
	 * @param min the minimum bound
	 * @param max the maximum bound
	 * @return a random decimal number between min and max
	 */
	private static Num randDecimal(Num min, Num max) {
		Num randomDecimal = null;
		if (min != null && max != null && min.isLessThan(max)) {
			Num range = max.minus(min);
			Num position = range.multipliedBy(PrecisionNum.valueOf(Math.random()));
			randomDecimal = min.plus(position);
		}
		return randomDecimal;
	}

	/**
	 * Generates a random bar.
	 * 
	 * @return a random bar
	 */
	private static Bar generateRandomBar() {
		final Num maxRange = PrecisionNum.valueOf("0.03"); // 3.0%
		Num openPrice = LAST_BAR_CLOSE_PRICE;
		Num minPrice = openPrice.minus(maxRange.multipliedBy(PrecisionNum.valueOf(Math.random())));
		Num maxPrice = openPrice.plus(maxRange.multipliedBy(PrecisionNum.valueOf(Math.random())));
		Num closePrice = randDecimal(minPrice, maxPrice);
		LAST_BAR_CLOSE_PRICE = closePrice;
		return new BaseBar(ZonedDateTime.now(), openPrice, maxPrice, minPrice, closePrice, PrecisionNum.valueOf(1),
				PrecisionNum.valueOf(1));
	}

	public static void main(String[] args) throws InterruptedException {

		System.out.println("********************** Initialization **********************");
		// Getting the time series
		TimeSeries series = initMovingTimeSeries(20);

		// Building the trading strategy
		Strategy strategy = buildStrategy(series);

		// Initializing the trading history
		TradingRecord tradingRecord = new BaseTradingRecord();
		System.out.println("************************************************************");

		/*
		 * We run the strategy for the 50 next bars.
		 */
		for (int i = 0; i < 50; i++) {

			// New bar
			Thread.sleep(30); // I know...
			Bar newBar = generateRandomBar();
			System.out.println("------------------------------------------------------\n" + "Bar " + i
					+ " added, close price = " + newBar.getClosePrice().doubleValue());
			series.addBar(newBar);

			int endIndex = series.getEndIndex();
			if (strategy.shouldEnter(endIndex, tradingRecord)) {
				// Our strategy should enter
				System.out.println("Strategy should ENTER on " + endIndex);
				boolean entered = tradingRecord.enter(endIndex, newBar.getClosePrice(), PrecisionNum.valueOf(10));
				if (entered) {
					Order entry = tradingRecord.getLastEntry();
					System.out.println("Entered on " + entry.getIndex() + " (price=" + entry.getPrice().doubleValue()
							+ ", amount=" + entry.getAmount().doubleValue() + ")");
				}
			} 
				if (strategy.shouldExit(endIndex, tradingRecord)) {
				// Our strategy should exit
				System.out.println("Strategy should EXIT on " + endIndex);
				boolean exited = tradingRecord.exit(endIndex, newBar.getClosePrice(), PrecisionNum.valueOf(10));
				if (exited) {
					Order exit = tradingRecord.getLastExit();
					System.out.println("Exited on " + exit.getIndex() + " (price=" + exit.getPrice().doubleValue()
							+ ", amount=" + exit.getAmount().doubleValue() + ")");
				}
			}
		}
		
		for (Trade trade : tradingRecord.getTrades()) {
			double buy = trade.getEntry().getPrice().doubleValue();
			double sell = trade.getExit().getPrice().doubleValue();
			System.out.println(trade + " | " + (sell - buy));
		}
	}
}