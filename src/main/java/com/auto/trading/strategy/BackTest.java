package com.auto.trading.strategy;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.BaseTimeSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Indicator;
import org.ta4j.core.Order.OrderType;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.TimeSeriesManager;
import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.TrailingStopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;
import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.*;

public class BackTest {

	static TimeSeries series;
	private static BufferedReader csvReader;
	private static RSIIndicator rsi;
	private static ClosePriceIndicator closePrice;
	private static Strategy strategy;
	private static TradingRecord tradingRecord;

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		init();
		buildIndicators();
		buildStrategies();
		runStrategies();
		analysisStrategies();
//		showCashFlowChart();
	}

	public static void analysisStrategies() throws IOException {
		TotalProfitCriterion totalProfit = new TotalProfitCriterion();
		System.out.println("Total profit: " + totalProfit.calculate(series, tradingRecord));
		// Number of bars
		System.out.println("Number of bars: " + new NumberOfBarsCriterion().calculate(series, tradingRecord));
		// Average profit (per bar)
		System.out
				.println("Average profit (per bar): " + new AverageProfitCriterion().calculate(series, tradingRecord));
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
				+ new LinearTransactionCostCriterion(1000, 0.001).calculate(series, tradingRecord));
		// Buy-and-hold
		System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));
		// Total profit vs buy-and-hold
		System.out.println("Custom strategy profit vs buy-and-hold strategy profit: "
				+ new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));

		FileWriter csvWriter = new FileWriter("backtest_result.csv");
		csvWriter.append("TimeBuy,PriceBuy,TimeSell,PriceSell,Profile\n");

		for (Trade trade : tradingRecord.getTrades()) {
			csvWriter.append(series.getBar(trade.getEntry().getIndex()).getDateName() + ",");
			csvWriter.append(trade.getEntry().getPrice().doubleValue() + ",");

			csvWriter.append(series.getBar(trade.getExit().getIndex()).getDateName() + ",");
			csvWriter.append(trade.getExit().getPrice().doubleValue() + ",");

			csvWriter.append(
					(trade.getExit().getPrice().doubleValue() - trade.getEntry().getPrice().doubleValue()) + "");

			csvWriter.append("\n");

		}

		csvWriter.flush();
		csvWriter.close();

		System.out.println(series.getMaximumBarCount());
		System.out.println(series.getBarCount());
	}

	public static void showCashFlowChart() {
		CashFlow cashFlow = new CashFlow(series, tradingRecord);

		TimeSeriesCollection datasetAxis1 = new TimeSeriesCollection();
		datasetAxis1.addSeries(buildChartTimeSeries(series, new ClosePriceIndicator(series), "Bitstamp Bitcoin (BTC)"));
		TimeSeriesCollection datasetAxis2 = new TimeSeriesCollection();
		datasetAxis2.addSeries(buildChartTimeSeries(series, cashFlow, "Cash Flow"));

		JFreeChart chart = ChartFactory.createTimeSeriesChart("Binance BTC", // title
				"Date", // x-axis label
				"Price", // y-axis label
				datasetAxis1, // data
				true, // create legend?
				true, // generate tooltips?
				false // generate URLs?
		);
		XYPlot plot = (XYPlot) chart.getPlot();
		DateAxis axis = (DateAxis) plot.getDomainAxis();
		axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));

		/*
		 * Adding the cash flow axis (on the right)
		 */
		addCashFlowAxis(plot, datasetAxis2);

		displayChart(chart);
	}

	private static org.jfree.data.time.TimeSeries buildChartTimeSeries(TimeSeries barseries, Indicator<Num> indicator,
			String name) {
		org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
		for (int i = 0; i < barseries.getBarCount(); i++) {
			Bar bar = barseries.getBar(i);
			chartTimeSeries.add(new Minute(new Date(bar.getEndTime().toEpochSecond() * 1000)),
					indicator.getValue(i).doubleValue());
		}
		return chartTimeSeries;
	}

	private static void addCashFlowAxis(XYPlot plot, TimeSeriesCollection dataset) {
		final NumberAxis cashAxis = new NumberAxis("Cash Flow Ratio");
		cashAxis.setAutoRangeIncludesZero(false);
		plot.setRangeAxis(1, cashAxis);
		plot.setDataset(1, dataset);
		plot.mapDatasetToRangeAxis(1, 1);
		final StandardXYItemRenderer cashFlowRenderer = new StandardXYItemRenderer();
		cashFlowRenderer.setSeriesPaint(0, Color.blue);
		plot.setRenderer(1, cashFlowRenderer);
	}

	private static void displayChart(JFreeChart chart) {
		// Chart panel
		ChartPanel panel = new ChartPanel(chart);
		panel.setFillZoomRectangle(true);
		panel.setMouseWheelEnabled(true);
		panel.setPreferredSize(new Dimension(1024, 400));
		// Application frame
		ApplicationFrame frame = new ApplicationFrame("Ta4j example - Cash flow to chart");
		frame.setContentPane(panel);
		frame.pack();
		RefineryUtilities.centerFrameOnScreen(frame);
		frame.setVisible(true);
	}

	public static void runStrategies() {
		TimeSeriesManager manager = new TimeSeriesManager(series) {
			public TradingRecord run(Strategy strategy, OrderType orderType, Num amount, int startIndex,
					int finishIndex) {

				int runBeginIndex = Math.max(startIndex, series.getBeginIndex());
				int runEndIndex = Math.min(finishIndex, series.getEndIndex());

//		        log.trace("Running strategy (indexes: {} -> {}): {} (starting with {})", runBeginIndex, runEndIndex, strategy, orderType);
				TradingRecord tradingRecord = new BaseTradingRecord(orderType);
				for (int i = runBeginIndex; i <= runEndIndex; i++) {
					// For each bar between both indexes...
					if (strategy.shouldOperate(i, tradingRecord)) {
						tradingRecord.operate(i, series.getBar(i).getClosePrice(), amount);
					}
				}

				if (!tradingRecord.isClosed()) {
					// If the last trade is still opened, we search out of the run end index.
					// May works if the end index for this run was inferior to the actual number of
					// bars
					int seriesMaxSize = Math.max(series.getEndIndex() + 1, series.getBarData().size());
					for (int i = runEndIndex + 1; i < seriesMaxSize; i++) {
						// For each bar after the end index of this run...
						// --> Trying to close the last trade
						if (strategy.shouldOperate(i, tradingRecord)) {
							tradingRecord.operate(i, series.getBar(i).getClosePrice(), amount);
							break;
						}
					}
				}
				return tradingRecord;
			}
		};
//		tradingRecord = manager.run(strategy,series.getBeginIndex()+1,series.getEndIndex());
		tradingRecord = manager.run(strategy, series.getBeginIndex() + 6, series.getBarCount());
	}

	public static void buildIndicators() {
		closePrice = new ClosePriceIndicator(series);
		rsi = new RSIIndicator(closePrice, 6);
		for (int i = 0; i < series.getBarCount(); i++)
			if (rsi.getValue(i).doubleValue() < 30 && rsi.getValue(i).doubleValue() > 10)
				System.out.println(series.getBar(i).getDateName() + "  " + rsi.getValue(i));
	}

	public static void buildStrategies() {
		Rule buyingRule = new UnderIndicatorRule(rsi, 30d);
		Rule sellingRule = new TrailingStopLossRule(closePrice, PrecisionNum.valueOf(10));

		strategy = new BaseStrategy(buyingRule, sellingRule) {
			public boolean shouldOperate(int index, TradingRecord tradingRecord) {

				Trade trade = tradingRecord.getCurrentTrade();
				if (trade.isNew()) {
					return shouldEnter(index, tradingRecord);
				}
				if (trade.isOpened()) {
					System.out.print("############");

					return shouldExit(index, tradingRecord);
				}
				return false;
			};
		};
	}

	public static void init() throws IOException {
		series = new BaseTimeSeries.SeriesBuilder().withName("historyCSV").build();
		csvReader = new BufferedReader(new FileReader("history.csv"));
		String row;
		csvReader.readLine();
		while ((row = csvReader.readLine()) != null) {
			String[] data = row.split(",");

			ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(data[0])),
					ZoneId.systemDefault());
			series.addBar(zdt, Double.parseDouble(data[1]), Double.parseDouble(data[2]), Double.parseDouble(data[3]),
					Double.parseDouble(data[4]), Double.parseDouble(data[5]));
//			System.out.println(count++);
		}
	}
}
