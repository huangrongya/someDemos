package com.etekcity.vbmp.timing.modules.schedule.bean;

import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Data
public class DeviceScheduleView extends DeviceSchedule{

    private String deviceName;

    private String onceTomorrow;

    private String nextDay;


}