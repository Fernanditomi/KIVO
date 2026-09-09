package com.example.kivo.data.remote

import android.content.Context
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import android.view.SurfaceView

class AgoraManager(private val context: Context) {
    private var rtcEngine: RtcEngine? = null

    var onRemoteUserJoined: ((Int) -> Unit)? = null
    var onRemoteUserLeft: ((Int) -> Unit)? = null
    var onJoinChannelSuccess: ((String, Int) -> Unit)? = null
    var onLeaveChannel: (() -> Unit)? = null
    var onError: ((Int) -> Unit)? = null

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            channel?.let { onJoinChannelSuccess?.invoke(it, uid) }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            onRemoteUserJoined?.invoke(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            onRemoteUserLeft?.invoke(uid)
        }

        override fun onLeaveChannel(stats: IRtcEngineEventHandler.RtcStats?) {
            onLeaveChannel?.invoke()
        }

        override fun onError(errCode: Int) {
            onError?.invoke(errCode)
        }
    }

    fun initialize() {
        if (rtcEngine != null) return
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = com.example.kivo.BuildConfig.AGORA_APP_ID
                mEventHandler = eventHandler
            }
            rtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun joinVoiceChannel(token: String?, channelName: String, uid: Int) {
        rtcEngine?.apply {
            enableAudio()
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                publishCameraTrack = false
                autoSubscribeAudio = true
                autoSubscribeVideo = false
            }
            joinChannel(token, channelName, uid, options)
        }
    }

    fun joinVideoChannel(token: String?, channelName: String, uid: Int) {
        rtcEngine?.apply {
            enableVideo()
            enableAudio()
            val config = VideoEncoderConfiguration(
                VideoEncoderConfiguration.VideoDimensions(640, 360),
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            )
            setVideoEncoderConfiguration(config)
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                publishCameraTrack = true
                autoSubscribeAudio = true
                autoSubscribeVideo = true
            }
            joinChannel(token, channelName, uid, options)
        }
    }

    fun setupLocalVideo(container: android.widget.FrameLayout) {
        rtcEngine?.let { engine ->
            container.removeAllViews()
            val surfaceView = SurfaceView(context).apply {
                setZOrderMediaOverlay(true)
            }
            container.addView(surfaceView)
            engine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
            engine.startPreview()
        }
    }

    fun setupRemoteVideo(uid: Int, container: android.widget.FrameLayout) {
        rtcEngine?.let { engine ->
            container.removeAllViews()
            val surfaceView = SurfaceView(context)
            container.addView(surfaceView)
            engine.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        }
    }

    fun toggleMute(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }

    fun toggleCamera(enabled: Boolean) {
        rtcEngine?.apply {
            if (enabled) {
                enableVideo()
                startPreview()
            } else {
                stopPreview()
            }
        }
    }

    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    fun leaveChannel() {
        rtcEngine?.apply {
            stopPreview()
            leaveChannel()
        }
    }

    fun destroy() {
        leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
