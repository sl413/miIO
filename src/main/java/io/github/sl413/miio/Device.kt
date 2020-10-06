/*
 * Copyright (c) 2018 Joerg Bayer
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
package io.github.sl413.miio

import de.sg_o.app.miio.base.CommandExecutionException
import de.sg_o.app.miio.base.Token
import de.sg_o.app.miio.base.messages.Command
import de.sg_o.app.miio.base.messages.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.*
import java.util.*

open class Device(ip: InetAddress?, token: Token?, acceptableModels: Array<String>?, timeout: Int, retries: Int) : Serializable {
    /**
     * @return The IP address of the device.
     */
    var ip: InetAddress?
        private set

    /**
     * @return The token of the device.
     */
    var token: Token?
        private set

    /**
     * @return The number of retries when discovering a device or sending a command.
     */
    val retries: Int

    /**
     * @return The model strings this device must have.
     */
    val acceptableModels: Array<String>?

    @Transient
    private var socket: DatagramSocket? = null
    private var deviceID = -1
    private var timeStamp = -1
    private var methodID: Long = 0

    /**
     * @return The timeout for the communication to fail.
     */
    val timeout: Int
        get() = try {
            socket!!.soTimeout
        } catch (e: Exception) {
            0
        }

    /**
     * Try to connect to a device or discover it.
     * @param broadcast The InetAddress to broadcast to if no ip was given
     * @return True if a device was found
     */
    private fun hello(broadcast: InetAddress): Boolean {
        if (socket == null) return false
        val hello = Command()
        val helloMsg: ByteArray = hello.create()
        var packet: DatagramPacket
        if (ip == null) {
            if (acceptableModels == null) return false
            packet = DatagramPacket(helloMsg, helloMsg.size, broadcast, PORT)
        } else {
            packet = DatagramPacket(helloMsg, helloMsg.size, ip, PORT)
        }
        try {
            socket!!.send(packet)
        } catch (e: IOException) {
            return false
        }
        packet = DatagramPacket(rcv, rcv.size)
        try {
            socket!!.receive(packet)
        } catch (e: IOException) {
            return false
        }
        if (ip == null) {
            ip = packet.address
        }
        var worker = ByteArray(2)
        System.arraycopy(rcv, 2, worker, 0, 2)
        val length = de.sg_o.app.miio.util.ByteArray.fromBytes(worker).toInt()
        worker = ByteArray(length)
        System.arraycopy(rcv, 0, worker, 0, length)
        val response: Response
        response = try {
            Response(worker, null)
        } catch (e: CommandExecutionException) {
            return false
        }
        if (token == null) {
            token = if (!(response.token == Token("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16) || response.token == Token("00000000000000000000000000000000", 16))) {
                response.token
            } else {
                return false
            }
        }
        if (!(response.deviceID == -1 || response.timeStamp == -1)) {
            deviceID = response.deviceID
            timeStamp = response.timeStamp
            methodID = (timeStamp and 8191.toLong().toInt()).toLong() // Possible collision about every 2 hours > acceptable
            if (acceptableModels != null) {
                var modelOk = false
                for (s in acceptableModels) {
                    try {
                        if (s == model()) modelOk = true
                    } catch (ignored: CommandExecutionException) {
                    }
                }
                return modelOk
            }
            return true
        }
        return false
    }

    /**
     * Connect to a device and send a Hello message. If no IP has been specified, this will try do discover a device on the network.
     * @return True if the device has been successfully acquired.
     */
    fun discover(): Boolean {
        var helloResponse = false
        for (helloRetries in retries downTo 0) {
            val broadcast = listAllBroadcastAddresses() ?: return false
            for (i in broadcast) {
                if (hello(i)) {
                    helloResponse = true
                    break
                }
            }
            if (helloResponse) break
        }
        return helloResponse
    }

    /**
     * Send a command to a device. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @param params The command to execute on the device. Must be a JSONArray or JSONObject.
     * @return The response from the device.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun send(method: String?, params: Any?): Response? {
        if (deviceID == -1 || timeStamp == -1 || token == null || ip == null) {
            if (!discover()) throw CommandExecutionException(CommandExecutionException.Error.DEVICE_NOT_FOUND)
        }
        if (methodID >= 10000) methodID = 1
        if (ip == null || token == null) throw CommandExecutionException(CommandExecutionException.Error.IP_OR_TOKEN_UNKNOWN)
        if (socket == null) return null
        timeStamp++
        val msg = Command(token, deviceID, timeStamp, methodID, method, params)
        methodID++
        var retriesLeft = retries
        while (true) {
            return try {
                parseResponse(send(msg.create()))
            } catch (e: CommandExecutionException) {
                if (retriesLeft > 0) {
                    retriesLeft--
                    continue
                }
                throw e
            }
        }
    }

    /**
     * Send an arbitrary string as payload to the device.
     * @param payload The string to send.
     * @return The response of the device as an unparsed string.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun send(payload: String?): String? {
        if (payload == null) throw CommandExecutionException(CommandExecutionException.Error.INVALID_PARAMETERS)
        if (deviceID == -1 || timeStamp == -1 || token == null || ip == null) {
            if (!discover()) throw CommandExecutionException(CommandExecutionException.Error.DEVICE_NOT_FOUND)
        }
        if (methodID >= 10000) methodID = 1
        if (ip == null || token == null) throw CommandExecutionException(CommandExecutionException.Error.IP_OR_TOKEN_UNKNOWN)
        if (socket == null) return null
        timeStamp++
        val msg = Command(token, deviceID, timeStamp, methodID, "", null)
        methodID++
        var retriesLeft = retries
        while (true) {
            try {
                val resp: ByteArray? = send(msg.create(payload))
                if (!Response.testMessage(resp, token)) throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
                if (resp != null) {
                    if (resp.size > 0x20) {
                        val pl = ByteArray(resp.size - 0x20)
                        System.arraycopy(resp, 0x20, pl, 0, pl.size)
                        return Response.decryptPayload(pl, token)
                                ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
                    }
                }
            } catch (e: CommandExecutionException) {
                if (retriesLeft > 0) {
                    retriesLeft--
                    continue
                }
                throw e
            }
        }
    }

    @Throws(CommandExecutionException::class)
    private fun send(binMsg: ByteArray): ByteArray? {
        var packet = DatagramPacket(binMsg, binMsg.size, ip, PORT)
        try {
            socket!!.send(packet)
        } catch (to: SocketTimeoutException) {
            throw CommandExecutionException(CommandExecutionException.Error.TIMEOUT)
        } catch (e: IOException) {
            return null
        }
        packet = DatagramPacket(rcv, rcv.size)
        try {
            socket!!.receive(packet)
        } catch (to: SocketTimeoutException) {
            throw CommandExecutionException(CommandExecutionException.Error.TIMEOUT)
        } catch (e: IOException) {
            return null
        }
        var worker = ByteArray(2)
        System.arraycopy(rcv, 2, worker, 0, 2)
        val length = de.sg_o.app.miio.util.ByteArray.fromBytes(worker).toInt()
        worker = ByteArray(length)
        System.arraycopy(rcv, 0, worker, 0, length)
        return worker
    }

    @Throws(CommandExecutionException::class)
    private fun parseResponse(rawData: ByteArray?): Response {
        val response = Response(rawData, token)
        if (!response.isValid) {
            throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        }
        if (response.payloadID != methodID - 1) {
            throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        }
        if (!(response.deviceID == -1 || response.timeStamp == -1)) {
            if (response.params == null) {
                throw CommandExecutionException(CommandExecutionException.Error.EMPTY_RESPONSE)
            }
            if (response.params.javaClass == String::class.java) {
                if (response.params == "unknown_method") throw CommandExecutionException(CommandExecutionException.Error.UNKNOWN_METHOD)
            }
            return response
        }
        throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
    }

    private fun listAllBroadcastAddresses(): List<InetAddress>? {
        val broadcastList: MutableList<InetAddress> = ArrayList()
        val interfaces: Enumeration<NetworkInterface>
        interfaces = try {
            NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            return null
        }
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            try {
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }
            } catch (e: SocketException) {
                continue
            }
            for (address in networkInterface.interfaceAddresses) {
                if (address == null) continue
                val broadcast = address.broadcast
                if (broadcast != null) {
                    broadcastList.add(broadcast)
                }
            }
        }
        return broadcastList
    }
    /**
     * Send a command to a device. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @param params The command to execute on the device. Must be a JSONArray or JSONObject.
     * @return The response from the device as a JSONObject.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    /**
     * Send a command to a device without parameters. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @return The response from the device as a JSONObject.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @JvmOverloads
    @Throws(CommandExecutionException::class)
    fun sendToObject(method: String?, params: Any? = null): JSONObject {
        val resp = send(method, params)
                ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        if (resp.params == null) throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        if (resp.params.javaClass != JSONObject::class.java) throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        return resp.params as JSONObject
    }
    /**
     * Send a command to a device. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @param params The command to execute on the device. Must be a JSONArray or JSONObject.
     * @return The response from the device as a JSONArray.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    /**
     * Send a command to a device without parameters. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @return The response from the device as a JSONArray.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @JvmOverloads
    @Throws(CommandExecutionException::class)
    fun sendToArray(method: String?, params: Any? = null): JSONArray {
        val resp = send(method, params)
                ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        if (resp.params == null) throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        if (resp.params.javaClass != JSONArray::class.java) throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        return resp.params as JSONArray
    }
    /**
     * Send a command to a device. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @param params The command to execute on the device. Must be a JSONArray or JSONObject.
     * @return True if a ok was received from the device.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    /**
     * Send a command to a device without parameters. If no IP has been specified, this will try do discover a device on the network.
     * @param method The method to execute on the device.
     * @return True if a ok was received from the device.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @JvmOverloads
    @Throws(CommandExecutionException::class)
    fun sendOk(method: String?, params: Any? = null): Boolean {
        return sendToArray(method, params).optString(0).toLowerCase() == "ok"
    }

    /**
     * Get the device info from the device
     * @return The device info as a JSONObject
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun info(): JSONObject {
        return sendToObject("miIO.info")
    }

    /**
     * Command the device to update
     * @param url The URL to update from
     * @param md5 The MD5 Checksum for the update
     * @return True if the command has been received. This does not mean that the update was successful.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun update(url: String?, md5: String?): Boolean {
        if (url == null || md5 == null) throw CommandExecutionException(CommandExecutionException.Error.INVALID_PARAMETERS)
        if (md5.length != 32) throw CommandExecutionException(CommandExecutionException.Error.INVALID_PARAMETERS)
        val params = JSONObject()
        params.put("mode", "normal")
        params.put("install", "1")
        params.put("app_url", url)
        params.put("file_md5", md5)
        params.put("proc", "dnld install")
        return sendOk("miIO.ota", params)
    }

    /**
     * Request the update progress as a percentage value from 0 to 100
     * @return The current progress.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun updateProgress(): Int {
        val resp = sendToArray("miIO.get_ota_progress").optInt(0, -1)
        if (resp < 0 || resp > 100) throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        return resp
    }

    /**
     * Request the update status.
     * @return The update status.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun updateStatus(): String {
        return sendToArray("miIO.get_ota_state").optString(0, null)
                ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
    }

    /**
     * Set the deviced network connection up.
     * @param ssid The SSID to device should connect to
     * @param password The password for that connection
     * @return True if the command was received successfully. This does not mean that the connection has been correctly established.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @JvmOverloads
    @Throws(CommandExecutionException::class)
    fun configureRouter(ssid: String?, password: String?, uid: Int = 0): Boolean {
        if (ssid == null || password == null) throw CommandExecutionException(CommandExecutionException.Error.INVALID_PARAMETERS)
        val params = JSONObject()
        params.put("ssid", ssid)
        params.put("passwd", password)
        params.put("uid", uid)
        return sendOk("miIO.config_router", params)
    }

    /**
     * Get the devices model id.
     * @return The devices model id.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun model(): String {
        val `in` = info() ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        return `in`.optString("model")
    }

    /**
     * Get the devices firmware version.
     * @return The devices current firmware version.
     * @throws CommandExecutionException When there has been a error during the communication or the response was invalid.
     */
    @Throws(CommandExecutionException::class)
    fun firmware(): String {
        val `in` = info() ?: throw CommandExecutionException(CommandExecutionException.Error.INVALID_RESPONSE)
        return `in`.optString("fw_ver")
    }

    @Throws(IOException::class)
    private fun writeObject(out: ObjectOutputStream) {
        out.defaultWriteObject()
        out.writeInt(socket!!.soTimeout)
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`in`: ObjectInputStream) {
        `in`.defaultReadObject()
        socket = DatagramSocket()
        socket!!.soTimeout = `in`.readInt()
    }

    companion object {
        private const val PORT = 54321
        private const val serialVersionUID = -924264471464948810L
        private val rcv: ByteArray = ByteArray(65507)
    }

    /**
     * Baseclass for all miIO devices.
     * @param ip The IP address of the device to connect to. If the address is null the first device that is an acceptableModel will be chosen.
     * @param token The token for that device. If the token is null the token will be extracted from unprovisioned devices.
     * @param acceptableModels An array of acceptable devices to connect to.
     * @param timeout The timeout for the communication
     * @param retries The number of retries after a failed communication
     */
    init {
        var timeout = timeout
        var retries = retries
        this.ip = ip
        this.token = token
        this.acceptableModels = acceptableModels
        if (timeout < 1) timeout = 1000
        if (retries < 0) retries = 0
        this.retries = retries
        try {
            socket = DatagramSocket()
            socket!!.soTimeout = timeout
        } catch (ignored: SocketException) {
        }
    }
}