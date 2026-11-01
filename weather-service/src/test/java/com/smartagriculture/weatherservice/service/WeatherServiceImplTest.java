package com.smartagriculture.weatherservice.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherServiceImplTest {

    @Test
    void roundsCoordinatesToTwoDecimalsForTheForecastCacheKey() {
        assertThat(WeatherServiceImpl.roundCoordinate(23.8103)).isEqualTo(23.81);
        assertThat(WeatherServiceImpl.roundCoordinate(90.4125)).isEqualTo(90.41);
        assertThat(WeatherServiceImpl.roundCoordinate(90.4150)).isEqualTo(90.42);
        assertThat(WeatherServiceImpl.roundCoordinate(-1.2921)).isEqualTo(-1.29);
    }
}
