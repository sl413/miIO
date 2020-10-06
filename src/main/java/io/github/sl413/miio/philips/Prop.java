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

package io.github.sl413.miio.philips;

import de.sg_o.app.miio.base.CommandExecutionException;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * This class handles the properties of a light.
 */
public class Prop {
    private final Names[] props;

    /**
     * Create a new light properties object.
     *
     * @param props The properties to retrieve.
     */
    public Prop(Names[] props) {
        if (props == null) props = new Names[0];
        this.props = props;
    }

    /**
     * @return The properties to retrieve.
     */
    public Names[] getProps() {
        return props;
    }

    /**
     * @return A JSONArray that is sent to the device to retrieve the property values.
     * * @throws CommandExecutionException  When the properties array was invalid.
     */
    public JSONArray getRequestArray() throws CommandExecutionException {
        if (props.length < 1) throw new CommandExecutionException(CommandExecutionException.Error.INVALID_PARAMETERS);
        JSONArray ret = new JSONArray();
        for (Names p : props) {
            ret.put(p.toString());
        }
        return ret;
    }

    /**
     * Parse the response from the device and return a map containing the property names and their values.
     *
     * @param response The response from the device.
     * @return A map containing the property names as the keys and their values.
     */
    public Map<Names, String> parseResponse(JSONArray response) throws CommandExecutionException {
        if (response == null) throw new CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE);
        if (response.length() != props.length)
            throw new CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE);
        Map<Names, String> ret = new HashMap<>(props.length);
        for (int i = 0; i < props.length; i++) {
            ret.put(props[i], response.optString(i, ""));
        }
        return ret;
    }

    /**
     * The retrievable properties. Not all lights implement every property.
     */
    public enum Names {

        /**
         * The power state of the lamp. "on" or "off"
         */
        POWER("power"),
        /**
         * The power state of the ambient lamp. "on" or "off"
         */
        AMBIENT_LIGHT_POWER("ambstatus"),
        /**
         * The brightness of the lamp. 1 to 100 inclusive
         */
        BRIGHTNESS("bright"),
        /**
         * The brightness of the ambient lamp. 1 to 100 inclusive
         */
        AMBIENT_LIGHT_BRIGHTNESS("ambvalue"),
        /**
         * The eyecare mode
         */
        EYECARE_MODE("eyecare"),
        /**
         * The eyecare scene number. 1 to 4 inclusive: study, reading, phone, default
         */
        SCENE_MODE("scene_num"),
        /**
         * Eye fatigue reminder
         */
        EYE_FATIGUE_REMINDER("notifystatus"),
        /**
         * Smart night light mode
         */
        NIGHT_LIGHT("bls"),
        /**
         * The remaining time of the sleep timer in minutes. 0 to 60 inclusive. 0 - mode inactive
         */
        SLEEP_TIME_LEFT("dvalue");

        private final String name;

        Names(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
