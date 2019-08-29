package com.auto.trading.indicators;

import org.ta4j.core.Trade;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.AbstractRule;

public class CustTrailingStopLossRule extends AbstractRule{
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

	/**
     * Constructor.
     * @param closePrice the close price indicator
	 * @param lossPercentage the loss percentage
	 */
	public CustTrailingStopLossRule(ClosePriceIndicator closePrice, Num lossPercentage) {
		this.closePrice = closePrice;
		this.lossPercentage = lossPercentage;
	}
	
	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean satisfied = false;
        // No trading history or no trade opened, no loss
        if (tradingRecord != null) {
            Trade currentTrade = tradingRecord.getCurrentTrade();
            if ( currentTrade.isOpened() ) {
            	if ( ! currentTrade.equals(supervisedTrade) ) {
            		supervisedTrade = currentTrade;
					currentExtremum = closePrice.getValue(index - 1);
					Num lossRatioThreshold = currentExtremum.numOf(100).minus(lossPercentage)
							.dividedBy(currentExtremum.numOf(100));
					threshold = currentExtremum.multipliedBy(lossRatioThreshold);
            	}
            	Num currentPrice = closePrice.getValue(index);
                if ( currentTrade.getEntry().isBuy() ) {
                	satisfied = isBuySatisfied(currentPrice);
                } else {
                	satisfied = isSellSatisfied(currentPrice);
                }
            }
        }
        traceIsSatisfied(index, satisfied);
        return satisfied;
	}

	private boolean isBuySatisfied(Num currentPrice) {
		boolean satisfied = false;
		if ( currentExtremum == null ) {
			currentExtremum = currentPrice.numOf(Float.MIN_VALUE);
		}
		if ( currentPrice.isGreaterThan(currentExtremum) ) {
			currentExtremum = currentPrice;
			Num lossRatioThreshold = currentPrice.numOf(100).minus(lossPercentage).dividedBy(currentPrice.numOf(100));
			threshold = currentExtremum.multipliedBy(lossRatioThreshold);
		}
		if ( threshold != null ) {
			satisfied = currentPrice.isLessThanOrEqual(threshold);
		}
		return satisfied;
	}

	private boolean isSellSatisfied(Num currentPrice) {
		boolean satisfied = false;
		if ( currentExtremum == null ) {
			currentExtremum = currentPrice.numOf(Float.MAX_VALUE);
		}
		if ( currentPrice.isLessThan(currentExtremum) ) {
			currentExtremum = currentPrice;
			Num lossRatioThreshold = currentPrice.numOf(100).plus(lossPercentage).dividedBy(currentPrice.numOf(100));
		    threshold = currentExtremum.multipliedBy(lossRatioThreshold);
		}
		if ( threshold != null ) {
			satisfied = currentPrice.isGreaterThanOrEqual(threshold);
		}
		return satisfied;
	}
}
