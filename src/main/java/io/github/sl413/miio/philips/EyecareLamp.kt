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
package io.github.sl413.miio.philips

import de.sg_o.app.miio.base.CommandExecutionException
import de.sg_o.app.miio.base.Token
import io.github.sl413.miio.Device
import org.json.JSONArray
import java.net.InetAddress

/**
 * Class for Xiaomi Philips smart lamp device
 */
class EyecareLamp
/**
 * @param ip      The IP address of the light to connect to. If the address is null the first light that was found will be chosen
 * @param token   The token for that device. If the token is null the token will be extracted from unprovisioned devices
 * @param timeout The timeout for the communication
 * @param retries The number of retries after a failed communication
 */
(ip: InetAddress?, token: Token?, timeout: Int, retries: Int) : Device(ip, token, arrayOf("philips.light.sread1"), timeout, retries) {
    /**
     * Get several property values at once from the device
     *
     * @param props The properties to get
     * @return The property names and values
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun getProps(props: Array<Prop.Names>?): Map<Prop.Names, String> {
        val prop = Prop(props)
        return prop.parseResponse(sendToArray("get_prop", prop.requestArray))
    }

    /**
     * Get a single property value from the device
     *
     * @param prop The property to get
     * @return The value of the specified property name
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun getSingleProp(prop: Prop.Names): String {
        val value = getProps(arrayOf(prop))
        return value[prop]
                ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
    }

    /**
     * Get a single property value and try to convert it to an int
     *
     * @param prop The property to get
     * @return The value of the specified property name
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun getIntProp(prop: Prop.Names): Int {
        val value = getSingleProp(prop)
        if (value == "") throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        return try {
            value.toInt()
        } catch (e: Exception) {
            throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        }
    }

    /**
     * @return Thoggle the devices power
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun togglePower(): Boolean {
        return sendOk("toggle", JSONArray())
    }

    /**
     * @param on True: turn the device on; False: turn the device off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setPower(on: Boolean): Boolean {
        val col = JSONArray()
        col.put(if (on) "on" else "off")
        return sendOk("set_power", col)
    }

    /**
     * @return The lamp power state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val isOn: Boolean
        get() = getSingleProp(Prop.Names.POWER) == "on"

    /**
     * @param on True: turn the ambient light on; False: turn the ambient light off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setAmbientLightPower(on: Boolean): Boolean {
        val col = JSONArray()
        col.put(if (on) "on" else "off")
        return sendOk("enable_amb", col)
    }

    /**
     * @return The ambient light power state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val isAmbientLightOn: Boolean
        get() = getSingleProp(Prop.Names.AMBIENT_LIGHT_POWER) == "on"

    /**
     * @param brightness The brightness to change to. 1-100
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setBrightness(brightness: Int): Boolean {
        val col = JSONArray()
        col.put(brightness)
        return sendOk("set_bright", col)
    }

    /**
     * @return The brightness the device is currently set to
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val brightness: Int
        get() = getIntProp(Prop.Names.BRIGHTNESS)

    /**
     * @param brightness The brightness to change to. 1-100
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setAmbientLightBrightness(brightness: Int): Boolean {
        val col = JSONArray()
        col.put(brightness)
        return sendOk("set_amb_bright", col)
    }

    /**
     * @return The brightness the ambient light is currently set to
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val ambientLightBrightness: Int
        get() = getIntProp(Prop.Names.AMBIENT_LIGHT_BRIGHTNESS)

    /**
     * @param on True: turn the eyecare mode on; False: turn the eyecare mode off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setEyecare(on: Boolean): Boolean {
        val col = JSONArray()
        col.put(if (on) "on" else "off")
        return sendOk("notify_on", col)
    }

    /**
     * @return The eyecare mode state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val isEyecareOn: Boolean
        get() = getSingleProp(Prop.Names.EYECARE_MODE) == "on"

    /**
     * Turn on eyecare scene for the activity: study, reading, phone, default
     *
     * @param mode Set eyecare scene number. 1-4
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setUserScene(mode: Int): Boolean {
        val col = JSONArray()
        col.put(mode)
        return sendOk("set_user_scene", col)
    }

    /**
     * @return The eyecare scene number
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val userScene: Int
        get() = getIntProp(Prop.Names.SCENE_MODE)

    /**
     * Turn on lamp blinking every 40 minutes
     *
     * @param on True: turn the eye fatigue reminder on; False: turn the reminder off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setEyeStrainReminder(on: Boolean): Boolean {
        val col = JSONArray()
        col.put(if (on) "on" else "off")
        return sendOk("set_notifyuser", col)
    }

    /**
     * @return The eyecare mode state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val isEyeStrainReminderOn: Boolean
        get() = getSingleProp(Prop.Names.EYE_FATIGUE_REMINDER) == "on"

    /**
     * When the lamp is turned on in the dark, it only turns on ambient light first
     *
     * @param on True: turn the night mode on; False: turn the night mode off
     * @return True if the command was received successfully
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @Throws(CommandExecutionException::class)
    fun setNightMode(on: Boolean): Boolean {
        val col = JSONArray()
        col.put(if (on) "on" else "off")
        return sendOk("enable_bl", col)
    }

    /**
     * @return The night mode state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid
     */
    @get:Throws(CommandExecutionException::class)
    val isNightModeOn: Boolean
        get() = getSingleProp(Prop.Names.NIGHT_LIGHT) == "on"

    /**
     * Power the device off after some time
     *
     * @param minutes The time until the device is turned off in minutes. 0-60. 0 for cancel delay
     * @return True if the command was received successfully.\
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.\
     */
    @Throws(CommandExecutionException::class)
    fun setTimeUntilPowerOff(minutes: Int): Boolean {
        val col = JSONArray()
        col.put(minutes)
        return sendOk("delay_off", col)
    }

    /**
     * @return The time until the device is turned off in minutes
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @get:Throws(CommandExecutionException::class)
    val timeUntilPowerOff: Int
        get() = getIntProp(Prop.Names.SLEEP_TIME_LEFT)

    /**
     * @return Current all lamp props state
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @get:Throws(CommandExecutionException::class)
    val status: Map<Prop.Names, String>
        get() = getProps(Prop.Names.values())
}