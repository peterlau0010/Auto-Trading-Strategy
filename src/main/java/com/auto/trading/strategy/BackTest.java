package com.auto.trading.strategy;

import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.StochasticRSIIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.adx.MinusDIIndicator;
import org.ta4j.core.indicators.adx.PlusDIIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.MaxPriceIndicator;
import org.ta4j.core.indicators.helpers.MinPriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.volume.ChaikinMoneyFlowIndicator;
import org.ta4j.core.indicators.volume.OnBalanceVolumeIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.num.PrecisionNum;
import org.ta4j.core.trading.rules.BooleanIndicatorRule;
import org.ta4j.core.trading.rules.CrossedDownIndicatorRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.IsEqualRule;
import org.ta4j.core.trading.rules.IsFallingRule;
import org.ta4j.core.trading.rules.IsRisingRule;
import org.ta4j.core.trading.rules.OverIndicatorRule;
import org.ta4j.core.trading.rules.StopGainRule;
import org.ta4j.core.trading.rules.StopLossRule;
import org.ta4j.core.trading.rules.TrailingStopLossRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import com.auto.trading.indicators.CustTrailingStopLossRule;
import com.auto.trading.indicators.CustVWAPIndicator;

import org.ta4j.core.analysis.CashFlow;
import org.ta4j.core.analysis.criteria.*;

public class BackTest {

	static TimeSeries series;
	private static BufferedReader csvReader;
	private static RSIIndicator rsi;
	private static ClosePriceIndicator closePrice;
	private static Strategy strategy;
	private static TradingRecord tradingRecord;
	private static RSIIndicator rsiShort;
	private static RSIIndicator rsiLong;
	private static StochasticRSIIndicator stochasticRSI;
	private static SMAIndicator smaShort;
	private static SMAIndicator smaLong;
	private static ParabolicSarIndicator pSar;
	private static BollingerBandsLowerIndicator bbl;
	private static BollingerBandsUpperIndicator bbh;
	private static SMAIndicator stochasticOscillatorK;
	private static SMAIndicator stochasticOscillatorD;
	private static ChaikinMoneyFlowIndicator cmf;
	private static OnBalanceVolumeIndicator obv;
	private static ADXIndicator adx;
	private static PlusDIIndicator pdi;
	private static MinusDIIndicator mdi;
	private static CustVWAPIndicator vwap;
	
	static DecimalFormat df = new DecimalFormat("#.####");
	private static MACDIndicator macd;
	private static EMAIndicator signal;

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
				+ new LinearTransactionCostCriterion(1, 0.001).calculate(series, tradingRecord));
		// Buy-and-hold
		System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));
		// Total profit vs buy-and-hold
		System.out.println("Custom strategy profit vs buy-and-hold strategy profit: "
				+ new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));

		FileWriter csvWriter = new FileWriter("backtest_result.csv");
		csvWriter.append("IndexBuy,TimeBuy,PriceBuy,IndexSell,TimeSell,PriceSell,Duration,TradFee,Profit\n");

		double sumNetProfit = 0;
		double sumTradeFee = 0;
		double sumTProfit = 0;
		

		List<Trade> trades;

		trades = tradingRecord.getTrades();
		Collections.reverse(trades);
