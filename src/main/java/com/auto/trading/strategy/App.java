package com.auto.trading.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;

/**
 * Hello world!
 *
 */
public class App {
	BinanceApiWebSocketClient client;

	public static final int PERIODS_AVERAGE = 5;
//	public static final int TOTAL_PERIODS = 3;
	List<Double> closeList = new ArrayList<Double>();
	double[] closePrice ;
	double[] out;
	public static void main(String[] args) {
		new App();
	}

	public void init() {
		client = BinanceApiClientFactory.newInstance().newWebSocketClient();
	}

	public App() {
		init();
		client.onCandlestickEvent("ethbtc", CandlestickInterval.ONE_MINUTE, response -> {
			System.out.println(response.getClose());
			closeList.add(Double.parseDouble(response.getClose()));
			if(closeList.size()>=5) {
				
				
				closePrice = ArrayUtils.toPrimitive(closeList.stream().toArray(Double[]::new));
				out = new double [closeList.size()];
				getSMA();
				
				
			}
			
			
		});

	}
	
	public void getSMA() {
//		double[] closePrice = new double[TOTAL_PERIODS];
//		double[] out = new double[TOTAL_PERIODS];
		MInteger begin = new MInteger();
		MInteger length = new MInteger();

//		for (int i = 0; i < closePrice.length; i++) {
//			closePrice[i] = (double) i;
//		}

		
		
		Core c = new Core();
		RetCode retCode = c.sma(0, closeList.size() - 1, closePrice, PERIODS_AVERAGE, begin, length,out);

		if (retCode == RetCode.Success) {
			System.out.println("Output Start Period: " + begin.value);
			System.out.println("Output End Period: " + (begin.value + length.value - 1));

//			for (int i = begin.value; i < begin.value + length.value; i++) {
				StringBuilder line = new StringBuilder();
//				line.append("Period #");
//				line.append(i);
//				line.append(" close=");
//				line.append(closePrice[i]);
				line.append(" mov_avg=");
//				System.out.println(i - begin.value);
//				System.out.println(length.value - 1);
				line.append(out[length.value - 1]);
				System.out.println(line.toString());
//			}
		} else {
			System.out.println("Error");
		}
	}
}
