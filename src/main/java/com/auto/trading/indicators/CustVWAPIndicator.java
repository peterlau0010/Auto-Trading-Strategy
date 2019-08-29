package com.auto.trading.indicators;

/*******************************************************************************
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2014-2017 Marc de Verdelhan, 2017-2018 Ta4j Organization 
 *   & respective authors (see AUTHORS)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of
 *   this software and associated documentation files (the "Software"), to deal in
 *   the Software without restriction, including without limitation the rights to
 *   use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *   the Software, and to permit persons to whom the Software is furnished to do so,
 *   subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/


import org.ta4j.core.Indicator;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.indicators.helpers.TypicalPriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.volume.VWAPIndicator;
import org.ta4j.core.num.Num;

/**
 * The volume-weighted average price (VWAP) Indicator.
 * @see <a href="http://www.investopedia.com/articles/trading/11/trading-with-vwap-mvwap.asp">
 *     http://www.investopedia.com/articles/trading/11/trading-with-vwap-mvwap.asp</a>
 * @see <a href="http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:vwap_intraday">
 *     http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:vwap_intraday</a>
 * @see <a href="https://en.wikipedia.org/wiki/Volume-weighted_average_price">
 *     https://en.wikipedia.org/wiki/Volume-weighted_average_price</a>
 */
public class CustVWAPIndicator extends CachedIndicator<Num> {

    private final int barCount;
    
    private final Indicator<Num> typicalPrice;
    
    private final Indicator<Num> volume;

    private final Num ZERO;
    
    private final int timeFrame;
    
    /**
     * Constructor.
     * @param series the series
     * @param barCount the time frame
     */
    public CustVWAPIndicator(TimeSeries series, int barCount) {
        super(series);
        this.barCount = barCount;
        this.timeFrame = 0;
        typicalPrice = new TypicalPriceIndicator(series);
        volume = new VolumeIndicator(series);
        this.ZERO = numOf(0);
    }
    
    public CustVWAPIndicator(TimeSeries series,int barCount, int tf) {
        super(series);
        this.barCount = barCount;
        this.timeFrame=tf;
        typicalPrice = new TypicalPriceIndicator(series);
        volume = new VolumeIndicator(series);
        this.ZERO = numOf(0);
    }

    @Override
    protected Num calculate(int index) {
        if (index <= 0) {
            return typicalPrice.getValue(index);
        }
        int hour = super.getTimeSeries().getBar(index).getEndTime().minusHours(8).getHour();
        int min = super.getTimeSeries().getBar(index).getEndTime().getMinute();
//        System.out.println("Hour: " + hour);
//        System.out.println("Min: " + min);
        
        int barcount = (hour * 60 + min )/timeFrame + 1;
//        if(index ==62691) {
//        	System.out.println("###########: " + super.getTimeSeries().getBar(index).getEndTime());
//        	System.out.println("###########: " + hour);
//        	System.out.println("###########: " + min);
//        	System.out.println("###########: " + barcount);
//        }
        
        
//        int startIndex = Math.max(0, index - barcount);
//        Num cumulativeTPV = ZERO;
//        Num cumulativeVolume = ZERO;
//        for (int i = startIndex; i <= index; i++) {
//            Num currentVolume = volume.getValue(i);
//            cumulativeTPV = cumulativeTPV.plus(typicalPrice.getValue(i).multipliedBy(currentVolume));
//            cumulativeVolume = cumulativeVolume.plus(currentVolume);
//        }
        
        return new VWAPIndicator(super.getTimeSeries(),barcount).getValue(index);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " barCount: " + barCount;
    }
}
