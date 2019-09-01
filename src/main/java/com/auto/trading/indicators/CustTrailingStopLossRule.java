package com.auto.trading.indicators;

import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.AbstractRule;

public class CustTrailingStopLossRule extends AbstractRule {
	/** The close price indicator */
	private final ClosePriceIndicator closePrice;
	/** the loss-distance as percentage */
	private final Num lossPercentage;
	/** the current price extremum */
	private Num currentExtremum = null;
	/** the current threshold */
	private Num threshold = null;
	/** the current trade */
	private Trade supervisedTrade;

	private Num lossPoint = null;

	/**
	 * Constructor.
	 * 
	 * @param closePrice     the close price indicator
	 * @param lossPercentage the loss percentage
	 */
	public CustTrailingStopLossRule(ClosePriceIndicator closePrice, Num lossPercentage) {
		this.closePrice = closePrice;
		this.lossPercentage = lossPercentage;
	}

	public CustTrailingStopLossRule(ClosePriceIndicator closePrice, Num lossPercentage, Num lossPoint) {
		this.closePrice = closePrice;
		this.lossPercentage = lossPercentage;
		this.lossPoint = lossPoint;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean satisfied = false;
		// No trading history or no trade opened, no loss
		if (tradingRecord != null) {
			Trade currentTrade = tradingRecord.getCurrentTrade();
			if (currentTrade.isOpened()) {
				if (!currentTrade.equals(supervisedTrade)) {
					supervisedTrade = currentTrade;
					currentExtremum = closePrice.getValue(index - 1);
					Num lossRatioThreshold;
					if (lossPoint != null) {
						lossRatioThreshold = currentExtremum.minus(lossPoint).dividedBy(currentExtremum);
//						System.out.println("currentPrice.numOf(100)" + currentPrice.numOf(100));
//						System.out.println("lossRatioThreshold" + lossRatioThreshold);
					} else {
						lossRatioThreshold = currentExtremum.numOf(100).minus(lossPercentage)
								.dividedBy(currentExtremum.numOf(100));
					}
					 
					threshold = currentExtremum.multipliedBy(lossRatioThreshold);
				}
				Num currentPrice = closePrice.getValue(index);
				if (currentTrade.getEntry().isBuy()) {
//					System.out.println("############1111111111");
					satisfied = isBuySatisfied(currentPrice);
				} else {
					System.out.println("############2222222222");
					satisfied = isSellSatisfied(currentPrice);
				}
			}
		}
		traceIsSatisfied(index, satisfied);
		return satisfied;
	}

	private boolean isBuySatisfied(Num currentPrice) {
		boolean satisfied = false;
		if (currentExtremum == null) {
			currentExtremum = currentPrice.numOf(Float.MIN_VALUE);
		}
		if (currentPrice.isGreaterThan(currentExtremum)) {
			currentExtremum = currentPrice;
			Num lossRatioThreshold;
			if (lossPoint != null) {
				lossRatioThreshold = currentPrice.minus(lossPoint).dividedBy(currentPrice);
//				System.out.println("currentPrice.numOf(100)" + currentPrice.numOf(100));
//				System.out.println("lossRatioThreshold" + lossRatioThreshold);
			} else {
//				System.out.println("############");
				lossRatioThreshold = currentPrice.numOf(100).minus(lossPercentage).dividedBy(currentPrice.numOf(100));

			}
			threshold = currentExtremum.multipliedBy(lossRatioThreshold);
		}
		if (threshold != null) {
			satisfied = currentPrice.isLessThanOrEqual(threshold);
		}
		return satisfied;
	}

	private boolean isSellSatisfied(Num currentPrice) {
		boolean satisfied = false;
		if (currentExtremum == null) {
			currentExtremum = currentPrice.numOf(Float.MAX_VALUE);
		}
		if (currentPrice.isLessThan(currentExtremum)) {
			Num lossRatioThreshold;
			currentExtremum = currentPrice;
			lossRatioThreshold = currentPrice.numOf(100).plus(lossPercentage).dividedBy(currentPrice.numOf(100));

			threshold = currentExtremum.multipliedBy(lossRatioThreshold);
		}
		if (threshold != null) {
			satisfied = currentPrice.isGreaterThanOrEqual(threshold);
		}
		return satisfied;
	}
}
