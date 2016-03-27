#pragma once

#define USE_DATALOGGING

#include <pebble.h>

#ifdef USE_DATALOGGING
#include "../modules/commlog.h"
#define SEND_DATA commlog_send_data
#define IS_BUSY commlog_is_busy
#else
#include "../modules/comm.h"
#define SEND_DATA comm_send_data
#define IS_BUSY comm_is_busy
#endif

void main_window_push();