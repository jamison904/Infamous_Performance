package com.brewcrewfoo.performance.util;

import java.io.File;

/**
 * Created by h0rn3t on 05.01.2014.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
public class VibratorClass {
    private int max,min;
    public int get_min(){
        return min;
    }
    public int get_max(){
        return max;
    }
    public String get_path(){
        if (new File("/sys/class/vibetonz/immDuty/pwmvalue_intensity").exists()) {
            this.min=0;
            this.max=127;
            return "/sys/class/vibetonz/immDuty/pwmvalue_intensity";
        }
        else if (new File("/sys/vibrator/pwmvalue").exists()) {
            this.min=0;
            this.max=127;
            return "/sys/vibrator/pwmvalue";
        }
        else if (new File("/sys/class/misc/vibratorcontrol/vibrator_strength").exists()) {
            this.min=1000;
            this.max=1600;
            return "/sys/class/misc/vibratorcontrol/vibrator_strength";
        }
        else if (new File("/sys/vibe/pwmduty").exists()) {
            this.min=1000;
            this.max=1450;
            return "/sys/vibe/pwmduty";
        }
        else if (new File("/sys/class/timed_output/vibrator/amp").exists()) {
            this.min=0;
            this.max=100;
            return "/sys/class/timed_output/vibrator/amp";
        }
        else if (new File("/sys/class/misc/pwm_duty/pwm_duty").exists()) {
            this.min=0;
            this.max=100;
            return "/sys/class/misc/pwm_duty/pwm_duty";
        }
        else if (new File("/sys/devices/virtual/timed_output/vibrator/voltage_level").exists()) {
            this.min=1200;
            this.max=3100;
            return "/sys/devices/virtual/timed_output/vibrator/voltage_level";
        }
        else{
            return null;
        }
    }

}
