package com.example.kivo.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallType { VOICE, VIDEO }

sealed class CallState {
    object Idle : CallState()
    object Outgoing : CallState()
    object Incoming : CallState()
    object Connecting : CallState()
    object Active : CallState()
    object Ended : CallState()
    data class Error(val message: String) : CallState()
}

data class CallInfo(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val type: CallType = CallType.VOICE,
    val state: CallState = CallState.Idle,
    val channelName: String = ""
)

object CallManager {
    private val _currentCall = MutableStateFlow(CallInfo())
    val currentCall: StateFlow<CallInfo> = _currentCall.asStateFlow()

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeaker = MutableStateFlow(false)
    val isSpeaker: StateFlow<Boolean> = _isSpeaker.asStateFlow()

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()

    private val _remoteUserJoined = MutableStateFlow(false)
    val remoteUserJoined: StateFlow<Boolean> = _remoteUserJoined.asStateFlow()

    private var callStartTime: Long = 0L
    private var agoraManager: AgoraManager? = null

    private fun generateChannelName(id1: String, id2: String): String {
        val sorted = listOf(id1, id2).sorted()
        return "kivo_${sorted[0]}_${sorted[1]}"
    }

    fun setAgoraManager(manager: AgoraManager) {
        agoraManager = manager
        manager.onRemoteUserJoined = { uid ->
            _remoteUserJoined.value = true
            if (_callState.value == CallState.Connecting) {
                callStartTime = System.currentTimeMillis()
                _callState.value = CallState.Active
                _currentCall.value = _currentCall.value.copy(state = CallState.Active)
            }
        }
        manager.onRemoteUserLeft = { _ ->
            endCall()
        }
        manager.onJoinChannelSuccess = { _, _ ->
            if (_callState.value == CallState.Outgoing || _callState.value == CallState.Connecting) {
                _callState.value = CallState.Connecting
                _currentCall.value = _currentCall.value.copy(state = CallState.Connecting)
            }
        }
        manager.onError = { errCode ->
            _callState.value = CallState.Error("Error: $errCode")
        }
    }

    fun initiateCall(
        callerId: String,
        callerName: String,
        receiverId: String,
        receiverName: String,
        type: CallType
    ) {
        val callId = "call_${System.currentTimeMillis()}"
        val channelName = generateChannelName(callerId, receiverId)

        _currentCall.value = CallInfo(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            receiverId = receiverId,
            receiverName = receiverName,
            type = type,
            state = CallState.Outgoing,
            channelName = channelName
        )
        _callState.value = CallState.Outgoing

        SocketManager.emitCallOffer(callId, callerId, callerName, receiverId, type)
    }

    fun joinAgoraChannel(isVideo: Boolean) {
        val call = _currentCall.value
        if (call.channelName.isEmpty()) return

        val uid = (System.currentTimeMillis() % 10000).toInt()
        if (isVideo) {
            agoraManager?.joinVideoChannel(null, call.channelName, uid)
        } else {
            agoraManager?.joinVoiceChannel(null, call.channelName, uid)
        }
    }

    fun receiveCall(
        callId: String,
        callerId: String,
        callerName: String,
        receiverId: String,
        type: CallType
    ) {
        val channelName = generateChannelName(callerId, receiverId)
        _currentCall.value = CallInfo(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            receiverId = receiverId,
            type = type,
            state = CallState.Incoming,
            channelName = channelName
        )
        _callState.value = CallState.Incoming
    }

    fun acceptCall() {
        val call = _currentCall.value
        _callState.value = CallState.Connecting
        _currentCall.value = call.copy(state = CallState.Connecting)

        val isVideo = call.type == CallType.VIDEO
        joinAgoraChannel(isVideo)

        SocketManager.emitCallAnswer(call.callId, call.callerId, call.receiverId)
    }

    fun rejectCall() {
        val call = _currentCall.value
        SocketManager.emitCallReject(call.callId, call.callerId)
        endCall()
    }

    fun endCall() {
        val call = _currentCall.value
        if (call.callId.isNotEmpty()) {
            SocketManager.emitCallEnd(call.callId, call.callerId)
        }
        agoraManager?.leaveChannel()
        _callState.value = CallState.Ended
        _currentCall.value = CallInfo()
        _isMuted.value = false
        _isSpeaker.value = false
        _callDuration.value = 0L
        _remoteUserJoined.value = false
        callStartTime = 0L
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        agoraManager?.toggleMute(_isMuted.value)
    }

    fun toggleSpeaker() {
        _isSpeaker.value = !_isSpeaker.value
    }

    fun toggleCamera() {
        agoraManager?.switchCamera()
    }

    fun updateDuration() {
        if (callStartTime > 0 && _callState.value == CallState.Active) {
            _callDuration.value = (System.currentTimeMillis() - callStartTime) / 1000
        }
    }

    fun handleCallOffer(callId: String, callerId: String, callerName: String, receiverId: String, type: String) {
        receiveCall(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            receiverId = receiverId,
            type = if (type == "video") CallType.VIDEO else CallType.VOICE
        )
    }

    fun handleCallAnswer(callId: String) {
        if (_currentCall.value.callId == callId) {
            val isVideo = _currentCall.value.type == CallType.VIDEO
            joinAgoraChannel(isVideo)
        }
    }

    fun handleCallReject(callId: String) {
        if (_currentCall.value.callId == callId) {
            endCall()
        }
    }

    fun handleCallEnd(callId: String) {
        if (_currentCall.value.callId == callId) {
            endCall()
        }
    }
}
