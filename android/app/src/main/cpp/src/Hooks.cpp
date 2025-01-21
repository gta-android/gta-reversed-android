//
// Created by SyXhOwN on 17/01/2025.
//

#include "reGTA.h"
#include "ReversibleHooks/ReversibleHooks.h"

// TODO: In the future this will be moved
int GetInputType() {
    return 0;
}

void InstallHooks()
{
    // Fix mouse/keyboard bad
    ReversibleHooks::Redirect("_ZN4CHID12GetInputTypeEv", &GetInputType);
}
