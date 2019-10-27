/*-
 * #%L
 * benchmarks
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.robobuf.benchmarks.comparison;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;
import protos.benchmarks.real_logic.robo.Examples;
import protos.benchmarks.real_logic.robo.Examples.Car;
import protos.benchmarks.real_logic.robo.Fix;
import us.hebi.robobuf.ProtoSink;
import us.hebi.robobuf.ProtoSource;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * === Robobuf (with Unsafe)
 * Benchmark                                     Mode  Cnt     Score     Error   Units
 * SbeThroughputBenchmarkRobo.testCarDecode     thrpt   10  2041.544 ±  24.943  ops/ms
 * SbeThroughputBenchmarkRobo.testCarEncode     thrpt   10  2728.320 ± 163.706  ops/ms
 * SbeThroughputBenchmarkRobo.testMarketDecode  thrpt   10  5976.679 ± 196.894  ops/ms
 * SbeThroughputBenchmarkRobo.testMarketEncode  thrpt   10  6654.291 ±  99.163  ops/ms
 *
 * === change .proto definition to use fixed width types for 7+ bit values ===
 * Benchmark                                     Mode  Cnt     Score     Error   Units
 * SbeThroughputBenchmarkRobo.testMarketDecode  thrpt   10  6583.222 ± 206.918  ops/ms
 * SbeThroughputBenchmarkRobo.testMarketEncode  thrpt   10  8245.170 ±  40.951  ops/ms
 *
 * @author Florian Enner
 * @since 16 Oct 2019
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SbeThroughputBenchmarkRobo {

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + SbeThroughputBenchmarkRobo.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();
    }

    final Fix.MarketDataIncrementalRefreshTrades marketData = Fix.MarketDataIncrementalRefreshTrades.newInstance();
    final byte[] marketDecodeBuffer = buildMarketData(marketData).toByteArray();
    final Car car = Car.newInstance();
    final byte[] carDecodeBuffer = buildCarData(car).toByteArray();

    final byte[] encodeBuffer = new byte[Math.max(marketDecodeBuffer.length, carDecodeBuffer.length)];
    final ProtoSource source = ProtoSource.newInstance();
    final ProtoSink sink = ProtoSink.newInstance();

    @Benchmark
    public int testMarketEncode() throws IOException {
        sink.wrap(encodeBuffer);
        buildMarketData(marketData).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public Object testMarketDecode() throws IOException {
        source.wrap(marketDecodeBuffer);
        return marketData.clearQuick().mergeFrom(source);
    }

    @Benchmark
    public int testCarEncode() throws IOException {
        sink.wrap(encodeBuffer);
        buildCarData(car).writeTo(sink);
        return sink.position();
    }

    @Benchmark
    public Object testCarDecode() throws IOException {
        source.wrap(carDecodeBuffer);
        return car.clearQuick().mergeFrom(source);
    }

    static Fix.MarketDataIncrementalRefreshTrades buildMarketData(Fix.MarketDataIncrementalRefreshTrades marketData) {
        marketData.clearQuick()
                .setTransactTime(1234L)
                .setEventTimeDelta(987)
                .setMatchEventIndicator(Fix.MarketDataIncrementalRefreshTrades.MatchEventIndicator.END_EVENT_VALUE);

        for (int i = 0; i < 2; i++) {

            Fix.MdIncGrp group = marketData.getMutableMdIncGroup().next()
                    .setTradeId(1234L)
                    .setSecurityId(56789L)
                    .setNumberOfOrders(1)
                    .setRepSeq(1)
                    .setMdUpdateAction(Fix.MdIncGrp.MdUpdateAction.NEW_VALUE)
                    .setAggressorSide(Fix.MdIncGrp.Side.BUY_VALUE)
                    .setMdEntryType(Fix.MdIncGrp.MdEntryType.BID_VALUE);
            group.getMutableMdEntryPx().setMantissa(50);
            group.getMutableMdEntrySize().setMantissa(10);

        }

        return marketData;
    }

    static Car buildCarData(Car car) {
        car.clearQuick()
                .setSerialNumber(12345)
                .setModelYear(2005)
                .setCode(Car.Model.A_VALUE)
                .setAvailable(true);

        car.getMutableEngine()
                .setCapacity(4200)
                .setNumCylinders(8)
                .setManufacturerCode(ENG_MAN_CODE);

        car
                .setVehicleCode(VEHICLE_CODE)
                .setMake(MAKE)
                .setModel(MODEL);

        for (int i = 0; i < 5; i++) {
            car.addSomeNumbers(i);
        }

        car.getMutableOptionalExtras()
                .add(Car.Extras.SPORTS_PACK_VALUE)
                .add(Car.Extras.SUN_ROOF_VALUE);

        car.getMutableFuelFigures().next().setSpeed(30).setMpg(35.9F);
        car.getMutableFuelFigures().next().setSpeed(30).setMpg(49.0F);
        car.getMutableFuelFigures().next().setSpeed(30).setMpg(40.0F);

        Examples.PerformanceFigures perfFigures = car.getMutablePerformance().next().setOctaneRating(95);
        perfFigures.getMutableAcceleration().next().setMph(30).setSeconds(4.0f);
        perfFigures.getMutableAcceleration().next().setMph(60).setSeconds(7.5f);
        perfFigures.getMutableAcceleration().next().setMph(100).setSeconds(12.2f);

        perfFigures = car.getMutablePerformance().next().setOctaneRating(99);
        perfFigures.getMutableAcceleration().next().setMph(30).setSeconds(3.8f);
        perfFigures.getMutableAcceleration().next().setMph(60).setSeconds(7.1f);
        perfFigures.getMutableAcceleration().next().setMph(100).setSeconds(11.8f);

        return car;
    }

    static final String VEHICLE_CODE = "abcdef";
    static final String ENG_MAN_CODE = "abc";
    static final String MAKE = "AUDI";
    static final String MODEL = "R8";

}