//		trades = tradingRecord.getTrades().subList(tradingRecord.getTrades().size() - 10,
//				tradingRecord.getTrades().size() - 1);

		for (Trade trade : trades) {

			// Buy Info
			csvWriter.append(trade.getEntry().getIndex() + ",");
			csvWriter.append(series.getBar(trade.getEntry().getIndex()).getDateName() + ",");
			csvWriter.append(trade.getEntry().getPrice().doubleValue() + ",");

//			csvWriter.append(df.format(bbl.getValue(trade.getEntry().getIndex()).doubleValue()) + ",");
//			csvWriter.append(df.format(stochasticRSI.getValue(trade.getEntry().getIndex()).doubleValue()) + ",");
//			csvWriter
//					.append(df.format(stochasticOscillatorK.getValue(trade.getEntry().getIndex()).doubleValue()) + ",");
//			csvWriter
//					.append(df.format(stochasticOscillatorD.getValue(trade.getEntry().getIndex()).doubleValue()) + ",");
//			csvWriter.append(df.format(pSar.getValue(trade.getEntry().getIndex()).doubleValue()) + ",");

			// Sell Info
			csvWriter.append(trade.getExit().getIndex() + ",");
			csvWriter.append(series.getBar(trade.getExit().getIndex()).getDateName() + ",");
			csvWriter.append(df.format(trade.getExit().getPrice().doubleValue()) + ",");

//			csvWriter.append(df.format(bbh.getValue(trade.getExit().getIndex()).doubleValue()) + ",");
//			csvWriter.append(df.format(stochasticRSI.getValue(trade.getExit().getIndex()).doubleValue()) + ",");
//			csvWriter.append(df.format(stochasticOscillatorK.getValue(trade.getExit().getIndex()).doubleValue()) + ",");
//			csvWriter.append(df.format(stochasticOscillatorD.getValue(trade.getExit().getIndex()).doubleValue()) + ",");
//			csvWriter.append(df.format(pSar.getValue(trade.getExit().getIndex()).doubleValue()) + ",");

			csvWriter.append(Duration.between(series.getBar(trade.getEntry().getIndex()).getEndTime(),
					series.getBar(trade.getExit().getIndex()).getEndTime()) + ",");

			// Trading Fee
			double profit = trade.getExit().getPrice().doubleValue() - trade.getEntry().getPrice().doubleValue();
			double tradeFee = (trade.getExit().getPrice().doubleValue() + trade.getEntry().getPrice().doubleValue())
					* 0.001;

			csvWriter.append(df.format(tradeFee) + ",");

			// Profit
			csvWriter.append((profit - tradeFee) + "");

			csvWriter.append("\n");

			sumTradeFee += tradeFee;
			sumTProfit += profit;
		}

		csvWriter.flush();
		csvWriter.close();

		sumNetProfit = sumTProfit - sumTradeFee;
		System.out.println("Sum of Profit: " + sumTProfit);
		System.out.println("Sum of Trade Fee: " + sumTradeFee);
		System.out.println("Net Profit: " + sumNetProfit);
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
		TimeSeriesManager manager = new TimeSeriesManager(series);
		tradingRecord = manager.run(strategy, series.getBeginIndex() + 50, series.getBarCount());
