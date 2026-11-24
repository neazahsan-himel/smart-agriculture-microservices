package com.smartagriculture.aiadvisorservice.service;

import com.smartagriculture.aiadvisorservice.dto.CommandCenterDto;

public interface CommandCenterService {

    /** The farmer's next {@code days} days (1-14): planned work per day, overdue work, forecast, recent events. */
    CommandCenterDto.Response getWeek(String farmerId, int days);
}
