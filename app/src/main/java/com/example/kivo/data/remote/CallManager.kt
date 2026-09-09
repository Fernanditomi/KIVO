package com.example.kivo.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

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
    val state: CallState = CallState.Idle
)

object CallManager {
    private val gson = Gson()

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

    private var callStartTime: Long = 0L

    fun initiateCall(
        callerId: String,
        callerName: String,
        receiverId: String,
        receiverName: String,
        type: CallType
    ) {
        val callId = "call_${System.currentTimeMillis()}"
        _currentCall.value = CallInfo(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            receiverId = receiverId,
            receiverName = receiverName,
            type = type,
            state = CallState.Outgoing
        )
        _callState.value = CallState.Outgoing

        SocketManager.emitCallOffer(callId, callerId, callerName, receiverId, type)
    }

    fun receiveCall(
        callId: String,
        callerId: String,
        callerName: String,
        receiverId: String,
        type: CallType
    ) {
        _currentCall.value = CallInfo(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            receiverId = receiverId,
            type = type,
            state = CallState.Incoming
        )
        _callState.value = CallState.Incoming
    }

    fun acceptCall() {
        val call = _currentCall.value
        _callState.value = CallState.Connecting
        _currentCall.value = call.copy(state = CallState.Connecting)

        SocketManager.emitCallAnswer(call.callId, call.callerId, call.receiverId)

        callStartTime = System.currentTimeMillis()
        _callState.value = CallState.Active
        _currentCall.value = call.copy(state = CallState.Active)
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
        _callState.value = CallState.Ended
        _currentCall.value = CallInfo()
        _isMuted.value = false
        _isSpeaker.value = false
        _callDuration.value = 0L
        callStartTime = 0L
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeaker.value = !_isSpeaker.value
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
            callStartTime = System.currentTimeMillis()
            _callState.value = CallState.Active
            _currentCall.value = _currentCall.value.copy(state = CallState.Active)
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
