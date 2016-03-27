#pragma once

#include <pebble.h>

//extern static uint s_commlog_data_sent;

void commlog_init();

void commlog_start();

bool commlog_is_busy();

bool commlog_send_data(AccelData *data, uint32_t num_samples);

void commlog_stop();