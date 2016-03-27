#include <pebble.h>
#include "modules/commlog.h"

static uint s_commlog_data_sent;

static DataLoggingSessionRef s_logging_session;

static uint s_session_tag;

void commlog_init() {
  s_commlog_data_sent = 0;
  s_session_tag = 0x1402;
}

bool commlog_is_busy() {
  return false;
}

void commlog_start() {
  s_logging_session = data_logging_create(
    s_session_tag++,            // the unique tag of the logging session
    DATA_LOGGING_BYTE_ARRAY,  // we are logging byte arrays, each of which is
    sizeof(AccelData),        // this length (2+2+2+8=14 bytes) long
    false                     // we do not resume a previous session. They would be overridden.
  ); 
}

bool commlog_send_data(AccelData *data, uint32_t num_samples) {
  s_commlog_data_sent += num_samples * sizeof(AccelData);
  for (uint i=0;i<num_samples;++i) {
    DataLoggingResult res = data_logging_log(s_logging_session, &data[i], 1);
    if (DATA_LOGGING_SUCCESS != res) {
      APP_LOG(APP_LOG_LEVEL_ERROR, "There was a problem logging accel data: %d", res);
      return false;
    }
  }
  return true;
}

void commlog_stop() {
  data_logging_finish(s_logging_session);
}