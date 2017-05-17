# AOSPModification

Modified Android Framework to listen for special double-tap events and trigger a system screenshot. 

Implemented solution supports switching the feature on/off in settings, vibration during the sucsessfull double tap, toast if taps are not in quick succession and feature idle when the device is locked or displays the lock/PIN screen.

![screenshot](https://cloud.githubusercontent.com/assets/6971421/26136337/3c6d2c78-3a70-11e7-963e-154a6821920b.jpg)

## Solution
### InputFlinger and Native classification library
Implemented a native classification library: classificationlib with `int customToolType(float x, float y)` method, which receives (x, y) screen coordinates from `inputflinger` service and returns a new toolType: `AMOTION_EVENT_TOOL_TYPE_FINGER_TOP_LEFT_CORNER` if both x and y coordinates are less than 200. Otherwise a standard toolType: `TOOL_TYPE_FINGER` will be returned. The classification library is added as a dependency to Android.bp file in `inputflinger` service.

### Custom PointerEventListener
Implemented a `TopLeftCornerPointerEventListener` class that gets an `onPointerEvent` callback.
The new gesture listener would get a callback on double tap. If a double tap occurred and the new custom tool type was set, it triggers a system screenshot.

### Rejecting unwonted touches
The `ViewRootImpl` `processPointerEvent()` method was modified so that the special toolType events are not affecting the normal Android UI.
If the feature is turned on in Settings and if passed `MotionEvent` has the new toolType it blocks dispatching this event to child views. Otherwise the touch event is processed in usual way.

### Settings
The `gesture_settings.xml` was modified to add new gesture: `gesture_double_tap_top_left_corner`.
And the appropriate strings were added to the res/values/strings.xml file. The src/com/android/settings/gestures/GestureSettings.java was modified to support `screenshotEnabled` setting. And the appropriate checks were added into `TopLeftCornerPointerEventListener` and `ViewRootImpl`. With those changes Settings app menu will show one more switch settings in gestures menu. 

### Don’t take screenshot when the device is locked or displays the lock/PIN screen
The feature support implemented in`TopLeftCornerPointerEventListener`. The appropriate check of `KeyguardManager` is added in `onDoubleTap()` method.  

### Vibration 
Added a small vibration to `TopLeftCornerPointerEventListener` in `onDoubleTap()` method for all double tap events including not only our toolType. 

### Taps that are not in quick succession
If user double taps, but taps are not in quick succession, a Toast will be displayed.
Functionality added to the overrided `onSingleTapConfirmed()` method.  (Only if the feature is turned on in Settings).
