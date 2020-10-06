/*
 * Copyright (c) 2020 Vyacheslav Pisarevskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package miio.philips;

import de.sg_o.app.miio.base.CommandExecutionException;
import de.sg_o.app.miio.base.Device;
import de.sg_o.app.miio.base.Token;
import org.json.JSONArray;

import java.net.InetAddress;
import java.util.Map;

/**
 * Class for Xiaomi Philips smart lamp device
 */
@SuppressWarnings("WeakerAccess")
public class EyecareLamp extends Device {

    /**
     * @param ip      The IP address of the light to connect to. If the address is null the first light that was found will be chosen
     * @param token   The token for that device. If the token is null the token will be extracted from unprovisioned devices
     * @param timeout The timeout for the communication
     * @param retries The number of retries after a failed communication
     */
    public EyecareLamp(InetAddress ip, Token token, int timeout, int retries) {
        super(ip, token, new String[]{"philips.light.sread1"}, timeout, retries);
    }

    /**
     * Get several property values at once from the device
     *
     * @param props The properties to get
     * @return The property names and values
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public Map<Prop.Names, String> getProps(Prop.Names[] props) throws CommandExecutionException {
        Prop prop = new Prop(props);
        return prop.parseResponse(sendToArray("get_prop", prop.getRequestArray()));
    }

    /**
     * Get a single property value from the device
     *
     * @param prop The property to get
     * @return The value of the specified property name
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public String getSingleProp(Prop.Names prop) throws CommandExecutionException {
        Map<Prop.Names, String> value = getProps(new Prop.Names[]{prop});
        String valueString = value.get(prop);
        if (valueString == null) throw new CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE);
        return valueString;
    }

    /**
     * Get a single property value and try to convert it to an int
     *
     * @param prop The property to get
     * @return The value of the specified property name
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public int getIntProp(Prop.Names prop) throws CommandExecutionException {
        String value = getSingleProp(prop);
        if (value.equals("")) throw new CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE);
        }
    }

    /**
     * @return Thoggle the devices power
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean togglePower() throws CommandExecutionException {
        return sendOk("toggle", new JSONArray());
    }

    /**
     * @param on True: turn the device on; False: turn the device off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setPower(boolean on) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(on ? "on" : "off");
        return sendOk("set_power", col);
    }

    /**
     * @return The lamp power state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean isOn() throws CommandExecutionException {
        return getSingleProp(Prop.Names.POWER).equals("on");
    }

    /**
     * @param on True: turn the ambient light on; False: turn the ambient light off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setAmbientLightPower(boolean on) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(on ? "on" : "off");
        return sendOk("enable_amb", col);
    }

    /**
     * @return The ambient light power state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean isAmbientLightOn() throws CommandExecutionException {
        return getSingleProp(Prop.Names.AMBIENT_LIGHT_POWER).equals("on");
    }

    /**
     * @param brightness The brightness to change to. 1-100
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setBrightness(int brightness) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(brightness);
        return sendOk("set_bright", col);
    }

    /**
     * @return The brightness the device is currently set to
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public int getBrightness() throws CommandExecutionException {
        return getIntProp(Prop.Names.BRIGHTNESS);
    }

    /**
     * @param brightness The brightness to change to. 1-100
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setAmbientLightBrightness(int brightness) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(brightness);
        return sendOk("set_amb_bright", col);
    }

    /**
     * @return The brightness the ambient light is currently set to
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public int getAmbientLightBrightness() throws CommandExecutionException {
        return getIntProp(Prop.Names.AMBIENT_LIGHT_BRIGHTNESS);
    }

    /**
     * @param on True: turn the eyecare mode on; False: turn the eyecare mode off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setEyecare(boolean on) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(on ? "on" : "off");
        return sendOk("notify_on", col);
    }

    /**
     * @return The eyecare mode state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean isEyecareOn() throws CommandExecutionException {
        return getSingleProp(Prop.Names.EYECARE_MODE).equals("on");
    }

    /**
     * Turn on eyecare scene for the activity: study, reading, phone, default
     *
     * @param mode Set eyecare scene number. 1-4
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setUserScene(int mode) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(mode);
        return sendOk("set_user_scene", col);
    }

    /**
     * @return The eyecare scene number
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public int getUserScene() throws CommandExecutionException {
        return getIntProp(Prop.Names.SCENE_MODE);
    }

    /**
     * Turn on lamp blinking every 40 minutes
     *
     * @param on True: turn the eye fatigue reminder on; False: turn the reminder off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setEyeStrainReminder(boolean on) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(on ? "on" : "off");
        return sendOk("set_notifyuser", col);
    }

    /**
     * @return The eyecare mode state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean isEyeStrainReminderOn() throws CommandExecutionException {
        return getSingleProp(Prop.Names.EYE_FATIGUE_REMINDER).equals("on");
    }

    /**
     * When the lamp is turned on in the dark, it only turns on ambient light first
     *
     * @param on True: turn the night mode on; False: turn the night mode off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean setNightMode(boolean on) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(on ? "on" : "off");
        return sendOk("enable_bl", col);
    }

    /**
     * @return The night mode state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    public boolean isNightModeOn() throws CommandExecutionException {
        return getSingleProp(Prop.Names.NIGHT_LIGHT).equals("on");
    }

    /**
     * Power the device off after some time
     *
     * @param minutes The time until the device is turned off in minutes. 0-60. 0 for cancel delay
     * @return True if the command was received successfully.\
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.\
     */
    public boolean setTimeUntilPowerOff(int minutes) throws CommandExecutionException {
        JSONArray col = new JSONArray();
        col.put(minutes);
        return sendOk("delay_off", col);
    }

    /**
     * @return The time until the device is turned off in minutes
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    public int getTimeUntilPowerOff() throws CommandExecutionException {
        return getIntProp(Prop.Names.SLEEP_TIME_LEFT);
    }

    /**
     * @return Current all lamp props state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    public Map<Prop.Names, String> getStatus() throws CommandExecutionException {
        return getProps(Prop.Names.values());
    }
}
