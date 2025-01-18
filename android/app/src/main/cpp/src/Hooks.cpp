//
// Created by SyXhOwN on 17/01/2025.
//

#include "main.h"

#include "vendor/patch/patch.h"

int GetInputType() {
    return 0;
}

void InstallHooks()
{
    // Fix mouse/keyboard bad
    Patch::Redirect("_ZN4CHID12GetInputTypeEv", &GetInputType);
}
