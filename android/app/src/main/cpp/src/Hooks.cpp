//
// Created by SyXhOwN on 17/01/2025.
//

#include "main.h"

#include "vendor/patch/patch.h"
#include "vendor/nvidia/nv_event/nv_event.h"

int lastNvEvent;

int32_t(*NVEventGetNextEvent_hooked)(NVEvent* ev, int waitMSecs);
int32_t NVEventGetNextEvent_hook(NVEvent* ev, int waitMSecs)
{
    if(!ev)
        return 0;

    int32_t ret = NVEventGetNextEvent_hooked(ev, waitMSecs);

    lastNvEvent =  ev->m_type;

    NVEvent event;
    if(NVEventGetNextEvent(&event))
    {
        int type = event.m_data.m_multi.m_action & NV_MULTITOUCH_ACTION_MASK;
        int num = (event.m_data.m_multi.m_action & NV_MULTITOUCH_POINTER_MASK) >> NV_MULTITOUCH_POINTER_SHIFT;

        int x1 = event.m_data.m_multi.m_x1;
        int y1 = event.m_data.m_multi.m_y1;

        int x2 = event.m_data.m_multi.m_x2;
        int y2 = event.m_data.m_multi.m_y2;

        int x3 = event.m_data.m_multi.m_x3;
        int y3 = event.m_data.m_multi.m_y3;

        if (type == NV_MULTITOUCH_CANCEL)
        {
            type = NV_MULTITOUCH_UP;
        }

        if ((x1 || y1) || num == 0)
        {
            if (num == 0 && type != NV_MULTITOUCH_MOVE)
            {
                ((void(*)(int, int, int posX, int posY))(g_libGTASA + (VER_x32 ? 0x00269740  + 1 : 0x31EC0C)))(type, 0, x1, y1); // AND_TouchEvent
            }
            else
            {
                ((void(*)(int, int, int posX, int posY))(g_libGTASA +  (VER_x32 ? 0x00269740  + 1 : 0x31EC0C)))(NV_MULTITOUCH_MOVE, 0, x1, y1); // AND_TouchEvent
            }
        }

        if ((x2 || y2) || num == 1)
        {
            if (num == 1 && type != NV_MULTITOUCH_MOVE)
            {
                ((void(*)(int, int, int posX, int posY))(g_libGTASA +  (VER_x32 ? 0x00269740  + 1 : 0x31EC0C)))(type, 1, x2, y2); // AND_TouchEvent
            }
            else
            {
                ((void(*)(int, int, int posX, int posY))(g_libGTASA +  (VER_x32 ? 0x00269740  + 1 : 0x31EC0C)))(NV_MULTITOUCH_MOVE, 1, x2, y2); // AND_TouchEvent
            }
        }
        if ((x3 || y3) || num == 2)
        {
            if (num == 2 && type != NV_MULTITOUCH_MOVE)
            {
                ((void(*)(int, int, int posX, int posY))(g_libGTASA +  (VER_x32 ? 0x00269740  + 1 : 0x31EC0C)))(type, 2, x3, y3); // AND_TouchEvent
            }
            else
            {
                ((void(*)(int, int, int posX, int posY))(g_libGTASA +  (VER_x32 ? 0x00269740  + 1 : 0x31EC0C)))(NV_MULTITOUCH_MOVE, 2, x3, y3); // AND_TouchEvent
            }
        }
    }
    return ret;
}

void InjectHooks()
{
    // Implement 3 touches
    Patch::InlineHook("_Z19NVEventGetNextEventP7NVEventi", NVEventGetNextEvent_hook, &NVEventGetNextEvent_hooked);
}
