#include <pebble.h>

#include "windows/main_window.h"

static void init() {
#ifdef USE_DATALOGGING
  commlog_init();
#else
  comm_init();
#endif
  main_window_push();
}

static void deinit() { }

int main() {
  init();
  app_event_loop();
  deinit();
}
