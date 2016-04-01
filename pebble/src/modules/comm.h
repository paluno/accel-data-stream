#pragma once

#include <pebble.h>

#define COMM_SIZE_INBOX          256
#define COMM_SIZE_OUTBOX         COMM_SIZE_INBOX
#define COMM_NUM_PACKET_ELEMENTS 3

void comm_init();

typedef void(* ActionTrigger)();

void registerStartCallback(ActionTrigger newStartCallback);
void registerStopCallback(ActionTrigger newStopCallback);