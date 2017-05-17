
#include <android/input.h>

int customToolType(float x, float y) {
    if(x <= 200.00 && y <= 200.00) {
        return AMOTION_EVENT_TOOL_TYPE_FINGER_TOP_LEFT_CORNER;
    } else {
        return AMOTION_EVENT_TOOL_TYPE_FINGER;
    }
}