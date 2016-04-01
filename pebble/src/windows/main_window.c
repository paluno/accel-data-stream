#include "main_window.h"

#define SAMPLES_PER_UPDATE 5

static Window *s_window;

static TextLayer *s_text_layer_hand;
static TextLayer *s_text_layer_info;
static TextLayer *s_text_layer_data;
static TextLayer *s_text_layer_battery;

static bool s_sending;
static int s_send_counter;

static int s_hand_sequence = 0;  // how many of the sequence of keys to change the hand have been pressed?
static bool s_f_left_hand;

static void update_data_sent() {
  static char data_sent_text[9];
  uint data_sent_current = get_commlog_data_sent() / 1000;
  static uint data_sent_last = -1;
  if (data_sent_last == data_sent_current)
    return;  // info is up-to-date enough
  data_sent_last = data_sent_current;

  snprintf(data_sent_text, sizeof(data_sent_text), "%d KB", data_sent_current);
  text_layer_set_text(s_text_layer_data, data_sent_text);
}

static void accel_data_handler(AccelData *data, uint32_t num_samples) {
  if(commlog_send_data(data, num_samples)) {
    s_send_counter++;
    update_data_sent();    
  }
  else {
    text_layer_set_text(s_text_layer_info, "Transmission error!");
  }
}

static void battery_handler(BatteryChargeState charge_state) {
  static char battery_text[9];

  if (charge_state.is_charging) {
    snprintf(battery_text, sizeof(battery_text), "charging");
  } else {
    snprintf(battery_text, sizeof(battery_text), "%d %%", charge_state.charge_percent);
  }
  text_layer_set_text(s_text_layer_battery, battery_text);
}

static void begin_sending_data() {
  if(s_sending)
    return;  // has already begun

  // Begin sending data
  s_sending = true;

  accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);
  accel_data_service_subscribe(SAMPLES_PER_UPDATE, accel_data_handler);

  text_layer_set_text(s_text_layer_info, "Started");

  commlog_start();
}

static void stop_sending_data() {
  if(!s_sending)
    return;  // does not send currently

  // Stop sending data
  s_sending = false;

  accel_data_service_unsubscribe();

  text_layer_set_text(s_text_layer_info, "Stopped");

  commlog_stop();
}

static void select_click_handler(ClickRecognizerRef recognizer, void *context) {
  s_hand_sequence = 0;
  
  if(!s_sending) {
    begin_sending_data();
  } else {
    stop_sending_data();
  }
}

static void change_hand() {
  s_hand_sequence = 0;
  s_f_left_hand = !s_f_left_hand;
  if (s_f_left_hand) {
    text_layer_set_text(s_text_layer_hand, "L");
  }
  else {
    text_layer_set_text(s_text_layer_hand, "R");
  }
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  // correct sequence: up down up
  if (s_hand_sequence == 0)
    ++s_hand_sequence;
  else if (s_hand_sequence == 2)
    change_hand();
  else
    s_hand_sequence = 0;
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  // correct sequence: up down up
  if (s_hand_sequence == 1)
    ++s_hand_sequence;
  else
    s_hand_sequence = 0;
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_SELECT, select_click_handler);
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

/* Window Layout
 *********************
 *  ||     |  -OOOO= *
 *  ||     |  -OOOO= *
 *  ||===  |    I    *
 *-------------------*
 *   Current Status  *
 *-------------------*
 * 55k     | 60 %    *
 *********************/

static void window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  s_text_layer_hand = text_layer_create(GRect(bounds.origin.x, bounds.origin.y, bounds.size.w/2, bounds.size.h/2));
  text_layer_set_text(s_text_layer_hand, "L");
  text_layer_set_text_alignment(s_text_layer_hand, GTextAlignmentCenter);
  text_layer_set_font(s_text_layer_hand, fonts_get_system_font(FONT_KEY_BITHAM_42_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(s_text_layer_hand));
  
  s_text_layer_info = text_layer_create(GRect(bounds.origin.x, bounds.origin.y + bounds.size.h/2, bounds.size.w, bounds.size.h/4));
  text_layer_set_text(s_text_layer_info, "Press Select to begin");
  text_layer_set_text_alignment(s_text_layer_info, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(s_text_layer_info));
  
  s_text_layer_data = text_layer_create(GRect(bounds.origin.x, bounds.origin.y + bounds.size.h*3/4, bounds.size.w/2, bounds.size.h/4));
  text_layer_set_text(s_text_layer_data, "Data Buffer");
  text_layer_set_text_alignment(s_text_layer_data, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(s_text_layer_data));
  
  s_text_layer_battery = text_layer_create(GRect(bounds.origin.x + bounds.size.w/2, bounds.origin.y + bounds.size.h*3/4, bounds.size.w/2, bounds.size.h/4));
  text_layer_set_text(s_text_layer_battery, "Battery");
  text_layer_set_text_alignment(s_text_layer_battery, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(s_text_layer_battery));
  
  change_hand();
  update_data_sent();
  
  battery_state_service_subscribe(battery_handler);
  battery_handler(battery_state_service_peek()); 
  
  registerStartCallback(begin_sending_data);
  registerStopCallback(stop_sending_data);
}

static void window_unload(Window *window) {
  registerStartCallback(NULL);
  registerStopCallback(NULL);
  
  battery_state_service_unsubscribe();
  
  text_layer_destroy(s_text_layer_hand);
  text_layer_destroy(s_text_layer_info);
  text_layer_destroy(s_text_layer_data);
  text_layer_destroy(s_text_layer_battery);

  window_destroy(s_window);
}

void main_window_push() {
  s_sending = false;
  s_send_counter = 0;

  s_window = window_create();
  window_set_click_config_provider(s_window, click_config_provider);
  window_set_window_handlers(s_window, (WindowHandlers) {
    .load = window_load,
    .unload = window_unload,
  });
  window_stack_push(s_window, true);
}