//		tradingRecord = manager.run(strategy, series.getBarCount(), series.getBarCount());
	}

	public static void buildIndicators() {
		closePrice = new ClosePriceIndicator(series);
		rsiShort = new RSIIndicator(closePrice, 6);
		rsiLong = new RSIIndicator(closePrice, 14);
		smaShort = new SMAIndicator(closePrice, 7);
		smaLong = new SMAIndicator(closePrice, 25);
		cmf = new ChaikinMoneyFlowIndicator(series, 20);
		obv = new OnBalanceVolumeIndicator(series);
		mdi = new MinusDIIndicator(series, 14);
		pdi = new PlusDIIndicator(series, 14);
		adx = new ADXIndicator(series, 14, 14);
		vwap = new CustVWAPIndicator(series, 65);
		macd = new MACDIndicator(closePrice,12,26);
		signal = new EMAIndicator(macd,9);
		
		stochasticRSI = new StochasticRSIIndicator(new RSIIndicator(closePrice, 9), 9);
		stochasticOscillatorK = new SMAIndicator(stochasticRSI, 3);
		stochasticOscillatorD = new SMAIndicator(stochasticOscillatorK, 3);

		StandardDeviationIndicator standardDeviation = new StandardDeviationIndicator(closePrice, 20);
		SMAIndicator sma = new SMAIndicator(closePrice, 20);
		BollingerBandsMiddleIndicator bbm = new BollingerBandsMiddleIndicator(sma);

		bbl = new BollingerBandsLowerIndicator(bbm, standardDeviation);
		bbh = new BollingerBandsUpperIndicator(bbm, standardDeviation);
		pSar = new ParabolicSarIndicator(series);
		vwap = new CustVWAPIndicator(series,30,15);
		
		
		int barIndex = 62992;
//		System.out.println(
//				series.getBar(barIndex).getDateName() + " K:" + k.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " MACD:" + macd.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " Signal:" + signal.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " OBV:" + obv.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " cmf:" + cmf.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " BBL:" + bbl.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " BBM:" + bbm.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " BBH:" + bbh.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " Close Price:" + closePrice.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " Rsi Short:" + rsiShort.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " Rsi Long:" + rsiLong.getValue(barIndex));
		System.out
				.println(series.getBar(barIndex).getDateName() + " Stochastic RSI:" + stochasticRSI.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " +DI:" + pdi.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " -DI:" + mdi.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " ADX:" + adx.getValue(barIndex));
		System.out.println(series.getBar(barIndex).getDateName() + " vwap:" + vwap.getValue(barIndex));
//		System.out.println(series.getBar(62691).getDateName() + " vwap:" + vwap.getValue(62691));

//		VWAPIndicator vwapo = new VWAPIndicator(series, 65);
//		for (int i = 0; i < 100; i++) {
//			VWAPIndicator vwapo = new VWAPIndicator(series, i);
//			if(df.format(vwapo.getValue(62691).doubleValue()).equals("26.4451")) {
//				System.out.println(i);
//				System.out.println(series.getBar(62691).getDateName() + " vwap:" + vwapo.getValue(62691));	
//			}
//			if(df.format(vwapo.getValue(62692).doubleValue()).equals("26.3357")) {
//				System.out.println(i);
//				System.out.println(series.getBar(62692).getDateName() + " vwap:" + vwapo.getValue(62692));	
//			}
//		}
		

//		System.out.println(series.getBar(62691).getDateName() + " vwap:" + vwap.getValue(62691));
//		System.out.println(series.getBar(62692).getDateName() + " vwap:" + vwap.getValue(62692));
			
			
			
//		for (int i = 0; i < series.getBarCount() - 1; i++) {
//
//			if (closePrice.getValue(i).doubleValue() < bbl.getValue(i).doubleValue()) {
//				System.out.println(series.getBar(i).getDateName() + " BBL:" + bbl.getValue(i));
//				System.out.println(series.getBar(i).getDateName() + " ClosePrice:" + closePrice.getValue(i));
//			}
//		}
	}

	public static void buildStrategies() {

		Rule buyingRule = new OverIndicatorRule(closePrice, 0)
				.and(new UnderIndicatorRule(closePrice, bbl))
//				.and(new UnderIndicatorRule(pSar,closePrice))
				.and(new UnderIndicatorRule(stochasticOscillatorK, 0.1d))
				.and(new UnderIndicatorRule(cmf, -0.2d))
//				.and(new UnderIndicatorRule(rsiLong, 60d))
//				.and(new OverIndicatorRule(adx, 25d))
//				.and(new OverIndicatorRule(pdi, mdi))
//				.and(new OverIndicatorRule(closePrice, vwap))
//				.and(new OverIndicatorRule(macd, signal))
//				.and(new OverIndicatorRule(macd, 0d))
				.and(new UnderIndicatorRule(rsiShort, 20d))

//				.and(new IsRisingRule(adx, 5))
//				.and(new UnderIndicatorRule(obv, 0d));
		;

//		Rule sellingRule = new OverIndicatorRule(closePrice, bbh).or(new StopLossRule(closePrice,2));
//		.and(new OverIndicatorRule(pSar, closePrice));
		Rule sellingRule = new StopGainRule(closePrice, 20)
				
//				.or(new CrossedDownIndicatorRule(closePrice, smaLong))
//				.or(new CrossedDownIndicatorRule(closePrice, vwap))
//				.or(new OverIndicatorRule(cmf, 0.4d))
//				.or(new UnderIndicatorRule(closePrice,pSar))
//				.or(new CrossedUpIndicatorRule(closePrice, bbh))
//				.and(new OverIndicatorRule(rsiLong,70d))
				.or(new CustTrailingStopLossRule(closePrice, PrecisionNum.valueOf(10)))
				.or(new StopLossRule(closePrice, PrecisionNum.valueOf(10)));

		strategy = new BaseStrategy(buyingRule, sellingRule);
	}

	public static void init() throws IOException {
		series = new BaseTimeSeries.SeriesBuilder().withName("historyCSV").build();
//		series.setMaximumBarCount(200);
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
