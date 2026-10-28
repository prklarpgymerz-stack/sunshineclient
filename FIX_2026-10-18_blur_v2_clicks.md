# Motion blur v2 + click indicator fix (UNTESTED in game)

MotionBlurRenderer.java - rewritten as camera-speed directional blur (no more ghost trails).
  Blur streak follows how fast you turn (deg/s, frame-rate independent), 24-sample tent filter,
  smoothed velocity, zero cost and perfectly sharp when the camera is still. Strength 1-100% = shutter time.
  Keeps all GL state save/restore + error checks from the previous fix.
MouseClickCpsMixin.java - now forwards press AND release events: SunshineClient.recordMouseEvent(button, action).
SunshineClient.java     - LMB/RMB keystroke boxes use real press/release events and stay lit >= 90 ms,
  so quick double clicks are no longer missed by the 20 Hz tick polling. CPS counting still uses every press.
